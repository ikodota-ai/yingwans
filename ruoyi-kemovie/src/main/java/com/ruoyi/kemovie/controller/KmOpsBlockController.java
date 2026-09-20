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
import com.ruoyi.kemovie.domain.KmOpsBlock;
import com.ruoyi.kemovie.service.IKmOpsBlockService;

/**
 * 运营位 后台管理（私域二维码/公告等，工作人员编辑）
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/opsBlock")
public class KmOpsBlockController extends BaseController
{
    @Autowired private IKmOpsBlockService opsBlockService;

    @PreAuthorize("@ss.hasPermi('kemovie:ops:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmOpsBlock kmOpsBlock)
    {
        startPage();
        return getDataTable(opsBlockService.selectKmOpsBlockList(kmOpsBlock));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:ops:query')")
    @GetMapping("/{blockId}")
    public AjaxResult getInfo(@PathVariable Long blockId)
    {
        return success(opsBlockService.selectKmOpsBlockById(blockId));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:ops:add')")
    @Log(title = "运营位", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KmOpsBlock kmOpsBlock)
    {
        kmOpsBlock.setCreateBy(SecurityUtils.getUsername());
        return toAjax(opsBlockService.insertKmOpsBlock(kmOpsBlock));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:ops:edit')")
    @Log(title = "运营位", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KmOpsBlock kmOpsBlock)
    {
        kmOpsBlock.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(opsBlockService.updateKmOpsBlock(kmOpsBlock));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:ops:remove')")
    @Log(title = "运营位", businessType = BusinessType.DELETE)
    @DeleteMapping("/{blockIds}")
    public AjaxResult remove(@PathVariable Long[] blockIds)
    {
        return toAjax(opsBlockService.deleteKmOpsBlockByIds(blockIds));
    }
}
