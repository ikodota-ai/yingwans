package com.ruoyi.kemovie.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.kemovie.domain.KmProvider;
import com.ruoyi.kemovie.service.IKmProviderService;

/**
 * 自定义播放服务商 后台管理
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/provider")
public class KmProviderController extends BaseController
{
    @Autowired private IKmProviderService providerService;

    @PreAuthorize("@ss.hasPermi('kemovie:provider:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmProvider kmProvider)
    {
        startPage();
        return getDataTable(providerService.selectKmProviderList(kmProvider));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:provider:query')")
    @GetMapping("/{providerId}")
    public AjaxResult getInfo(@PathVariable Long providerId)
    {
        return success(providerService.selectKmProviderById(providerId));
    }

    /** 全部启用的服务商（影片编辑勾选框用，媒体编辑权限即可访问） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:query')")
    @GetMapping("/enabled")
    public AjaxResult enabled()
    {
        return success(providerService.selectEnabled());
    }

    /** 影片已勾选的服务商 ID 列表（媒体编辑权限即可访问） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:query')")
    @GetMapping("/media/{mediaId}")
    public AjaxResult mediaProviderIds(@PathVariable Long mediaId)
    {
        return success(providerService.selectMediaProviderIds(mediaId));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:provider:add')")
    @Log(title = "服务商", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KmProvider kmProvider)
    {
        kmProvider.setCreateBy(SecurityUtils.getUsername());
        return toAjax(providerService.insertKmProvider(kmProvider));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:provider:edit')")
    @Log(title = "服务商", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KmProvider kmProvider)
    {
        kmProvider.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(providerService.updateKmProvider(kmProvider));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:provider:remove')")
    @Log(title = "服务商", businessType = BusinessType.DELETE)
    @DeleteMapping("/{providerIds}")
    public AjaxResult remove(@PathVariable Long[] providerIds)
    {
        return toAjax(providerService.deleteKmProviderByIds(providerIds));
    }
}
