package com.ruoyi.kemovie.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.kemovie.domain.vo.KmWishMedia;
import com.ruoyi.kemovie.domain.vo.KmWishUser;
import com.ruoyi.kemovie.mapper.KmWishAdminMapper;

/**
 * 想看运营视图（按影片聚合 + 想看用户明细）
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/wish")
public class KmWishAdminController extends BaseController
{
    @Autowired private KmWishAdminMapper wishAdminMapper;

    @PreAuthorize("@ss.hasPermi('kemovie:wish:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmWishMedia query)
    {
        startPage();
        List<KmWishMedia> list = wishAdminMapper.selectWishMediaList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('kemovie:wish:list')")
    @GetMapping("/users/{mediaId}")
    public AjaxResult users(@PathVariable Long mediaId)
    {
        List<KmWishUser> users = wishAdminMapper.selectWishUsers(mediaId);
        return success(users);
    }
}
