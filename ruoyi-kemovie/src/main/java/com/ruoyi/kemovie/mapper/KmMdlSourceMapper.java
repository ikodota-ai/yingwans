package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmMdlSource;

/**
 * MDL 抓取源 Mapper
 *
 * @author kemovie
 */
public interface KmMdlSourceMapper
{
    public KmMdlSource selectKmMdlSourceById(Long sourceId);

    public List<KmMdlSource> selectKmMdlSourceList(KmMdlSource kmMdlSource);

    public List<KmMdlSource> selectEnabledSources();

    /** 查询指定来源类型下所有启用的抓取源 */
    public List<KmMdlSource> selectEnabledByType(@Param("sourceType") String sourceType);

    public int insertKmMdlSource(KmMdlSource kmMdlSource);

    public int updateKmMdlSource(KmMdlSource kmMdlSource);

    public int deleteKmMdlSourceByIds(Long[] sourceIds);
}
