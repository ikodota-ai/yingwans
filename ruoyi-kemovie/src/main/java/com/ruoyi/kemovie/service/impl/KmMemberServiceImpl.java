package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.kemovie.auth.MemberLoginUser;
import com.ruoyi.kemovie.auth.MemberTokenService;
import com.ruoyi.kemovie.domain.KmMember;
import com.ruoyi.kemovie.mapper.KmMemberMapper;
import com.ruoyi.kemovie.service.IKmMemberService;

/**
 * 前台会员服务实现。
 *
 * <p>与后台 sys_user 无任何耦合：独立表 km_member、独立令牌（MemberTokenService）。
 * 前台会员登录不使用图片验证码。</p>
 *
 * @author kemovie
 */
@Service
public class KmMemberServiceImpl implements IKmMemberService
{
    @Autowired
    private KmMemberMapper memberMapper;

    @Autowired
    private MemberTokenService memberTokenService;

    @Override
    public String login(String username, String password, String code, String uuid)
    {
        // 电影前台会员登录不使用图片验证码（后台管理端仍独立启用）
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new ServiceException("用户名或密码不能为空");
        }
        KmMember member = memberMapper.selectMemberByUsername(username);
        if (member == null)
        {
            throw new ServiceException("用户不存在或密码错误");
        }
        if (!SecurityUtils.matchesPassword(password, member.getPassword()))
        {
            throw new ServiceException("用户不存在或密码错误");
        }
        if ("1".equals(member.getStatus()))
        {
            throw new ServiceException("账号已停用，请联系管理员");
        }
        // 更新登录信息
        memberMapper.updateLoginInfo(member.getMemberId(), IpUtils.getIpAddr());
        // 生成令牌
        MemberLoginUser loginUser = new MemberLoginUser(member);
        return memberTokenService.createToken(loginUser);
    }

    @Override
    public Long register(String username, String password, String nickname)
    {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new ServiceException("用户名或密码不能为空");
        }
        if (username.length() < 2 || username.length() > 20)
        {
            throw new ServiceException("账号长度需在 2 到 20 个字符之间");
        }
        if (password.length() < 5 || password.length() > 20)
        {
            throw new ServiceException("密码长度需在 5 到 20 个字符之间");
        }
        if (memberMapper.checkUsernameUnique(username) > 0)
        {
            throw new ServiceException("账号「" + username + "」已存在");
        }
        KmMember member = new KmMember();
        member.setUsername(username);
        member.setNickname(StringUtils.isNotEmpty(nickname) ? nickname : username);
        member.setPassword(SecurityUtils.encryptPassword(password));
        member.setStatus("0");
        memberMapper.insertMember(member);
        return member.getMemberId();
    }

    @Override
    public void logout(String token)
    {
        memberTokenService.delLoginUser(token);
    }

    @Override
    public KmMember selectMemberById(Long memberId)
    {
        return memberMapper.selectMemberById(memberId);
    }

    @Override
    public List<KmMember> selectMemberList(KmMember member)
    {
        return memberMapper.selectMemberList(member);
    }

    @Override
    public int updateMember(KmMember member)
    {
        return memberMapper.updateMember(member);
    }

    @Override
    public int deleteMemberByIds(Long[] memberIds)
    {
        return memberMapper.deleteMemberByIds(memberIds);
    }
}
