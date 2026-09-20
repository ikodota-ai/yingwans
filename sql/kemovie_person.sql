-- ----------------------------------------------------------------------------
-- 影弯 Kemovie 演职人员表（来源 TMDB /person、/{media}/credits）
-- km_person：人物档案；km_media_cast：影视-人物关联（演员/导演/编剧等）
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `km_person`;
CREATE TABLE `km_person` (
  `person_id`      bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '内部主键',
  `tmdb_id`        bigint(20)    NOT NULL COMMENT 'TMDB person id',
  `imdb_id`        varchar(20)   DEFAULT NULL COMMENT 'IMDB id',
  `name`           varchar(255)  NOT NULL DEFAULT '' COMMENT '姓名(中文优先)',
  `original_name`  varchar(255)  DEFAULT NULL COMMENT '原名',
  `also_known_as`  varchar(1000) DEFAULT NULL COMMENT '别名(逗号分隔)',
  `gender`         tinyint(4)    DEFAULT 0 COMMENT '性别(0未知 1女 2男)',
  `birthday`       date          DEFAULT NULL COMMENT '出生日期',
  `deathday`       date          DEFAULT NULL COMMENT '离世日期',
  `place_of_birth` varchar(255)  DEFAULT NULL COMMENT '出生地',
  `known_for`      varchar(50)   DEFAULT NULL COMMENT '主要领域(Acting/Directing...)',
  `biography`      text          COMMENT '简介',
  `profile_path`   varchar(255)  DEFAULT NULL COMMENT 'TMDB 头像相对路径',
  `profile_local_url` varchar(500) DEFAULT NULL COMMENT '本地/CDN 头像(优先)',
  `popularity`     decimal(10,3) DEFAULT 0.000 COMMENT 'TMDB 热度',
  `homepage`       varchar(500)  DEFAULT NULL COMMENT '主页',
  `last_synced_at` datetime      DEFAULT NULL COMMENT '最后同步时间',
  `del_flag`       char(1)       DEFAULT '0' COMMENT '删除标志',
  `create_by`      varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`    datetime      DEFAULT NULL COMMENT '创建时间',
  `update_by`      varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`    datetime      DEFAULT NULL COMMENT '更新时间',
  `remark`         varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`person_id`),
  UNIQUE KEY `uk_tmdb` (`tmdb_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='演职人员';

DROP TABLE IF EXISTS `km_media_cast`;
CREATE TABLE `km_media_cast` (
  `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `media_id`     bigint(20)   NOT NULL COMMENT '影视ID',
  `person_id`    bigint(20)   NOT NULL COMMENT '人物ID',
  `credit_type`  varchar(10)  NOT NULL DEFAULT 'cast' COMMENT '类型 cast演员/crew剧组',
  `department`   varchar(50)  DEFAULT NULL COMMENT '部门(Directing/Writing/Acting...)',
  `job`          varchar(100) DEFAULT NULL COMMENT '职务(Director/Writer...crew用)',
  `character`    varchar(255) DEFAULT NULL COMMENT '饰演角色(cast用)',
  `cast_order`   int(11)      DEFAULT 999 COMMENT '排序(cast order,越小越靠前)',
  `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_person_job` (`media_id`,`person_id`,`credit_type`,`job`),
  KEY `idx_person` (`person_id`),
  KEY `idx_media` (`media_id`,`credit_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='影视-演职人员关联';

SET FOREIGN_KEY_CHECKS = 1;
