# 影弯 Kemovie 服务器部署指南（含阿里云 OSS / 自定义域名）

## 0. 本次已验证的 OSS 资源
- 区域 Endpoint：`oss-cn-hongkong.aliyuncs.com`
- Bucket：`yingwans`
- 自定义域名：`https://image.yingwans.com`（经 Cloudflare 回源到该 Bucket）
- 已实测：应用 → OSS（CNAME 方式，读写都走自定义域名）上传、公共读、CDN 缓存命中、删除均正常。

> 说明：新 Bucket 强制“三级域名”，且服务器/本机可能解析不了
> `yingwans.oss-cn-hongkong.aliyuncs.com`。因此当配置了 `domain` 时，
> `OssUploader` 自动以该域名为 Host 并开启 CNAME（读写均走
> `https://image.yingwans.com/...`），不依赖 bucket 子域名解析。

## 1. 准备
- JDK 17+、MySQL（utf8mb4）、Redis、Nginx。
- 真实 AK/SK 已写入本机 `ruoyi-admin/src/main/resources/application-prod.yml`
  （该文件已加入 .gitignore，不会提交；构建时会打进 jar）。
  如从 Git 重新构建，需自行创建该文件（参考 `application-prod.yml.example`）
  或改用环境变量（见第 4 节）。

## 2. 构建
```bash
export JAVA_HOME=/path/to/jdk
mvn clean package -DskipTests          # 产物 ruoyi-admin/target/ruoyi-admin.jar（已含 prod 配置）

cd ruoyi-ui && npm install && npm run build:prod   # 产物 ruoyi-ui/dist
```

## 3. 数据库
导入 `sql/` 下 schema 与业务 SQL（新库连接加 `--default-character-set=utf8mb4`）。
影弯业务 SQL 执行顺序（已执行过的增量文件跳过即可）：
```
kemovie.sql → kemovie_job.sql → kemovie_menu.sql → kemovie_member.sql
→ kemovie_person.sql → kemovie_justwatch.sql → kemovie_letterboxd.sql
→ kemovie_legacy.sql → kemovie_wish.sql → kemovie_jw_new.sql
→ kemovie_provider.sql → kemovie_feishu.sql → kemovie_resource_notify.sql
```
> 全新安装可跳过 kemovie_justwatch.sql / kemovie_letterboxd.sql
> （基础 kemovie.sql 已含其表结构，这两个是给旧库补列用的）。

## 4. 启用 OSS 的两种方式
方式 A（当前 jar，最简单）：启动时激活 prod profile
```bash
java -jar ruoyi-admin.jar --spring.profiles.active=druid,prod
# 或
export SPRING_PROFILES_ACTIVE=druid,prod
java -jar ruoyi-admin.jar
```

方式 B（环境变量，不落盘密钥）：不使用 prod 文件，改用
`OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET / OSS_BUCKET_NAME / OSS_DOMAIN`，
并把 `kemovie.image.storage` 设为 `oss`。

配置项含义（`kemovie.image.oss`）：
- `endpoint` 区域端点；`bucket-name` 桶名；`dir` 对象前缀（默认 kemovie）。
- `domain` 自定义/CDN 域名；配置后读写都走它。
- `public-read` 是否给对象设置公共读（当前已验证 true 可用）。

存储切换：`kemovie.image.storage=local`（默认，本地盘）或 `oss`。

## 4.1 飞书资源文档（可选）
影片下载资源可一键生成/更新飞书文档（前台详情页只展示文档链接，资源不进前台）。
个人即可免费开通（见 `doc/deploy/feishu-setup.md`），拿到凭证后配置：
- 环境变量 `FEISHU_APP_ID` / `FEISHU_APP_SECRET`（supervisor 配置里已有占位注释），或
- 启动参数 `--kemovie.feishu.app-id=... --kemovie.feishu.app-secret=...`
验证：后台「影视库 → 编辑 → 资源文档 → 测试连接」。

## 4.2 微信模板消息（可选，第二期）
订阅资源通知可推送到微信（接口测试号个人即可申请）：
环境变量 `WECHAT_MP_APP_ID` / `WECHAT_MP_APP_SECRET` / `WECHAT_MP_TEMPLATE_ID`，
步骤见 `doc/deploy/wechat-sandbox-setup.md`。

## 4.3 地区名归一（旧站导入后执行一次）
库中地区统一存字母码、前台显示补全中文。旧站导入或历史混存后执行：
```bash
curl -X POST http://127.0.0.1:8080/kemovie/media/normalizeRegions \
  -H "Authorization: Bearer <admin-token>"
```

## 5. 迁移已有本地图片到 OSS（历史库需要）
现有库约有：484 张海报、3331 张人物头像、72 个平台 logo（573 行），
迁移只读本地文件并上传 OSS，幂等、不删本地文件，耗时较长（建议服务器后台执行）：
```bash
# 先以 oss 模式启动，再用管理员 token 调用
curl -X POST http://127.0.0.1:8080/kemovie/media/migrateImagesToOss \
  -H "Authorization: Bearer <admin-token>"
```
覆盖 `km_media.poster_local_url/backdrop_local_url`、
`km_person.profile_local_url`、`km_media_release.platform_logo`。
平台 logo 单独全量本地化：`POST /kemovie/media/localizeProviderLogos`。
新部署的空库无需迁移，后续同步会直接落到 OSS。

## 6. Nginx
参考 `doc/deploy/nginx-kemovie.conf`：
- 根目录指向 `ruoyi-ui/dist`，history 路由回退 `index.html`；
- `/prod-api/` 反代 `http://127.0.0.1:8080/`；
- OSS 模式下图片走 `image.yingwans.com`，`/profile/` 段仅在仍有本地图片时保留。

## 7. 安全建议
- 为该 AK 使用仅授权 `yingwans` 桶的 RAM 子账号；如密钥曾外泄请在阿里云轮换。
- 回滚：`storage` 改回 `local` 重启即可；库中 OSS 绝对 URL 不受影响。

## 8. 旧站数据导入
见 `doc/deploy/import-legacy-plan.md`：本地导出 JSONL → 海报传 OSS →
后台「影弯管理 → 旧站导入」试跑/正式导入（写入服务器库，幂等可重跑）。

## 9. 使用 Supervisor 管理进程（推荐，与服务器其它服务一致）
配置文件见 `doc/deploy/supervisor-kemovie.conf`。

```bash
# 1) 目录与产物
sudo mkdir -p /opt/kemovie /var/log/supervisor
#   上传 ruoyi-admin.jar 到 /opt/kemovie/，前端 dist 到 nginx 目录

# 2) 确认 JAVA_HOME（按实际 JDK 路径修改 command 与 environment）
readlink -f "$(command -v java)"          # 常见：/usr/lib/jvm/java-17-openjdk-amd64/bin/java

# 3) 安装 supervisor（如未安装）
#   Debian/Ubuntu: sudo apt-get install -y supervisor
#   CentOS/RHEL:   sudo yum install -y supervisor && sudo systemctl enable --now supervisord

# 4) 放置配置并加载
sudo cp doc/deploy/supervisor-kemovie.conf /etc/supervisor/conf.d/kemovie.conf
sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl status kemovie          # RUNNING 即正常
sudo supervisorctl tail -f kemovie         # 查看实时日志
# 常用：supervisorctl start|stop|restart kemovie
```

注意：
- 配置里 `command` 用绝对路径的 `java`，并已显式激活 `druid,prod`（OSS 香港桶）。
- `user` 默认 `www-data`；请改成你运行服务的用户，并确保该用户对
  `/opt/kemovie`、日志目录及 MySQL/Redis 网络可访问。
- 上传目录：OSS 模式下不依赖本地磁盘图片；若同时用 `storage=local`，
  需让该用户可写 `ruoyi.profile`（默认上传根目录）。
- Nginx 与 Supervisor 各司其职：Supervisor 管 8080 的 jar，Nginx 管静态页面与 /prod-api 反代。
