package com.ruoyi.kemovie.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.kemovie.domain.KmMember;

/**
 * 前台会员 Mapper
 *
 * @author kemovie
 */
public interface KmMemberMapper
{
    public KmMember selectMemberById(Long memberId);

    public KmMember selectMemberByUsername(String username);

    public List<KmMember> selectMemberList(KmMember member);

    public int insertMember(KmMember member);

    public int updateMember(KmMember member);

    public int updateLoginInfo(@Param("memberId") Long memberId, @Param("loginIp") String loginIp);

    public int deleteMemberByIds(Long[] memberIds);

    public int checkUsernameUnique(String username);
}
