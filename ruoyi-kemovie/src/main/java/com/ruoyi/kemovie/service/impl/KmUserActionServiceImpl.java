package com.ruoyi.kemovie.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.kemovie.domain.KmUserMediaAction;
import com.ruoyi.kemovie.mapper.KmUserMediaActionMapper;
import com.ruoyi.kemovie.service.IKmMediaService;
import com.ruoyi.kemovie.service.IKmUserActionService;

/**
 * 用户影片行为 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmUserActionServiceImpl implements IKmUserActionService
{
    @Autowired private KmUserMediaActionMapper actionMapper;
    @Autowired private IKmMediaService mediaService;

    @Override
    public KmUserMediaAction get(Long userId, Long mediaId)
    {
        return actionMapper.selectByUserAndMedia(userId, mediaId);
    }

    private KmUserMediaAction ensure(Long userId, Long mediaId)
    {
        KmUserMediaAction a = actionMapper.selectByUserAndMedia(userId, mediaId);
        if (a == null)
        {
            a = new KmUserMediaAction();
            a.setUserId(userId);
            a.setMediaId(mediaId);
            a.setLiked("0");
            a.setWished("0");
            a.setWatched("0");
            actionMapper.insertAction(a);
        }
        return a;
    }

    @Override
    @Transactional
    public KmUserMediaAction toggleLike(Long userId, Long mediaId)
    {
        KmUserMediaAction a = ensure(userId, mediaId);
        boolean on = !"1".equals(a.getLiked());
        a.setLiked(on ? "1" : "0");
        a.setLikedTime(on ? new Date() : null);
        // 只更新 liked 字段
        KmUserMediaAction upd = key(userId, mediaId);
        upd.setLiked(a.getLiked());
        upd.setLikedTime(a.getLikedTime());
        actionMapper.updateAction(upd);
        mediaService.refreshMediaStats(mediaId);
        return actionMapper.selectByUserAndMedia(userId, mediaId);
    }

    @Override
    @Transactional
    public KmUserMediaAction toggleWish(Long userId, Long mediaId)
    {
        KmUserMediaAction a = ensure(userId, mediaId);
        boolean on = !"1".equals(a.getWished());
        KmUserMediaAction upd = key(userId, mediaId);
        upd.setWished(on ? "1" : "0");
        upd.setWishedTime(on ? new Date() : null);
        actionMapper.updateAction(upd);
        mediaService.refreshMediaStats(mediaId);
        return actionMapper.selectByUserAndMedia(userId, mediaId);
    }

    @Override
    @Transactional
    public KmUserMediaAction toggleWatched(Long userId, Long mediaId)
    {
        KmUserMediaAction a = ensure(userId, mediaId);
        boolean on = !"1".equals(a.getWatched());
        KmUserMediaAction upd = key(userId, mediaId);
        upd.setWatched(on ? "1" : "0");
        upd.setWatchedTime(on ? new Date() : null);
        actionMapper.updateAction(upd);
        mediaService.refreshMediaStats(mediaId);
        return actionMapper.selectByUserAndMedia(userId, mediaId);
    }

    @Override
    @Transactional
    public KmUserMediaAction rate(Long userId, Long mediaId, BigDecimal rating)
    {
        ensure(userId, mediaId);
        actionMapper.updateRating(userId, mediaId, rating, rating == null ? null : new Date());
        mediaService.refreshMediaStats(mediaId);
        return actionMapper.selectByUserAndMedia(userId, mediaId);
    }

    private KmUserMediaAction key(Long userId, Long mediaId)
    {
        KmUserMediaAction a = new KmUserMediaAction();
        a.setUserId(userId);
        a.setMediaId(mediaId);
        return a;
    }
}
