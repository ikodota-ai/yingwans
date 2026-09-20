package com.ruoyi.kemovie.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaResource;
import com.ruoyi.kemovie.mapper.KmMediaResourceMapper;
import com.ruoyi.kemovie.service.IKmDocService;
import com.ruoyi.kemovie.service.IKmImageService;
import com.ruoyi.kemovie.service.IKmMediaService;
import com.ruoyi.kemovie.service.IKmProviderService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;
import com.ruoyi.kemovie.service.IKmNotificationService;
import com.ruoyi.kemovie.service.IKmPersonService;

/**
 * 影视条目 后台管理
 *
 * @author kemovie
 */
@RestController
@RequestMapping("/kemovie/media")
public class KmMediaAdminController extends BaseController
{
    @Autowired private IKmMediaService mediaService;
    @Autowired private IKmMediaSyncService mediaSyncService;
    @Autowired private IKmPersonService personService;
    @Autowired private IKmImageService imageService;
    @Autowired private IKmProviderService providerService;
    @Autowired private KmMediaResourceMapper resourceMapper;
    @Autowired private IKmDocService docService;
    @Autowired private IKmNotificationService notificationService;

    @PreAuthorize("@ss.hasPermi('kemovie:media:list')")
    @GetMapping("/list")
    public TableDataInfo list(KmMedia kmMedia)
    {
        startPage();
        return getDataTable(mediaService.selectKmMediaList(kmMedia));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:media:query')")
    @GetMapping("/{mediaId}")
    public AjaxResult getInfo(@PathVariable Long mediaId)
    {
        KmMedia media = mediaService.selectDetail(mediaId, null);
        if (media != null)
        {
            media.setProviderIds(providerService.selectMediaProviderIds(mediaId));
        }
        return success(media);
    }

    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @Log(title = "影视条目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KmMedia kmMedia)
    {
        return toAjax(mediaService.updateKmMedia(kmMedia));
    }

    @PreAuthorize("@ss.hasPermi('kemovie:media:remove')")
    @Log(title = "影视条目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{mediaIds}")
    public AjaxResult remove(@PathVariable Long[] mediaIds)
    {
        return toAjax(mediaService.deleteKmMediaByMediaIds(mediaIds));
    }

    /** 手动从 TMDB 导入/刷新一条 */
    @PreAuthorize("@ss.hasPermi('kemovie:media:sync')")
    @Log(title = "TMDB同步", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult sync(@RequestParam Long tmdbId, @RequestParam(defaultValue = "movie") String type)
    {
        KmMedia m = mediaSyncService.syncFromTmdb(tmdbId, type);
        return m == null ? error("同步失败，请检查 TMDB 连接") : success(m);
    }

    /** 手动同步该影片的演职人员（TMDB credits） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:sync')")
    @Log(title = "TMDB演职人员同步", businessType = BusinessType.OTHER)
    @PostMapping("/{mediaId}/syncCast")
    public AjaxResult syncCast(@PathVariable Long mediaId)
    {
        int n = personService.syncCastForMedia(mediaId);
        return success("已同步 " + n + " 位演职人员");
    }

    /** 影片下载资源列表（后台管理用，不进前台） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:query')")
    @GetMapping("/{mediaId}/resources")
    public AjaxResult resources(@PathVariable Long mediaId)
    {
        return success(resourceMapper.selectByMediaId(mediaId));
    }

    /** 新增下载资源 */
    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @Log(title = "影片下载资源", businessType = BusinessType.INSERT)
    @PostMapping("/resource")
    public AjaxResult addResource(@RequestBody KmMediaResource resource)
    {
        if (resource.getMediaId() == null || resource.getUrl() == null || resource.getUrl().isEmpty()
                || resource.getPlatform() == null || resource.getPlatform().isEmpty())
        {
            return error("mediaId / platform / url 必填");
        }
        resource.setSource("manual");
        resource.setCreateBy(com.ruoyi.common.utils.SecurityUtils.getUsername());
        return toAjax(resourceMapper.insertKmMediaResource(resource));
    }

    /** 删除下载资源 */
    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @Log(title = "影片下载资源", businessType = BusinessType.DELETE)
    @DeleteMapping("/resource/{resourceId}")
    public AjaxResult removeResource(@PathVariable Long resourceId)
    {
        return toAjax(resourceMapper.deleteById(resourceId));
    }

    /** 更新下载资源（网盘链接失效维护） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @Log(title = "影片下载资源", businessType = BusinessType.UPDATE)
    @PutMapping("/resource")
    public AjaxResult editResource(@RequestBody KmMediaResource resource)
    {
        if (resource.getResourceId() == null || resource.getUrl() == null || resource.getUrl().isEmpty()
                || resource.getPlatform() == null || resource.getPlatform().isEmpty())
        {
            return error("resourceId / platform / url 必填");
        }
        resource.setUpdateBy(com.ruoyi.common.utils.SecurityUtils.getUsername());
        return toAjax(resourceMapper.updateKmMediaResource(resource));
    }

    /** 生成/更新影片的飞书资源文档，返回文档链接 */
    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @Log(title = "飞书资源文档", businessType = BusinessType.OTHER)
    @PostMapping("/{mediaId}/doc/feishu")
    public AjaxResult generateFeishuDoc(@PathVariable Long mediaId)
    {
        String url = docService.generateFeishuDoc(mediaId);
        // 推送给该片全部有效订阅者（含免责声明）
        int notified = notificationService.notifySubscribersResourceDoc(mediaId);
        AjaxResult ajax = success("飞书文档已生成/更新" + (notified > 0 ? "，已通知 " + notified + " 位订阅者" : ""));
        ajax.put("url", url);
        return ajax;
    }

    /** 飞书连接测试：验证 app-id/app-secret 是否可用 */
    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @GetMapping("/doc/feishu/test")
    public AjaxResult testFeishu()
    {
        String err = docService.testFeishuConnection();
        return err == null ? success("飞书连接正常，凭证有效") : error(err);
    }

    /** 一次性全量回填所有影片的上线平台/上线日期（含未来排期） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:sync')")
    @Log(title = "全量回填上线排期", businessType = BusinessType.OTHER)
    @PostMapping("/backfillReleases")
    public AjaxResult backfillReleases()
    {
        int n = mediaSyncService.backfillAllReleases();
        return success("已回填 " + n + " 部影片的上线排期");
    }

    /** 存量地区名归一为字母码（中文名/别名 -> 码，幂等可重跑） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:edit')")
    @Log(title = "地区名归一", businessType = BusinessType.UPDATE)
    @PostMapping("/normalizeRegions")
    public AjaxResult normalizeRegions()
    {
        int changed = 0;
        for (KmMedia m : mediaService.selectKmMediaList(new KmMedia()))
        {
            String norm = com.ruoyi.kemovie.util.KmRegionCatalog.normalizeCodes(m.getRegion());
            if (!java.util.Objects.equals(norm, m.getRegion()))
            {
                KmMedia upd = new KmMedia();
                upd.setMediaId(m.getMediaId());
                upd.setRegion(norm);
                mediaService.updateKmMedia(upd);
                changed++;
            }
        }
        return success("已归一 " + changed + " 部影片的地区名（存字母码，前台显示自动补全中文）");
    }

    /** 一次性本地化所有平台 logo（下载远程 logo 落地并回写） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:sync')")
    @Log(title = "本地化平台logo", businessType = BusinessType.OTHER)
    @PostMapping("/localizeProviderLogos")
    public AjaxResult localizeProviderLogos()
    {
        int n = imageService.localizeAllPlatformLogos();
        return success("已本地化 " + n + " 个平台 logo");
    }

    /** 一次性把本地图片（海报/剧照/头像/平台 logo）迁移到阿里云 OSS（需 storage=oss） */
    @PreAuthorize("@ss.hasPermi('kemovie:media:sync')")
    @Log(title = "迁移图片到OSS", businessType = BusinessType.OTHER)
    @PostMapping("/migrateImagesToOss")
    public AjaxResult migrateImagesToOss()
    {
        int n = imageService.migrateLocalImagesToOss();
        return success("已迁移 " + n + " 处图片到 OSS");
    }
}
