package com.ruoyi.kemovie.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.JustWatchClient;
import com.ruoyi.kemovie.client.JustWatchClient.JwNewOffer;
import com.ruoyi.kemovie.client.JustWatchClient.JwNewPage;
import com.ruoyi.kemovie.client.JustWatchClient.JwNewTitle;
import com.ruoyi.kemovie.config.KemovieProperties;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaRelease;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaReleaseMapper;
import com.ruoyi.kemovie.service.IKmJustWatchNewSyncService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;

/**
 * JustWatch 区域上新同步实现
 *
 * @author kemovie
 */
@Service
public class KmJustWatchNewSyncServiceImpl implements IKmJustWatchNewSyncService
{
    private static final Logger log = LoggerFactory.getLogger(KmJustWatchNewSyncServiceImpl.class);

    @Autowired private JustWatchClient jwClient;
    @Autowired private KemovieProperties props;
    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private KmMediaReleaseMapper releaseMapper;
    @Autowired private IKmMediaSyncService mediaSyncService;

    @Override
    public String syncAllCountries()
    {
        String configured = props.getJustwatch().getNewCountries();
        StringBuilder sb = new StringBuilder();
        for (String c : configured.split(","))
        {
            String country = c.trim().toUpperCase(Locale.ROOT);
            if (country.isEmpty()) { continue; }
            try
            {
                if (sb.length() > 0) { sb.append("; "); }
                sb.append(syncCountry(country));
            }
            catch (Exception e)
            {
                log.error("[JW上新] 区域抓取失败 {}", country, e);
                sb.append(country).append(" 失败:").append(e.getMessage());
            }
        }
        return sb.toString();
    }

    @Override
    public String syncCountry(String country)
    {
        String cc = country.trim().toUpperCase(Locale.ROOT);
        // 用纯日期（java.sql.Date）：online_date 列是 DATE 类型，带时间去重会失效
        java.sql.Date today = java.sql.Date.valueOf(java.time.LocalDate.now());
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(today);
        int maxPages = Math.max(1, props.getJustwatch().getNewMaxPages());
        int pageSize = Math.min(100, Math.max(10, props.getJustwatch().getNewPageSize()));
        int syncLimit = Math.max(0, props.getJustwatch().getNewMediaSyncLimit());
        long interval = props.getJustwatch().getRequestIntervalMs();

        String after = null;
        int titles = 0, synced = 0, inserted = 0, skipped = 0;
        for (int p = 0; p < maxPages; p++)
        {
            JwNewPage page = jwClient.fetchNewTitles(cc, dateStr, pageSize, after);
            for (JwNewTitle t : page.getTitles())
            {
                titles++;
                if (t.getTmdbId() == null || t.getOffers().isEmpty()) { skipped++; continue; }
                KmMedia media = mediaMapper.selectKmMediaByTmdb(t.getTmdbId(), t.getMediaType());
                if (media == null)
                {
                    if (synced >= syncLimit) { skipped++; continue; }
                    media = syncFromTmdbWithRetry(t.getTmdbId(), t.getMediaType());
                    if (media == null) { skipped++; continue; }
                    synced++;
                }
                inserted += insertOffers(media.getMediaId(), cc, t.getOffers(), today);
            }
            if (!page.isHasNext()) { break; }
            after = page.getEndCursor();
            if (interval > 0)
            {
                try { Thread.sleep(interval); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        String summary = cc + " 条目" + titles + " 新入库" + synced + " 新增观看方式" + inserted + " 跳过" + skipped;
        log.info("[JW上新] {}", summary);
        return summary;
    }

    /** TMDB 同步（失败重试一次，间隔2秒，抗偶发网络抖动；失败返回 null） */
    private KmMedia syncFromTmdbWithRetry(Long tmdbId, String mediaType)
    {
        for (int attempt = 1; attempt <= 2; attempt++)
        {
            try
            {
                KmMedia media = mediaSyncService.syncFromTmdb(tmdbId, mediaType);
                if (media != null) { return media; }
            }
            catch (Exception e)
            {
                log.warn("[JW上新] TMDB同步失败(第{}次) {} tmdbId={}: {}", attempt, mediaType, tmdbId, e.getMessage());
            }
            if (attempt < 2)
            {
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return null; }
            }
        }
        log.warn("[JW上新] TMDB同步放弃 {} tmdbId={}（下次整点任务会重试当日条目）", mediaType, tmdbId);
        return null;
    }

    /** 将单条上新条目的观看方式写入 release（自然键去重，返回新增条数） */
    private int insertOffers(Long mediaId, String country, java.util.List<JwNewOffer> offers, Date onlineDate)
    {
        int n = 0;
        for (JwNewOffer o : offers)
        {
            String platform = StringUtils.isNotEmpty(o.getPackageName()) ? o.getPackageName() : o.getPackageShort();
            if (platform != null) { platform = platform.trim(); }
            if (StringUtils.isEmpty(platform)) { continue; }
            String offerType = mapOfferType(o.getMonetizationType());
            KmMediaRelease r = new KmMediaRelease();
            r.setMediaId(mediaId);
            r.setPlatform(platform);
            r.setCountry(country);
            r.setOfferType(offerType);
            r.setOnlineDate(onlineDate);
            r.setLink(o.getUrl());
            r.setStatus("available");
            r.setSource("justwatch");
            n += releaseMapper.insertJwReleaseIgnore(r);
        }
        return n;
    }

    /** JustWatch monetizationType -> 站内 offerType（flatrate/rent/buy/free/ads） */
    private String mapOfferType(String monetizationType)
    {
        if (monetizationType == null) { return "flatrate"; }
        switch (monetizationType.toUpperCase(Locale.ROOT))
        {
            case "FLATRATE":
            case "LINEAR_FLATRATE":
                return "flatrate";
            case "FREE":
                return "free";
            case "ADS":
            case "FAST":
                return "ads";
            case "RENT":
                return "rent";
            case "BUY":
                return "buy";
            default:
                return monetizationType.toLowerCase(Locale.ROOT);
        }
    }
}
