package com.ruoyi.kemovie.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 运营位配置 km_ops_block
 *
 * @author kemovie
 */
public class KmOpsBlock extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long blockId;
    private String blockKey;
    private String title;
    private String content;
    private String imageUrl;
    private String link;
    private Integer sort;
    private String enabled;

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }
    public String getBlockKey() { return blockKey; }
    public void setBlockKey(String blockKey) { this.blockKey = blockKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
}
