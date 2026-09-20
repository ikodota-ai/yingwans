-- ----------------------------------------------------------------------------
-- 影弯增量迁移：订阅资源通知 + 免责声明配置 + 微信通道 + 订阅管理菜单
-- 在已执行过 kemovie_feishu.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) 免责声明（后台 系统管理→参数设置 可改；通知中引用）
INSERT IGNORE INTO `sys_config`
  (`config_name`,`config_key`,`config_value`,`config_type`,`create_by`,`create_time`,`remark`)
VALUES
  ('资源通知免责声明','kemovie.resource.disclaimer',
   '免责声明：所有资源均来自互联网公开渠道，仅供个人学习、研究、交流，严禁用于商业用途。请于24小时内自行删除。若相关权利人认为内容侵犯您的合法权益，请联系：[请填写联系邮箱]，我们将在核实后立即删除。本站不存储、不制作、不上传任何资源。',
   'N','admin',sysdate(),'订阅资源通知尾部附加的免责声明');

-- 2) 会员微信 openid（公众号/测试号模板消息推送用）
ALTER TABLE `km_member`
  ADD COLUMN `wechat_openid` varchar(64) DEFAULT NULL COMMENT '微信openid(公众号推送)' AFTER `email`;

-- 3) 后台菜单：订阅管理（影弯管理 2000 下）
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2010,'订阅管理',2000,10,'subscription','kemovie/subscription/index',1,0,'C','0','0','kemovie:subscription:list','peoples','admin',sysdate(),'订阅动态与汇总');
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2101,'订阅查询',2010,1,'','',1,0,'F','0','0','kemovie:subscription:query','#','admin',sysdate(),'');
