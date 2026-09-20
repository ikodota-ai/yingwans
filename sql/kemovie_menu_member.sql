-- ----------------------------------------------------------------------------
-- 影弯 Kemovie 后台菜单：前台会员管理（菜单 2005，按钮 2051+）
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2005, '会员管理', 2000, 5, 'member', 'kemovie/member/index', 1, 0, 'C', '0', '0', 'kemovie:member:list', 'peoples', 'admin', sysdate(), '前台会员管理');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2051, '会员查询', 2005, 1, '', '', 1, 0, 'F', '0', '0', 'kemovie:member:query', '#', 'admin', sysdate(), ''),
(2052, '会员修改', 2005, 2, '', '', 1, 0, 'F', '0', '0', 'kemovie:member:edit', '#', 'admin', sysdate(), ''),
(2053, '会员删除', 2005, 3, '', '', 1, 0, 'F', '0', '0', 'kemovie:member:remove', '#', 'admin', sysdate(), ''),
(2054, '重置密码', 2005, 4, '', '', 1, 0, 'F', '0', '0', 'kemovie:member:resetPwd', '#', 'admin', sysdate(), '');

-- 授予超级管理员角色(admin, role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2005 AND 2054
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
