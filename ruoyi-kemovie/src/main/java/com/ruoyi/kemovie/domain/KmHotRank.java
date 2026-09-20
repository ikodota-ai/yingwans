package com.ruoyi.kemovie.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 热门排行榜快照 km_hot_rank
 *
 * @author kemovie
 */
public class KmHotRank extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String rankType;
    private Integer rankNo;
    private Long mediaId;
    private BigDecimal hotScore;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date snapshotDate;

    /** 关联影片（非表字段） */
    private KmMedia media;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRankType() { return rankType; }
    public void setRankType(String rankType) { this.rankType = rankType; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public BigDecimal getHotScore() { return hotScore; }
    public void setHotScore(BigDecimal hotScore) { this.hotScore = hotScore; }
    public Date getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(Date snapshotDate) { this.snapshotDate = snapshotDate; }
    public KmMedia getMedia() { return media; }
    public void setMedia(KmMedia media) { this.media = media; }
}
