package com.ruoyi.kemovie.auth;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.kemovie.domain.KmMember;

/**
 * 前台会员登录态（缓存于 Redis，作为 Security 主体）。
 * 与后台 {@code com.ruoyi.common.core.domain.model.LoginUser} 完全隔离。
 *
 * @author kemovie
 */
public class MemberLoginUser implements UserDetails
{
    private static final long serialVersionUID = 1L;

    /** 会员ID */
    private Long memberId;

    /** 登录令牌(uuid) */
    private String token;

    /** 登录时间 */
    private Long loginTime;

    /** 过期时间 */
    private Long expireTime;

    /** 登录IP */
    private String ipaddr;

    /** 会员信息 */
    private KmMember member;

    public MemberLoginUser()
    {
    }

    public MemberLoginUser(KmMember member)
    {
        this.member = member;
        this.memberId = member.getMemberId();
    }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getLoginTime() { return loginTime; }
    public void setLoginTime(Long loginTime) { this.loginTime = loginTime; }

    public Long getExpireTime() { return expireTime; }
    public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }

    public String getIpaddr() { return ipaddr; }
    public void setIpaddr(String ipaddr) { this.ipaddr = ipaddr; }

    public KmMember getMember() { return member; }
    public void setMember(KmMember member) { this.member = member; }

    @JsonIgnore
    @Override
    public String getPassword()
    {
        return member == null ? null : member.getPassword();
    }

    @Override
    public String getUsername()
    {
        return member == null ? null : member.getUsername();
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() { return true; }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() { return true; }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @JsonIgnore
    @Override
    public boolean isEnabled() { return true; }

    @JSONField(serialize = false)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }
}
