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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.kemovie.client.JustWatchClient;
import com.ruoyi.kemovie.domain.KmMdlSource;
import com.ruoyi.kemovie.service.IKmLetterboxdSyncService;
import com.ruoyi.kemovie.service.IKmJustWatchSyncService;
import com.ruoyi.kemovie.service.IKmMdlSourceService;
import com.ruoyi.kemovie.service.IKmMdlSyncService;

/**
 * MDL 抓取源 后台管理
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/mdlSource")
public class KmMdlSourceController extends BaseController
{
    @Autowired private IKmMdlSourceService sourceService;
    @Autowired private IKmMdlSyncService mdlSyncService;
    @Autowired private IKmJustWatchSyncService justWatchSyncService;
    @Autowired private IKmLetterboxdSyncService letterboxdSyncService;
    @Autowired private JustWatchClient justWatchClient;

    @PreAuthorize("@ss.hasPermi('kemovie:mdl:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmMdlSource kmMdlSource)
    {
        startPage();
        return getDataTable(sourceService.selectKmMdlSourceList(kmMdlSource));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:mdl:query')")
    @GetMapping("/{sourceId}")
    public AjaxResult getInfo(@PathVariable Long sourceId)
    {
        return success(sourceService.selectKmMdlSourceById(sourceId));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:mdl:add')")
    @Log(title = "MDL抓取源", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KmMdlSource kmMdlSource)
    {
        kmMdlSource.setCreateBy(SecurityUtils.getUsername());
        return toAjax(sourceService.insertKmMdlSource(kmMdlSource));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:mdl:edit')")
    @Log(title = "MDL抓取源", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KmMdlSource kmMdlSource)
    {
        return toAjax(sourceService.updateKmMdlSource(kmMdlSource));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:mdl:remove')")
    @Log(title = "MDL抓取源", businessType = BusinessType.DELETE)
    @DeleteMapping("/{sourceIds}")
    public AjaxResult remove(@PathVariable Long[] sourceIds)
    {
        return toAjax(sourceService.deleteKmMdlSourceByIds(sourceIds));
    }

    /** 手动立即抓取单个源 */
    @PreAuthorize("@ss.hasPermi('kemovie:mdl:run')")
    @Log(title = "MDL抓取执行", businessType = BusinessType.OTHER)
    @PostMapping("/run/{sourceId}")
    public AjaxResult run(@PathVariable Long sourceId)
    {
        KmMdlSource s = sourceService.selectKmMdlSourceById(sourceId);
        if (s == null) { return error("抓取源不存在"); }
        String summary;
        if ("justwatch".equalsIgnoreCase(s.getSourceType()))
        {
            summary = justWatchSyncService.runSource(s);
        }
        else if ("letterboxd".equalsIgnoreCase(s.getSourceType()))
        {
            summary = letterboxdSyncService.runSource(s);
        }
        else
        {
            summary = mdlSyncService.runSource(s);
        }
        return success(summary);
    }

    /** 抓取全部启用源 */
    @PreAuthorize("@ss.hasPermi('kemovie:mdl:run')")
    @Log(title = "MDL抓取执行(全部)", businessType = BusinessType.OTHER)
    @PostMapping("/runAll")
    public AjaxResult runAll()
    {
        String result = mdlSyncService.runAllEnabled() + "\n"
                + justWatchSyncService.runAllEnabled() + "\n"
                + letterboxdSyncService.runAllEnabled();
        return success(result.trim());
    }

    /** JustWatch 指定国家可用的播放平台目录（供前端下拉选择） */
    @PreAuthorize("@ss.hasPermi('kemovie:mdl:list')")
    @GetMapping("/justwatchPackages")
    public AjaxResult justWatchPackages(@RequestParam(defaultValue = "US") String country)
    {
        return success(justWatchClient.packages(country));
    }
}
