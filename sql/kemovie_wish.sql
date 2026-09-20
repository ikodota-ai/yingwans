-- ----------------------------------------------------------------------------
-- 影弯增量迁移：想看运营视图（菜单）
-- 在已执行过 kemovie_legacy.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2008,'想看管理',2000,8,'wish','kemovie/wish/index',1,0,'C','0','0','kemovie:wish:list','heart','admin',sysdate(),'用户想看影片运营视图');
INSERT IGNORE INTO `sys_menu`
  (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`create_by`,`create_time`,`remark`)
VALUES
  (2081,'想看查询',2008,1,'','',1,0,'F','0','0','kemovie:wish:list','#','admin',sysdate(),'');
