package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmMediaCast;

/**
 * 影视-演职人员关联 Mapper
 *
 * @author kemovie
 */
public interface KmMediaCastMapper
{
    /** 某影视的演职人员（join km_person，按 creditType+order 排序） */
    public List<KmMediaCast> selectCastByMediaId(Long mediaId);

    /** 某人物在站内已收录影片中的参演（join km_media） */
    public List<KmMediaCast> selectCreditsByPersonId(Long personId);

    public int deleteByMediaId(Long mediaId);

    public int batchInsert(@Param("list") List<KmMediaCast> list);
}
