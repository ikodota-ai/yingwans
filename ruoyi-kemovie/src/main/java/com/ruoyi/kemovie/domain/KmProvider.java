package com.ruoyi.kemovie.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 自定义播放服务商 km_provider
 *
 * @author kemovie
 */
public class KmProvider extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long providerId;
    private String name;
    private String slug;
    private String siteUrl;
    private String logoUrl;
    private String region;
    private String source;
    private String enabled;
    private Integer sort;

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
