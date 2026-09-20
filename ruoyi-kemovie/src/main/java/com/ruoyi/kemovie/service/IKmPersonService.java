package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmMediaCast;
import com.ruoyi.kemovie.domain.KmPerson;

/**
 * 演职人员 Service
 *
 * @author kemovie
 */
public interface IKmPersonService
{
    /** 后台列表 */
    public List<KmPerson> selectPersonList(KmPerson person);

    /** 人物详情（含站内参演作品） */
    public KmPerson selectDetail(Long personId);

    /** 按内部ID取人物 */
    public KmPerson selectPersonById(Long personId);

    /** 某影视的演职人员列表 */
    public List<KmMediaCast> selectCastByMediaId(Long mediaId);

    /** 从 TMDB 同步某影视的演职人员（会顺带 upsert 涉及人物） */
    public int syncCastForMedia(Long mediaId);

    /** 从 TMDB 刷新单个人物档案（按内部ID） */
    public KmPerson refreshPerson(Long personId);

    public int deletePersonByIds(Long[] personIds);
}
