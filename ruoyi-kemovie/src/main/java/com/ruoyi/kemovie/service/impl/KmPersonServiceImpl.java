package com.ruoyi.kemovie.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.kemovie.client.TmdbClient;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaCast;
import com.ruoyi.kemovie.domain.KmPerson;
import com.ruoyi.kemovie.mapper.KmMediaCastMapper;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmPersonMapper;
import com.ruoyi.kemovie.service.IKmImageService;
import com.ruoyi.kemovie.service.IKmPersonService;

/**
 * 演职人员 Service 实现。
 *
 * <p>syncCastForMedia：拉取 TMDB credits → 对每个人物 upsert km_person → 建立
 * km_media_cast 关联；控制演员数量上限，避免龙套过多。</p>
 *
 * @author kemovie
 */
@Service
public class KmPersonServiceImpl implements IKmPersonService
{
    private static final Logger log = LoggerFactory.getLogger(KmPersonServiceImpl.class);

    /** 每部影片保留的演员上限（按 order 取前 N），剧组核心职务全部保留 */
    private static final int MAX_CAST = 20;

    @Autowired private TmdbClient tmdbClient;
    @Autowired private KmPersonMapper personMapper;
    @Autowired private KmMediaCastMapper castMapper;
    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private IKmImageService imageService;

    @Override
    public List<KmPerson> selectPersonList(KmPerson person)
    {
        return personMapper.selectPersonList(person);
    }

    @Override
    public KmPerson selectDetail(Long personId)
    {
        KmPerson p = personMapper.selectPersonById(personId);
        if (p == null) { return null; }
        p.setCredits(castMapper.selectCreditsByPersonId(personId));
        return p;
    }

    @Override
    public KmPerson selectPersonById(Long personId)
    {
        return personMapper.selectPersonById(personId);
    }

    @Override
    public List<KmMediaCast> selectCastByMediaId(Long mediaId)
    {
        return castMapper.selectCastByMediaId(mediaId);
    }

    @Override
    @Transactional
    public int syncCastForMedia(Long mediaId)
    {
        KmMedia m = mediaMapper.selectKmMediaByMediaId(mediaId);
        if (m == null || m.getTmdbId() == null) { return 0; }
        List<KmMediaCast> credits = tmdbClient.fetchCredits(m.getTmdbId(), m.getMediaType());
        if (credits == null || credits.isEmpty()) { return 0; }

        List<KmMediaCast> toInsert = new ArrayList<>();
        int castCount = 0;
        for (KmMediaCast c : credits)
        {
            if ("cast".equals(c.getCreditType()))
            {
                if (castCount >= MAX_CAST) { continue; }
                castCount++;
            }
            Long personId = upsertPerson(c.getPersonTmdbId(), c.getPersonName(), c.getProfilePath());
            if (personId == null) { continue; }
            c.setMediaId(mediaId);
            c.setPersonId(personId);
            toInsert.add(c);
        }
        castMapper.deleteByMediaId(mediaId);
        if (!toInsert.isEmpty())
        {
            castMapper.batchInsert(toInsert);
        }
        return toInsert.size();
    }

    /**
     * upsert 人物：已存在则返回其 personId；不存在则从 TMDB 拉详情落库。
     * 为控制外部请求量，仅当本地缺失时才请求 /person 详情；已有则只更新头像/姓名的轻量信息。
     */
    private Long upsertPerson(Long personTmdbId, String name, String profilePath)
    {
        if (personTmdbId == null) { return null; }
        KmPerson exist = personMapper.selectPersonByTmdbId(personTmdbId);
        if (exist != null) { return exist.getPersonId(); }
        KmPerson detail = tmdbClient.fetchPerson(personTmdbId);
        if (detail == null)
        {
            // 详情拉取失败时用 credits 里的基本信息兜底建档
            detail = new KmPerson();
            detail.setTmdbId(personTmdbId);
            detail.setName(name);
            detail.setProfilePath(profilePath);
        }
        detail.setCreateBy("tmdb-sync");
        try
        {
            personMapper.insertPerson(detail);
            localizeProfileQuietly(detail);
            return detail.getPersonId();
        }
        catch (Exception e)
        {
            // 并发下可能重复插入，回查一次
            KmPerson again = personMapper.selectPersonByTmdbId(personTmdbId);
            return again == null ? null : again.getPersonId();
        }
    }

    @Override
    @Transactional
    public KmPerson refreshPerson(Long personId)
    {
        KmPerson exist = personMapper.selectPersonById(personId);
        if (exist == null || exist.getTmdbId() == null) { return null; }
        KmPerson fresh = tmdbClient.fetchPerson(exist.getTmdbId());
        if (fresh == null) { return exist; }
        fresh.setPersonId(personId);
        fresh.setUpdateBy("tmdb-sync");
        personMapper.updatePerson(fresh);
        return personMapper.selectPersonById(personId);
    }

    /** 本地化头像，失败不影响主流程 */
    private void localizeProfileQuietly(KmPerson person)
    {
        try
        {
            imageService.localizePersonProfile(person);
        }
        catch (Exception e)
        {
            log.warn("本地化头像失败 personId={}", person == null ? null : person.getPersonId(), e);
        }
    }

    @Override
    public int deletePersonByIds(Long[] personIds)
    {
        return personMapper.deletePersonByIds(personIds);
    }
}
