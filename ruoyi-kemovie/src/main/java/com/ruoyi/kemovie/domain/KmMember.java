package com.ruoyi.kemovie.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 前台会员 km_member
 *
 * <p>前台用户与后台 sys_user 完全独立，单独一张表、单独鉴权。</p>
 *
 * @author kemovie
 */
public class KmMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会员ID */
    private Long memberId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 密码(bcrypt)，仅入参接收，不随响应下发 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 邮箱 */
    private String email;

    /** 微信openid（公众号模板消息推送） */
    private String wechatOpenid;

    /** 手机号 */
    private String phone;

    /** 头像地址 */
    private String avatar;

    /** 性别(0男 1女 2未知) */
    private String sex;

    /** 状态(0正常 1停用) */
    private String status;

    /** 删除标志(0存在 2删除) */
    private String delFlag;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginDate;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    @JsonIgnore
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWechatOpenid() { return wechatOpenid; }
    public void setWechatOpenid(String wechatOpenid) { this.wechatOpenid = wechatOpenid; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getLoginIp() { return loginIp; }
    public void setLoginIp(String loginIp) { this.loginIp = loginIp; }

    public Date getLoginDate() { return loginDate; }
    public void setLoginDate(Date loginDate) { this.loginDate = loginDate; }
}
