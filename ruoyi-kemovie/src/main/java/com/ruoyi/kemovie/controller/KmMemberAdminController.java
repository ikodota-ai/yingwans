package com.ruoyi.kemovie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.domain.KmMember;
import com.ruoyi.kemovie.service.IKmMemberService;

/**
 * 前台会员 后台管理（列表/详情/停用启用/重置密码/删除）。
 *
 * <p>后台管理员通过 sys_user 权限访问，管理的是独立的 km_member 前台会员。</p>
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/member")
public class KmMemberAdminController extends BaseController
{
    @Autowired
    private IKmMemberService memberService;

    @PreAuthorize("@ss.hasPermi('kemovie:member:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmMember kmMember)
    {
        startPage();
        return getDataTable(memberService.selectMemberList(kmMember));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:member:query')")
    @GetMapping("/{memberId}")
    public AjaxResult getInfo(@PathVariable Long memberId)
    {
        return success(memberService.selectMemberById(memberId));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:member:edit')")
    @Log(title = "前台会员", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KmMember kmMember)
    {
        // 后台不允许直接改密码/用户名，仅昵称/邮箱/手机/备注
        KmMember update = new KmMember();
        update.setMemberId(kmMember.getMemberId());
        update.setNickname(kmMember.getNickname());
        update.setEmail(kmMember.getEmail());
        update.setPhone(kmMember.getPhone());
        update.setRemark(kmMember.getRemark());
        return toAjax(memberService.updateMember(update));
    }

    /** 停用/启用 */
    @PreAuthorize("@ss.hasPermi('kemovie:member:edit')")
    @Log(title = "前台会员状态", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody KmMember kmMember)
    {
        KmMember update = new KmMember();
        update.setMemberId(kmMember.getMemberId());
        update.setStatus(kmMember.getStatus());
        return toAjax(memberService.updateMember(update));
    }

    /** 重置密码 */
    @PreAuthorize("@ss.hasPermi('kemovie:member:resetPwd')")
    @Log(title = "前台会员重置密码", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody KmMember kmMember)
    {
        if (StringUtils.isEmpty(kmMember.getPassword()))
        {
            return error("新密码不能为空");
        }
        KmMember update = new KmMember();
        update.setMemberId(kmMember.getMemberId());
        update.setPassword(SecurityUtils.encryptPassword(kmMember.getPassword()));
        return toAjax(memberService.updateMember(update));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:member:remove')")
    @Log(title = "前台会员", businessType = BusinessType.DELETE)
    @DeleteMapping("/{memberIds}")
    public AjaxResult remove(@PathVariable Long[] memberIds)
    {
        return toAjax(memberService.deleteMemberByIds(memberIds));
    }
}
