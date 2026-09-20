package com.ruoyi.kemovie.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户订阅 km_user_subscription
 *
 * @author kemovie
 */
public class KmUserSubscription extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long subId;
    private Long userId;
    private Long mediaId;
    private String notifyChannel;
    private String notified;
    private String status;

    /** 关联影片（时间轴展示，非表字段） */
    private KmMedia media;

    public Long getSubId() { return subId; }
    public void setSubId(Long subId) { this.subId = subId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getNotifyChannel() { return notifyChannel; }
    public void setNotifyChannel(String notifyChannel) { this.notifyChannel = notifyChannel; }
    public String getNotified() { return notified; }
    public void setNotified(String notified) { this.notified = notified; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public KmMedia getMedia() { return media; }
    public void setMedia(KmMedia media) { this.media = media; }
}
