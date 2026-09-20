package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmNotification;

/**
 * 用户通知 Mapper
 *
 * @author kemovie
 */
public interface KmNotificationMapper
{
    public List<KmNotification> selectByUser(Long userId);

    public int countUnread(Long userId);

    public int insertNotification(KmNotification notification);

    public int markRead(@Param("userId") Long userId, @Param("notifyId") Long notifyId);

    public int markAllRead(Long userId);

    /** 查询用户邮箱（用于邮件渠道通知） */
    public String selectUserEmail(Long userId);

    /** 取用户微信 openid（公众号模板消息，无则空） */
    public String selectUserOpenid(Long userId);
}
