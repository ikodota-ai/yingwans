package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmMember;

/**
 * 前台会员服务
 *
 * @author kemovie
 */
public interface IKmMemberService
{
    /** 会员登录，成功返回令牌 */
    public String login(String username, String password, String code, String uuid);

    /** 会员注册，返回新会员ID */
    public Long register(String username, String password, String nickname);

    /** 退出登录 */
    public void logout(String token);

    public KmMember selectMemberById(Long memberId);

    public List<KmMember> selectMemberList(KmMember member);

    public int updateMember(KmMember member);

    public int deleteMemberByIds(Long[] memberIds);
}
