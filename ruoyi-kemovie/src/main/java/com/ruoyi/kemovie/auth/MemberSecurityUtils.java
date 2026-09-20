package com.ruoyi.kemovie.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 前台会员安全工具：从 SecurityContext 取当前登录会员。
 *
 * @author kemovie
 */
public class MemberSecurityUtils
{
    /** 当前登录会员登录态，未登录返回 null */
    public static MemberLoginUser getLoginUserOrNull()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberLoginUser)
        {
            return (MemberLoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /** 当前登录会员ID，未登录返回 null */
    public static Long getMemberIdOrNull()
    {
        MemberLoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getMemberId();
    }

    /** 当前登录会员ID，未登录抛异常 */
    public static Long getMemberId()
    {
        Long memberId = getMemberIdOrNull();
        if (memberId == null)
        {
            throw new RuntimeException("会员未登录");
        }
        return memberId;
    }

    /** 当前登录会员用户名 */
    public static String getUsername()
    {
        MemberLoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getUsername();
    }
}
