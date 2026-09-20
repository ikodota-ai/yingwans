-- ----------------------------------------------------------------------------
-- 影弯 Kemovie 后台菜单：演职人员管理（菜单 2006，按钮 2061+）
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2006, '演职人员', 2000, 6, 'person', 'kemovie/person/index', 1, 0, 'C', '0', '0', 'kemovie:person:list', 'user', 'admin', sysdate(), '演职人员管理');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2061, '人员查询', 2006, 1, '', '', 1, 0, 'F', '0', '0', 'kemovie:person:query', '#', 'admin', sysdate(), ''),
(2062, '人员刷新', 2006, 2, '', '', 1, 0, 'F', '0', '0', 'kemovie:person:edit', '#', 'admin', sysdate(), ''),
(2063, '人员删除', 2006, 3, '', '', 1, 0, 'F', '0', '0', 'kemovie:person:remove', '#', 'admin', sysdate(), '');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2006 AND 2063
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
