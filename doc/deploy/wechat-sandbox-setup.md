# 微信模板消息 · 接口测试号接入指南（第二期）

正式服务号需要企业认证（个人办不了）。微信公众平台提供**接口测试号**，
个人微信号即可申请，支持模板消息接口（最多 100 个关注者），
足够验证「订阅资源通知推送到微信」的完整链路。

## 申请与配置（约 5 分钟）

1. 用微信扫码登录测试号后台：
   https://mp.weixin.qq.com/debug/cgi-bin/sandbox?t=sandbox/login
2. 页面顶部即得 **appID** 和 **appsecret**
3. 「测试号二维码」用自己的微信扫码关注 → 下方用户列表里能看到你的 **openid**
4. 「模板消息接口」→ 新增测试模板，模板内容：
   ```
   你订阅的《{{keyword1.DATA}}》有新通知
   {{keyword2.DATA}}
   ```
   提交后得到 **模板ID**（消息点击跳转链接由代码自动带飞书文档地址）
5. 服务器配置环境变量并重启后端：
   ```ini
   WECHAT_MP_APP_ID=wxXXXXXXXX
   WECHAT_MP_APP_SECRET=XXXXXXXX
   WECHAT_MP_TEMPLATE_ID=XXXXXXXX
   ```
6. 把你的 openid 绑到会员（测试阶段直接改库）：
   ```sql
   UPDATE km_member SET wechat_openid='你的openid' WHERE username='你的会员名';
   ```
   并把该会员订阅的 notify_channel 加上 wechat：
   ```sql
   UPDATE km_user_subscription SET notify_channel='site,email,wechat'
   WHERE user_id=会员ID AND status='0';
   ```

## 验证

会员订阅/资源文档更新触发通知后，微信会收到测试号的模板消息，
点击消息直接打开飞书文档。后端日志可见「微信推送-失败」原因（如有）。

## 说明与边界

- 测试号消息无「服务通知」强提醒样式，体验不如正式服务号，仅用于链路验证
- 正式服第三期：有企业资质后建服务号，接口完全一致，只换 appID/secret/模板ID
- 代码位置：`WechatMpClient`（token 缓存 + 模板消息），通知渠道 `wechat`
