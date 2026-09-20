# 飞书资源文档 · 开通与配置指南

## 个人账号可以吗？

**可以。** 飞书注册时即可免费创建一个「企业/团队」（不需要营业执照、不需要企业认证），
你自己就是管理员。自建应用的 `tenant_access_token` 模式属于应用级调用，
创建文档/编辑/分享这些 OpenAPI 对未认证团队同样开放，个人完全够用。

腾讯文档的开放 API 面向企业服务商、需审核，个人基本走不通——所以腾讯文档
在系统里保持「手动维护链接」，自动生成只做了飞书。

## 开通步骤（约 10 分钟）

1. 注册飞书：https://www.feishu.cn （手机号注册，按提示创建团队，名字随意）
2. 进入开发者后台：https://open.feishu.cn/app → 「创建企业自建应用」
   - 名称如「影弯资源文档」，图标随意
3. 应用详情页 → 「凭证与基础信息」：复制 **App ID** 和 **App Secret**
4. 左侧「权限管理」添加以下权限（搜索勾选）：
   - `docx:document`（查看、评论、编辑和管理文档）
   - `drive:drive`（查看、评论、编辑和管理云空间中所有文件）
   - `drive:file`（上传、下载文件）
5. 左侧「版本管理与发布」→ 创建版本（1.0.0）→ 发布
   - 自建应用发布只需管理员（即你）确认，无需飞书官方审核
6. 服务器配置环境变量后重启后端：

   ```ini
   # supervisor 配置 environment 段追加
   FEISHU_APP_ID=cli_xxxxxxxxxxxx
   FEISHU_APP_SECRET=xxxxxxxxxxxxxxxx
   ```

   或启动参数 `--kemovie.feishu.app-id=... --kemovie.feishu.app-secret=...`

## 验证

后台「影视库」→ 任意影片「编辑」→「资源文档」区 → **测试连接** 按钮：
- 提示「飞书连接正常，凭证有效」→ 可用
- 提示鉴权失败 → 检查 App ID/Secret 是否复制完整、应用是否已发布

之后「生成文档」即可自动创建资源文档；再次点击为清空重写，**链接不变**。

## 说明

- 文档创建在应用名下的云空间，链接分享默认 `anyone_readable`（任何人可读），
  配置项 `kemovie.feishu.link-share` 可改
- 海外版 Lark 需把 `kemovie.feishu.base-url` 改为 https://open.larksuite.com
- 资源链接本身仍只存后台（km_media_resource），前台详情页只展示生成的文档链接
