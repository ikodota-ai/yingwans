package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmUserMediaAction;
import com.ruoyi.kemovie.mapper.KmMediaCastMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaReleaseMapper;
import com.ruoyi.kemovie.mapper.KmUserMediaActionMapper;
import com.ruoyi.kemovie.mapper.KmUserSubscriptionMapper;
import com.ruoyi.kemovie.service.IKmMediaService;
import com.ruoyi.kemovie.service.IKmProviderService;

/**
 * 影视条目 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmMediaServiceImpl implements IKmMediaService
{
    @Autowired private KmMediaMapper kmMediaMapper;
    @Autowired private KmMediaReleaseMapper releaseMapper;
    @Autowired private KmUserMediaActionMapper actionMapper;
    @Autowired private KmUserSubscriptionMapper subscriptionMapper;
    @Autowired private KmMediaCastMapper castMapper;
    @Autowired private IKmProviderService providerService;

    @Override
    public KmMedia selectKmMediaByMediaId(Long mediaId)
    {
        return kmMediaMapper.selectKmMediaByMediaId(mediaId);
    }

    @Override
    public KmMedia selectDetail(Long mediaId, Long userId)
    {
        KmMedia media = kmMediaMapper.selectKmMediaByMediaId(mediaId);
        if (media == null) { return null; }
        media.setReleases(releaseMapper.selectByMediaId(mediaId));
        media.setCast(castMapper.selectCastByMediaId(mediaId));
        if (userId != null)
        {
            media.setMyAction(actionMapper.selectByUserAndMedia(userId, mediaId));
            com.ruoyi.kemovie.domain.KmUserSubscription sub = subscriptionMapper.selectByUserAndMedia(userId, mediaId);
            media.setSubscribed(sub != null && "0".equals(sub.getStatus()));
        }
        return media;
    }

    @Override
    public List<KmMedia> selectKmMediaList(KmMedia kmMedia)
    {
        return kmMediaMapper.selectKmMediaList(kmMedia);
    }

    @Override
    public List<KmMedia> selectPublicMediaList(KmMedia kmMedia)
    {
        return kmMediaMapper.selectPublicMediaList(kmMedia);
    }

    @Override
    public List<KmMedia> selectRecentReleases(int limit)
    {
        return kmMediaMapper.selectRecentReleases(limit);
    }

    @Override
    public List<KmMedia> selectRecentCollected(int limit)
    {
        return kmMediaMapper.selectRecentCollected(limit);
    }

    @Override
    public List<com.ruoyi.kemovie.domain.vo.KmNewRelease> selectNewReleases(String country, int days, int limit)
    {
        return kmMediaMapper.selectNewReleases(country, days, limit);
    }

    @Override
    public List<Integer> selectDistinctYears()
    {
        return kmMediaMapper.selectDistinctYears();
    }

    @Override
    public List<String> selectDistinctPlatforms()
    {
        return kmMediaMapper.selectDistinctPlatforms();
    }

    @Override
    public List<String> selectDistinctRegions()
    {
        return kmMediaMapper.selectDistinctRegions();
    }

    @Override
    public List<java.util.Map<String, Object>> selectPlatformLogos()
    {
        return kmMediaMapper.selectPlatformLogos();
    }

    @Override
    public List<KmMedia> selectLibrary(Long userId, String tab)
    {
        return kmMediaMapper.selectLibrary(userId, tab);
    }

    @Override
    public int insertKmMedia(KmMedia kmMedia)
    {
        return kmMediaMapper.insertKmMedia(kmMedia);
    }

    @Override
    public int updateKmMedia(KmMedia kmMedia)
    {
        int n = kmMediaMapper.updateKmMedia(kmMedia);
        // 后台编辑保存时同步自定义服务商勾选（null 表示未提交该字段，不动）
        if (kmMedia.getProviderIds() != null)
        {
            providerService.syncMediaProviders(kmMedia.getMediaId(), kmMedia.getProviderIds());
        }
        return n;
    }

    @Override
    public int deleteKmMediaByMediaIds(Long[] mediaIds)
    {
        return kmMediaMapper.deleteKmMediaByMediaIds(mediaIds);
    }

    @Override
    public void refreshMediaStats(Long mediaId)
    {
        kmMediaMapper.refreshAggregates(mediaId);
        kmMediaMapper.refreshHotScore(mediaId);
    }
}
