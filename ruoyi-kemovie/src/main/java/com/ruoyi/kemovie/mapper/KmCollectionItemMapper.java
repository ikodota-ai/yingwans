package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmCollectionItem;
import com.ruoyi.kemovie.domain.KmMedia;

/**
 * 片单影片关联 Mapper
 *
 * @author kemovie
 */
public interface KmCollectionItemMapper
{
    public List<KmMedia> selectMediaByCollectionId(Long collectionId);

    /** 分页 + 筛选查询片单内影片（配合 PageHelper startPage 使用） */
    public List<KmMedia> selectMediaOfCollectionPaged(@Param("collectionId") Long collectionId, @Param("q") KmMedia query);

    public List<String> selectCoverPosters(@Param("collectionId") Long collectionId, @Param("limit") int limit);

    public int countItem(@Param("collectionId") Long collectionId, @Param("mediaId") Long mediaId);

    public int insertItem(KmCollectionItem item);

    public int deleteItem(@Param("collectionId") Long collectionId, @Param("mediaId") Long mediaId);

    public int deleteByCollectionId(Long collectionId);
}
