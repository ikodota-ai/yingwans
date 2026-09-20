package com.ruoyi.kemovie.service;

import com.ruoyi.kemovie.domain.KmMdlSource;

/**
 * JustWatch 片单抓取与落地 Service
 *
 * @author kemovie
 */
public interface IKmJustWatchSyncService
{
    /** 抓取单个源（按平台/国家分页、去重），生成/更新对应片单，返回结果摘要 */
    public String runSource(KmMdlSource source);

    /** 抓取所有启用的 JustWatch 源 */
    public String runAllEnabled();
}
