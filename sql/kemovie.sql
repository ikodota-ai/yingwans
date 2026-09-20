-- ----------------------------------------------------------------------------
-- 影弯 Kemovie 业务表结构
-- 私人影库 + 流媒体上线订阅提醒
-- 数据来源：TMDB(api) + MyDramaList(headless 抓取)
-- 命名前缀 km_，兼容 MySQL 5.7+/8.0，utf8mb4
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 影视条目主表（电影/剧集，来源 TMDB）
-- ----------------------------
DROP TABLE IF EXISTS `km_media`;
CREATE TABLE `km_media` (
  `media_id`        bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '内部主键',
  `tmdb_id`         bigint(20)    DEFAULT NULL COMMENT 'TMDB ID',
  `media_type`      varchar(10)   NOT NULL DEFAULT 'movie' COMMENT '类型 movie/tv',
  `imdb_id`         varchar(20)   DEFAULT NULL COMMENT 'IMDB ID',
  `mdl_id`          varchar(30)   DEFAULT NULL COMMENT 'MyDramaList ID(如 mdl-11183)',
  `letterboxd_slug` varchar(100)  DEFAULT NULL COMMENT 'Letterboxd 影片 slug(如 city-of-god)',
  `legacy_aid`      bigint(20)    DEFAULT NULL COMMENT '旧站 mod_article.aid，导入幂等锚点',
  `title`           varchar(255)  NOT NULL DEFAULT '' COMMENT '标题(中文优先)',
  `original_title`  varchar(255)  DEFAULT NULL COMMENT '原始标题',
  `overview`        text          COMMENT '简介',
  `poster_path`     varchar(255)  DEFAULT NULL COMMENT 'TMDB 海报相对路径',
  `poster_local_url` varchar(500) DEFAULT NULL COMMENT '本地/CDN 海报地址(优先)',
  `backdrop_local_url` varchar(500) DEFAULT NULL COMMENT '本地/CDN 剧照地址',
  `backdrop_path`   varchar(255)  DEFAULT NULL COMMENT 'TMDB 背景相对路径',
  `release_date`    date          DEFAULT NULL COMMENT '首播/上映日期',
  `region`          varchar(100)  DEFAULT NULL COMMENT '国家/地区(逗号分隔)',
  `genres`          varchar(255)  DEFAULT NULL COMMENT '类型标签(逗号分隔)',
  `tags`            varchar(500)  DEFAULT NULL COMMENT '题材标签(逗号分隔)：百合/男男纯爱/双性恋/跨性别/酷儿/异性恋/其他+补充',
  `source`          varchar(20)   DEFAULT 'tmdb' COMMENT '数据来源 tmdb/mdl/justwatch/letterboxd/legacy',
  `runtime`         int(11)       DEFAULT NULL COMMENT '片长(分钟)',
  `vote_average`    decimal(3,1)  DEFAULT 0.0 COMMENT 'TMDB 评分',
  `vote_count`      int(11)       DEFAULT 0 COMMENT 'TMDB 评分人数',
  `popularity`      decimal(10,3) DEFAULT 0.000 COMMENT 'TMDB 热度',
  `status`          varchar(20)   DEFAULT 'Released' COMMENT '状态 Released/Upcoming/Ended...',
  `like_count`      int(11)       DEFAULT 0 COMMENT '站内点赞数(冗余)',
  `wish_count`      int(11)       DEFAULT 0 COMMENT '站内想看数(冗余)',
  `watched_count`   int(11)       DEFAULT 0 COMMENT '站内看过数(冗余)',
  `rating_avg`      decimal(3,1)  DEFAULT 0.0 COMMENT '站内评分均分(冗余)',
  `rating_count`    int(11)       DEFAULT 0 COMMENT '站内评分人数(冗余)',
  `hot_score`       decimal(12,3) DEFAULT 0.000 COMMENT '综合热度分(冗余,排行榜用)',
  `last_synced_at`  datetime      DEFAULT NULL COMMENT '最后同步 TMDB 时间',
  `feishu_doc_token` varchar(64)  DEFAULT NULL COMMENT '飞书文档 token（更新定位用）',
  `feishu_doc_url`  varchar(500)  DEFAULT NULL COMMENT '飞书资源文档链接（前台展示）',
  `tencent_doc_url` varchar(500)  DEFAULT NULL COMMENT '腾讯资源文档链接（手动维护，前台展示）',
  `visible`         char(1)       DEFAULT '0' COMMENT '是否展示(0是 1否)',
  `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by`       varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  `update_by`       varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
  `remark`          varchar(500)  DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`media_id`),
  UNIQUE KEY `uk_tmdb` (`tmdb_id`, `media_type`),
  KEY `idx_mdl` (`mdl_id`),
  KEY `idx_letterboxd` (`letterboxd_slug`),
  KEY `idx_legacy_aid` (`legacy_aid`),
  KEY `idx_hot` (`hot_score`),
  KEY `idx_release` (`release_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='影视条目主表';

-- ----------------------------
-- 2. 上线平台/发行信息（JustWatch 式，来源 TMDB watch providers）
-- ----------------------------
DROP TABLE IF EXISTS `km_media_release`;
CREATE TABLE `km_media_release` (
  `release_id`   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `media_id`     bigint(20)   NOT NULL COMMENT '影视ID',
  `platform`     varchar(64)  NOT NULL COMMENT '平台名(Netflix/爱奇艺...)',
  `platform_logo` varchar(255) DEFAULT NULL COMMENT '平台 logo',
  `country`      varchar(10)  DEFAULT 'CN' COMMENT '地区',
  `offer_type`   varchar(20)  DEFAULT 'flatrate' COMMENT '类型 flatrate会员/rent租/buy购/free',
  `online_date`  date         DEFAULT NULL COMMENT '上线日期',
  `link`         varchar(500) DEFAULT NULL COMMENT '观看链接',
  `status`       varchar(20)  DEFAULT 'available' COMMENT 'available已上线/upcoming即将/offline下架',
  `create_time`  datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`release_id`),
  KEY `idx_media` (`media_id`),
  KEY `idx_online` (`online_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='影视上线平台信息';

-- ----------------------------
-- 3. 片单（系统抓取/用户自建）
-- ----------------------------
DROP TABLE IF EXISTS `km_collection`;
CREATE TABLE `km_collection` (
  `collection_id` bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '片单ID',
  `name`          varchar(200) NOT NULL COMMENT '片单名',
  `description`   varchar(1000) DEFAULT NULL COMMENT '描述',
  `source`        varchar(20)  DEFAULT 'manual' COMMENT '来源 system/tmdb/mdl/manual',
  `source_ref`    varchar(200) DEFAULT NULL COMMENT '来源引用(MDL list id/TMDB list id)',
  `cover_url`     varchar(500) DEFAULT NULL COMMENT '封面(空则用前4海报拼贴)',
  `owner_id`      bigint(20)   DEFAULT NULL COMMENT '创建用户(系统片单为空)',
  `item_count`    int(11)      DEFAULT 0 COMMENT '影片数(冗余)',
  `is_public`     char(1)      DEFAULT '0' COMMENT '是否公开(0公开 1私有)',
  `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`collection_id`),
  KEY `idx_source` (`source`, `source_ref`),
  KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='片单';

-- ----------------------------
-- 4. 片单-影片关联
-- ----------------------------
DROP TABLE IF EXISTS `km_collection_item`;
CREATE TABLE `km_collection_item` (
  `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `collection_id` bigint(20) NOT NULL COMMENT '片单ID',
  `media_id`      bigint(20) NOT NULL COMMENT '影视ID',
  `sort`          int(11)    DEFAULT 0 COMMENT '排序',
  `note`          varchar(255) DEFAULT NULL COMMENT '片单内备注',
  `create_time`   datetime   DEFAULT NULL COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coll_media` (`collection_id`, `media_id`),
  KEY `idx_media` (`media_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='片单影片关联';

-- ----------------------------
-- 4b. 片单关注（会员关注公共片单）
-- ----------------------------
DROP TABLE IF EXISTS `km_collection_follow`;
CREATE TABLE `km_collection_follow` (
  `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `member_id`     bigint(20) NOT NULL COMMENT '会员ID(km_member.member_id)',
  `collection_id` bigint(20) NOT NULL COMMENT '片单ID',
  `create_time`   datetime   DEFAULT NULL COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_coll` (`member_id`, `collection_id`),
  KEY `idx_coll` (`collection_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='片单关注';

-- ----------------------------
-- 5. 用户对影片的行为（点赞/想看/看过/评分，一行合并）
-- ----------------------------
DROP TABLE IF EXISTS `km_user_media_action`;
CREATE TABLE `km_user_media_action` (
  `id`         bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    bigint(20)  NOT NULL COMMENT '用户ID',
  `media_id`   bigint(20)  NOT NULL COMMENT '影视ID',
  `liked`      char(1)     DEFAULT '0' COMMENT '点赞(0否 1是)',
  `wished`     char(1)     DEFAULT '0' COMMENT '想看(0否 1是)',
  `watched`    char(1)     DEFAULT '0' COMMENT '看过(0否 1是)',
  `rating`     decimal(3,1) DEFAULT NULL COMMENT '评分(0-10,可空)',
  `liked_time`   datetime  DEFAULT NULL COMMENT '点赞时间',
  `wished_time`  datetime  DEFAULT NULL COMMENT '想看时间',
  `watched_time` datetime  DEFAULT NULL COMMENT '看过时间',
  `rating_time`  datetime  DEFAULT NULL COMMENT '评分时间',
  `create_time`  datetime  DEFAULT NULL COMMENT '创建时间',
  `update_time`  datetime  DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_media` (`user_id`, `media_id`),
  KEY `idx_media` (`media_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户影片行为';

-- ----------------------------
-- 6. 用户订阅（上线提醒跟踪）
-- ----------------------------
DROP TABLE IF EXISTS `km_user_subscription`;
CREATE TABLE `km_user_subscription` (
  `sub_id`     bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订阅ID',
  `user_id`    bigint(20) NOT NULL COMMENT '用户ID',
  `media_id`   bigint(20) NOT NULL COMMENT '影视ID',
  `notify_channel` varchar(50) DEFAULT 'site,email' COMMENT '渠道 site/email/wechat',
  `notified`   char(1)    DEFAULT '0' COMMENT '是否已通知上线(0否 1是)',
  `status`     char(1)    DEFAULT '0' COMMENT '状态(0有效 1取消)',
  `create_time` datetime  DEFAULT NULL COMMENT '订阅时间',
  `update_time` datetime  DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`sub_id`),
  UNIQUE KEY `uk_user_media` (`user_id`, `media_id`),
  KEY `idx_media` (`media_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅';

-- ----------------------------
-- 7. 热门排行榜快照
-- ----------------------------
DROP TABLE IF EXISTS `km_hot_rank`;
CREATE TABLE `km_hot_rank` (
  `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rank_type`  varchar(20) DEFAULT 'week' COMMENT '榜单类型 week/all/new',
  `rank_no`    int(11)    NOT NULL COMMENT '名次',
  `media_id`   bigint(20) NOT NULL COMMENT '影视ID',
  `hot_score`  decimal(12,3) DEFAULT 0.000 COMMENT '热度分',
  `snapshot_date` date    DEFAULT NULL COMMENT '快照日期',
  `create_time` datetime  DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_date` (`rank_type`, `snapshot_date`, `rank_no`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='热门排行榜快照';

-- ----------------------------
-- 8. 通知记录（站内信 + 邮件；预留 wechat）
-- ----------------------------
DROP TABLE IF EXISTS `km_notification`;
CREATE TABLE `km_notification` (
  `notify_id`  bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id`    bigint(20)  NOT NULL COMMENT '接收用户',
  `type`       varchar(20) DEFAULT 'release' COMMENT '类型 release上线/system系统',
  `title`      varchar(200) NOT NULL COMMENT '标题',
  `content`    varchar(1000) DEFAULT NULL COMMENT '内容',
  `media_id`   bigint(20)  DEFAULT NULL COMMENT '关联影视',
  `channel`    varchar(50) DEFAULT 'site' COMMENT '发送渠道',
  `is_read`    char(1)     DEFAULT '0' COMMENT '是否已读(0否 1是)',
  `send_status` char(1)    DEFAULT '0' COMMENT '发送状态(0成功 1失败)',
  `create_time` datetime   DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`notify_id`),
  KEY `idx_user` (`user_id`, `is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户通知记录';

-- ----------------------------
-- 9. MDL 抓取源配置（支持多个片单，去重按 media 唯一）
-- ----------------------------
DROP TABLE IF EXISTS `km_mdl_source`;
CREATE TABLE `km_mdl_source` (
  `source_id`   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_type` varchar(20) NOT NULL DEFAULT 'mdl' COMMENT '来源类型 mdl/justwatch',
  `list_id`     varchar(100) DEFAULT NULL COMMENT 'MDL list id / JustWatch 平台 slug(留空=全国热门)',
  `list_url`    varchar(500) NOT NULL COMMENT '完整URL',
  `name`        varchar(200) DEFAULT NULL COMMENT '片单名(抓取后回填)',
  `region_tag`  varchar(20) DEFAULT NULL COMMENT 'MDL 区域标记 / JustWatch 国家(US)',
  `object_type` varchar(10) DEFAULT NULL COMMENT 'JustWatch 影片类型 movie/tv(NULL=全部)',
  `max_page`    int(11)     DEFAULT 1 COMMENT '抓取页数',
  `target_collection_id` bigint(20) DEFAULT NULL COMMENT '生成/映射的片单ID',
  `enabled`     char(1)     DEFAULT '0' COMMENT '启用(0是 1否)',
  `last_run_at` datetime    DEFAULT NULL COMMENT '最后抓取时间',
  `last_status` varchar(500) DEFAULT NULL COMMENT '最后抓取结果',
  `create_by`   varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime    DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime    DEFAULT NULL COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`source_id`),
  UNIQUE KEY `uk_type_list` (`source_type`,`list_id`,`region_tag`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='片单抓取源配置(MDL/JustWatch)';

-- ----------------------------
-- 10. 运营位（后台可编辑：私域二维码/公告等）
-- ----------------------------
DROP TABLE IF EXISTS `km_ops_block`;
CREATE TABLE `km_ops_block` (
  `block_id`   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
  `block_key`  varchar(50) NOT NULL COMMENT '位置标识 detail_footer/home_side/qrcode',
  `title`      varchar(200) DEFAULT NULL COMMENT '标题',
  `content`    text        COMMENT '富文本/说明',
  `image_url`  varchar(500) DEFAULT NULL COMMENT '图片/二维码',
  `link`       varchar(500) DEFAULT NULL COMMENT '跳转链接',
  `sort`       int(11)     DEFAULT 0 COMMENT '排序',
  `enabled`    char(1)     DEFAULT '0' COMMENT '启用(0是 1否)',
  `create_by`  varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime   DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime   DEFAULT NULL COMMENT '更新时间',
  `remark`     varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`block_id`),
  KEY `idx_key` (`block_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='运营位配置';

-- ----------------------------
-- 12. 影片下载资源（仅供后台与第三方文档生成使用，任何前台接口不得返回）
-- ----------------------------
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

-- ----------------------------
-- 初始数据：MDL 抓取源（用户指定的亚洲片单，多页）
-- ----------------------------
INSERT INTO `km_mdl_source` (`list_id`,`list_url`,`name`,`region_tag`,`max_page`,`enabled`,`create_by`,`create_time`,`remark`) VALUES
('1R88rJN3','https://mydramalist.com/list/1R88rJN3','WLW','asia',6,'0','admin',NOW(),'用户指定亚洲片单(1-6页)');

-- ----------------------------
-- 初始数据：首页侧边运营位（广告位）
-- ----------------------------
INSERT INTO `km_ops_block` (`block_key`,`title`,`content`,`image_url`,`link`,`sort`,`enabled`,`create_by`,`create_time`,`remark`) VALUES
('home_side','影弯会员','解锁高清海报与上线提醒，追剧不错过。',NULL,NULL,1,'0','system',NOW(),'首页侧边运营位'),
('home_side','热门片单','编辑精选主题片单，一键收藏。',NULL,'/collections',2,'0','system',NOW(),'首页侧边运营位');

SET FOREIGN_KEY_CHECKS = 1;
