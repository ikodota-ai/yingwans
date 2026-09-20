package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmCollectionItem;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.mapper.KmCollectionItemMapper;
import com.ruoyi.kemovie.mapper.KmCollectionMapper;
import com.ruoyi.kemovie.service.IKmCollectionService;

/**
 * 片单 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmCollectionServiceImpl implements IKmCollectionService
{
    @Autowired private KmCollectionMapper collectionMapper;
    @Autowired private KmCollectionItemMapper itemMapper;

    @Override
    public KmCollection selectKmCollectionById(Long collectionId)
    {
        return collectionMapper.selectKmCollectionById(collectionId);
    }

    @Override
    public KmCollection selectDetail(Long collectionId)
    {
        KmCollection c = collectionMapper.selectKmCollectionById(collectionId);
        if (c == null) { return null; }
        c.setCoverPosters(itemMapper.selectCoverPosters(collectionId, 4));
        return c;
    }

    @Override
    public List<KmCollection> selectKmCollectionList(KmCollection kmCollection)
    {
        List<KmCollection> list = collectionMapper.selectKmCollectionList(kmCollection);
        for (KmCollection c : list)
        {
            c.setCoverPosters(itemMapper.selectCoverPosters(c.getCollectionId(), 4));
        }
        return list;
    }

    @Override
    public List<KmMedia> selectMediaOfCollection(Long collectionId)
    {
        return itemMapper.selectMediaByCollectionId(collectionId);
    }

    @Override
    public List<KmMedia> selectMediaOfCollectionPaged(Long collectionId, KmMedia query)
    {
        return itemMapper.selectMediaOfCollectionPaged(collectionId, query);
    }

    @Override
    public int insertKmCollection(KmCollection kmCollection)
    {
        return collectionMapper.insertKmCollection(kmCollection);
    }

    @Override
    public int updateKmCollection(KmCollection kmCollection)
    {
        return collectionMapper.updateKmCollection(kmCollection);
    }

    @Override
    @Transactional
    public int deleteKmCollectionByIds(Long[] collectionIds)
    {
        return collectionMapper.deleteKmCollectionByIds(collectionIds);
    }

    @Override
    @Transactional
    public int addMedia(Long collectionId, Long mediaId)
    {
        if (itemMapper.countItem(collectionId, mediaId) > 0) { return 0; }
        KmCollectionItem item = new KmCollectionItem();
        item.setCollectionId(collectionId);
        item.setMediaId(mediaId);
        item.setSort(0);
        int r = itemMapper.insertItem(item);
        collectionMapper.updateItemCount(collectionId);
        return r;
    }

    @Override
    @Transactional
    public int removeMedia(Long collectionId, Long mediaId)
    {
        int r = itemMapper.deleteItem(collectionId, mediaId);
        collectionMapper.updateItemCount(collectionId);
        return r;
    }

    @Override
    public List<KmCollection> selectFollowedCollections(Long memberId)
    {
        List<KmCollection> list = collectionMapper.selectFollowedByMember(memberId);
        for (KmCollection c : list)
        {
            c.setCoverPosters(itemMapper.selectCoverPosters(c.getCollectionId(), 4));
            c.setFollowed(true);
        }
        return list;
    }

    @Override
    public int followCollection(Long memberId, Long collectionId)
    {
        return collectionMapper.insertFollow(memberId, collectionId);
    }

    @Override
    public int unfollowCollection(Long memberId, Long collectionId)
    {
        return collectionMapper.deleteFollow(memberId, collectionId);
    }

    @Override
    public boolean isFollowed(Long memberId, Long collectionId)
    {
        if (memberId == null) { return false; }
        return collectionMapper.countFollow(memberId, collectionId) > 0;
    }
}
