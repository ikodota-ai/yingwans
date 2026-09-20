-- ----------------------------------------------------------------------------
-- 影弯增量迁移：自定义播放服务商（km_provider + 菜单）
-- 在已执行过 kemovie_jw_new.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) 自定义播放服务商（运营自维护，勾选后在影片上标记“可观看”）
CREATE TABLE IF NOT EXISTS `km_provider` (
  `provider_id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务商ID',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `slug` varchar(50) DEFAULT NULL COMMENT '标识（可选）',
  `site_url` varchar(500) DEFAULT NULL COMMENT '官网地址',
  `logo_url` varchar(500) DEFAULT NULL COMMENT 'Logo地址',
  `region` varchar(10) DEFAULT NULL COMMENT '主要区域（如 TW/HK/US）',
  `source` varchar(20) NOT NULL DEFAULT 'custom' COMMENT '来源 custom/justwatch',
  `enabled` char(1) NOT NULL DEFAULT '0' COMMENT '启用（0是 1否）',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序（小在前）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`provider_id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义播放服务商';

-- 2) 预置四个服务商
INSERT IGNORE INTO `km_provider`
  (`provider_id`,`name`,`slug`,`site_url`,`region`,`source`,`enabled`,`sort`,`create_by`,`create_time`,`remark`)
VALUES
  (1,'GagaOOLala','gagaoolala','https://www.gagaoolala.com/en/home','TW','custom','0',1,'admin',sysdate(),'同志影音平台'),
  (2,'LINE TV','linetv','https://www.linetv.tw/','TW','custom','0',2,'admin',sysdate(),''),
  (3,'HamiVideo','hamivideo','https://hamivideo.hinet.net/index.do','TW','custom','0',3,'admin',sysdate(),''),
  (4,'WeTV','wetv','https://wetv.vip/zh-tw','TW','custom','0',4,'admin',sysdate(),'腾讯视频海外版');

-- 3) 菜单：服务商管理 + 按钮
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2009,'服务商管理',2000,9,'provider','kemovie/provider/index',1,0,'C','0','0','kemovie:provider:list','link','admin',sysdate(),'自定义播放服务商维护');
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2091,'服务商查询',2009,1,'','',1,0,'F','0','0','kemovie:provider:query','#','admin',sysdate(),''),
  (2092,'服务商新增',2009,2,'','',1,0,'F','0','0','kemovie:provider:add','#','admin',sysdate(),''),
  (2093,'服务商修改',2009,3,'','',1,0,'F','0','0','kemovie:provider:edit','#','admin',sysdate(),''),
  (2094,'服务商删除',2009,4,'','',1,0,'F','0','0','kemovie:provider:remove','#','admin',sysdate(),'');

-- 4) 清理 JustWatch 早期重复行（保留每组自然键最小 release_id）
DELETE r FROM `km_media_release` r
JOIN (
  SELECT media_id, platform, country, offer_type, online_date, MIN(release_id) keep_id
  FROM `km_media_release` WHERE source = 'justwatch'
  GROUP BY media_id, platform, country, offer_type, online_date
) k ON r.media_id = k.media_id AND r.platform = k.platform
   AND r.country <=> k.country AND r.offer_type <=> k.offer_type AND r.online_date <=> k.online_date
   AND r.release_id > k.keep_id
WHERE r.source = 'justwatch';

-- 5) 自然键唯一索引（防止各来源重复写入；custom 行 online_date 为 NULL 不冲突）
ALTER TABLE `km_media_release`
  ADD UNIQUE KEY `uk_release_natural` (`media_id`, `source`, `platform`, `country`, `offer_type`, `online_date`);
