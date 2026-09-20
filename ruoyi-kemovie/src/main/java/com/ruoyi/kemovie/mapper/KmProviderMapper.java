package com.ruoyi.kemovie.mapper;

import java.util.List;
import com.ruoyi.kemovie.domain.KmProvider;

/**
 * 自定义播放服务商 Mapper
 *
 * @author kemovie
 */
public interface KmProviderMapper
{
    public KmProvider selectKmProviderById(Long providerId);

    public List<KmProvider> selectKmProviderList(KmProvider kmProvider);

    /** 全部启用的服务商（排序） */
    public List<KmProvider> selectEnabled();

    /** 影片已勾选的服务商 ID（release source=custom 按平台名关联） */
    public List<Long> selectMediaProviderIds(Long mediaId);

    public int insertKmProvider(KmProvider kmProvider);

    public int updateKmProvider(KmProvider kmProvider);

    public int deleteKmProviderByIds(Long[] providerIds);
}
