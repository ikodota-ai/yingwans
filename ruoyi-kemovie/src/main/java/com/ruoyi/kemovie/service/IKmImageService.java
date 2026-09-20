package com.ruoyi.kemovie.service;

import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmPerson;

/**
 * 图片本地化 Service：将 TMDB/MDL 的海报、剧照、人物头像下载到本地存储
 * （或后期 OSS），并回填 *_local_url 字段。
 *
 * @author kemovie
 */
public interface IKmImageService
{
    /** 是否开启本地化（配置 kemovie.image.localize） */
    boolean isEnabled();

    /**
     * 本地化影片海报（poster），必要时下载并回填 poster_local_url。
     *
     * @return 是否发生了下载并回填
     */
    boolean localizeMediaPoster(KmMedia media);

    /**
     * 本地化人物头像（profile），必要时下载并回填 profile_local_url。
     *
     * @return 是否发生了下载并回填
     */
    boolean localizePersonProfile(KmPerson person);

    /**
     * 本地化单个平台的 logo：下载远程 logo 落地到本地存储，返回本地可访问 URL。
     * 已是本地地址或无法下载时返回原始地址（不阻断主流程）。同一平台复用同一文件。
     *
     * @param platform   平台名（原始，如 Netflix / Amazon Video）
     * @param remoteLogo 远程 logo 地址（TMDB 等）
     * @return 本地 URL；失败回退原始地址
     */
    String localizePlatformLogo(String platform, String remoteLogo);

    /**
     * 一次性回填：将库中各平台的远程 logo 全部本地化，并回写 km_media_release。
     *
     * @return 成功本地化的平台数量
     */
    int localizeAllPlatformLogos();

    /**
     * 一次性迁移：将已落本地（/profile/kemovie/...）的海报、剧照、人物头像、
     * 平台 logo 上传到 OSS（仅在 kemovie.image.storage=oss 时可用），并回写数据库。
     *
     * @return 迁移的图片条数
     */
    int migrateLocalImagesToOss();
}
