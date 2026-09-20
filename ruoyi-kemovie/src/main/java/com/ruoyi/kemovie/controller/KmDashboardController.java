package com.ruoyi.kemovie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.kemovie.service.IKmDashboardService;

/**
 * 后台首页运营概览
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/dashboard")
public class KmDashboardController extends BaseController
{
    private static final int CARD_LIMIT = 8;

    @Autowired private IKmDashboardService dashboardService;

    /**
     * 首页概览：指标汇总 + 最近入库 + 即将上线（登录后台即可访问）
     */
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("overview", dashboardService.selectOverview());
        ajax.put("recent", dashboardService.selectRecentMedia(CARD_LIMIT));
        ajax.put("upcoming", dashboardService.selectUpcomingMedia(CARD_LIMIT));
        return ajax;
    }
}
