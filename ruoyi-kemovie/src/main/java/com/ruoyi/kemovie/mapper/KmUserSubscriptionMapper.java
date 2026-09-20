package com.ruoyi.kemovie.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmUserSubscription;

/**
 * 用户订阅 Mapper
 *
 * @author kemovie
 */
public interface KmUserSubscriptionMapper
{
    public KmUserSubscription selectByUserAndMedia(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    /** 我的订阅时间轴（带影片信息，按上线日期） */
    public List<KmUserSubscription> selectTimelineByUser(Long userId);

    /** 查某片所有有效订阅者（上线通知用） */
    public List<KmUserSubscription> selectActiveByMedia(Long mediaId);

    /** 查某片全部有效订阅者（资源文档通知用，不限 notified） */
    public List<KmUserSubscription> selectAllActiveByMedia(Long mediaId);

    /** 订阅汇总：按影片统计订阅人数（后台订阅管理） */
    public List<Map<String, Object>> selectSummary();

    /** 订阅动态：最近的订阅/取消事件（带用户与影片名） */
    public List<Map<String, Object>> selectEvents();

    public int insertSubscription(KmUserSubscription sub);

    public int updateSubscription(KmUserSubscription sub);
}
