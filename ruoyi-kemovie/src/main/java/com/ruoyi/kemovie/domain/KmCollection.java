package com.ruoyi.kemovie.domain;

import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 片单 km_collection
 *
 * @author kemovie
 */
public class KmCollection extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long collectionId;
    private String name;
    private String description;
    private String source;
    private String sourceRef;
    private String coverUrl;
    private Long ownerId;
    private Integer itemCount;
    private String isPublic;
    private String delFlag;

    /** 拼贴封面用的前几张海报（非表字段） */
    private List<String> coverPosters;
    /** 创建人名称（非表字段） */
    private String ownerName;
    /** 当前会员是否已关注（非表字段） */
    private Boolean followed;

    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public String getIsPublic() { return isPublic; }
    public void setIsPublic(String isPublic) { this.isPublic = isPublic; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public List<String> getCoverPosters() { return coverPosters; }
    public void setCoverPosters(List<String> coverPosters) { this.coverPosters = coverPosters; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Boolean getFollowed() { return followed; }
    public void setFollowed(Boolean followed) { this.followed = followed; }
}
