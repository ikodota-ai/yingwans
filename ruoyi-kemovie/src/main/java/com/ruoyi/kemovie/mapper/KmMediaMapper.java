package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.vo.KmNewRelease;

/**
 * 影视条目 Mapper
 *
 * @author kemovie
 */
public interface KmMediaMapper
{
    public KmMedia selectKmMediaByMediaId(Long mediaId);

    public KmMedia selectKmMediaByTmdb(@Param("tmdbId") Long tmdbId, @Param("mediaType") String mediaType);

    /** 按旧站 aid 查询（导入幂等锚点） */
    public KmMedia selectKmMediaByLegacyAid(Long legacyAid);

    public List<KmMedia> selectKmMediaList(KmMedia kmMedia);

    /** 前台发现列表（仅可见、未删） */
    public List<KmMedia> selectPublicMediaList(KmMedia kmMedia);

    /** 近期上线 */
    public List<KmMedia> selectRecentReleases(@Param("limit") int limit);

    /** 最近收录：按本站收录时间倒序（含旧站导入片） */
    public List<KmMedia> selectRecentCollected(@Param("limit") int limit);

    /** JustWatch 最近上新（按影片+区域聚合，取最近上线日期与平台汇总） */
    public List<KmNewRelease> selectNewReleases(@Param("country") String country,
            @Param("days") int days, @Param("limit") int limit);

    /** 需刷新上线排期的影片：未来/近期上线、连载剧集、或被订阅且未完结（供定时任务刷新 online_date） */
    public List<KmMedia> selectMediaForReleaseRefresh(@Param("limit") int limit);

    /** 全部有 TMDB id 的影片 id（供一次性全量回填 online_date），按 media_id 升序 */
    public List<Long> selectAllTmdbMediaIds(@Param("afterId") Long afterId, @Param("limit") int limit);

    /** 发现页筛选项：可选年份（降序） */
    public List<Integer> selectDistinctYears();

    /** 发现页筛选项：可选播放平台 */
    public List<String> selectDistinctPlatforms();

    /** 发现页筛选项：可选国家/地区（原始 region 字符串，可能含逗号） */
    public List<String> selectDistinctRegions();

    /** 平台名 -> logo（每个平台取一张），供品牌图标展示 */
    public List<java.util.Map<String, Object>> selectPlatformLogos();

    /** 迁移用：存在本地（/profile）海报或剧照的影片 */
    public List<java.util.Map<String, Object>> selectLocalImageMedia();

    /** 我的片库：tab=wish/watched/rated/liked */
    public List<KmMedia> selectLibrary(@Param("userId") Long userId, @Param("tab") String tab);

    public int insertKmMedia(KmMedia kmMedia);

    public int updateKmMedia(KmMedia kmMedia);

    /** 刷新站内行为聚合冗余字段 */
    public int refreshAggregates(Long mediaId);

    /** 刷新综合热度分 */
    public int refreshHotScore(Long mediaId);

    public int deleteKmMediaByMediaIds(Long[] mediaIds);
}
