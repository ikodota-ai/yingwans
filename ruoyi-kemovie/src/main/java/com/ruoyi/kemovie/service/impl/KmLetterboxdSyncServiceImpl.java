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
import com.ruoyi.kemovie.client.LetterboxdMediaResolver;
import com.ruoyi.kemovie.client.LetterboxdMediaResolver.Result;
import com.ruoyi.kemovie.client.LetterboxdScraper;
import com.ruoyi.kemovie.client.LetterboxdScraper.LbxItem;
import com.ruoyi.kemovie.client.LetterboxdScraper.LbxPage;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmMdlSource;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmCollectionMapper;
import com.ruoyi.kemovie.mapper.KmMdlSourceMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.service.IKmCollectionService;
import com.ruoyi.kemovie.service.IKmLetterboxdSyncService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;

/**
 * Letterboxd 片单抓取与落地实现。
 * 流程：无头 Chrome 渲染列表页 -> 解析条目（片名+年份+slug）
 *       -> 落地优先级：本地 letterboxd_slug 复用 -> 标题+年份 TMDB 匹配
 *          -> 渲染影片页取精确 tmdbId -> 关联到对应系统片单（去重）。
 * 同一 TMDB 影片可同时存在于多个片单（slug 映射 + 片单内去重保证不重复关联）。
 *
 * @author kemovie
 */
@Service
public class KmLetterboxdSyncServiceImpl implements IKmLetterboxdSyncService
{
    private static final Logger log = LoggerFactory.getLogger(KmLetterboxdSyncServiceImpl.class);

    @Autowired private LetterboxdScraper scraper;
    @Autowired private LetterboxdMediaResolver resolver;
    @Autowired private IKmMediaSyncService mediaSyncService;
    @Autowired private IKmCollectionService collectionService;
    @Autowired private KmCollectionMapper collectionMapper;
    @Autowired private KmMdlSourceMapper sourceMapper;
    @Autowired private KmMediaMapper mediaMapper;

    @Override
    public String runAllEnabled()
    {
        List<KmMdlSource> sources = sourceMapper.selectEnabledByType(KmMdlSourceServiceImpl.TYPE_LETTERBOXD);
        StringBuilder sb = new StringBuilder();
        for (KmMdlSource s : sources)
        {
            try
            {
                sb.append(runSource(s)).append("\n");
            }
            catch (Exception e)
            {
                sb.append("[").append(s.getName()).append("] 失败: ").append(e.getMessage()).append("\n");
            }
        }
        return sb.length() == 0 ? "无启用的 Letterboxd 抓取源" : sb.toString().trim();
    }

    @Override
    public String runSource(KmMdlSource source)
    {
        long start = System.currentTimeMillis();
        int total = 0, reused = 0, titleMatched = 0, pageResolved = 0, added = 0, failed = 0;
        Set<String> seen = new HashSet<>();

        Long collectionId = ensureCollection(source);
        int maxPage = source.getMaxPage() == null || source.getMaxPage() < 1 ? 1 : Math.min(source.getMaxPage(), 50);

        for (int page = 1; page <= maxPage; page++)
        {
            LbxPage listPage;
            try
            {
                listPage = scraper.fetchListPage(source.getListId(), page);
            }
            catch (Exception e)
            {
                log.error("Letterboxd 抓取失败 list={} page={}", source.getListId(), page, e);
                break;
            }
            if (listPage.items.isEmpty() && page > 1) { break; }

            for (LbxItem it : listPage.items)
            {
                total++;
                if (StringUtils.isEmpty(it.slug) || !seen.add(it.slug)) { continue; }

                Result result = null;
                try
                {
                    result = resolver.resolve(it.slug, it.title, it.year);
                }
                catch (Exception e)
                {
                    log.warn("Letterboxd 条目落地失败 slug={} err={}", it.slug, e.getMessage());
                    failed++;
                    continue;
                }

                if (result == null || result.media == null)
                {
                    log.info("Letterboxd 未匹配: {} ({})", it.title, it.year);
                    failed++;
                    continue;
                }
                if (collectionService.addMedia(collectionId, result.media.getMediaId()) > 0) { added++; }

                switch (result.way)
                {
                    case 1: reused++; break;
                    case 2: titleMatched++; break;
                    case 3: pageResolved++; break;
                    default: break;
                }
            }

            int totalPages = Math.max(listPage.totalPages, 1);
            if (page >= totalPages) { break; }
        }

        String summary = String.format(
                "抓取完成: 条目%d 复用%d 标题匹配%d 影片页解析%d 新关联%d 失败%d 耗时%dms",
                total, reused, titleMatched, pageResolved, added, failed, System.currentTimeMillis() - start);
        KmMdlSource upd = new KmMdlSource();
        upd.setSourceId(source.getSourceId());
        upd.setLastRunAt(new Date());
        upd.setLastStatus(summary);
        upd.setTargetCollectionId(collectionId);
        sourceMapper.updateKmMdlSource(upd);
        log.info("[LBX-{}] {}", source.getListId(), summary);
        return summary;
    }

    private Long ensureCollection(KmMdlSource source)
    {
        if (source.getTargetCollectionId() != null)
        {
            KmCollection exist = collectionMapper.selectKmCollectionById(source.getTargetCollectionId());
            if (exist != null && "0".equals(exist.getDelFlag())) { return exist.getCollectionId(); }
        }
        String ref = "letterboxd:" + source.getListId();
        KmCollection exist = collectionMapper.selectBySourceRef(ref);
        if (exist != null) { return exist.getCollectionId(); }

        String slugTail = source.getListId().substring(source.getListId().lastIndexOf('/') + 1);
        KmCollection c = new KmCollection();
        c.setName(StringUtils.isNotEmpty(source.getName()) ? source.getName() : ("Letterboxd " + slugTail));
        c.setDescription("由 Letterboxd 片单自动抓取生成: " + source.getListUrl());
        c.setSource("letterboxd");
        c.setSourceRef(ref);
        c.setIsPublic("0");
        c.setItemCount(0);
        c.setCreateBy("letterboxd-sync");
        collectionMapper.insertKmCollection(c);
        return c.getCollectionId();
    }
}
