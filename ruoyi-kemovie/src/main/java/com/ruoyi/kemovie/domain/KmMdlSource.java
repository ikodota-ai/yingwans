package com.ruoyi.kemovie.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * MDL 抓取源配置 km_mdl_source
 *
 * @author kemovie
 */
public class KmMdlSource extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long sourceId;
    /** 来源类型：mdl / justwatch，缺省 mdl */
    private String sourceType;
    private String listId;
    private String listUrl;
    private String name;
    private String regionTag;
    /** JustWatch 影片类型：movie / tv，空为全部 */
    private String objectType;
    private Integer maxPage;
    private Long targetCollectionId;
    private String enabled;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastRunAt;
    private String lastStatus;

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getListId() { return listId; }
    public void setListId(String listId) { this.listId = listId; }
    public String getListUrl() { return listUrl; }
    public void setListUrl(String listUrl) { this.listUrl = listUrl; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegionTag() { return regionTag; }
    public void setRegionTag(String regionTag) { this.regionTag = regionTag; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Integer getMaxPage() { return maxPage; }
    public void setMaxPage(Integer maxPage) { this.maxPage = maxPage; }
    public Long getTargetCollectionId() { return targetCollectionId; }
    public void setTargetCollectionId(Long targetCollectionId) { this.targetCollectionId = targetCollectionId; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public Date getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(Date lastRunAt) { this.lastRunAt = lastRunAt; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
}
