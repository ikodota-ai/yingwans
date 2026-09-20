package com.ruoyi.kemovie.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 影视-演职人员关联 km_media_cast
 *
 * @author kemovie
 */
public class KmMediaCast extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long mediaId;
    private Long personId;
    /** cast演员 / crew剧组 */
    private String creditType;
    private String department;
    private String job;
    /** 饰演角色 */
    private String character;
    private Integer castOrder;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // 关联展示字段（join 出来，非本表列）
    private String personName;
    private String profilePath;
    private String profileLocalUrl;
    private Long personTmdbId;
    // 人物作品页展示影片信息
    private String mediaTitle;
    private String mediaPosterPath;
    private String mediaPosterLocalUrl;
    private String mediaType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date mediaReleaseDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public String getCreditType() { return creditType; }
    public void setCreditType(String creditType) { this.creditType = creditType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public Integer getCastOrder() { return castOrder; }
    public void setCastOrder(Integer castOrder) { this.castOrder = castOrder; }
    @Override
    public Date getCreateTime() { return createTime; }
    @Override
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getProfileLocalUrl() { return profileLocalUrl; }
    public void setProfileLocalUrl(String profileLocalUrl) { this.profileLocalUrl = profileLocalUrl; }
    public Long getPersonTmdbId() { return personTmdbId; }
    public void setPersonTmdbId(Long personTmdbId) { this.personTmdbId = personTmdbId; }
    public String getMediaTitle() { return mediaTitle; }
    public void setMediaTitle(String mediaTitle) { this.mediaTitle = mediaTitle; }
    public String getMediaPosterPath() { return mediaPosterPath; }
    public void setMediaPosterPath(String mediaPosterPath) { this.mediaPosterPath = mediaPosterPath; }
    public String getMediaPosterLocalUrl() { return mediaPosterLocalUrl; }
    public void setMediaPosterLocalUrl(String mediaPosterLocalUrl) { this.mediaPosterLocalUrl = mediaPosterLocalUrl; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public Date getMediaReleaseDate() { return mediaReleaseDate; }
    public void setMediaReleaseDate(Date mediaReleaseDate) { this.mediaReleaseDate = mediaReleaseDate; }
}
