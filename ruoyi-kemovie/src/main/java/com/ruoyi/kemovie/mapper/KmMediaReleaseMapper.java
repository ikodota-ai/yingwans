package com.ruoyi.kemovie.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmMediaRelease;

/**
 * 影视上线平台 Mapper
 *
 * @author kemovie
 */
public interface KmMediaReleaseMapper
{
    public List<KmMediaRelease> selectByMediaId(Long mediaId);

    public int insertKmMediaRelease(KmMediaRelease release);

    /** JustWatch 上新写入（唯一键冲突时忽略，返回受影响行数 0/1） */
    public int insertJwReleaseIgnore(KmMediaRelease release);

    /** 仅删除 TMDB 同步产生的记录（保留 justwatch 抓取与运营自维护的记录） */
    public int deleteByMediaId(Long mediaId);

    /** 判断 JustWatch 上新记录是否已存在（自然键去重） */
    public int existsJwRelease(@Param("mediaId") Long mediaId, @Param("platform") String platform,
            @Param("country") String country, @Param("offerType") String offerType,
            @Param("onlineDate") Date onlineDate);

    /** 删除影片全部自维护服务商勾选（source=custom） */
    public int deleteCustomByMediaId(Long mediaId);

    /** 删除某平台全部自维护勾选（服务商被删除时调用） */
    public int deleteCustomByPlatform(String platform);

    /** 服务商改名时同步影片展示的平台名 */
    public int updateCustomPlatformName(@Param("oldName") String oldName, @Param("newName") String newName);

    /** 服务商 Logo 变更时同步影片展示 */
    public int updateCustomPlatformLogo(@Param("platform") String platform, @Param("logo") String logo);

    /** 服务商官网变更时同步影片直达链接 */
    public int updateCustomPlatformLink(@Param("platform") String platform, @Param("link") String link);

    /** 查询某平台已本地化的 logo（/profile 开头），无则返回 null */
    public String selectLocalLogoByPlatform(String platform);

    /** 将某平台所有记录的 logo 更新为指定（本地）地址 */
    public int updatePlatformLogoByPlatform(@Param("platform") String platform, @Param("logo") String logo);
}
