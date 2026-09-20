package com.ruoyi.kemovie.service;

/**
 * JustWatch 区域上新同步：按国家抓取 JustWatch “最新上线” 片单（含观看平台/方式/链接），
 * 影片不在库时自动从 TMDB 同步入库，观看方式写入 km_media_release（source=justwatch）。
 *
 * @author kemovie
 */
public interface IKmJustWatchNewSyncService
{
    /** 抓取配置中的所有区域（kemovie.justwatch.new-countries），返回各区域汇总 */
    public String syncAllCountries();

    /** 抓取单个区域（ISO 两位大写，如 HK/TW/US），返回汇总 */
    public String syncCountry(String country);
}
