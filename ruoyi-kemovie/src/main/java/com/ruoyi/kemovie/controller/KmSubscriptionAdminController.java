package com.ruoyi.kemovie.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.kemovie.mapper.KmUserSubscriptionMapper;

/**
 * 后台订阅管理：订阅动态 + 订阅汇总（只读）
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/subscription")
public class KmSubscriptionAdminController extends BaseController
{
    @Autowired private KmUserSubscriptionMapper subscriptionMapper;

    /** 订阅汇总：按影片统计订阅人数 */
    @PreAuthorize("@ss.hasPermi('kemovie:subscription:query')")
    @GetMapping("/summary")
    public TableDataInfo summary()
    {
        startPage();
        List<Map<String, Object>> list = subscriptionMapper.selectSummary();
        return getDataTable(list);
    }

    /** 订阅动态：最近的订阅/取消事件 */
    @PreAuthorize("@ss.hasPermi('kemovie:subscription:query')")
    @GetMapping("/events")
    public TableDataInfo events()
    {
        startPage();
        List<Map<String, Object>> list = subscriptionMapper.selectEvents();
        return getDataTable(list);
    }
}
