-- ----------------------------------------------------------------------------
-- 影弯增量迁移：JustWatch 多区域每小时上新 + release 来源列
-- 在已执行过 kemovie_wish.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) 上线平台表增加来源：tmdb(TMDB同步)/justwatch(上新抓取)/custom(运营自维护)
ALTER TABLE `km_media_release`
  ADD COLUMN `source` varchar(20) NOT NULL DEFAULT 'tmdb' COMMENT '来源 tmdb/justwatch/custom' AFTER `status`,
  ADD KEY `idx_media_source` (`media_id`, `source`);

-- 2) 每小时抓取 JustWatch 各区域最新上线（默认启用）
INSERT IGNORE INTO `sys_job`
  (`job_id`,`job_name`,`job_group`,`invoke_target`,`cron_expression`,`misfire_policy`,`concurrent`,`status`,`create_by`,`create_time`,`remark`)
VALUES
  (106,'JustWatch区域上新','KEMOVIE','kemovieTask.syncJustWatchNew()','0 20 * * * ?','0','1','0','admin',sysdate(),'每小时抓取 HK/TW/US 最新上线(配置 kemovie.justwatch.new-countries)');
