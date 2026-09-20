-- ----------------------------------------------------------------------------
-- 影弯增量迁移：片单抓取源支持 JustWatch
-- 在已部署的库上执行一次（幂等性说明见各条注释）
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) 抓取源表新增来源类型 / 影片类型列
ALTER TABLE `km_mdl_source`
  ADD COLUMN `source_type` varchar(20) NOT NULL DEFAULT 'mdl'
    COMMENT '来源类型 mdl/justwatch' AFTER `source_id`,
  ADD COLUMN `object_type` varchar(10) DEFAULT NULL
    COMMENT 'JustWatch 影片类型 movie/tv(NULL=全部)' AFTER `region_tag`,
  MODIFY COLUMN `list_id` varchar(100) DEFAULT NULL
    COMMENT 'MDL list id / JustWatch 平台 slug(留空=全国热门)';

-- 2) 唯一键改为 (来源类型, list_id, 国家)，允许同平台多国家 & 全国热门(NULL)
ALTER TABLE `km_mdl_source` DROP INDEX `uk_list`;
ALTER TABLE `km_mdl_source`
  ADD UNIQUE KEY `uk_type_list` (`source_type`,`list_id`,`region_tag`);

-- 历史数据兜底为 mdl（默认值已覆盖，保险起见再刷一次）
UPDATE `km_mdl_source` SET `source_type` = 'mdl' WHERE `source_type` IS NULL OR `source_type` = '';

-- 3) 后台菜单改名（权限标识保持 kemovie:mdl:* 不变，无需重新授权）
UPDATE `sys_menu` SET `menu_name` = '片单抓取源',
       `remark` = 'MyDramaList / JustWatch 抓取源配置'
 WHERE `menu_id` = 2003;

-- 4) 新增 JustWatch 定时任务（默认停用，配置抓取源后再到“定时任务”里启用）
INSERT IGNORE INTO `sys_job`
  (`job_id`,`job_name`,`job_group`,`invoke_target`,`cron_expression`,`misfire_policy`,`concurrent`,`status`,`create_by`,`create_time`,`remark`)
VALUES
  (104, '抓取JustWatch片单', 'KEMOVIE', 'kemovieTask.scrapeJustWatch()', '0 30 3 * * ?', '3', '1', '1', 'admin', NOW(),
   '按国家/播放平台抓取 JustWatch 片单，依 tmdbId 精确入库并关联片单');
