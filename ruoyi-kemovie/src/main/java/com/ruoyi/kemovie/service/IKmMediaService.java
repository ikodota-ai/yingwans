package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmMedia;

/**
 * 影视条目 Service 接口
 *
 * @author kemovie
 */
public interface IKmMediaService
{
    public KmMedia selectKmMediaByMediaId(Long mediaId);

    /** 详情（含平台、当前用户行为、订阅态） */
    public KmMedia selectDetail(Long mediaId, Long userId);

    public List<KmMedia> selectKmMediaList(KmMedia kmMedia);

    public List<KmMedia> selectPublicMediaList(KmMedia kmMedia);

    public List<KmMedia> selectRecentReleases(int limit);

    /** 最近收录（按本站收录时间倒序） */
    public List<KmMedia> selectRecentCollected(int limit);

    /** JustWatch 最近上新（影片+区域聚合） */
    public List<com.ruoyi.kemovie.domain.vo.KmNewRelease> selectNewReleases(String country, int days, int limit);

    /** 发现页筛选项：年份列表 */
    public List<Integer> selectDistinctYears();

    /** 发现页筛选项：平台列表 */
    public List<String> selectDistinctPlatforms();

    /** 发现页筛选项：可选国家/地区（原始 region 字符串） */
    public List<String> selectDistinctRegions();

    /** 平台名 -> logo，供品牌图标展示 */
    public List<java.util.Map<String, Object>> selectPlatformLogos();

    public List<KmMedia> selectLibrary(Long userId, String tab);

    public int insertKmMedia(KmMedia kmMedia);

    public int updateKmMedia(KmMedia kmMedia);

    public int deleteKmMediaByMediaIds(Long[] mediaIds);

    /** 刷新单片聚合与热度 */
    public void refreshMediaStats(Long mediaId);
}
