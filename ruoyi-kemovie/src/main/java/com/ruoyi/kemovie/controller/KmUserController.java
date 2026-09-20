package com.ruoyi.kemovie.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.kemovie.auth.MemberSecurityUtils;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.service.IKmCollectionService;
import com.ruoyi.kemovie.service.IKmMediaService;
import com.ruoyi.kemovie.service.IKmNotificationService;
import com.ruoyi.kemovie.service.IKmSubscriptionService;
import com.ruoyi.kemovie.service.IKmUserActionService;

/**
 * 前台登录用户接口（点赞/想看/看过/评分/订阅/我的片库/通知）。
 * 需登录，走前台会员令牌鉴权（MemberTokenFilter）。
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/me")
public class KmUserController extends BaseController
{
    @Autowired private IKmUserActionService actionService;
    @Autowired private IKmSubscriptionService subscriptionService;
    @Autowired private IKmCollectionService collectionService;
    @Autowired private IKmMediaService mediaService;
    @Autowired private IKmNotificationService notificationService;

    // ---------- 行为 ----------
    @PostMapping("/like/{mediaId}")
    public AjaxResult like(@PathVariable Long mediaId)
    {
        return AjaxResult.success(actionService.toggleLike(MemberSecurityUtils.getMemberId(), mediaId));
    }

    @PostMapping("/wish/{mediaId}")
    public AjaxResult wish(@PathVariable Long mediaId)
    {
        return AjaxResult.success(actionService.toggleWish(MemberSecurityUtils.getMemberId(), mediaId));
    }

    @PostMapping("/watched/{mediaId}")
    public AjaxResult watched(@PathVariable Long mediaId)
    {
        return AjaxResult.success(actionService.toggleWatched(MemberSecurityUtils.getMemberId(), mediaId));
    }

    @PostMapping("/rate/{mediaId}")
    public AjaxResult rate(@PathVariable Long mediaId, @RequestParam(required = false) BigDecimal rating)
    {
        return AjaxResult.success(actionService.rate(MemberSecurityUtils.getMemberId(), mediaId, rating));
    }

    // ---------- 订阅 ----------
    @PostMapping("/subscribe/{mediaId}")
    public AjaxResult subscribe(@PathVariable Long mediaId)
    {
        Long memberId = MemberSecurityUtils.getMemberId();
        boolean on = subscriptionService.toggle(memberId, mediaId);
        AjaxResult ajax = AjaxResult.success(on ? "已订阅" : "已取消订阅").put("subscribed", on);
        if (on)
        {
            // 已有资源文档：立即把文档地址推给订阅者（站内通知/邮件/微信按渠道）
            ajax.put("docReady", notificationService.sendResourceDocIfAvailable(memberId, mediaId));
        }
        return ajax;
    }

    @GetMapping("/subscriptions")
    public AjaxResult subscriptions()
    {
        return AjaxResult.success(subscriptionService.timeline(MemberSecurityUtils.getMemberId()));
    }

    // ---------- 我的片库 ----------
    /** tab: wish/watched/rated */
    @GetMapping("/library")
    public TableDataInfo library(@RequestParam(defaultValue = "wish") String tab)
    {
        startPage();
        List<KmMedia> list = mediaService.selectLibrary(MemberSecurityUtils.getMemberId(), tab);
        return getDataTable(list);
    }

    // ---------- 我的片单 ----------
    @GetMapping("/collections")
    public AjaxResult myCollections()
    {
        KmCollection q = new KmCollection();
        q.setOwnerId(MemberSecurityUtils.getMemberId());
        return AjaxResult.success(collectionService.selectKmCollectionList(q));
    }

    /** 我关注的公共片单 */
    @GetMapping("/collections/followed")
    public AjaxResult followedCollections()
    {
        return AjaxResult.success(collectionService.selectFollowedCollections(MemberSecurityUtils.getMemberId()));
    }

    /** 关注公共片单 */
    @PostMapping("/collection/{collectionId}/follow")
    public AjaxResult followCollection(@PathVariable Long collectionId)
    {
        KmCollection exist = collectionService.selectKmCollectionById(collectionId);
        if (exist == null || !"0".equals(exist.getIsPublic())) { return AjaxResult.error("片单不存在或非公开"); }
        collectionService.followCollection(MemberSecurityUtils.getMemberId(), collectionId);
        return AjaxResult.success();
    }

    /** 取消关注 */
    @DeleteMapping("/collection/{collectionId}/follow")
    public AjaxResult unfollowCollection(@PathVariable Long collectionId)
    {
        collectionService.unfollowCollection(MemberSecurityUtils.getMemberId(), collectionId);
        return AjaxResult.success();
    }

    @PostMapping("/collection")
    public AjaxResult createCollection(@RequestBody KmCollection c)
    {
        c.setOwnerId(MemberSecurityUtils.getMemberId());
        c.setSource("manual");
        c.setCreateBy(MemberSecurityUtils.getUsername());
        collectionService.insertKmCollection(c);
        return AjaxResult.success(c);
    }

    @PutMapping("/collection")
    public AjaxResult updateCollection(@RequestBody KmCollection c)
    {
        KmCollection exist = collectionService.selectKmCollectionById(c.getCollectionId());
        if (exist == null || !MemberSecurityUtils.getMemberId().equals(exist.getOwnerId())) { return AjaxResult.error("无权操作"); }
        c.setUpdateBy(MemberSecurityUtils.getUsername());
        return toAjax(collectionService.updateKmCollection(c));
    }

    @DeleteMapping("/collection/{collectionId}")
    public AjaxResult deleteCollection(@PathVariable Long collectionId)
    {
        KmCollection exist = collectionService.selectKmCollectionById(collectionId);
        if (exist == null || !MemberSecurityUtils.getMemberId().equals(exist.getOwnerId())) { return AjaxResult.error("无权操作"); }
        return toAjax(collectionService.deleteKmCollectionByIds(new Long[]{collectionId}));
    }

    @PostMapping("/collection/{collectionId}/media/{mediaId}")
    public AjaxResult addToCollection(@PathVariable Long collectionId, @PathVariable Long mediaId)
    {
        KmCollection exist = collectionService.selectKmCollectionById(collectionId);
        if (exist == null || !MemberSecurityUtils.getMemberId().equals(exist.getOwnerId())) { return AjaxResult.error("无权操作"); }
        collectionService.addMedia(collectionId, mediaId);
        return AjaxResult.success();
    }

    @DeleteMapping("/collection/{collectionId}/media/{mediaId}")
    public AjaxResult removeFromCollection(@PathVariable Long collectionId, @PathVariable Long mediaId)
    {
        KmCollection exist = collectionService.selectKmCollectionById(collectionId);
        if (exist == null || !MemberSecurityUtils.getMemberId().equals(exist.getOwnerId())) { return AjaxResult.error("无权操作"); }
        collectionService.removeMedia(collectionId, mediaId);
        return AjaxResult.success();
    }

    // ---------- 通知 ----------
    @GetMapping("/notifications")
    public AjaxResult notifications()
    {
        AjaxResult ajax = AjaxResult.success(notificationService.listByUser(MemberSecurityUtils.getMemberId()));
        ajax.put("unread", notificationService.unreadCount(MemberSecurityUtils.getMemberId()));
        return ajax;
    }

    @GetMapping("/notifications/unread")
    public AjaxResult unread()
    {
        return AjaxResult.success(notificationService.unreadCount(MemberSecurityUtils.getMemberId()));
    }

    @PutMapping("/notifications/{notifyId}/read")
    public AjaxResult read(@PathVariable Long notifyId)
    {
        return toAjax(notificationService.markRead(MemberSecurityUtils.getMemberId(), notifyId));
    }

    @PutMapping("/notifications/read-all")
    public AjaxResult readAll()
    {
        return toAjax(notificationService.markAllRead(MemberSecurityUtils.getMemberId()));
    }
}
