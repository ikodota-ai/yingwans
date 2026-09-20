package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmUserSubscription;

/**
 * 用户订阅 Service
 *
 * @author kemovie
 */
public interface IKmSubscriptionService
{
    /** 切换订阅，返回是否已订阅 */
    public boolean toggle(Long userId, Long mediaId);

    public boolean isSubscribed(Long userId, Long mediaId);

    /** 我的订阅时间轴 */
    public List<KmUserSubscription> timeline(Long userId);
}
