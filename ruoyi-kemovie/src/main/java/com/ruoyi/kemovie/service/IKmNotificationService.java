package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmNotification;

/**
 * 通知 Service（站内信 + 邮件，预留 wechat）
 *
 * @author kemovie
 */
public interface IKmNotificationService
{
    public List<KmNotification> listByUser(Long userId);

    public int unreadCount(Long userId);

    public int markRead(Long userId, Long notifyId);

    public int markAllRead(Long userId);

    /** 发送通知（站内 + 按渠道尝试邮件） */
    public int send(KmNotification notification);

    /** 订阅成功后：若影片已有资源文档，立即把文档地址通知该用户；返回是否已发送 */
    public boolean sendResourceDocIfAvailable(Long userId, Long mediaId);

    /** 资源文档生成/更新后：推送给该片全部有效订阅者，返回发送数 */
    public int notifySubscribersResourceDoc(Long mediaId);

    /** 扫描已上线订阅并推送提醒，返回发送数 */
    public int dispatchReleaseNotifications();
}
