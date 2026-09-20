package com.ruoyi.kemovie.service;

import java.util.Map;

/**
 * 旧站数据导入 Service（读 tools/export_legacy.py 产出的 legacy_export.jsonl）
 *
 * @author kemovie
 */
public interface IKmLegacyImportService
{
    /**
     * 启动异步导入任务。
     *
     * @param filePath   服务器上的 jsonl 文件路径（由上传接口返回）
     * @param dryRun     true=只解析与匹配，不写任何库（用于试跑报告）
     * @param maxRows    最多处理行数，0=全部
     * @param posterBase 海报地址前缀（如 https://image.yingwans.com），空串则保留原始相对路径
     * @return 任务ID
     */
    public String startImport(String filePath, boolean dryRun, int maxRows, String posterBase);

    /** 查询任务进度（含各计数与失败/未匹配清单摘要） */
    public Map<String, Object> progress(String taskId);
}
