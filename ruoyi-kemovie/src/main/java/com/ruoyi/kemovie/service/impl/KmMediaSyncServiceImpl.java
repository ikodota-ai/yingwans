package com.ruoyi.kemovie.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.TmdbClient;
import com.ruoyi.kemovie.config.KemovieProperties;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaRelease;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaReleaseMapper;
import com.ruoyi.kemovie.service.IKmImageService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;
import com.ruoyi.kemovie.service.IKmPersonService;

/**
 * 影视数据同步实现（TMDB）
 *
 * @author kemovie
 */
@Service
public class KmMediaSyncServiceImpl implements IKmMediaSyncService
{
    private static final Logger log = LoggerFactory.getLogger(KmMediaSyncServiceImpl.class);

    @Autowired private TmdbClient tmdbClient;
    @Autowired private KemovieProperties props;
    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private KmMediaReleaseMapper releaseMapper;
    @Autowired private IKmPersonService personService;
    @Autowired private IKmImageService imageService;

    @Override
    @Transactional
    public KmMedia syncFromTmdb(long tmdbId, String type)
    {
        KmMedia fetched = tmdbClient.fetchDetail(tmdbId, type);
        if (fetched == null)
        {
            log.warn("TMDB详情为空 tmdbId={} type={}", tmdbId, type);
            return null;
        }
        KmMedia exist = mediaMapper.selectKmMediaByTmdb(tmdbId, type);
        if (exist == null)
        {
            fetched.setVisible("0");
            fetched.setCreateBy("tmdb-sync");
            mediaMapper.insertKmMedia(fetched);
            mediaMapper.refreshHotScore(fetched.getMediaId());
            syncReleases(fetched.getMediaId());
            syncCastQuietly(fetched.getMediaId());
            localizePosterQuietly(fetched);
            return fetched;
        }
        else
        {
            fetched.setMediaId(exist.getMediaId());
            fetched.setUpdateBy("tmdb-sync");
            mediaMapper.updateKmMedia(fetched);
            mediaMapper.refreshHotScore(exist.getMediaId());
            syncReleases(exist.getMediaId());
            syncCastQuietly(exist.getMediaId());
            KmMedia saved = mediaMapper.selectKmMediaByMediaId(exist.getMediaId());
            localizePosterQuietly(saved);
            return saved;
        }
    }

    @Override
    @Transactional
    public int syncReleases(Long mediaId)
    {
        KmMedia m = mediaMapper.selectKmMediaByMediaId(mediaId);
        if (m == null || m.getTmdbId() == null) { return 0; }
        List<KmMediaRelease> releases = tmdbClient.fetchWatchProviders(m.getTmdbId(), m.getMediaType());
        // 真实上线日期（尽量含未来排期）：数字发行/首播/下一集等，供上线提醒使用
        Date onlineDate = tmdbClient.fetchOnlineDate(m.getTmdbId(), m.getMediaType());
        if (onlineDate == null) { onlineDate = m.getReleaseDate(); }
        Date today = new Date();
        releaseMapper.deleteByMediaId(mediaId);
        int c = 0;
        for (KmMediaRelease r : releases)
        {
            r.setMediaId(mediaId);
            localizePlatformLogoQuietly(r);
            if (r.getOnlineDate() == null) { r.setOnlineDate(onlineDate); }
            // 未来日期标记为 upcoming，避免被上线提醒误判为“已上线”
            if (r.getOnlineDate() != null && r.getOnlineDate().after(today))
            {
                r.setStatus("upcoming");
            }
            releaseMapper.insertKmMediaRelease(r);
            c++;
        }
        // 尚无平台但已知未来上线日期：写入占位排期，驱动“即将上线”与上线提醒
        if (c == 0 && onlineDate != null && onlineDate.after(today))
        {
            KmMediaRelease placeholder = new KmMediaRelease();
            placeholder.setMediaId(mediaId);
            placeholder.setPlatform("待定");
            placeholder.setCountry(props.getTmdb().getRegion());
            placeholder.setOnlineDate(onlineDate);
            placeholder.setStatus("upcoming");
            releaseMapper.insertKmMediaRelease(placeholder);
            c++;
        }
        return c;
    }

    /** 同步演职人员，失败不影响主流程 */
    private void syncCastQuietly(Long mediaId)
    {
        try
        {
            personService.syncCastForMedia(mediaId);
        }
        catch (Exception e)
        {
            log.warn("同步演职人员失败 mediaId={}", mediaId, e);
        }
    }

    /** 本地化平台 logo，失败不影响主流程（回退远程地址） */
    private void localizePlatformLogoQuietly(KmMediaRelease r)
    {
        try
        {
            if (r != null && StringUtils.isNotEmpty(r.getPlatformLogo()))
            {
                r.setPlatformLogo(imageService.localizePlatformLogo(r.getPlatform(), r.getPlatformLogo()));
            }
        }
        catch (Exception e)
        {
            log.warn("本地化平台 logo 失败 platform={} err={}", r == null ? null : r.getPlatform(), e.getMessage());
        }
    }

    /** 本地化海报，失败不影响主流程 */
    private void localizePosterQuietly(KmMedia media)
    {
        try
        {
            imageService.localizeMediaPoster(media);
        }
        catch (Exception e)
        {
            log.warn("本地化海报失败 mediaId={}", media == null ? null : media.getMediaId(), e);
        }
    }

    @Override
    public int refreshUpcomingReleases(int limit)
    {
        List<KmMedia> list = mediaMapper.selectMediaForReleaseRefresh(limit);
        int n = 0;
        for (KmMedia m : list)
        {
            try
            {
                syncReleases(m.getMediaId());
                n++;
            }
            catch (Exception e)
            {
                log.warn("刷新上线排期失败 mediaId={} err={}", m.getMediaId(), e.getMessage());
            }
        }
        log.info("上线排期刷新完成：处理 {} 部", n);
        return n;
    }

    @Override
    public int backfillAllReleases()
    {
        int total = 0;
        int fail = 0;
        Long afterId = null;
        int batch = 200;
        while (true)
        {
            List<Long> ids = mediaMapper.selectAllTmdbMediaIds(afterId, batch);
            if (ids == null || ids.isEmpty()) { break; }
            for (Long id : ids)
            {
                try
                {
                    syncReleases(id);
                    total++;
                }
                catch (Exception e)
                {
                    fail++;
                    log.warn("全量回填上线排期失败 mediaId={} err={}", id, e.getMessage());
                }
                afterId = id;
            }
            log.info("[全量回填] 进度：已处理 {} 部（失败 {}）", total, fail);
        }
        log.info("[全量回填] 完成：成功 {} 部，失败 {} 部", total, fail);
        return total;
    }

    @Override
    @Transactional
    public KmMedia matchAndSyncByTitle(String title, Integer year, String preferType)
    {
        KmMedia best = matchOnly(title, year, preferType);
        if (best == null) { return null; }
        return syncFromTmdb(best.getTmdbId(), best.getMediaType());
    }

    /**
     * 只匹配不落库（dry-run 用）：与 matchAndSyncByTitle 相同的搜索与打分逻辑。
     */
    @Override
    public KmMedia matchOnly(String title, Integer year, String preferType)
    {
        if (StringUtils.isEmpty(title)) { return null; }
        // 先按偏好类型搜索，未命中再用 multi 兜底（MDL 类型判断可能不准）
        // zh 索引对英文/罗马字片名覆盖差（如 "City of God" 搜不到 598），合并 en-US 结果一并打分
        List<KmMedia> candidates = new ArrayList<>(tmdbClient.search(title, preferType));
        candidates.addAll(tmdbClient.search(title, preferType, "en-US"));
        KmMedia best = pickBest(candidates, title, year);
        if (best == null)
        {
            List<KmMedia> multi = new ArrayList<>(tmdbClient.search(title, "multi"));
            multi.addAll(tmdbClient.search(title, "multi", "en-US"));
            KmMedia mBest = pickBest(multi, title, year);
            if (mBest != null) { best = mBest; }
        }
        return best;
    }

    /**
     * 匹配结果的标题是否与目标片名精确一致（归一化后比较 title / originalTitle）。
     * 供调用方区分高置信命中与模糊命中：模糊命中不宜回填外部映射（如 letterboxd_slug）。
     */
    @Override
    public boolean isExactTitleMatch(KmMedia media, String title)
    {
        if (media == null || StringUtils.isEmpty(title)) { return false; }
        String norm = normalize(title);
        return !norm.isEmpty()
                && (norm.equals(normalize(media.getTitle())) || norm.equals(normalize(media.getOriginalTitle())));
    }

    /**
     * 打分挑选最佳候选：标题精确命中(6) / 归一化包含(3) + 年份吻合(4) / 相邻(2)
     * + 热度与投票数微调。分数过低视为不可信，返回 null 让上层用 multi 再试或放弃，
     * 避免盲目取 results[0] 导致错配（如 "Making of X" 抢占 "X"）。
     */
    private KmMedia pickBest(List<KmMedia> candidates, String title, Integer year)
    {
        if (candidates == null || candidates.isEmpty()) { return null; }
        String norm = normalize(title);
        KmMedia best = null;
        double bestScore = 0;
        for (KmMedia c : candidates)
        {
            double score = 0;
            String ct = normalize(c.getTitle());
            String cot = normalize(c.getOriginalTitle());
            if (norm.equals(ct) || norm.equals(cot))
            {
                score += 6;
            }
            else if (!norm.isEmpty() && (ct.contains(norm) || norm.contains(ct)
                    || cot.contains(norm) || norm.contains(cot)))
            {
                score += 3;
            }
            Integer cy = yearOf(c);
            if (year != null && cy != null)
            {
                int diff = Math.abs(year - cy);
                if (diff == 0) { score += 4; }
                else if (diff == 1) { score += 2; }
                else { score -= 1; }
            }
            // 热度/口碑作为微调（0~1 区间），避免同名冷门条目误选
            if (c.getVoteCount() != null && c.getVoteCount() > 0) { score += Math.min(1.0, c.getVoteCount() / 500.0); }
            if (c.getPopularity() != null) { score += Math.min(0.5, c.getPopularity().doubleValue() / 100.0); }
            if (score > bestScore)
            {
                bestScore = score;
                best = c;
            }
        }
        // 分数阈值：至少要有标题或年份层面的实质证据
        return bestScore >= 3 ? best : null;
    }

    private Integer yearOf(KmMedia m)
    {
        if (m.getReleaseDate() == null) { return null; }
        Calendar cal = Calendar.getInstance();
        cal.setTime(m.getReleaseDate());
        return cal.get(Calendar.YEAR);
    }

    private String normalize(String s)
    {
        if (s == null) { return ""; }
        return s.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "").trim();
    }
}
