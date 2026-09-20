-- ----------------------------------------------------------------------------
-- 影弯 Kemovie 后台菜单与权限
-- 目录 2000，子菜单 2001-2005，按钮 2010+
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 主目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2000, '影弯管理', 0, 5, 'kemovie', NULL, 1, 0, 'M', '0', '0', '', 'international', 'admin', sysdate(), '影弯业务管理目录');

-- 影视库
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2001, '影视库', 2000, 1, 'media', 'kemovie/media/index', 1, 0, 'C', '0', '0', 'kemovie:media:list', 'star', 'admin', sysdate(), '影视条目管理');
-- 片单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2002, '片单管理', 2000, 2, 'collection', 'kemovie/collection/index', 1, 0, 'C', '0', '0', 'kemovie:collection:list', 'list', 'admin', sysdate(), '片单管理');
-- 片单抓取源（MDL / JustWatch）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2003, '片单抓取源', 2000, 3, 'mdlSource', 'kemovie/mdlSource/index', 1, 0, 'C', '0', '0', 'kemovie:mdl:list', 'download', 'admin', sysdate(), 'MyDramaList / JustWatch 抓取源配置');
-- 运营位
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES (2004, '运营位', 2000, 4, 'opsBlock', 'kemovie/opsBlock/index', 1, 0, 'C', '0', '0', 'kemovie:ops:list', 'guide', 'admin', sysdate(), '运营位/私域二维码配置');

-- 影视库按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2011, '影视查询', 2001, 1, '', '', 1, 0, 'F', '0', '0', 'kemovie:media:query', '#', 'admin', sysdate(), ''),
(2012, '影视修改', 2001, 2, '', '', 1, 0, 'F', '0', '0', 'kemovie:media:edit', '#', 'admin', sysdate(), ''),
(2013, '影视删除', 2001, 3, '', '', 1, 0, 'F', '0', '0', 'kemovie:media:remove', '#', 'admin', sysdate(), ''),
(2014, 'TMDB同步', 2001, 4, '', '', 1, 0, 'F', '0', '0', 'kemovie:media:sync', '#', 'admin', sysdate(), '');

-- 片单按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2021, '片单查询', 2002, 1, '', '', 1, 0, 'F', '0', '0', 'kemovie:collection:query', '#', 'admin', sysdate(), ''),
(2022, '片单新增', 2002, 2, '', '', 1, 0, 'F', '0', '0', 'kemovie:collection:add', '#', 'admin', sysdate(), ''),
(2023, '片单修改', 2002, 3, '', '', 1, 0, 'F', '0', '0', 'kemovie:collection:edit', '#', 'admin', sysdate(), ''),
(2024, '片单删除', 2002, 4, '', '', 1, 0, 'F', '0', '0', 'kemovie:collection:remove', '#', 'admin', sysdate(), '');

-- MDL 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2031, '抓取源查询', 2003, 1, '', '', 1, 0, 'F', '0', '0', 'kemovie:mdl:query', '#', 'admin', sysdate(), ''),
(2032, '抓取源新增', 2003, 2, '', '', 1, 0, 'F', '0', '0', 'kemovie:mdl:add', '#', 'admin', sysdate(), ''),
(2033, '抓取源修改', 2003, 3, '', '', 1, 0, 'F', '0', '0', 'kemovie:mdl:edit', '#', 'admin', sysdate(), ''),
(2034, '抓取源删除', 2003, 4, '', '', 1, 0, 'F', '0', '0', 'kemovie:mdl:remove', '#', 'admin', sysdate(), ''),
(2035, '立即抓取', 2003, 5, '', '', 1, 0, 'F', '0', '0', 'kemovie:mdl:run', '#', 'admin', sysdate(), '');

-- 运营位按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(2041, '运营位查询', 2004, 1, '', '', 1, 0, 'F', '0', '0', 'kemovie:ops:query', '#', 'admin', sysdate(), ''),
(2042, '运营位新增', 2004, 2, '', '', 1, 0, 'F', '0', '0', 'kemovie:ops:add', '#', 'admin', sysdate(), ''),
(2043, '运营位修改', 2004, 3, '', '', 1, 0, 'F', '0', '0', 'kemovie:ops:edit', '#', 'admin', sysdate(), ''),
(2044, '运营位删除', 2004, 4, '', '', 1, 0, 'F', '0', '0', 'kemovie:ops:remove', '#', 'admin', sysdate(), '');

-- 授予超级管理员角色(admin, role_id=1)全部新菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2000 AND 2044
AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
