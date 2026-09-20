package com.ruoyi.kemovie.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 影片下载资源 km_media_resource
 * 仅供后台管理与第三方文档（飞书/腾讯文档）生成使用，任何前台接口不得返回。
 *
 * @author kemovie
 */
public class KmMediaResource extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long resourceId;
    private Long mediaId;
    private String groupName;
    private String platform;
    private String url;
    private String pwd;
    private Integer groupOrder;
    private String source;

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public Integer getGroupOrder() { return groupOrder; }
    public void setGroupOrder(Integer groupOrder) { this.groupOrder = groupOrder; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
