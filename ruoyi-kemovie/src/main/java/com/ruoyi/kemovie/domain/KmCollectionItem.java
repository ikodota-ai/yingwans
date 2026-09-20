package com.ruoyi.kemovie.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 片单影片关联 km_collection_item
 *
 * @author kemovie
 */
public class KmCollectionItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long collectionId;
    private Long mediaId;
    private Integer sort;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
