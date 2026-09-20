package com.ruoyi.kemovie.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.kemovie.storage.OssUploader;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.TmdbClient;
import com.ruoyi.kemovie.config.KemovieProperties;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmPerson;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaReleaseMapper;
import com.ruoyi.kemovie.mapper.KmPersonMapper;
import com.ruoyi.kemovie.service.IKmImageService;
import com.ruoyi.kemovie.storage.ImageStorage;

/**
 * 图片本地化实现：从 TMDB（poster_path / profile_path）或已知远程 URL 下载图片，
 * 通过 {@link ImageStorage} 落地并回填 *_local_url 字段。
 *
 * @author kemovie
 */
@Service
public class KmImageServiceImpl implements IKmImageService
{
    private static final Logger log = LoggerFactory.getLogger(KmImageServiceImpl.class);

    /** 海报默认尺寸 */
    private static final String POSTER_SIZE = "w500";
    /** 头像默认尺寸 */
    private static final String PROFILE_SIZE = "w342";
    /** 平台 logo 默认尺寸（远程 URL 里已包含尺寸，仅用于落地文件名） */
    private static final String PROVIDER_SIZE = "w92";

    @Autowired private KemovieProperties props;
    @Autowired private TmdbClient tmdbClient;
    @Autowired private ImageStorage imageStorage;
    @Autowired private ObjectProvider<OssUploader> ossUploaderProvider;
    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private KmMediaReleaseMapper releaseMapper;
    @Autowired private KmPersonMapper personMapper;

    @Override
    public boolean isEnabled()
    {
        return props.getImage().isLocalize();
    }

    @Override
    public boolean localizeMediaPoster(KmMedia media)
    {
        if (!isEnabled() || media == null) { return false; }
        String path = media.getPosterPath();
        if (StringUtils.isEmpty(path) || !path.startsWith("/")) { return false; }
        if (imageStorage.isManaged(media.getPosterLocalUrl())) { return false; }

        String src = tmdbClient.imageUrl(POSTER_SIZE, path);
        byte[] bytes = tmdbClient.downloadImage(src);
        if (bytes == null) { return false; }
        String type = StringUtils.isNotEmpty(media.getMediaType()) ? media.getMediaType() : "movie";
        String rel = "poster/" + type + "/" + media.getMediaId() + "_" + POSTER_SIZE + ext(path);
        String url = imageStorage.store(rel, bytes);
        if (url == null) { return false; }

        KmMedia upd = new KmMedia();
        upd.setMediaId(media.getMediaId());
        upd.setPosterLocalUrl(url);
        mediaMapper.updateKmMedia(upd);
        media.setPosterLocalUrl(url);
        return true;
    }

    @Override
    public boolean localizePersonProfile(KmPerson person)
    {
        if (!isEnabled() || person == null) { return false; }
        String path = person.getProfilePath();
        if (StringUtils.isEmpty(path) || !path.startsWith("/")) { return false; }
        if (imageStorage.isManaged(person.getProfileLocalUrl())) { return false; }

        String src = tmdbClient.imageUrl(PROFILE_SIZE, path);
        byte[] bytes = tmdbClient.downloadImage(src);
        if (bytes == null) { return false; }
        String rel = "profile/" + person.getPersonId() + "_" + PROFILE_SIZE + ext(path);
        String url = imageStorage.store(rel, bytes);
        if (url == null) { return false; }

        KmPerson upd = new KmPerson();
        upd.setPersonId(person.getPersonId());
        upd.setProfileLocalUrl(url);
        personMapper.updatePerson(upd);
        person.setProfileLocalUrl(url);
        return true;
    }

    @Override
    public String localizePlatformLogo(String platform, String remoteLogo)
    {
        if (!isEnabled() || StringUtils.isEmpty(platform) || StringUtils.isEmpty(remoteLogo))
        {
            return remoteLogo;
        }
        // 已是本地地址直接返回
        if (imageStorage.isManaged(remoteLogo)) { return remoteLogo; }
        // 同平台若已有本地化 logo，复用之，避免重复下载
        String existing = releaseMapper.selectLocalLogoByPlatform(platform);
        if (imageStorage.isManaged(existing)) { return existing; }

        byte[] bytes = tmdbClient.downloadImage(remoteLogo);
        if (bytes == null) { return remoteLogo; }
        String rel = "provider/" + slug(platform) + "_" + PROVIDER_SIZE + ext(remoteLogo);
        String url = imageStorage.store(rel, bytes);
        return url == null ? remoteLogo : url;
    }

    @Override
    public int localizeAllPlatformLogos()
    {
        if (!isEnabled()) { return 0; }
        List<Map<String, Object>> logos = mediaMapper.selectPlatformLogos();
        int done = 0;
        for (Map<String, Object> row : logos)
        {
            String platform = row.get("platform") == null ? null : row.get("platform").toString();
            String logo = row.get("logo") == null ? null : row.get("logo").toString();
            if (StringUtils.isEmpty(platform) || StringUtils.isEmpty(logo)) { continue; }
            if (imageStorage.isManaged(logo)) { continue; }
            String local = localizePlatformLogo(platform, logo);
            if (imageStorage.isManaged(local))
            {
                releaseMapper.updatePlatformLogoByPlatform(platform, local);
                done++;
            }
        }
        log.info("平台 logo 本地化完成，共 {} 个平台", done);
        return done;
    }

    @Override
    public int migrateLocalImagesToOss()
    {
        if (ossUploaderProvider.getIfAvailable() == null)
        {
            throw new ServiceException("当前未启用 OSS 存储（kemovie.image.storage=oss），无法迁移");
        }
        int changed = 0;

        // 1) 影片海报 + 剧照
        for (Map<String, Object> row : mediaMapper.selectLocalImageMedia())
        {
            Long mediaId = ((Number) row.get("mediaId")).longValue();
            String poster = row.get("posterLocalUrl") == null ? null : row.get("posterLocalUrl").toString();
            String backdrop = row.get("backdropLocalUrl") == null ? null : row.get("backdropLocalUrl").toString();
            String newPoster = migrateUrl(poster);
            String newBackdrop = migrateUrl(backdrop);
            if (newPoster != null || newBackdrop != null)
            {
                KmMedia upd = new KmMedia();
                upd.setMediaId(mediaId);
                upd.setPosterLocalUrl(newPoster);
                upd.setBackdropLocalUrl(newBackdrop);
                mediaMapper.updateKmMedia(upd);
                changed++;
            }
        }

        // 2) 人物头像
        for (Map<String, Object> row : personMapper.selectLocalImagePersons())
        {
            Long personId = ((Number) row.get("personId")).longValue();
            String profile = row.get("profileLocalUrl") == null ? null : row.get("profileLocalUrl").toString();
            String newProfile = migrateUrl(profile);
            if (newProfile != null)
            {
                KmPerson upd = new KmPerson();
                upd.setPersonId(personId);
                upd.setProfileLocalUrl(newProfile);
                personMapper.updatePerson(upd);
                changed++;
            }
        }

        // 3) 平台 logo（同平台共用一张）
        for (Map<String, Object> row : mediaMapper.selectPlatformLogos())
        {
            String platform = row.get("platform") == null ? null : row.get("platform").toString();
            String logo = row.get("logo") == null ? null : row.get("logo").toString();
            String newLogo = migrateUrl(logo);
            if (newLogo != null && StringUtils.isNotEmpty(platform))
            {
                releaseMapper.updatePlatformLogoByPlatform(platform, newLogo);
                changed++;
            }
        }
        log.info("本地图片迁移 OSS 完成，共更新 {} 处", changed);
        return changed;
    }

    /**
     * 将一个本地 /profile/kemovie/** URL 对应的文件上传到当前存储（OSS）。
     *
     * @return 新 URL；非本地地址/文件缺失/已是 OSS 时返回 null
     */
    private String migrateUrl(String localUrl)
    {
        if (StringUtils.isEmpty(localUrl) || !localUrl.startsWith(Constants.RESOURCE_PREFIX + "/kemovie/"))
        {
            return null;
        }
        if (imageStorage.isManaged(localUrl) && !localUrl.startsWith(Constants.RESOURCE_PREFIX))
        {
            return null;
        }
        String rel = localUrl.substring((Constants.RESOURCE_PREFIX + "/kemovie/").length());
        File file = new File(RuoYiConfig.getProfile(), localUrl.substring(Constants.RESOURCE_PREFIX.length()));
        if (!file.isFile())
        {
            log.warn("迁移时找不到本地文件 {}", file.getAbsolutePath());
            return null;
        }
        try
        {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String url = imageStorage.store(rel, bytes);
            if (StringUtils.isNotEmpty(url) && !url.equals(localUrl) && imageStorage.isManaged(url)
                    && !url.startsWith(Constants.RESOURCE_PREFIX))
            {
                return url;
            }
        }
        catch (Exception e)
        {
            log.warn("迁移图片失败 {} err={}", localUrl, e.getMessage());
        }
        return null;
    }

    /** 平台名 slug：仅保留字母数字，作为 logo 文件名 */
    private String slug(String name)
    {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray())
        {
            if (Character.isLetterOrDigit(c)) { sb.append(Character.toLowerCase(c)); }
        }
        String s = sb.toString();
        return s.isEmpty() ? "provider" : s;
    }

    /** 从原始 path 提取扩展名，默认 .jpg */
    private String ext(String path)
    {
        int dot = path.lastIndexOf('.');
        if (dot > -1 && dot < path.length() - 1)
        {
            String e = path.substring(dot);
            if (e.length() <= 5) { return e; }
        }
        return ".jpg";
    }
}
