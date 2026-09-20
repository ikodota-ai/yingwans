package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmCollection;

/**
 * 片单 Mapper
 *
 * @author kemovie
 */
public interface KmCollectionMapper
{
    public KmCollection selectKmCollectionById(Long collectionId);

    public KmCollection selectBySourceRef(String sourceRef);

    public List<KmCollection> selectKmCollectionList(KmCollection kmCollection);

    public int insertKmCollection(KmCollection kmCollection);

    public int updateKmCollection(KmCollection kmCollection);

    public int updateItemCount(Long collectionId);

    public int deleteKmCollectionByIds(Long[] collectionIds);

    // ---------- 片单关注 ----------
    /** 会员关注的公共片单列表 */
    public List<KmCollection> selectFollowedByMember(@Param("memberId") Long memberId);

    /** 是否已关注 */
    public int countFollow(@Param("memberId") Long memberId, @Param("collectionId") Long collectionId);

    /** 新增关注（去重靠唯一键） */
    public int insertFollow(@Param("memberId") Long memberId, @Param("collectionId") Long collectionId);

    /** 取消关注 */
    public int deleteFollow(@Param("memberId") Long memberId, @Param("collectionId") Long collectionId);
}
