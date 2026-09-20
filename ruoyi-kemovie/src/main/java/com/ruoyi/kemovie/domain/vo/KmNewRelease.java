package com.ruoyi.kemovie.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 前台“最近上新”条目：影片 + 区域 + 上线日期 + 观看平台汇总
 *
 * @author kemovie
 */
public class KmNewRelease
{
    private Long mediaId;
    private String title;
    private String mediaType;
    private String posterPath;
    private String posterLocalUrl;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date releaseDate;
    private String region;
    private Double ratingAvg;
    private Double voteAverage;
    /** 上新区域（ISO 两位，如 HK/TW/US） */
    private String country;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date onlineDate;
    /** 观看平台（逗号分隔） */
    private String platforms;
    /** 观看方式（逗号分隔，flatrate/rent/buy/free/ads） */
    private String offerTypes;
    /** 任一直达链接 */
    private String link;

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getPosterLocalUrl() { return posterLocalUrl; }
    public void setPosterLocalUrl(String posterLocalUrl) { this.posterLocalUrl = posterLocalUrl; }
    public Date getReleaseDate() { return releaseDate; }
    public void setReleaseDate(Date releaseDate) { this.releaseDate = releaseDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Double getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(Double ratingAvg) { this.ratingAvg = ratingAvg; }
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Date getOnlineDate() { return onlineDate; }
    public void setOnlineDate(Date onlineDate) { this.onlineDate = onlineDate; }
    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }
    public String getOfferTypes() { return offerTypes; }
    public void setOfferTypes(String offerTypes) { this.offerTypes = offerTypes; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
}
