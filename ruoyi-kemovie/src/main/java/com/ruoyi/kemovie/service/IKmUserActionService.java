package com.ruoyi.kemovie.service;

import java.math.BigDecimal;
import com.ruoyi.kemovie.domain.KmUserMediaAction;

/**
 * 用户影片行为 Service
 *
 * @author kemovie
 */
public interface IKmUserActionService
{
    public KmUserMediaAction get(Long userId, Long mediaId);

    /** 切换点赞，返回最新状态 */
    public KmUserMediaAction toggleLike(Long userId, Long mediaId);

    /** 切换想看 */
    public KmUserMediaAction toggleWish(Long userId, Long mediaId);

    /** 切换看过 */
    public KmUserMediaAction toggleWatched(Long userId, Long mediaId);

    /** 评分（0-10，传 null 清除） */
    public KmUserMediaAction rate(Long userId, Long mediaId, BigDecimal rating);
}
