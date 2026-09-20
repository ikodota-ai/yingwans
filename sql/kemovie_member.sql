-- ----------------------------------------------------------------------------
-- 影弯 Kemovie 前台会员表
-- 前台用户与后台 sys_user 完全解耦，独立鉴权（member_tokens: 前缀）
-- km_user_media_action / km_user_subscription / km_notification / km_collection
-- 中的 user_id / owner_id 自此指向 km_member.member_id
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `km_member`;
CREATE TABLE `km_member` (
  `member_id`     bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `username`      varchar(30)  NOT NULL COMMENT '登录账号',
  `nickname`      varchar(30)  DEFAULT '' COMMENT '昵称',
  `password`      varchar(100) NOT NULL DEFAULT '' COMMENT '密码(bcrypt)',
  `email`         varchar(50)  DEFAULT '' COMMENT '邮箱',
  `phone`         varchar(20)  DEFAULT '' COMMENT '手机号',
  `avatar`        varchar(255) DEFAULT '' COMMENT '头像地址',
  `sex`           char(1)      DEFAULT '2' COMMENT '性别(0男 1女 2未知)',
  `status`        char(1)      DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `login_ip`      varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date`    datetime     DEFAULT NULL COMMENT '最后登录时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='前台会员';
