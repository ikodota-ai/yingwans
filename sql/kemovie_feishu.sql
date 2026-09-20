-- ----------------------------------------------------------------------------
-- 影弯增量迁移：影片资源文档（飞书自动生成 + 腾讯文档手动链接）
-- 在已执行过 kemovie_provider.sql 的库上执行一次
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) km_media 增加资源文档链接字段
ALTER TABLE `km_media`
  ADD COLUMN `feishu_doc_token` varchar(64) DEFAULT NULL COMMENT '飞书文档 token（更新定位用）' AFTER `last_synced_at`,
  ADD COLUMN `feishu_doc_url` varchar(500) DEFAULT NULL COMMENT '飞书资源文档链接（前台展示）' AFTER `feishu_doc_token`,
  ADD COLUMN `tencent_doc_url` varchar(500) DEFAULT NULL COMMENT '腾讯资源文档链接（手动维护，前台展示）' AFTER `feishu_doc_url`;

-- 2) 资源表来源注释扩充：legacy 旧站导入 / manual 后台手动添加
ALTER TABLE `km_media_resource`
  MODIFY COLUMN `source` varchar(20) NOT NULL DEFAULT 'legacy' COMMENT '来源 legacy/manual';
