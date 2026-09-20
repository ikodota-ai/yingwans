-- ----------------------------------------------------------------------------
-- 影弯增量迁移：旧站（ywtrzm）数据导入
-- 在已执行过 kemovie_letterboxd.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) 影片表：旧站锚点 / 题材标签 / 数据来源
ALTER TABLE `km_media`
  ADD COLUMN `legacy_aid` bigint(20) DEFAULT NULL
    COMMENT '旧站 mod_article.aid，导入幂等锚点' AFTER `letterboxd_slug`,
  ADD KEY `idx_legacy_aid` (`legacy_aid`);
ALTER TABLE `km_media`
  ADD COLUMN `tags` varchar(500) DEFAULT NULL
    COMMENT '题材标签(逗号分隔)：百合/男男纯爱/双性恋/跨性别/酷儿/异性恋/其他+补充' AFTER `genres`,
  ADD COLUMN `source` varchar(20) DEFAULT 'tmdb'
    COMMENT '数据来源 tmdb/mdl/justwatch/letterboxd/legacy' AFTER `tags`;

-- 2) 影片资源表：旧站网盘资源 + 后续运营维护的下载资源
--    仅供后台与第三方文档(飞书等)生成使用，任何前台接口不得返回
DROP TABLE IF EXISTS `km_media_resource`;
CREATE TABLE `km_media_resource` (
  `resource_id`  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `media_id`     bigint(20)   NOT NULL COMMENT '影片ID',
  `group_name`   varchar(100) DEFAULT NULL COMMENT '资源组名(如 第一季/S1)',
  `platform`     varchar(20)  NOT NULL COMMENT '网盘平台 baidu/quark/aliyun/115/uc/xunlei/other',
  `url`          varchar(500) NOT NULL COMMENT '分享链接',
  `pwd`          varchar(50)  DEFAULT NULL COMMENT '提取码',
  `group_order`  int(11)      NOT NULL DEFAULT 0 COMMENT '组内排序',
  `source`       varchar(20)  NOT NULL DEFAULT 'legacy' COMMENT '来源 legacy/manual',
  `create_by`    varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`resource_id`),
  KEY `idx_media` (`media_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='影片下载资源(不对前台暴露)';

-- 3) 后台菜单：旧站导入（影弯管理 2000 下）
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2007,'旧站导入',2000,7,'legacy','kemovie/legacy/index',1,0,'C','0','0','kemovie:legacy:list','upload','admin',sysdate(),'旧站数据迁移');
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2071,'导入查询',2007,1,'','',1,0,'F','0','0','kemovie:legacy:query','#','admin',sysdate(),''),
  (2072,'执行导入',2007,2,'','',1,0,'F','0','0','kemovie:legacy:import','#','admin',sysdate(),'');
