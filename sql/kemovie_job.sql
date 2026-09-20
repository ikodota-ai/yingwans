-- 影弯定时任务（RuoYi Quartz）
-- 抓取 MDL 片单：每天凌晨 3 点
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (100, '抓取MDL片单', 'KEMOVIE', 'kemovieTask.scrapeMdl()', '0 0 3 * * ?', '3', '1', '1', 'admin', NOW(), '定期抓取启用的 MDL 片单并落地/去重');

-- 重建热门榜：每小时
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (101, '重建热门榜', 'KEMOVIE', 'kemovieTask.rebuildHotRank()', '0 0 * * * ?', '3', '1', '0', 'admin', NOW(), '按 hot_score 重建周榜 Top50 快照');

-- 派发上线提醒：每天早 9 点
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (102, '上线提醒派发', 'KEMOVIE', 'kemovieTask.dispatchNotifications()', '0 0 9 * * ?', '3', '1', '0', 'admin', NOW(), '为已订阅且上线的影片派发站内信+邮件提醒');

-- 刷新上线排期（含未来上线日期）：每天凌晨 4 点，单次处理 100 部
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (103, '刷新上线排期', 'KEMOVIE', 'kemovieTask.refreshReleases()', '0 0 4 * * ?', '3', '1', '0', 'admin', NOW(), '刷新未来/近期/被订阅影片的上线平台与 online_date，驱动即将上线与订阅提醒');

-- 抓取 JustWatch 片单：每天凌晨 3:30（默认停用，配置抓取源后再启用）
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (104, '抓取JustWatch片单', 'KEMOVIE', 'kemovieTask.scrapeJustWatch()', '0 30 3 * * ?', '3', '1', '1', 'admin', NOW(), '按国家/播放平台抓取 JustWatch 片单，依 tmdbId 精确入库并关联片单');

-- 抓取 Letterboxd 片单：每天凌晨 3:40（默认停用，配置抓取源后再启用）
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
VALUES (105, '抓取Letterboxd片单', 'KEMOVIE', 'kemovieTask.scrapeLetterboxd()', '0 40 3 * * ?', '3', '1', '1', 'admin', NOW(), '无头 Chrome 抓取 Letterboxd 片单，slug 映射+标题匹配+影片页精确 tmdbId 入库');
