package com.ruoyi.kemovie.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.kemovie.service.IKmHotRankService;
import com.ruoyi.kemovie.service.IKmJustWatchNewSyncService;
import com.ruoyi.kemovie.service.IKmJustWatchSyncService;
import com.ruoyi.kemovie.service.IKmLetterboxdSyncService;
import com.ruoyi.kemovie.service.IKmMdlSyncService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;
import com.ruoyi.kemovie.service.IKmNotificationService;

/**
 * 影弯定时任务，供 RuoYi Quartz 调度调用。
 * 在“系统监控 -> 定时任务”中以 kemovieTask.xxx() 形式配置。
 *
 * @author kemovie
 */
@Component("kemovieTask")
public class KemovieTask
{
    private static final Logger log = LoggerFactory.getLogger(KemovieTask.class);

    @Autowired private IKmMdlSyncService mdlSyncService;
    @Autowired private IKmJustWatchSyncService justWatchSyncService;
    @Autowired private IKmJustWatchNewSyncService justWatchNewSyncService;
    @Autowired private IKmLetterboxdSyncService letterboxdSyncService;
    @Autowired private IKmHotRankService hotRankService;
    @Autowired private IKmNotificationService notificationService;
    @Autowired private IKmMediaSyncService mediaSyncService;

    /** 定期抓取所有启用的 MDL 片单 */
    public void scrapeMdl()
    {
        log.info("[定时] 开始抓取 MDL 片单");
        String result = mdlSyncService.runAllEnabled();
        log.info("[定时] MDL 抓取结果: {}", result);
    }

    /** 定期抓取所有启用的 JustWatch 片单（按国家/播放平台） */
    public void scrapeJustWatch()
    {
        log.info("[定时] 开始抓取 JustWatch 片单");
        String result = justWatchSyncService.runAllEnabled();
        log.info("[定时] JustWatch 抓取结果: {}", result);
    }

    /** 定期抓取所有启用的 Letterboxd 片单 */
    public void scrapeLetterboxd()
    {
        log.info("[定时] 开始抓取 Letterboxd 片单");
        String result = letterboxdSyncService.runAllEnabled();
        log.info("[定时] Letterboxd 抓取结果: {}", result);
    }

    /** 重建热门排行榜（周榜 Top50） */
    public void rebuildHotRank()
    {
        int n = hotRankService.rebuild("week", 50);
        log.info("[定时] 热门榜重建完成，共 {} 条", n);
    }

    /** 派发上线提醒通知 */
    public void dispatchNotifications()
    {
        int sent = notificationService.dispatchReleaseNotifications();
        log.info("[定时] 上线提醒派发完成，共 {} 条", sent);
    }

    /** 刷新上线排期（含未来上线日期），默认单次处理 100 部 */
    public void refreshReleases()
    {
        int n = mediaSyncService.refreshUpcomingReleases(100);
        log.info("[定时] 上线排期刷新完成，共处理 {} 部", n);
    }

    /** 刷新上线排期（自定义单次处理数量） */
    public void refreshReleases(int limit)
    {
        int n = mediaSyncService.refreshUpcomingReleases(limit);
        log.info("[定时] 上线排期刷新完成，共处理 {} 部", n);
    }

    /** 一次性全量回填所有影片的上线排期/上线日期 */
    public void backfillReleases()
    {
        int n = mediaSyncService.backfillAllReleases();
        log.info("[定时] 全量回填上线排期完成，共处理 {} 部", n);
    }

    /** 每小时抓取 JustWatch 各区域最新上线（配置 kemovie.justwatch.new-countries） */
    public void syncJustWatchNew()
    {
        log.info("[定时] 开始抓取 JustWatch 区域上新");
        String result = justWatchNewSyncService.syncAllCountries();
        log.info("[定时] JustWatch 区域上新结果: {}", result);
    }
}
