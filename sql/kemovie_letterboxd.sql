-- ----------------------------------------------------------------------------
-- 影弯增量迁移：片单抓取源支持 Letterboxd
-- 在已执行过 kemovie_justwatch.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) 影片表新增 letterboxd_slug：片单条目的本地映射，跨片单/重跑秒级复用
ALTER TABLE `km_media`
  ADD COLUMN `letterboxd_slug` varchar(100) DEFAULT NULL
    COMMENT 'Letterboxd 影片 slug(如 city-of-god)' AFTER `mdl_id`,
  ADD KEY `idx_letterboxd` (`letterboxd_slug`);

-- 2) 抓取源表注释更新（支持 mdl/justwatch/letterboxd）
ALTER TABLE `km_mdl_source`
  MODIFY COLUMN `source_type` varchar(20) NOT NULL DEFAULT 'mdl'
    COMMENT '来源类型 mdl/justwatch/letterboxd';

-- 3) 新增 Letterboxd 定时任务（默认停用，配置抓取源后再启用）
INSERT IGNORE INTO `sys_job`
  (`job_id`,`job_name`,`job_group`,`invoke_target`,`cron_expression`,`misfire_policy`,`concurrent`,`status`,`create_by`,`create_time`,`remark`)
VALUES
  (105, '抓取Letterboxd片单', 'KEMOVIE', 'kemovieTask.scrapeLetterboxd()', '0 40 3 * * ?', '3', '1', '1', 'admin', NOW(),
   '无头 Chrome 抓取 Letterboxd 片单，slug 映射+标题匹配+影片页精确 tmdbId 入库');
