package com.ruoyi.kemovie.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 影视上线平台信息 km_media_release
 *
 * @author kemovie
 */
public class KmMediaRelease extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long releaseId;
    private Long mediaId;
    private String platform;
    private String platformLogo;
    private String country;
    private String offerType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date onlineDate;
    private String link;
    private String status;
    private String source;

    public Long getReleaseId() { return releaseId; }
    public void setReleaseId(Long releaseId) { this.releaseId = releaseId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getPlatformLogo() { return platformLogo; }
    public void setPlatformLogo(String platformLogo) { this.platformLogo = platformLogo; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }
    public Date getOnlineDate() { return onlineDate; }
    public void setOnlineDate(Date onlineDate) { this.onlineDate = onlineDate; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
