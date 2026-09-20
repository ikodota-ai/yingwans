package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmMediaResource;

/**
 * 影片下载资源 Mapper
 *
 * @author kemovie
 */
public interface KmMediaResourceMapper
{
    public List<KmMediaResource> selectByMediaId(Long mediaId);

    public int insertKmMediaResource(KmMediaResource resource);

    /** 更新单条资源（网盘失效维护，可清空提取码/分组名） */
    public int updateKmMediaResource(KmMediaResource resource);

    /** 删除某片某来源的全部资源（重导/更新前清理） */
    public int deleteByMediaAndSource(@Param("mediaId") Long mediaId, @Param("source") String source);

    /** 按主键删除单条资源 */
    public int deleteById(Long resourceId);
}
