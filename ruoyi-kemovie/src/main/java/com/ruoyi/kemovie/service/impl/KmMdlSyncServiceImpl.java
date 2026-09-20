package com.ruoyi.kemovie.service.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.MdlScraper;
import com.ruoyi.kemovie.client.MdlScraper.MdlItem;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmMdlSource;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmCollectionMapper;
import com.ruoyi.kemovie.mapper.KmMdlSourceMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.service.IKmCollectionService;
import com.ruoyi.kemovie.service.IKmMdlSyncService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;

/**
 * MDL 片单抓取与落地实现。
 * 流程：Chrome 抓取列表 -> 解析条目 -> 用标题+年份在 TMDB 匹配并 upsert media
 *       -> 关联到 source 对应的系统片单（去重）。
 *
 * @author kemovie
 */
@Service
public class KmMdlSyncServiceImpl implements IKmMdlSyncService
{
    private static final Logger log = LoggerFactory.getLogger(KmMdlSyncServiceImpl.class);

    @Autowired private MdlScraper mdlScraper;
    @Autowired private IKmMediaSyncService mediaSyncService;
    @Autowired private IKmCollectionService collectionService;
    @Autowired private KmCollectionMapper collectionMapper;
    @Autowired private KmMdlSourceMapper sourceMapper;
    @Autowired private KmMediaMapper mediaMapper;

    @Override
    public String runAllEnabled()
    {
        List<KmMdlSource> sources = sourceMapper.selectEnabledSources();
        StringBuilder sb = new StringBuilder();
        for (KmMdlSource s : sources)
        {
            sb.append(runSource(s)).append("\n");
        }
        return sb.length() == 0 ? "无启用的抓取源" : sb.toString().trim();
    }

    @Override
    public String runSource(KmMdlSource source)
    {
        long start = System.currentTimeMillis();
        int totalItems = 0, matched = 0, added = 0, skipped = 0;
        String mdlListName = null;
        Set<String> seen = new HashSet<>();

        // 1) 确保系统片单存在
        Long collectionId = ensureCollection(source);

        int maxPage = source.getMaxPage() == null || source.getMaxPage() < 1 ? 1 : source.getMaxPage();
        for (int page = 1; page <= maxPage; page++)
        {
            List<MdlItem> items;
            try
            {
                items = mdlScraper.fetchListPage(source.getListId(), page);
            }
            catch (Exception e)
            {
                log.error("抓取失败 list={} page={}", source.getListId(), page, e);
                continue;
            }
            for (MdlItem it : items)
            {
                totalItems++;
                if (it.mdlId == null || seen.contains(it.mdlId)) { skipped++; continue; }
                seen.add(it.mdlId);

                // 2) 若已存在同 mdlId 的 media，直接复用；否则用 TMDB 匹配
                KmMedia media = null;
                KmMedia byMdl = firstByMdlId(it.mdlId);
                if (byMdl != null)
                {
                    media = byMdl;
                }
                else
                {
                    String preferType = it.type != null && it.type.toLowerCase().contains("movie") ? "movie" : "tv";
                    media = mediaSyncService.matchAndSyncByTitle(it.title, it.year, preferType);
                    if (media != null)
                    {
                        // 回填 mdlId 与海报（TMDB 无图时用 MDL 图兜底）
                        KmMedia upd = new KmMedia();
                        upd.setMediaId(media.getMediaId());
                        upd.setMdlId(it.mdlId);
                        if (StringUtils.isEmpty(media.getPosterPath()) && StringUtils.isNotEmpty(it.posterUrl))
                        {
                            upd.setPosterLocalUrl(it.posterUrl);
                        }
                        mediaMapper.updateKmMedia(upd);
                        matched++;
                    }
                }

                if (media == null)
                {
                    log.info("TMDB未匹配到: {} ({})", it.title, it.year);
                    continue;
                }

                // 3) 关联到片单（去重）
                int r = collectionService.addMedia(collectionId, media.getMediaId());
                if (r > 0) { added++; }
            }
        }

        // 4) 回填源状态
        String summary = String.format("抓取完成: 条目%d 匹配%d 新增关联%d 跳过%d 耗时%dms",
                totalItems, matched, added, skipped, System.currentTimeMillis() - start);
        KmMdlSource upd = new KmMdlSource();
        upd.setSourceId(source.getSourceId());
        upd.setLastRunAt(new Date());
        upd.setLastStatus(summary);
        upd.setTargetCollectionId(collectionId);
        sourceMapper.updateKmMdlSource(upd);
        log.info("[MDL-{}] {}", source.getListId(), summary);
        return summary;
    }

    private Long ensureCollection(KmMdlSource source)
    {
        if (source.getTargetCollectionId() != null)
        {
            KmCollection exist = collectionMapper.selectKmCollectionById(source.getTargetCollectionId());
            if (exist != null && "0".equals(exist.getDelFlag())) { return exist.getCollectionId(); }
        }
        String ref = "mdl:" + source.getListId();
        KmCollection exist = collectionMapper.selectBySourceRef(ref);
        if (exist != null) { return exist.getCollectionId(); }

        KmCollection c = new KmCollection();
        c.setName(StringUtils.isNotEmpty(source.getName()) ? source.getName() : ("MDL片单 " + source.getListId()));
        c.setDescription("由 MyDramaList 片单自动抓取生成");
        c.setSource("mdl");
        c.setSourceRef(ref);
        c.setIsPublic("0");
        c.setItemCount(0);
        c.setCreateBy("mdl-sync");
        collectionMapper.insertKmCollection(c);
        return c.getCollectionId();
    }

    private KmMedia firstByMdlId(String mdlId)
    {
        KmMedia q = new KmMedia();
        q.setMdlId(mdlId);
        List<KmMedia> list = mediaMapper.selectKmMediaList(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
