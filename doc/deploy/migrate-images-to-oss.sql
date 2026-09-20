-- =====================================================================
-- 线上库执行：将本地图片路径切换为阿里云 OSS（自定义域名 image.yingwans.com）
-- 前提：kemovie-uploads/kemovie 下的 poster/ profile/ provider/ 已原样上传到
--       bucket(yingwans) 根目录，公网可访问 https://image.yingwans.com/<相对路径>
-- 说明：仅影响仍为 /profile/kemovie/ 前缀的行，可重复执行（幂等）；
--       执行前建议先备份相关三表。本地开发库不要执行。
-- =====================================================================

-- 1) 影片海报（484 行）
UPDATE km_media
   SET poster_local_url = REPLACE(poster_local_url, '/profile/kemovie/', 'https://image.yingwans.com/')
 WHERE poster_local_url LIKE '/profile/kemovie/%';

-- 2) 人物头像（3331 行）
UPDATE km_person
   SET profile_local_url = REPLACE(profile_local_url, '/profile/kemovie/', 'https://image.yingwans.com/')
 WHERE profile_local_url LIKE '/profile/kemovie/%';

-- 3) 播放平台 logo（573 行）
UPDATE km_media_release
   SET platform_logo = REPLACE(platform_logo, '/profile/kemovie/', 'https://image.yingwans.com/')
 WHERE platform_logo LIKE '/profile/kemovie/%';

-- ---------------- 校验：下面三个结果都应为 0 ----------------
SELECT COUNT(*) AS media_poster_left  FROM km_media         WHERE poster_local_url LIKE '/profile/kemovie/%';
SELECT COUNT(*) AS person_left        FROM km_person        WHERE profile_local_url LIKE '/profile/kemovie/%';
SELECT COUNT(*) AS release_logo_left  FROM km_media_release WHERE platform_logo    LIKE '/profile/kemovie/%';
