package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmDashboardOverview;
import com.ruoyi.kemovie.domain.KmMedia;

/**
 * 后台首页运营概览 Mapper
 *
 * @author kemovie
 */
public interface KmDashboardMapper
{
    /** 顶部指标汇总 */
    public KmDashboardOverview selectOverview();

    /** 最近入库影片（按 id 倒序） */
    public List<KmMedia> selectRecentMedia(@Param("limit") int limit);

    /** 即将上线影片（按最近排期升序） */
    public List<KmMedia> selectUpcomingMedia(@Param("limit") int limit);
}
