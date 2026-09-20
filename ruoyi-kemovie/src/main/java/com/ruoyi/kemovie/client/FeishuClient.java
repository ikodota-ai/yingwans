package com.ruoyi.kemovie.client;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * 飞书开放接口客户端（自建应用 tenant_access_token 模式）。
 * 用于把影片下载资源自动生成为飞书文档：创建/清空重写/设置链接分享。
 *
 * @author kemovie
 */
@Component
public class FeishuClient
{
    private static final Logger log = LoggerFactory.getLogger(FeishuClient.class);

    private final KemovieProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile HttpClient httpClient;
    private volatile String tenantToken;
    private volatile long tenantTokenExpireAt;

    public FeishuClient(KemovieProperties props)
    {
        this.props = props;
    }

    public boolean configured()
    {
        return StringUtils.isNotEmpty(props.getFeishu().getAppId())
                && StringUtils.isNotEmpty(props.getFeishu().getAppSecret());
    }

    /** 连接测试：验证凭证能否换取 tenant_access_token，正常返回 null，否则返回原因 */
    public String testConnection()
    {
        if (!configured())
        {
            return "未配置 kemovie.feishu.app-id / app-secret（环境变量 FEISHU_APP_ID / FEISHU_APP_SECRET）";
        }
        try
        {
            tenantAccessToken();
            return null;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /** 创建文档，返回 document_id */
    public String createDocument(String title)
    {
        ObjectNode body = mapper.createObjectNode();
        body.put("title", title);
        JsonNode data = call("POST", "/open-apis/docx/v1/documents", body);
        return data.path("document").path("document_id").asText(null);
    }

    /** 通过 drive meta 接口取文档完整访问链接 */
    public String fetchDocUrl(String documentId)
    {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode docs = body.putArray("request_docs");
        ObjectNode d = docs.addObject();
        d.put("doc_token", documentId);
        d.put("doc_type", "docx");
        JsonNode data = call("POST", "/open-apis/drive/v1/metas/batch_query", body);
        JsonNode metas = data.path("metas");
        if (metas.isArray() && metas.size() > 0)
        {
            return metas.get(0).path("url").asText(null);
        }
        return null;
    }

    /** 列出根块直属子块（最多500个） */
    public List<String> listRootChildrenIds(String documentId)
    {
        List<String> ids = new ArrayList<>();
        String pageToken = null;
        do
        {
            String path = "/open-apis/docx/v1/documents/" + documentId + "/blocks/" + documentId
                    + "/children?page_size=500" + (pageToken == null ? "" : "&page_token=" + pageToken);
            JsonNode data = call("GET", path, null);
            JsonNode items = data.path("items");
            if (items.isArray())
            {
                for (JsonNode b : items) { ids.add(b.path("block_id").asText()); }
            }
            pageToken = data.path("has_more").asBoolean(false) ? data.path("page_token").asText(null) : null;
        } while (pageToken != null);
        return ids;
    }

    /** 删除根块下 [0, count) 的子块（清空文档内容） */
    public void clearRootChildren(String documentId, int count)
    {
        if (count <= 0) { return; }
        ObjectNode body = mapper.createObjectNode();
        body.put("start_index", 0);
        body.put("end_index", count);
        call("DELETE", "/open-apis/docx/v1/documents/" + documentId + "/blocks/" + documentId
                + "/children/batch_delete", body);
    }

    /** 在根块末尾追加子块（单次最多50个，调用方分批） */
    public void appendBlocks(String documentId, ArrayNode blocks, int index)
    {
        ObjectNode body = mapper.createObjectNode();
        body.set("children", blocks);
        body.put("index", index);
        call("POST", "/open-apis/docx/v1/documents/" + documentId + "/blocks/" + documentId + "/children", body);
    }

    /** 设置链接分享权限（anyone_readable / tenant_readable） */
    public void setLinkShare(String documentId)
    {
        ObjectNode body = mapper.createObjectNode();
        if ("tenant_readable".equals(props.getFeishu().getLinkShare()))
        {
            body.put("external_access_entity", "closed");
            body.put("link_share_entity", "tenant_readable");
        }
        else
        {
            body.put("external_access_entity", "open");
            body.put("security_entity", "anyone_can_view");
            body.put("comment_entity", "anyone_can_view");
            body.put("share_entity", "anyone");
            body.put("link_share_entity", "anyone_readable");
        }
        call("PATCH", "/open-apis/drive/v1/permissions/" + documentId + "/public?type=docx", body);
    }

    // ---------------- 内部 ----------------

    private JsonNode call(String method, String path, ObjectNode body)
    {
        String token = tenantAccessToken();
        try
        {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(props.getFeishu().getBaseUrl() + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json; charset=utf-8");
            if ("GET".equals(method)) { b.GET(); }
            else if ("DELETE".equals(method)) { b.method("DELETE", publisher(body)); }
            else { b.method(method, publisher(body)); }
            HttpResponse<String> resp = client().send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(resp.body());
            int code = root.path("code").asInt(-1);
            if (code != 0)
            {
                // token 过期则刷新重试一次
                if (code == 99991661 || code == 99991663 || code == 99991671)
                {
                    tenantToken = null;
                    return call(method, path, body);
                }
                throw new RuntimeException("飞书接口错误 code=" + code + " msg=" + root.path("msg").asText());
            }
            return root.path("data");
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("飞书请求失败: " + e.getMessage(), e);
        }
    }

    private HttpRequest.BodyPublisher publisher(ObjectNode body) throws Exception
    {
        return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
    }

    private synchronized String tenantAccessToken()
    {
        if (tenantToken != null && System.currentTimeMillis() < tenantTokenExpireAt) { return tenantToken; }
        try
        {
            ObjectNode body = mapper.createObjectNode();
            body.put("app_id", props.getFeishu().getAppId());
            body.put("app_secret", props.getFeishu().getAppSecret());
            HttpRequest req = HttpRequest.newBuilder(URI.create(
                            props.getFeishu().getBaseUrl() + "/open-apis/auth/v3/tenant_access_token/internal"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(publisher(body))
                    .build();
            HttpResponse<String> resp = client().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(resp.body());
            if (root.path("code").asInt(-1) != 0)
            {
                throw new RuntimeException("飞书鉴权失败: " + root.path("msg").asText()
                        + "（请检查 kemovie.feishu.app-id / app-secret）");
            }
            tenantToken = root.path("tenant_access_token").asText();
            tenantTokenExpireAt = System.currentTimeMillis() + (root.path("expire").asLong(7200) - 300) * 1000L;
            return tenantToken;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("飞书鉴权请求失败: " + e.getMessage(), e);
        }
    }

    private HttpClient client()
    {
        if (httpClient == null)
        {
            synchronized (this)
            {
                if (httpClient == null)
                {
                    HttpClient.Builder b = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(15))
                            .followRedirects(HttpClient.Redirect.NORMAL);
                    if (props.getProxy().isEnabled() && props.getFeishu().isUseProxy())
                    {
                        b.proxy(ProxySelector.of(new InetSocketAddress(
                                props.getProxy().getHost(), props.getProxy().getPort())));
                    }
                    httpClient = b.build();
                }
            }
        }
        return httpClient;
    }
}
