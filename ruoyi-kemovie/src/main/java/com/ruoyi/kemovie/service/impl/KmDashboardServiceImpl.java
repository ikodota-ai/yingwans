package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.kemovie.domain.KmDashboardOverview;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmDashboardMapper;
import com.ruoyi.kemovie.service.IKmDashboardService;

/**
 * 后台首页运营概览 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmDashboardServiceImpl implements IKmDashboardService
{
    @Autowired private KmDashboardMapper dashboardMapper;

    @Override
    public KmDashboardOverview selectOverview() { return dashboardMapper.selectOverview(); }

    @Override
    public List<KmMedia> selectRecentMedia(int limit) { return dashboardMapper.selectRecentMedia(limit); }

    @Override
    public List<KmMedia> selectUpcomingMedia(int limit) { return dashboardMapper.selectUpcomingMedia(limit); }
}
