package com.ruoyi.kemovie.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.WechatMpClient;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaRelease;
import com.ruoyi.kemovie.domain.KmNotification;
import com.ruoyi.kemovie.domain.KmUserSubscription;
import com.ruoyi.kemovie.mapper.KmConfigMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaReleaseMapper;
import com.ruoyi.kemovie.mapper.KmNotificationMapper;
import com.ruoyi.kemovie.mapper.KmUserSubscriptionMapper;
import com.ruoyi.kemovie.service.IKmNotificationService;

/**
 * 通知 Service 实现（站内信为主，邮件/微信为扩展渠道）
 *
 * @author kemovie
 */
@Service
public class KmNotificationServiceImpl implements IKmNotificationService
{
    private static final Logger log = LoggerFactory.getLogger(KmNotificationServiceImpl.class);

    @Autowired private KmNotificationMapper notificationMapper;
    @Autowired private KmUserSubscriptionMapper subscriptionMapper;
    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private KmMediaReleaseMapper releaseMapper;
    @Autowired private KmConfigMapper configMapper;
    @Autowired private WechatMpClient wechatMpClient;
    /** 邮件发送器：仅当配置了 spring.mail 时才存在，避免未配置时启动报错 */
    @Autowired private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Value("${spring.mail.username:}") private String mailFrom;

    @Override
    public List<KmNotification> listByUser(Long userId) { return notificationMapper.selectByUser(userId); }

    @Override
    public int unreadCount(Long userId) { return notificationMapper.countUnread(userId); }

    @Override
    public int markRead(Long userId, Long notifyId) { return notificationMapper.markRead(userId, notifyId); }

    @Override
    public int markAllRead(Long userId) { return notificationMapper.markAllRead(userId); }

    @Override
    public int send(KmNotification n)
    {
        // 站内信始终落库
        int r = notificationMapper.insertNotification(n);
        // 邮件渠道：配置了 SMTP 且用户有邮箱时才发送
        if (n.getChannel() != null && n.getChannel().contains("email"))
        {
            sendEmail(n);
        }
        // 微信公众号模板消息：用户绑定 openid 且已配置测试号/服务号时发送
        if (n.getChannel() != null && n.getChannel().contains("wechat"))
        {
            sendWechat(n);
        }
        return r;
    }

    /** 微信模板消息推送；未配置或未绑定 openid 时静默跳过并记录日志 */
    private void sendWechat(KmNotification n)
    {
        if (!wechatMpClient.configured())
        {
            log.info("[微信推送-未配置，跳过] user={} title={}", n.getUserId(), n.getTitle());
            return;
        }
        String openid = notificationMapper.selectUserOpenid(n.getUserId());
        if (StringUtils.isEmpty(openid))
        {
            log.info("[微信推送-用户未绑定openid，跳过] user={}", n.getUserId());
            return;
        }
        String url = extractUrl(n.getContent());
        String err = wechatMpClient.sendTemplate(openid, n.getTitle(),
                StringUtils.isNotEmpty(n.getContent()) ? n.getContent() : "点击查看详情", url);
        if (err != null) { log.warn("[微信推送-失败] user={} err={}", n.getUserId(), err); }
    }

    /** 从通知内容中提取第一个 http 链接（模板消息跳转用） */
    private String extractUrl(String content)
    {
        if (StringUtils.isEmpty(content)) { return null; }
        int i = content.indexOf("http");
        if (i < 0) { return null; }
        int end = i;
        while (end < content.length() && !Character.isWhitespace(content.charAt(end))
                && content.charAt(end) != '，' && content.charAt(end) != '。')
        {
            end++;
        }
        return content.substring(i, end);
    }

    /** 后台配置的免责声明（sys_config: kemovie.resource.disclaimer） */
    private String disclaimer()
    {
        String text = configMapper.selectConfigByKey("kemovie.resource.disclaimer");
        return StringUtils.isNotEmpty(text) ? text : "";
    }

    /** 资源文档通知内容（含链接与免责声明） */
    private String buildResourceContent(KmMedia m)
    {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(m.getFeishuDocUrl()))
        {
            sb.append("飞书文档：").append(m.getFeishuDocUrl()).append("\n");
        }
        if (StringUtils.isNotEmpty(m.getTencentDocUrl()))
        {
            sb.append("腾讯文档：").append(m.getTencentDocUrl()).append("\n");
        }
        sb.append("文档内含网盘链接与提取码，请勿外传。");
        String d = disclaimer();
        if (StringUtils.isNotEmpty(d)) { sb.append("\n").append(d); }
        return sb.toString();
    }

    @Override
    public boolean sendResourceDocIfAvailable(Long userId, Long mediaId)
    {
        KmMedia m = mediaMapper.selectKmMediaByMediaId(mediaId);
        if (m == null || (StringUtils.isEmpty(m.getFeishuDocUrl()) && StringUtils.isEmpty(m.getTencentDocUrl())))
        {
            return false;
        }
        KmUserSubscription sub = subscriptionMapper.selectByUserAndMedia(userId, mediaId);
        KmNotification n = new KmNotification();
        n.setUserId(userId);
        n.setType("resource");
        n.setMediaId(mediaId);
        n.setChannel(sub != null && StringUtils.isNotEmpty(sub.getNotifyChannel()) ? sub.getNotifyChannel() : "site");
        n.setTitle("你订阅的《" + m.getTitle() + "》资源已就绪");
        n.setContent(buildResourceContent(m));
        send(n);
        return true;
    }

    @Override
    public int notifySubscribersResourceDoc(Long mediaId)
    {
        KmMedia m = mediaMapper.selectKmMediaByMediaId(mediaId);
        if (m == null || (StringUtils.isEmpty(m.getFeishuDocUrl()) && StringUtils.isEmpty(m.getTencentDocUrl())))
        {
            return 0;
        }
        int sent = 0;
        for (KmUserSubscription s : subscriptionMapper.selectAllActiveByMedia(mediaId))
        {
            KmNotification n = new KmNotification();
            n.setUserId(s.getUserId());
            n.setType("resource");
            n.setMediaId(mediaId);
            n.setChannel(StringUtils.isNotEmpty(s.getNotifyChannel()) ? s.getNotifyChannel() : "site");
            n.setTitle("你订阅的《" + m.getTitle() + "》资源已更新");
            n.setContent(buildResourceContent(m));
            send(n);
            sent++;
        }
        log.info("资源文档通知派发完成 mediaId={} 共发送 {} 条", mediaId, sent);
        return sent;
    }

    /** 发送邮件通知；未配置 SMTP 或无收件邮箱时静默跳过并记录日志 */
    private void sendEmail(KmNotification n)
    {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null)
        {
            log.info("[邮件通知-未配置SMTP，跳过] user={} title={}", n.getUserId(), n.getTitle());
            return;
        }
        String to = notificationMapper.selectUserEmail(n.getUserId());
        if (StringUtils.isEmpty(to))
        {
            log.info("[邮件通知-用户无邮箱，跳过] user={}", n.getUserId());
            return;
        }
        try
        {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (StringUtils.isNotEmpty(mailFrom)) { msg.setFrom(mailFrom); }
            msg.setTo(to);
            msg.setSubject("【影弯】" + n.getTitle());
            msg.setText(StringUtils.isNotEmpty(n.getContent()) ? n.getContent() : n.getTitle());
            sender.send(msg);
            log.info("[邮件通知-已发送] to={} title={}", to, n.getTitle());
        }
        catch (Exception e)
        {
            log.warn("[邮件通知-发送失败] to={} err={}", to, e.getMessage());
        }
    }

    @Override
    public int dispatchReleaseNotifications()
    {
        int sent = 0;
        Date today = DateUtils.getNowDate();
        // 找到今天(或更早)已上线、且存在有效未通知订阅的影片
        KmMedia q = new KmMedia();
        List<KmMedia> all = mediaMapper.selectKmMediaList(q);
        for (KmMedia m : all)
        {
            List<KmMediaRelease> releases = releaseMapper.selectByMediaId(m.getMediaId());
            KmMediaRelease online = null;
            for (KmMediaRelease rel : releases)
            {
                // 已到/已过上线日期才算“已上线”，未来排期(upcoming)不触发提醒；
                // 无日期但标记 available 的历史数据兜底视为已上线
                boolean dateReached = rel.getOnlineDate() != null && !rel.getOnlineDate().after(today);
                boolean availableNoDate = rel.getOnlineDate() == null && "available".equals(rel.getStatus());
                if (dateReached || availableNoDate)
                {
                    online = rel;
                    break;
                }
            }
            if (online == null) { continue; }
            List<KmUserSubscription> subs = subscriptionMapper.selectActiveByMedia(m.getMediaId());
            for (KmUserSubscription s : subs)
            {
                KmNotification n = new KmNotification();
                n.setUserId(s.getUserId());
                n.setType("release");
                n.setMediaId(m.getMediaId());
                n.setChannel(StringUtils.isNotEmpty(s.getNotifyChannel()) ? s.getNotifyChannel() : "site");
                n.setTitle("你订阅的《" + m.getTitle() + "》已上线");
                n.setContent("《" + m.getTitle() + "》已在 " + online.getPlatform()
                        + " 上线（" + labelOffer(online.getOfferType()) + "）。");
                send(n);
                // 标记该订阅已通知
                KmUserSubscription upd = new KmUserSubscription();
                upd.setUserId(s.getUserId());
                upd.setMediaId(m.getMediaId());
                upd.setNotified("1");
                subscriptionMapper.updateSubscription(upd);
                sent++;
            }
        }
        log.info("上线提醒派发完成，共发送 {} 条", sent);
        return sent;
    }

    private String labelOffer(String offerType)
    {
        if (offerType == null) { return "会员"; }
        switch (offerType)
        {
            case "flatrate": return "会员";
            case "rent": return "租赁";
            case "buy": return "购买";
            case "free": return "免费";
            default: return offerType;
        }
    }
}
