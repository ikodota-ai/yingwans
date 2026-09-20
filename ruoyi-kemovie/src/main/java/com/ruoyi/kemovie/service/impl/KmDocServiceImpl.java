package com.ruoyi.kemovie.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.FeishuClient;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaResource;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaResourceMapper;
import com.ruoyi.kemovie.service.IKmDocService;

/**
 * 影片资源文档（飞书）Service 实现
 *
 * @author kemovie
 */
@Service
public class KmDocServiceImpl implements IKmDocService
{
    private static final Logger log = LoggerFactory.getLogger(KmDocServiceImpl.class);
    private static final Map<String, String> PLATFORM_NAMES = new LinkedHashMap<>();
    static
    {
        PLATFORM_NAMES.put("baidu", "百度网盘");
        PLATFORM_NAMES.put("115", "115网盘");
        PLATFORM_NAMES.put("quark", "夸克网盘");
        PLATFORM_NAMES.put("aliyun", "阿里云盘");
        PLATFORM_NAMES.put("thunder", "迅雷云盘");
        PLATFORM_NAMES.put("magnet", "磁力链接");
        PLATFORM_NAMES.put("other", "其他");
    }

    @Autowired private FeishuClient feishuClient;
    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private KmMediaResourceMapper resourceMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String testFeishuConnection()
    {
        return feishuClient.testConnection();
    }

    @Override
    @Transactional
    public String generateFeishuDoc(Long mediaId)
    {
        if (!feishuClient.configured())
        {
            throw new ServiceException("未配置飞书应用凭证，请先在配置文件中填写 kemovie.feishu.app-id / app-secret");
        }
        KmMedia media = mediaMapper.selectKmMediaByMediaId(mediaId);
        if (media == null) { throw new ServiceException("影片不存在"); }
        List<KmMediaResource> resources = resourceMapper.selectByMediaId(mediaId);
        if (resources.isEmpty()) { throw new ServiceException("该片暂无下载资源，请先添加资源"); }

        String title = media.getTitle() + (media.getReleaseDate() != null
                ? " (" + new SimpleDateFormat("yyyy").format(media.getReleaseDate()) + ")" : "") + " 下载资源";

        String docId = media.getFeishuDocToken();
        if (StringUtils.isEmpty(docId))
        {
            docId = feishuClient.createDocument(title);
            if (StringUtils.isEmpty(docId)) { throw new ServiceException("飞书文档创建失败"); }
            log.info("[飞书文档] 新建 mediaId={} docId={}", mediaId, docId);
        }
        else
        {
            int childCount = feishuClient.listRootChildrenIds(docId).size();
            feishuClient.clearRootChildren(docId, childCount);
            log.info("[飞书文档] 清空重写 mediaId={} docId={} 旧块数={}", mediaId, docId, childCount);
        }

        writeBlocks(docId, media, resources);
        feishuClient.setLinkShare(docId);

        String url = feishuClient.fetchDocUrl(docId);
        if (StringUtils.isEmpty(url))
        {
            url = props_feishuFallbackUrl(docId);
        }
        KmMedia upd = new KmMedia();
        upd.setMediaId(mediaId);
        upd.setFeishuDocToken(docId);
        upd.setFeishuDocUrl(url);
        mediaMapper.updateKmMedia(upd);
        log.info("[飞书文档] 完成 mediaId={} url={}", mediaId, url);
        return url;
    }

    private String props_feishuFallbackUrl(String docId)
    {
        return "https://feishu.cn/docx/" + docId;
    }

    /** 组装文档内容块并分批写入（每批<=50） */
    private void writeBlocks(String docId, KmMedia media, List<KmMediaResource> resources)
    {
        ArrayNode blocks = mapper.createArrayNode();
        blocks.add(textBlock("更新时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())
                + "　共 " + resources.size() + " 条资源"));
        String lastGroup = null;
        for (KmMediaResource r : resources)
        {
            String group = StringUtils.isNotEmpty(r.getGroupName()) ? r.getGroupName() : "资源";
            if (!group.equals(lastGroup))
            {
                blocks.add(headingBlock(group));
                lastGroup = group;
            }
            blocks.add(resourceBlock(r));
        }
        int index = 0;
        for (int from = 0; from < blocks.size(); from += 50)
        {
            ArrayNode batch = mapper.createArrayNode();
            for (int i = from; i < Math.min(from + 50, blocks.size()); i++) { batch.add(blocks.get(i)); }
            feishuClient.appendBlocks(docId, batch, index);
            index += batch.size();
        }
    }

    private ObjectNode headingBlock(String text)
    {
        ObjectNode b = mapper.createObjectNode();
        b.put("block_type", 3);
        ObjectNode h = b.putObject("heading1");
        h.putArray("elements").add(textRun(text, null));
        h.putObject("style");
        return b;
    }

    private ObjectNode textBlock(String text)
    {
        ObjectNode b = mapper.createObjectNode();
        b.put("block_type", 2);
        ObjectNode t = b.putObject("text");
        t.putArray("elements").add(textRun(text, null));
        t.putObject("style");
        return b;
    }

    private ObjectNode resourceBlock(KmMediaResource r)
    {
        ObjectNode b = mapper.createObjectNode();
        b.put("block_type", 2);
        ObjectNode t = b.putObject("text");
        ArrayNode elements = t.putArray("elements");
        String label = PLATFORM_NAMES.getOrDefault(r.getPlatform(), r.getPlatform());
        elements.add(textRun("【" + label + "】 ", null));
        elements.add(textRun(r.getUrl(), r.getUrl()));
        if (StringUtils.isNotEmpty(r.getPwd()))
        {
            elements.add(textRun("　提取码：" + r.getPwd(), null));
        }
        t.putObject("style");
        return b;
    }

    private ObjectNode textRun(String content, String link)
    {
        ObjectNode run = mapper.createObjectNode();
        ObjectNode tr = run.putObject("text_run");
        tr.put("content", content);
        if (StringUtils.isNotEmpty(link))
        {
            ObjectNode style = tr.putObject("text_element_style");
            ObjectNode ln = style.putObject("link");
            ln.put("url", URLEncoder.encode(link, StandardCharsets.UTF_8));
        }
        return run;
    }
}
