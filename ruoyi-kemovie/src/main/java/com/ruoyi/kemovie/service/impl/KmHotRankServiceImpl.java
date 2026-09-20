package com.ruoyi.kemovie.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.kemovie.domain.KmHotRank;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmHotRankMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.service.IKmHotRankService;

/**
 * 热门排行榜 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmHotRankServiceImpl implements IKmHotRankService
{
    @Autowired private KmHotRankMapper hotRankMapper;
    @Autowired private KmMediaMapper mediaMapper;

    @Override
    public List<KmHotRank> latest(String rankType, int limit)
    {
        return hotRankMapper.selectLatestRank(rankType, limit);
    }

    @Override
    @Transactional
    public int rebuild(String rankType, int topN)
    {
        // 取按 hot_score 排序的前 topN 可见影片
        KmMedia query = new KmMedia();
        query.setVisible("0");
        List<KmMedia> list = mediaMapper.selectPublicMediaList(query);
        hotRankMapper.deleteByType(rankType);
        Date today = new Date();
        int no = 1;
        for (KmMedia m : list)
        {
            if (no > topN) { break; }
            KmHotRank r = new KmHotRank();
            r.setRankType(rankType);
            r.setRankNo(no++);
            r.setMediaId(m.getMediaId());
            r.setHotScore(m.getHotScore());
            r.setSnapshotDate(today);
            hotRankMapper.insertRank(r);
        }
        return no - 1;
    }
}
