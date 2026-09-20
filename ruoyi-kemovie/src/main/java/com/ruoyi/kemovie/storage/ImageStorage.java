package com.ruoyi.kemovie.storage;

/**
 * 图片存储抽象：当前提供本地实现，后期可替换/新增 OSS 实现（阿里云 OSS、S3 等）。
 *
 * @author kemovie
 */
public interface ImageStorage
{
    /**
     * 保存图片字节并返回可对外访问的 URL。
     *
     * @param relativePath 相对路径（含子目录与文件名），如 {@code poster/movie/123_w500.jpg}
     * @param bytes        图片字节
     * @return 可访问 URL；失败返回 null
     */
    String store(String relativePath, byte[] bytes);

    /**
     * 判断给定的 URL 是否已由本存储托管（用于避免重复下载）。
     */
    boolean isManaged(String url);
}
