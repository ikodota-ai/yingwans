package com.ruoyi.kemovie.controller;

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
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.service.IKmCollectionService;

/**
 * 片单 后台管理
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/collection")
public class KmCollectionAdminController extends BaseController
{
    @Autowired private IKmCollectionService collectionService;

    @PreAuthorize("@ss.hasPermi('kemovie:collection:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmCollection kmCollection)
    {
        startPage();
        return getDataTable(collectionService.selectKmCollectionList(kmCollection));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:collection:query')")
    @GetMapping("/{collectionId}")
    public AjaxResult getInfo(@PathVariable Long collectionId)
    {
        AjaxResult ajax = success(collectionService.selectDetail(collectionId));
        ajax.put("items", collectionService.selectMediaOfCollection(collectionId));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('kemovie:collection:add')")
    @Log(title = "片单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KmCollection kmCollection)
    {
        kmCollection.setCreateBy(SecurityUtils.getUsername());
        if (kmCollection.getSource() == null) { kmCollection.setSource("system"); }
        return toAjax(collectionService.insertKmCollection(kmCollection));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:collection:edit')")
    @Log(title = "片单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KmCollection kmCollection)
    {
        kmCollection.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(collectionService.updateKmCollection(kmCollection));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:collection:remove')")
    @Log(title = "片单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{collectionIds}")
    public AjaxResult remove(@PathVariable Long[] collectionIds)
    {
        return toAjax(collectionService.deleteKmCollectionByIds(collectionIds));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:collection:edit')")
    @PostMapping("/{collectionId}/media/{mediaId}")
    public AjaxResult addMedia(@PathVariable Long collectionId, @PathVariable Long mediaId)
    {
        collectionService.addMedia(collectionId, mediaId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('kemovie:collection:edit')")
    @DeleteMapping("/{collectionId}/media/{mediaId}")
    public AjaxResult removeMedia(@PathVariable Long collectionId, @PathVariable Long mediaId)
    {
        collectionService.removeMedia(collectionId, mediaId);
        return success();
    }
}
