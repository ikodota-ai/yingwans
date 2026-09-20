package com.ruoyi.kemovie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.kemovie.domain.KmPerson;
import com.ruoyi.kemovie.service.IKmPersonService;

/**
 * 演职人员 后台管理（列表/详情/刷新/删除）。
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/person")
public class KmPersonAdminController extends BaseController
{
    @Autowired private IKmPersonService personService;

    @PreAuthorize("@ss.hasPermi('kemovie:person:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmPerson kmPerson)
    {
        startPage();
        return getDataTable(personService.selectPersonList(kmPerson));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:person:query')")
    @GetMapping("/{personId}")
    public AjaxResult getInfo(@PathVariable Long personId)
    {
        return success(personService.selectDetail(personId));
    }

    /** 从 TMDB 刷新人物档案 */
    @PreAuthorize("@ss.hasPermi('kemovie:person:edit')")
    @Log(title = "人物TMDB刷新", businessType = BusinessType.OTHER)
    @PostMapping("/{personId}/refresh")
    public AjaxResult refresh(@PathVariable Long personId)
    {
        KmPerson p = personService.refreshPerson(personId);
        return p == null ? error("刷新失败，请检查 TMDB 连接") : success(p);
    }

    @PreAuthorize("@ss.hasPermi('kemovie:person:remove')")
    @Log(title = "演职人员", businessType = BusinessType.DELETE)
    @DeleteMapping("/{personIds}")
    public AjaxResult remove(@PathVariable Long[] personIds)
    {
        return toAjax(personService.deletePersonByIds(personIds));
    }
}
