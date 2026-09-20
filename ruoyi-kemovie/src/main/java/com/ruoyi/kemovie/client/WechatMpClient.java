package com.ruoyi.kemovie.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * 微信公众号模板消息客户端（接口测试号/服务号通用，接口一致）。
 * access_token 缓存复用；消息可带跳转链接（如飞书文档地址）。
 *
 * @author kemovie
 */
@Component
public class WechatMpClient
{
    private static final Logger log = LoggerFactory.getLogger(WechatMpClient.class);
    private static final String API = "https://api.weixin.qq.com";

    private final KemovieProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();
    private volatile String accessToken;
    private volatile long accessTokenExpireAt;

    public WechatMpClient(KemovieProperties props)
    {
        this.props = props;
    }

    public boolean configured()
    {
        return props.getWechat().isEnabled()
                && StringUtils.isNotEmpty(props.getWechat().getAppId())
                && StringUtils.isNotEmpty(props.getWechat().getAppSecret())
                && StringUtils.isNotEmpty(props.getWechat().getTemplateId());
    }

    /**
     * 发送模板消息（两个关键词槽位 + 跳转链接）。
     *
     * @param openid  接收者 openid
     * @param title   标题（keyword1，如影片名）
     * @param remark  备注（keyword2，如提示语）
     * @param url     点击跳转链接（飞书文档地址，可空）
     * @return 失败原因，成功返回 null
     */
    public String sendTemplate(String openid, String title, String remark, String url)
    {
        if (!configured()) { return "微信推送未配置"; }
        try
        {
            ObjectNode data = mapper.createObjectNode();
            data.putObject("keyword1").put("value", title);
            data.putObject("keyword2").put("value", remark);
            ObjectNode body = mapper.createObjectNode();
            body.put("touser", openid);
            body.put("template_id", props.getWechat().getTemplateId());
            if (StringUtils.isNotEmpty(url)) { body.put("url", url); }
            body.set("data", data);
            JsonNode root = post("/cgi-bin/message/template/send?access_token=" + token(), body);
            int code = root.path("errcode").asInt(-1);
            if (code == 0) { return null; }
            // token 失效重试一次
            if (code == 40001 || code == 42001)
            {
                accessToken = null;
                root = post("/cgi-bin/message/template/send?access_token=" + token(), body);
                if (root.path("errcode").asInt(-1) == 0) { return null; }
            }
            return "微信接口错误: " + root.path("errmsg").asText();
        }
        catch (Exception e)
        {
            log.warn("[微信推送] 发送失败 openid={} err={}", openid, e.getMessage());
            return "微信推送失败: " + e.getMessage();
        }
    }

    private JsonNode post(String path, ObjectNode body) throws Exception
    {
        HttpRequest req = HttpRequest.newBuilder(URI.create(API + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        return mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body());
    }

    private synchronized String token() throws Exception
    {
        if (accessToken != null && System.currentTimeMillis() < accessTokenExpireAt) { return accessToken; }
        String url = API + "/cgi-bin/token?grant_type=client_credential&appid="
                + props.getWechat().getAppId() + "&secret=" + props.getWechat().getAppSecret();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
        JsonNode root = mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body());
        String token = root.path("access_token").asText(null);
        if (token == null)
        {
            throw new RuntimeException("微信鉴权失败: " + root.path("errmsg").asText());
        }
        accessToken = token;
        accessTokenExpireAt = System.currentTimeMillis() + (root.path("expires_in").asLong(7200) - 300) * 1000L;
        return accessToken;
    }
}
