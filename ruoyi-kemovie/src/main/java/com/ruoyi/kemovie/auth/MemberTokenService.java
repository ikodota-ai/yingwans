package com.ruoyi.kemovie.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 前台会员令牌服务。
 *
 * <p>复用后台的 token.secret / token.expireTime，但令牌载荷标识与 Redis 缓存前缀均独立
 * （member_login_key / member_tokens:），因此会员令牌与后台管理员令牌互不干扰。</p>
 *
 * @author kemovie
 */
@Component
public class MemberTokenService
{
    private static final String MEMBER_LOGIN_KEY = "member_login_key";

    private static final String MEMBER_TOKEN_CACHE = "member_tokens:";

    private static final long MILLIS_MINUTE = 60 * 1000L;

    private static final long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    @Autowired
    private RedisCache redisCache;

    /** 从请求解析会员登录态 */
    public MemberLoginUser getLoginUser(HttpServletRequest request)
    {
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                String uuid = (String) claims.get(MEMBER_LOGIN_KEY);
                if (StringUtils.isEmpty(uuid))
                {
                    return null;
                }
                return redisCache.getCacheObject(getTokenKey(uuid));
            }
            catch (Exception e)
            {
                // 令牌无效或过期，视为未登录
            }
        }
        return null;
    }

    /** 创建会员令牌并缓存登录态 */
    public String createToken(MemberLoginUser loginUser)
    {
        String uuid = IdUtils.fastUUID();
        loginUser.setToken(uuid);
        loginUser.setIpaddr(IpUtils.getIpAddr());
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(MEMBER_LOGIN_KEY, uuid);
        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /** 有效期不足 20 分钟自动续期 */
    public void verifyToken(MemberLoginUser loginUser)
    {
        long expire = loginUser.getExpireTime();
        if (expire - System.currentTimeMillis() <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    public void refreshToken(MemberLoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        redisCache.setCacheObject(getTokenKey(loginUser.getToken()), loginUser, expireTime, TimeUnit.MINUTES);
    }

    public void delLoginUser(String uuid)
    {
        if (StringUtils.isNotEmpty(uuid))
        {
            redisCache.deleteObject(getTokenKey(uuid));
        }
    }

    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith("Bearer "))
        {
            token = token.replace("Bearer ", "");
        }
        return token;
    }

    private Claims parseToken(String token)
    {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    private String getTokenKey(String uuid)
    {
        return MEMBER_TOKEN_CACHE + uuid;
    }
}
