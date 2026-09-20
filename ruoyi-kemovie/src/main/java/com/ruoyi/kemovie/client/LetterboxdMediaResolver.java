package com.ruoyi.kemovie.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.LetterboxdScraper.LbxFilm;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.service.IKmMediaSyncService;

/**
 * Letterboxd 条目 -> 本地影片 的解析器（抓取与 CSV 导入共用）。
 * 落地优先级：本地 letterboxd_slug 复用 -> 标题+年份 TMDB 匹配
 *             -> 渲染影片页取精确 tmdbId。
 *
 * @author kemovie
 */
@Component
public class LetterboxdMediaResolver
{
    @Autowired private LetterboxdScraper scraper;
    @Autowired private IKmMediaSyncService mediaSyncService;
    @Autowired private KmMediaMapper mediaMapper;

    /** 解析结果：media + 落地方式（1 复用 / 2 标题匹配 / 3 影片页解析） */
    public static class Result
    {
        public final KmMedia media;
        public final int way;

        public Result(KmMedia media, int way) { this.media = media; this.way = way; }
    }

    /**
     * 解析单条 Letterboxd 条目为本地影片。
     *
     * @param slug  影片 slug（可为 null，则跳过本地映射与影片页兜底）
     * @param title 片名（Letterboxd 数据源自 TMDB，标题+年份命中率高）
     * @param year  年份（可为 null）
     */
    public Result resolve(String slug, String title, Integer year)
    {
        // 1) 本地 slug 映射直接复用
        if (StringUtils.isNotEmpty(slug))
        {
            KmMedia bySlug = firstByLetterboxdSlug(slug);
            if (bySlug != null)
            {
                return new Result(bySlug, 1);
            }
        }
        // 2) 标题+年份 TMDB 匹配：仅接受高置信（精确片名）命中
        KmMedia matched = mediaSyncService.matchAndSyncByTitle(title, year, "movie");
        if (matched != null && mediaSyncService.isExactTitleMatch(matched, title))
        {
            backfillSlug(matched, slug);
            return new Result(matched, 2);
        }
        // 3) 无命中或仅模糊命中时，渲染影片页取精确 tmdbId
        if (StringUtils.isNotEmpty(slug))
        {
            LbxFilm film = scraper.resolveFilm(slug);
            if (film != null && film.tmdbId != null)
            {
                KmMedia media = mediaMapper.selectKmMediaByTmdb(film.tmdbId, film.mediaType);
                if (media == null)
                {
                    media = mediaSyncService.syncFromTmdb(film.tmdbId, film.mediaType);
                }
                if (media != null)
                {
                    backfillSlug(media, slug);
                    return new Result(media, 3);
                }
            }
        }
        // 4) 影片页也失败时退回模糊匹配，但不回填 slug（避免错误映射被后续运行复用）
        return matched != null ? new Result(matched, 2) : null;
    }

    /** 回填 letterboxd_slug（已有值则不覆盖，避免错配互踩） */
    private void backfillSlug(KmMedia media, String slug)
    {
        if (media == null || media.getMediaId() == null
                || StringUtils.isEmpty(slug) || StringUtils.isNotEmpty(media.getLetterboxdSlug()))
        {
            return;
        }
        KmMedia upd = new KmMedia();
        upd.setMediaId(media.getMediaId());
        upd.setLetterboxdSlug(slug);
        mediaMapper.updateKmMedia(upd);
        media.setLetterboxdSlug(slug);
    }

    private KmMedia firstByLetterboxdSlug(String slug)
    {
        KmMedia q = new KmMedia();
        q.setLetterboxdSlug(slug);
        List<KmMedia> list = mediaMapper.selectKmMediaList(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
