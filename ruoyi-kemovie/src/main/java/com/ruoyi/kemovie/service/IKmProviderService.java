package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmProvider;

/**
 * 自定义播放服务商 Service
 *
 * @author kemovie
 */
public interface IKmProviderService
{
    public KmProvider selectKmProviderById(Long providerId);

    public List<KmProvider> selectKmProviderList(KmProvider kmProvider);

    /** 全部启用的服务商（排序） */
    public List<KmProvider> selectEnabled();

    public int insertKmProvider(KmProvider kmProvider);

    public int updateKmProvider(KmProvider kmProvider);

    public int deleteKmProviderByIds(Long[] providerIds);

    /** 影片已勾选的服务商 ID 列表（km_media_release source=custom） */
    public List<Long> selectMediaProviderIds(Long mediaId);

    /** 重置影片的服务商勾选：删旧（source=custom）写新 */
    public int syncMediaProviders(Long mediaId, List<Long> providerIds);
}
