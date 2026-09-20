package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.domain.KmMediaRelease;
import com.ruoyi.kemovie.domain.KmProvider;
import com.ruoyi.kemovie.mapper.KmMediaReleaseMapper;
import com.ruoyi.kemovie.mapper.KmProviderMapper;
import com.ruoyi.kemovie.service.IKmProviderService;

/**
 * 自定义播放服务商 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmProviderServiceImpl implements IKmProviderService
{
    @Autowired private KmProviderMapper providerMapper;
    @Autowired private KmMediaReleaseMapper releaseMapper;

    @Override
    public KmProvider selectKmProviderById(Long providerId)
    {
        return providerMapper.selectKmProviderById(providerId);
    }

    @Override
    public List<KmProvider> selectKmProviderList(KmProvider kmProvider)
    {
        return providerMapper.selectKmProviderList(kmProvider);
    }

    @Override
    public List<KmProvider> selectEnabled()
    {
        return providerMapper.selectEnabled();
    }

    @Override
    public int insertKmProvider(KmProvider kmProvider)
    {
        return providerMapper.insertKmProvider(kmProvider);
    }

    @Override
    @Transactional
    public int updateKmProvider(KmProvider kmProvider)
    {
        KmProvider old = providerMapper.selectKmProviderById(kmProvider.getProviderId());
        int n = providerMapper.updateKmProvider(kmProvider);
        // 名称/Logo/官网变更时，同步已勾选影片上的展示信息（release 按平台名展示）
        if (old != null && StringUtils.isNotEmpty(kmProvider.getName()) && !old.getName().equals(kmProvider.getName()))
        {
            releaseMapper.updateCustomPlatformName(old.getName(), kmProvider.getName());
        }
        if (StringUtils.isNotEmpty(kmProvider.getLogoUrl()) && !kmProvider.getLogoUrl().equals(old == null ? null : old.getLogoUrl()))
        {
            releaseMapper.updateCustomPlatformLogo(kmProvider.getName(), kmProvider.getLogoUrl());
        }
        if (StringUtils.isNotEmpty(kmProvider.getSiteUrl()) && !kmProvider.getSiteUrl().equals(old == null ? null : old.getSiteUrl()))
        {
            releaseMapper.updateCustomPlatformLink(kmProvider.getName(), kmProvider.getSiteUrl());
        }
        return n;
    }

    @Override
    @Transactional
    public int deleteKmProviderByIds(Long[] providerIds)
    {
        // 删除服务商同时清除影片上的勾选展示
        for (Long id : providerIds)
        {
            KmProvider p = providerMapper.selectKmProviderById(id);
            if (p != null)
            {
                releaseMapper.deleteCustomByPlatform(p.getName());
            }
        }
        return providerMapper.deleteKmProviderByIds(providerIds);
    }

    @Override
    public List<Long> selectMediaProviderIds(Long mediaId)
    {
        return providerMapper.selectMediaProviderIds(mediaId);
    }

    @Override
    @Transactional
    public int syncMediaProviders(Long mediaId, List<Long> providerIds)
    {
        releaseMapper.deleteCustomByMediaId(mediaId);
        int n = 0;
        if (providerIds == null) { return n; }
        for (Long pid : providerIds)
        {
            if (pid == null) { continue; }
            KmProvider p = providerMapper.selectKmProviderById(pid);
            if (p == null || !"0".equals(p.getEnabled())) { continue; }
            KmMediaRelease r = new KmMediaRelease();
            r.setMediaId(mediaId);
            r.setPlatform(p.getName());
            r.setPlatformLogo(p.getLogoUrl());
            r.setCountry(p.getRegion());
            r.setOfferType("flatrate");
            r.setLink(p.getSiteUrl());
            r.setStatus("available");
            r.setSource("custom");
            releaseMapper.insertKmMediaRelease(r);
            n++;
        }
        return n;
    }
}
