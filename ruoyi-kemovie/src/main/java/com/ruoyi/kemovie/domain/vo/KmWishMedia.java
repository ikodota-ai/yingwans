package com.ruoyi.kemovie.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 想看运营视图：按影片聚合的想看统计
 *
 * @author kemovie
 */
public class KmWishMedia
{
    private Long mediaId;
    private String title;
    private String originalTitle;
    private String mediaType;
    private String posterLocalUrl;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date releaseDate;
    private String region;
    private String source;
    private Integer wishCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastWishTime;
    private Integer resourceCount;
    private Integer providerCount;

    /** 筛选：标题关键字 */
    private String keyword;
    /** 筛选：仅看无资源 */
    private Boolean lackResource;

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getPosterLocalUrl() { return posterLocalUrl; }
    public void setPosterLocalUrl(String posterLocalUrl) { this.posterLocalUrl = posterLocalUrl; }
    public Date getReleaseDate() { return releaseDate; }
    public void setReleaseDate(Date releaseDate) { this.releaseDate = releaseDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getWishCount() { return wishCount; }
    public void setWishCount(Integer wishCount) { this.wishCount = wishCount; }
    public Date getLastWishTime() { return lastWishTime; }
    public void setLastWishTime(Date lastWishTime) { this.lastWishTime = lastWishTime; }
    public Integer getResourceCount() { return resourceCount; }
    public void setResourceCount(Integer resourceCount) { this.resourceCount = resourceCount; }
    public Integer getProviderCount() { return providerCount; }
    public void setProviderCount(Integer providerCount) { this.providerCount = providerCount; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Boolean getLackResource() { return lackResource; }
    public void setLackResource(Boolean lackResource) { this.lackResource = lackResource; }
}
