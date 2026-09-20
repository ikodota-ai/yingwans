package com.ruoyi.kemovie.auth;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 前台会员令牌过滤器。
 *
 * <p>仅当请求携带有效会员令牌且当前 SecurityContext 尚未认证时，才把会员登录态写入上下文，
 * 因此不会与后台 {@code JwtAuthenticationTokenFilter} 冲突（两者令牌载荷/缓存前缀不同）。</p>
 *
 * @author kemovie
 */
@Component
public class MemberTokenFilter extends OncePerRequestFilter
{
    @Autowired
    private MemberTokenService memberTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (SecurityContextHolder.getContext().getAuthentication() == null)
        {
            MemberLoginUser loginUser = memberTokenService.getLoginUser(request);
            if (loginUser != null)
            {
                memberTokenService.verifyToken(loginUser);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }
}
