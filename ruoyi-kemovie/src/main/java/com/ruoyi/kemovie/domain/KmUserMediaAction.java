package com.ruoyi.kemovie.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户影片行为 km_user_media_action
 *
 * @author kemovie
 */
public class KmUserMediaAction extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long mediaId;
    private String liked;
    private String wished;
    private String watched;
    private BigDecimal rating;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date likedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date wishedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date watchedTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date ratingTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getLiked() { return liked; }
    public void setLiked(String liked) { this.liked = liked; }
    public String getWished() { return wished; }
    public void setWished(String wished) { this.wished = wished; }
    public String getWatched() { return watched; }
    public void setWatched(String watched) { this.watched = watched; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Date getLikedTime() { return likedTime; }
    public void setLikedTime(Date likedTime) { this.likedTime = likedTime; }
    public Date getWishedTime() { return wishedTime; }
    public void setWishedTime(Date wishedTime) { this.wishedTime = wishedTime; }
    public Date getWatchedTime() { return watchedTime; }
    public void setWatchedTime(Date watchedTime) { this.watchedTime = watchedTime; }
    public Date getRatingTime() { return ratingTime; }
    public void setRatingTime(Date ratingTime) { this.ratingTime = ratingTime; }
}
