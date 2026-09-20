package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmMdlSource;

/**
 * MDL 抓取源配置 Service
 *
 * @author kemovie
 */
public interface IKmMdlSourceService
{
    public KmMdlSource selectKmMdlSourceById(Long sourceId);

    public List<KmMdlSource> selectKmMdlSourceList(KmMdlSource kmMdlSource);

    public int insertKmMdlSource(KmMdlSource kmMdlSource);

    public int updateKmMdlSource(KmMdlSource kmMdlSource);

    public int deleteKmMdlSourceByIds(Long[] sourceIds);
}
