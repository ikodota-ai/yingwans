package com.ruoyi.kemovie.service.impl;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;
import com.ruoyi.kemovie.domain.KmMdlSource;
import com.ruoyi.kemovie.mapper.KmMdlSourceMapper;
import com.ruoyi.kemovie.service.IKmMdlSourceService;

/**
 * 抓取源 Service 实现（MDL / JustWatch 两类）。
 *
 * @author kemovie
 */
@Service
public class KmMdlSourceServiceImpl implements IKmMdlSourceService
{
    public static final String TYPE_MDL = "mdl";
    public static final String TYPE_JUSTWATCH = "justwatch";
    public static final String TYPE_LETTERBOXD = "letterboxd";

    /** MDL 片单地址前缀，list_url 缺省时由 list_id 自动补全 */
    private static final String MDL_LIST_URL_PREFIX = "https://mydramalist.com/list/";
    private static final String JW_URL_PREFIX = "https://www.justwatch.com/";
    private static final String LBX_URL_PREFIX = "https://letterboxd.com/";

    @Autowired private KmMdlSourceMapper mapper;
    @Autowired private KemovieProperties props;

    @Override
    public KmMdlSource selectKmMdlSourceById(Long sourceId) { return mapper.selectKmMdlSourceById(sourceId); }

    @Override
    public List<KmMdlSource> selectKmMdlSourceList(KmMdlSource kmMdlSource) { return mapper.selectKmMdlSourceList(kmMdlSource); }

    @Override
    public int insertKmMdlSource(KmMdlSource kmMdlSource) { return mapper.insertKmMdlSource(normalize(kmMdlSource)); }

    @Override
    public int updateKmMdlSource(KmMdlSource kmMdlSource) { return mapper.updateKmMdlSource(normalize(kmMdlSource)); }

    @Override
    public int deleteKmMdlSourceByIds(Long[] sourceIds) { return mapper.deleteKmMdlSourceByIds(sourceIds); }

    /**
     * 规范化抓取源：区分 MDL / JustWatch，补全 list_url（数据库 NOT NULL），兼容粘贴完整链接。
     */
    private KmMdlSource normalize(KmMdlSource source)
    {
        String type = source.getSourceType() == null ? "" : source.getSourceType().trim().toLowerCase(Locale.ROOT);
        if (TYPE_JUSTWATCH.equals(type)) { source.setSourceType(TYPE_JUSTWATCH); }
        else if (TYPE_LETTERBOXD.equals(type)) { source.setSourceType(TYPE_LETTERBOXD); }
        else { source.setSourceType(TYPE_MDL); }
        if (TYPE_JUSTWATCH.equals(source.getSourceType())) { return normalizeJustWatch(source); }
        if (TYPE_LETTERBOXD.equals(source.getSourceType())) { return normalizeLetterboxd(source); }
        return normalizeMdl(source);
    }

    private KmMdlSource normalizeMdl(KmMdlSource source)
    {
        source.setObjectType(null);
        String listId = source.getListId() == null ? null : source.getListId().trim();
        if (StringUtils.isNotEmpty(listId))
        {
            int marker = listId.indexOf("/list/");
            if (marker >= 0)
            {
                listId = listId.substring(marker + "/list/".length());
                int query = listId.indexOf('?');
                if (query >= 0) { listId = listId.substring(0, query); }
                int slash = listId.indexOf('/');
                if (slash >= 0) { listId = listId.substring(0, slash); }
                source.setListId(listId);
            }
        }
        if (StringUtils.isEmpty(listId))
        {
            throw new ServiceException("MDL List ID 不能为空");
        }
        String listUrl = source.getListUrl() == null ? null : source.getListUrl().trim();
        if (StringUtils.isEmpty(listUrl))
        {
            source.setListUrl(MDL_LIST_URL_PREFIX + listId);
        }
        return source;
    }

    /**
     * JustWatch 源：list_id 存平台 slug（如 netflix，留空=全国热门）；
     * region_tag 存国家(US)；object_type 存 movie/tv（空=全部）。list_url 生成规范链接。
     */
    private KmMdlSource normalizeJustWatch(KmMdlSource source)
    {
        String country = source.getRegionTag();
        country = StringUtils.isEmpty(country) ? props.getJustwatch().getCountry() : country.trim().toUpperCase(Locale.ROOT);
        source.setRegionTag(country);

        String objectType = source.getObjectType();
        objectType = objectType == null ? null : objectType.trim().toLowerCase(Locale.ROOT);
        if ("all".equals(objectType) || StringUtils.isEmpty(objectType)) { objectType = null; }
        else if (!"movie".equals(objectType) && !"tv".equals(objectType)) { objectType = null; }
        source.setObjectType(objectType);

        String token = extractProviderSlug(source.getListId());
        source.setListId(token);

        String cc = country.toLowerCase(Locale.ROOT);
        String listUrl = token == null
                ? JW_URL_PREFIX + cc + "/new"
                : JW_URL_PREFIX + cc + "/provider/" + token + "/new";
        source.setListUrl(listUrl);
        return source;
    }

    /** 从完整 provider URL 提取 slug；非 URL 原样小写；全国热门（空/含 /new 但无 /provider/）返回 null */
    private String extractProviderSlug(String raw)
    {
        if (raw == null) { return null; }
        String t = raw.trim();
        if (StringUtils.isEmpty(t) || "all".equalsIgnoreCase(t)) { return null; }
        int marker = t.indexOf("/provider/");
        if (marker >= 0)
        {
            t = t.substring(marker + "/provider/".length());
        }
        else if (t.startsWith("http") && t.contains("/" ))
        {
            // 形如 https://www.justwatch.com/us/new 的链接 => 全国热门
            return null;
        }
        int slash = t.indexOf('/');
        if (slash >= 0) { t = t.substring(0, slash); }
        int query = t.indexOf('?');
        if (query >= 0) { t = t.substring(0, query); }
        t = t.trim().toLowerCase(Locale.ROOT);
        return StringUtils.isEmpty(t) ? null : t;
    }

    /**
     * Letterboxd 源：list_id 存片单路径（如 official/list/top-250-...），
     * 兼容粘贴完整链接；region_tag 固定 global（唯一键需要），object_type 置空。
     */
    private KmMdlSource normalizeLetterboxd(KmMdlSource source)
    {
        source.setObjectType(null);
        String listId = source.getListId() == null ? null : source.getListId().trim();
        if (StringUtils.isNotEmpty(listId))
        {
            int host = listId.indexOf("letterboxd.com/");
            if (host >= 0) { listId = listId.substring(host + "letterboxd.com/".length()); }
            int query = listId.indexOf('?');
            if (query >= 0) { listId = listId.substring(0, query); }
            while (listId.startsWith("/")) { listId = listId.substring(1); }
            while (listId.endsWith("/")) { listId = listId.substring(0, listId.length() - 1); }
        }
        if (StringUtils.isEmpty(listId) || !listId.contains("/list/"))
        {
            throw new ServiceException("Letterboxd 片单链接需形如 https://letterboxd.com/{用户}/list/{片单}/");
        }
        source.setListId(listId);
        source.setListUrl(LBX_URL_PREFIX + listId + "/");
        if (StringUtils.isEmpty(source.getRegionTag()))
        {
            source.setRegionTag("global");
        }
        return source;
    }
}
