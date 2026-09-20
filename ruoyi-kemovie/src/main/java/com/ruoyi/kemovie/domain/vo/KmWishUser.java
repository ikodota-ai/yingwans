package com.ruoyi.kemovie.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 想看用户明细
 *
 * @author kemovie
 */
public class KmWishUser
{
    private Long userId;
    private String username;
    private String nickname;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date wishedTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Date getWishedTime() { return wishedTime; }
    public void setWishedTime(Date wishedTime) { this.wishedTime = wishedTime; }
}
