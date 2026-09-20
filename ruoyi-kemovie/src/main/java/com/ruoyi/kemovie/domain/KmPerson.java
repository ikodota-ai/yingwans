package com.ruoyi.kemovie.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 演职人员 km_person（来源 TMDB /person）
 *
 * @author kemovie
 */
public class KmPerson extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long personId;
    private Long tmdbId;
    private String imdbId;
    private String name;
    private String originalName;
    private String alsoKnownAs;
    private Integer gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date deathday;
    private String placeOfBirth;
    private String knownFor;
    private String biography;
    private String profilePath;
    private String profileLocalUrl;
    private BigDecimal popularity;
    private String homepage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncedAt;
    private String delFlag;

    /** 关联：参演作品（详情页用，非表字段） */
    private List<KmMediaCast> credits;

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getAlsoKnownAs() { return alsoKnownAs; }
    public void setAlsoKnownAs(String alsoKnownAs) { this.alsoKnownAs = alsoKnownAs; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }
    public Date getDeathday() { return deathday; }
    public void setDeathday(Date deathday) { this.deathday = deathday; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    public String getKnownFor() { return knownFor; }
    public void setKnownFor(String knownFor) { this.knownFor = knownFor; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getProfileLocalUrl() { return profileLocalUrl; }
    public void setProfileLocalUrl(String profileLocalUrl) { this.profileLocalUrl = profileLocalUrl; }
    public BigDecimal getPopularity() { return popularity; }
    public void setPopularity(BigDecimal popularity) { this.popularity = popularity; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public Date getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Date lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<KmMediaCast> getCredits() { return credits; }
    public void setCredits(List<KmMediaCast> credits) { this.credits = credits; }
}
