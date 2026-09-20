package com.ruoyi.kemovie.service.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.JustWatchClient;
import com.ruoyi.kemovie.client.JustWatchClient.JwPackage;
import com.ruoyi.kemovie.client.JustWatchClient.JwTitle;
import com.ruoyi.kemovie.config.KemovieProperties;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmMdlSource;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmCollectionMapper;
import com.ruoyi.kemovie.mapper.KmMdlSourceMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.service.IKmCollectionService;
import com.ruoyi.kemovie.service.IKmJustWatchSyncService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;

/**
 * JustWatch 片单抓取与落地实现。
 * 流程：GraphQL 按“国家 + 播放平台”分页拉取 -> 条目自带 tmdbId，精确 upsert media
 *       （本地已存在的影片直接复用，不重复请求 TMDB）-> 关联到对应系统片单（去重）。
 *
 * @author kemovie
 */
@Service
public class KmJustWatchSyncServiceImpl implements IKmJustWatchSyncService
{
    private static final Logger log = LoggerFactory.getLogger(KmJustWatchSyncServiceImpl.class);

    @Autowired private JustWatchClient jwClient;
    @Autowired private KemovieProperties props;
    @Autowired private IKmMediaSyncService mediaSyncService;
    @Autowired private IKmCollectionService collectionService;
    @Autowired private KmCollectionMapper collectionMapper;
    @Autowired private KmMdlSourceMapper sourceMapper;
    @Autowired private KmMediaMapper mediaMapper;

    @Override
    public String runAllEnabled()
    {
        List<KmMdlSource> sources = sourceMapper.selectEnabledByType(KmMdlSourceServiceImpl.TYPE_JUSTWATCH);
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
        return sb.length() == 0 ? "无启用的 JustWatch 抓取源" : sb.toString().trim();
    }

    @Override
    public String runSource(KmMdlSource source)
    {
        long start = System.currentTimeMillis();
        String country = StringUtils.isNotEmpty(source.getRegionTag())
                ? source.getRegionTag().trim().toUpperCase(Locale.ROOT)
                : props.getJustwatch().getCountry();
        String objectType = source.getObjectType();

        // 1) 解析播放平台（留空表示全国热门）
        JwPackage pkg = null;
        if (StringUtils.isNotEmpty(source.getListId()))
        {
            pkg = jwClient.resolvePackage(country, source.getListId());
            if (pkg == null)
            {
                String msg = "无法识别的 JustWatch 平台: " + source.getListId() + "（国家 " + country + "）";
                persistStatus(source, null, "抓取失败: " + msg);
                throw new ServiceException(msg);
            }
        }

        // 2) 确保系统片单存在
        Long collectionId = ensureCollection(source, country, pkg);

        int total = 0, noTmdb = 0, synced = 0, linked = 0, added = 0, failed = 0;
        Set<String> seen = new HashSet<>();
        int maxPage = source.getMaxPage() == null || source.getMaxPage() < 1 ? 1 : Math.min(source.getMaxPage(), 50);
        int pageSize = Math.min(Math.max(props.getJustwatch().getPageSize(), 1), 100);
        String cursor = null;

        for (int page = 1; page <= maxPage; page++)
        {
            JustWatchClient.JwPage result;
            try
            {
                result = jwClient.fetchPopular(country, pageSize, cursor,
                        pkg == null ? null : pkg.getShortName(), objectType);
            }
            catch (Exception e)
            {
                log.error("JustWatch 抓取失败 source={} page={}", source.getSourceId(), page, e);
                failed++;
                break;
            }

            for (JwTitle t : result.getTitles())
            {
                total++;
                if (t.getTmdbId() == null) { noTmdb++; continue; }
                String key = t.getMediaType() + ":" + t.getTmdbId();
                if (!seen.add(key)) { continue; }

                KmMedia media = mediaMapper.selectKmMediaByTmdb(t.getTmdbId(), t.getMediaType());
                try
                {
                    if (media == null)
                    {
                        media = mediaSyncService.syncFromTmdb(t.getTmdbId(), t.getMediaType());
                        if (media != null) { synced++; }
                    }
                    else
                    {
                        linked++;
                    }
                }
                catch (Exception e)
                {
                    log.warn("TMDB 同步失败 tmdbId={} type={} err={}", t.getTmdbId(), t.getMediaType(), e.getMessage());
                    failed++;
                    continue;
                }

                if (media == null) { noTmdb++; continue; }
                if (collectionService.addMedia(collectionId, media.getMediaId()) > 0) { added++; }
            }

            if (!result.isHasNext() || StringUtils.isEmpty(result.getEndCursor())) { break; }
            cursor = result.getEndCursor();
            if (props.getJustwatch().getRequestIntervalMs() > 0 && page < maxPage)
            {
                try { Thread.sleep(props.getJustwatch().getRequestIntervalMs()); } catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        String summary = String.format(Locale.ROOT,
                "抓取完成: 条目%d 新入库%d 复用%d 新关联%d 无TMDB%d 失败%d 耗时%dms",
                total, synced, linked, added, noTmdb, failed, System.currentTimeMillis() - start);
        persistStatus(source, collectionId, summary);
        log.info("[JW-{}-{}] {}", country, pkg == null ? "all" : pkg.getSlug(), summary);
        return summary;
    }

    private void persistStatus(KmMdlSource source, Long collectionId, String status)
    {
        KmMdlSource upd = new KmMdlSource();
        upd.setSourceId(source.getSourceId());
        upd.setLastRunAt(new Date());
        upd.setLastStatus(status);
        if (collectionId != null) { upd.setTargetCollectionId(collectionId); }
        sourceMapper.updateKmMdlSource(upd);
    }

    private Long ensureCollection(KmMdlSource source, String country, JwPackage pkg)
    {
        if (source.getTargetCollectionId() != null)
        {
            KmCollection exist = collectionMapper.selectKmCollectionById(source.getTargetCollectionId());
            if (exist != null && "0".equals(exist.getDelFlag())) { return exist.getCollectionId(); }
        }
        String ref = "justwatch:" + country + ":" + (pkg == null ? "all" : pkg.getSlug());
        KmCollection exist = collectionMapper.selectBySourceRef(ref);
        if (exist != null) { return exist.getCollectionId(); }

        String defaultName = pkg == null
                ? "JustWatch " + country + " 热门"
                : (StringUtils.isNotEmpty(pkg.getClearName()) ? pkg.getClearName() : pkg.getSlug()) + "（" + country + "）";
        KmCollection c = new KmCollection();
        c.setName(StringUtils.isNotEmpty(source.getName()) ? source.getName() : defaultName);
        c.setDescription("由 JustWatch 片单（" + country
                + (pkg == null ? "，全站热门" : "，" + pkg.getClearName()) + "）自动抓取生成");
        c.setSource("justwatch");
        c.setSourceRef(ref);
        c.setIsPublic("0");
        c.setItemCount(0);
        c.setCreateBy("justwatch-sync");
        collectionMapper.insertKmCollection(c);
        return c.getCollectionId();
    }
}
