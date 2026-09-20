package com.ruoyi.kemovie.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 图片存储：{@code kemovie.image.storage=oss} 时启用，
 * 与 {@link LocalImageStorage} 互斥。业务侧仅依赖 {@link ImageStorage} 接口。
 *
 * @author kemovie
 */
@Component
@ConditionalOnProperty(prefix = "kemovie.image", name = "storage", havingValue = "oss")
public class OssImageStorage implements ImageStorage
{
    @Autowired private OssUploader ossUploader;

    @Override
    public String store(String relativePath, byte[] bytes)
    {
        if (bytes == null || bytes.length == 0 || relativePath == null || relativePath.isEmpty()) { return null; }
        String contentType = contentTypeOf(relativePath);
        return ossUploader.putObject(relativePath, bytes, contentType);
    }

    @Override
    public boolean isManaged(String url)
    {
        return ossUploader.isOssUrl(url);
    }

    private static String contentTypeOf(String key)
    {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) { return "image/png"; }
        if (lower.endsWith(".webp")) { return "image/webp"; }
        if (lower.endsWith(".avif")) { return "image/avif"; }
        if (lower.endsWith(".gif")) { return "image/gif"; }
        if (lower.endsWith(".svg")) { return "image/svg+xml"; }
        return "image/jpeg";
    }
}
