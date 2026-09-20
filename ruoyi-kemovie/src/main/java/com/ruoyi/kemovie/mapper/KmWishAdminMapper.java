package com.ruoyi.kemovie.mapper;

import java.util.List;
import com.ruoyi.kemovie.domain.vo.KmWishMedia;
import com.ruoyi.kemovie.domain.vo.KmWishUser;

/**
 * 想看运营视图 Mapper
 *
 * @author kemovie
 */
public interface KmWishAdminMapper
{
    /** 按影片聚合想看统计（想看人数倒序） */
    public List<KmWishMedia> selectWishMediaList(KmWishMedia query);

    /** 某片的想看用户明细（最多 500 条） */
    public List<KmWishUser> selectWishUsers(Long mediaId);
}
