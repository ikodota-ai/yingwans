package com.ruoyi.kemovie.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmUserMediaAction;

/**
 * 用户影片行为 Mapper
 *
 * @author kemovie
 */
public interface KmUserMediaActionMapper
{
    public KmUserMediaAction selectByUserAndMedia(@Param("userId") Long userId, @Param("mediaId") Long mediaId);

    public int insertAction(KmUserMediaAction action);

    public int updateAction(KmUserMediaAction action);

    /** 显式更新评分（支持置空清除） */
    public int updateRating(@Param("userId") Long userId, @Param("mediaId") Long mediaId,
            @Param("rating") java.math.BigDecimal rating, @Param("ratingTime") java.util.Date ratingTime);
}
