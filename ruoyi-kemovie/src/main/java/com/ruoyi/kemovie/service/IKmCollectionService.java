package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmMedia;

/**
 * 片单 Service 接口
 *
 * @author kemovie
 */
public interface IKmCollectionService
{
    public KmCollection selectKmCollectionById(Long collectionId);

    /** 片单详情（含影片列表、封面拼贴） */
    public KmCollection selectDetail(Long collectionId);

    public List<KmCollection> selectKmCollectionList(KmCollection kmCollection);

    public List<KmMedia> selectMediaOfCollection(Long collectionId);

    /** 分页 + 筛选查询片单内影片（配合 PageHelper startPage） */
    public List<KmMedia> selectMediaOfCollectionPaged(Long collectionId, KmMedia query);

    public int insertKmCollection(KmCollection kmCollection);

    public int updateKmCollection(KmCollection kmCollection);

    public int deleteKmCollectionByIds(Long[] collectionIds);

    /** 向片单添加影片（去重） */
    public int addMedia(Long collectionId, Long mediaId);

    /** 从片单移除影片 */
    public int removeMedia(Long collectionId, Long mediaId);

    /** 会员关注的公共片单 */
    public List<KmCollection> selectFollowedCollections(Long memberId);

    /** 关注片单 */
    public int followCollection(Long memberId, Long collectionId);

    /** 取消关注片单 */
    public int unfollowCollection(Long memberId, Long collectionId);

    /** 是否已关注 */
    public boolean isFollowed(Long memberId, Long collectionId);
}
