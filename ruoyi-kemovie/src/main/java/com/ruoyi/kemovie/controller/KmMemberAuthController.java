package com.ruoyi.kemovie.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.auth.MemberLoginUser;
import com.ruoyi.kemovie.auth.MemberSecurityUtils;
import com.ruoyi.kemovie.auth.MemberTokenService;
import com.ruoyi.kemovie.domain.KmMember;
import com.ruoyi.kemovie.service.IKmMemberService;

/**
 * 前台会员认证接口（注册/登录/退出/当前信息）。
 *
 * <p>登录/注册匿名开放；/member/info 需携带会员令牌。与后台 /login 完全独立。</p>
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/member")
public class KmMemberAuthController extends BaseController
{
    @Autowired
    private IKmMemberService memberService;

    @Autowired
    private MemberTokenService memberTokenService;

    /** 会员登录 */
    @Anonymous
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body)
    {
        String token = memberService.login(
                body.get("username"), body.get("password"), body.get("code"), body.get("uuid"));
        AjaxResult ajax = AjaxResult.success();
        ajax.put("token", token);
        return ajax;
    }

    /** 会员注册 */
    @Anonymous
    @PostMapping("/register")
    public AjaxResult register(@RequestBody Map<String, String> body)
    {
        memberService.register(body.get("username"), body.get("password"), body.get("nickname"));
        return AjaxResult.success("注册成功");
    }

    /** 当前登录会员信息 */
    @GetMapping("/info")
    public AjaxResult info()
    {
        Long memberId = MemberSecurityUtils.getMemberId();
        KmMember member = memberService.selectMemberById(memberId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("member", member);
        return ajax;
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public AjaxResult logout()
    {
        MemberLoginUser loginUser = MemberSecurityUtils.getLoginUserOrNull();
        if (loginUser != null && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            memberTokenService.delLoginUser(loginUser.getToken());
        }
        return AjaxResult.success("退出成功");
    }

    /** 更新当前会员资料（昵称/头像/邮箱） */
    @PostMapping("/profile")
    public AjaxResult profile(@RequestBody KmMember body)
    {
        KmMember update = new KmMember();
        update.setMemberId(MemberSecurityUtils.getMemberId());
        update.setNickname(body.getNickname());
        update.setAvatar(body.getAvatar());
        update.setEmail(body.getEmail());
        return toAjax(memberService.updateMember(update));
    }
}
