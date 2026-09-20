package com.ruoyi.kemovie.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.service.IKmLegacyImportService;

/**
 * 旧站数据导入（legacy_export.jsonl 上传 + 异步导入 + 进度查询）
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/legacy")
public class KmLegacyImportController extends BaseController
{
    @Autowired private IKmLegacyImportService legacyImportService;

    /** 上传导出文件，返回服务器端路径 */
    @PreAuthorize("@ss.hasPermi('kemovie:legacy:import')")
    @Log(title = "旧站导入", businessType = BusinessType.IMPORT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) throws Exception
    {
        if (file == null || file.isEmpty())
        {
            return error("请选择文件");
        }
        String name = StringUtils.trimToEmpty(file.getOriginalFilename()).toLowerCase();
        if (!(name.endsWith(".jsonl") || name.endsWith(".json") || name.endsWith(".txt")))
        {
            return error("仅支持 .jsonl 导出文件");
        }
        File dir = new File(RuoYiConfig.getProfile() + "/kemovie/legacy");
        if (!dir.exists() && !dir.mkdirs())
        {
            return error("无法创建上传目录");
        }
        File dest = new File(dir, "legacy-" + UUID.randomUUID().toString().replace("-", "") + ".jsonl");
        file.transferTo(dest);
        Map<String, Object> data = new HashMap<>();
        data.put("filePath", dest.getAbsolutePath());
        data.put("size", dest.length());
        return success(data);
    }

    /** 启动导入（dryRun=true 试跑不写库） */
    @PreAuthorize("@ss.hasPermi('kemovie:legacy:import')")
    @Log(title = "旧站导入", businessType = BusinessType.IMPORT)
    @PostMapping("/start")
    public AjaxResult start(@RequestBody Map<String, Object> body)
    {
        String filePath = StringUtils.trimToNull((String) body.get("filePath"));
        if (filePath == null)
        {
            return error("缺少 filePath");
        }
        boolean dryRun = Boolean.parseBoolean(String.valueOf(body.getOrDefault("dryRun", "true")));
        int maxRows = 0;
        try { maxRows = Integer.parseInt(String.valueOf(body.getOrDefault("maxRows", "0"))); }
        catch (NumberFormatException ignore) { }
        String posterBase = StringUtils.trimToEmpty((String) body.get("posterBase"));
        try
        {
            String taskId = legacyImportService.startImport(filePath, dryRun, maxRows, posterBase);
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            return success(data);
        }
        catch (IllegalArgumentException e)
        {
            return error(e.getMessage());
        }
    }

    /** 进度与结果（含失败/未匹配清单） */
    @PreAuthorize("@ss.hasPermi('kemovie:legacy:query')")
    @GetMapping("/progress/{taskId}")
    public AjaxResult progress(@PathVariable String taskId)
    {
        Map<String, Object> p = legacyImportService.progress(taskId);
        return p != null ? success(p) : error("任务不存在");
    }
}
