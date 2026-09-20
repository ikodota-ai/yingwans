package com.ruoyi.kemovie.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 影视条目 km_media
 *
 * @author kemovie
 */
public class KmMedia extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long mediaId;
    private Long tmdbId;
    private String mediaType;
    private String imdbId;
    private String mdlId;
    private String letterboxdSlug;
    private Long legacyAid;
    private String title;
    private String originalTitle;
    private String overview;
    private String posterPath;
    private String posterLocalUrl;
    private String backdropLocalUrl;
    private String backdropPath;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date releaseDate;
    private String region;
    private String genres;
    private String tags;
    private String source;
    private Integer runtime;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private BigDecimal popularity;
    private String status;
    private Integer likeCount;
    private Integer wishCount;
    private Integer watchedCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private BigDecimal hotScore;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSyncedAt;
    private String visible;
    private String delFlag;
    /** 飞书资源文档 token（更新定位用） */
    private String feishuDocToken;
    /** 飞书资源文档链接（前台展示） */
    private String feishuDocUrl;
    /** 腾讯资源文档链接（手动维护，前台展示） */
    private String tencentDocUrl;

    /** 关联：上线平台（详情页用，非表字段） */
    private List<KmMediaRelease> releases;
    /** 后台编辑：勾选的自定义服务商 ID（非表字段） */
    private List<Long> providerIds;
    /** 有效上线日期：平台上线日优先，兜底首映日（非表字段，时间轴/提醒用） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date onlineDate;
    /** 当前用户对该片的行为（登录态注入，非表字段） */
    private KmUserMediaAction myAction;
    /** 当前用户是否订阅（非表字段） */
    private Boolean subscribed;
    /** 关联：演职人员（详情页用，非表字段） */
    private java.util.List<KmMediaCast> cast;

    /** 筛选：上映年份 */
    private Integer year;
    /** 筛选：最低评分 */
    private java.math.BigDecimal minRating;
    /** 筛选：评分来源 tmdb/site(站内)/combined(综合，默认) */
    private String ratingSource;
    /** 筛选：播放平台 */
    private String platform;
    /** 排序方式：hot(默认)/rating/popular/recent/newest */
    private String sortBy;
    /** 筛选：上映阶段 upcoming(即将上映)/airing(正在上映)/ended(最近完结) */
    private String phase;

    /** 筛选：观看方式 flatrate(会员)/free(免费)/rent(租)/buy(购) */
    private String offerType;
    /** 筛选：平台原始名集合（由平台品牌分组解析而来，供 IN 查询，非表字段） */
    private java.util.List<String> platformNames;

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }
    public String getMdlId() { return mdlId; }
    public void setMdlId(String mdlId) { this.mdlId = mdlId; }
    public String getLetterboxdSlug() { return letterboxdSlug; }
    public void setLetterboxdSlug(String letterboxdSlug) { this.letterboxdSlug = letterboxdSlug; }
    public Long getLegacyAid() { return legacyAid; }
    public void setLegacyAid(Long legacyAid) { this.legacyAid = legacyAid; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getPosterLocalUrl() { return posterLocalUrl; }
    public void setPosterLocalUrl(String posterLocalUrl) { this.posterLocalUrl = posterLocalUrl; }
    public String getBackdropLocalUrl() { return backdropLocalUrl; }
    public void setBackdropLocalUrl(String backdropLocalUrl) { this.backdropLocalUrl = backdropLocalUrl; }
    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }
    public Date getReleaseDate() { return releaseDate; }
    public void setReleaseDate(Date releaseDate) { this.releaseDate = releaseDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }
    public BigDecimal getVoteAverage() { return voteAverage; }
    public void setVoteAverage(BigDecimal voteAverage) { this.voteAverage = voteAverage; }
    public Integer getVoteCount() { return voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
    public BigDecimal getPopularity() { return popularity; }
    public void setPopularity(BigDecimal popularity) { this.popularity = popularity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getWishCount() { return wishCount; }
    public void setWishCount(Integer wishCount) { this.wishCount = wishCount; }
    public Integer getWatchedCount() { return watchedCount; }
    public void setWatchedCount(Integer watchedCount) { this.watchedCount = watchedCount; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public BigDecimal getHotScore() { return hotScore; }
    public void setHotScore(BigDecimal hotScore) { this.hotScore = hotScore; }
    public Date getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Date lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public String getFeishuDocToken() { return feishuDocToken; }
    public void setFeishuDocToken(String feishuDocToken) { this.feishuDocToken = feishuDocToken; }
    public String getFeishuDocUrl() { return feishuDocUrl; }
    public void setFeishuDocUrl(String feishuDocUrl) { this.feishuDocUrl = feishuDocUrl; }
    public String getTencentDocUrl() { return tencentDocUrl; }
    public void setTencentDocUrl(String tencentDocUrl) { this.tencentDocUrl = tencentDocUrl; }
    public String getVisible() { return visible; }
    public void setVisible(String visible) { this.visible = visible; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<KmMediaRelease> getReleases() { return releases; }
    public void setReleases(List<KmMediaRelease> releases) { this.releases = releases; }
    public List<Long> getProviderIds() { return providerIds; }
    public void setProviderIds(List<Long> providerIds) { this.providerIds = providerIds; }
    public Date getOnlineDate() { return onlineDate; }
    public void setOnlineDate(Date onlineDate) { this.onlineDate = onlineDate; }
    public KmUserMediaAction getMyAction() { return myAction; }
    public void setMyAction(KmUserMediaAction myAction) { this.myAction = myAction; }
    public Boolean getSubscribed() { return subscribed; }
    public void setSubscribed(Boolean subscribed) { this.subscribed = subscribed; }

    public java.util.List<KmMediaCast> getCast() { return cast; }
    public void setCast(java.util.List<KmMediaCast> cast) { this.cast = cast; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public java.math.BigDecimal getMinRating() { return minRating; }
    public void setMinRating(java.math.BigDecimal minRating) { this.minRating = minRating; }
    public String getRatingSource() { return ratingSource; }
    public void setRatingSource(String ratingSource) { this.ratingSource = ratingSource; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }
    public java.util.List<String> getPlatformNames() { return platformNames; }
    public void setPlatformNames(java.util.List<String> platformNames) { this.platformNames = platformNames; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
}
