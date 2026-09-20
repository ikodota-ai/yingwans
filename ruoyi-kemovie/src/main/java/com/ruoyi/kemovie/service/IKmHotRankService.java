package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmHotRank;

/**
 * 热门排行榜 Service
 *
 * @author kemovie
 */
public interface IKmHotRankService
{
    /** 取最新一期榜单 */
    public List<KmHotRank> latest(String rankType, int limit);

    /** 重新计算并生成快照 */
    public int rebuild(String rankType, int topN);
}
