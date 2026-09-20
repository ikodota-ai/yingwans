package com.ruoyi.kemovie.service;

import com.ruoyi.kemovie.domain.KmMedia;

/**
 * 影视数据同步 Service（TMDB + MDL）
 *
 * @author kemovie
 */
public interface IKmMediaSyncService
{
    /** 按 TMDB id 拉详情并 upsert，返回本地 media */
    public KmMedia syncFromTmdb(long tmdbId, String type);

    /** 刷新某片的上线平台信息 */
    public int syncReleases(Long mediaId);

    /** 用标题+年份在 TMDB 匹配并 upsert（MDL 抓取项落地用） */
    public KmMedia matchAndSyncByTitle(String title, Integer year, String preferType);

    /** 只匹配不落库（dry-run/预览用），逻辑同 matchAndSyncByTitle */
    public KmMedia matchOnly(String title, Integer year, String preferType);

    /** 匹配结果标题是否与目标片名精确一致（归一化后），用于区分高置信/模糊命中 */
    public boolean isExactTitleMatch(KmMedia media, String title);

    /** 批量刷新上线排期（含未来上线日期），供定时任务调用。返回处理影片数 */
    public int refreshUpcomingReleases(int limit);

    /** 一次性全量回填所有影片的上线排期/上线日期（含未来），返回处理影片数 */
    public int backfillAllReleases();
}
