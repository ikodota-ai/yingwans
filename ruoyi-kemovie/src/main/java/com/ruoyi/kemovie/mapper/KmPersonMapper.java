package com.ruoyi.kemovie.mapper;

import java.util.List;
import com.ruoyi.kemovie.domain.KmPerson;

/**
 * 演职人员 Mapper
 *
 * @author kemovie
 */
public interface KmPersonMapper
{
    public KmPerson selectPersonById(Long personId);

    public KmPerson selectPersonByTmdbId(Long tmdbId);

    public List<KmPerson> selectPersonList(KmPerson person);

    public int insertPerson(KmPerson person);

    public int updatePerson(KmPerson person);

    public int deletePersonByIds(Long[] personIds);

    /** 迁移用：存在本地（/profile）头像的人物 */
    public List<java.util.Map<String, Object>> selectLocalImagePersons();
}
