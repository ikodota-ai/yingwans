package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmHotRank;

/**
 * 热门排行榜 Mapper
 *
 * @author kemovie
 */
public interface KmHotRankMapper
{
    /** 最新一期榜单（带影片） */
    public List<KmHotRank> selectLatestRank(@Param("rankType") String rankType, @Param("limit") int limit);

    public int insertRank(KmHotRank rank);

    /** 清空某类型旧快照 */
    public int deleteByType(String rankType);
}
