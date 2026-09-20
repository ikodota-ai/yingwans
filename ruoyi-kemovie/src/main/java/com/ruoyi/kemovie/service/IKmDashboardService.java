package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmDashboardOverview;
import com.ruoyi.kemovie.domain.KmMedia;

/**
 * 后台首页运营概览 Service
 *
 * @author kemovie
 */
public interface IKmDashboardService
{
    /** 顶部指标汇总 */
    public KmDashboardOverview selectOverview();

    /** 最近入库影片 */
    public List<KmMedia> selectRecentMedia(int limit);

    /** 即将上线影片 */
    public List<KmMedia> selectUpcomingMedia(int limit);
}
