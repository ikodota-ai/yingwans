package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.kemovie.domain.KmUserSubscription;
import com.ruoyi.kemovie.mapper.KmUserSubscriptionMapper;
import com.ruoyi.kemovie.service.IKmSubscriptionService;

/**
 * 用户订阅 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmSubscriptionServiceImpl implements IKmSubscriptionService
{
    @Autowired private KmUserSubscriptionMapper subscriptionMapper;

    @Override
    public boolean toggle(Long userId, Long mediaId)
    {
        KmUserSubscription sub = subscriptionMapper.selectByUserAndMedia(userId, mediaId);
        if (sub == null)
        {
            KmUserSubscription s = new KmUserSubscription();
            s.setUserId(userId);
            s.setMediaId(mediaId);
            subscriptionMapper.insertSubscription(s);
            return true;
        }
        boolean nowActive = !"0".equals(sub.getStatus());
        KmUserSubscription upd = new KmUserSubscription();
        upd.setUserId(userId);
        upd.setMediaId(mediaId);
        upd.setStatus(nowActive ? "0" : "1");
        if (nowActive) { upd.setNotified("0"); }
        subscriptionMapper.updateSubscription(upd);
        return nowActive;
    }

    @Override
    public boolean isSubscribed(Long userId, Long mediaId)
    {
        KmUserSubscription sub = subscriptionMapper.selectByUserAndMedia(userId, mediaId);
        return sub != null && "0".equals(sub.getStatus());
    }

    @Override
    public List<KmUserSubscription> timeline(Long userId)
    {
        return subscriptionMapper.selectTimelineByUser(userId);
    }
}
