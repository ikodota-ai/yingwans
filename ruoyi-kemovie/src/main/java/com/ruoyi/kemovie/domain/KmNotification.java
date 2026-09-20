package com.ruoyi.kemovie.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户通知记录 km_notification
 *
 * @author kemovie
 */
public class KmNotification extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long notifyId;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Long mediaId;
    private String channel;
    private String isRead;
    private String sendStatus;

    public Long getNotifyId() { return notifyId; }
    public void setNotifyId(Long notifyId) { this.notifyId = notifyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getIsRead() { return isRead; }
    public void setIsRead(String isRead) { this.isRead = isRead; }
    public String getSendStatus() { return sendStatus; }
    public void setSendStatus(String sendStatus) { this.sendStatus = sendStatus; }
}
