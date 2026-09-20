package com.ruoyi.kemovie.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.StringUtils;

/**
 * 本地图片存储：写入 {@code ruoyi.profile} 目录下的 kemovie 子目录，
 * 通过若依已有的 {@code /profile/**} 静态资源映射对外提供访问。
 *
 * <p>后期接入 OSS 时，新增一个 {@code OssImageStorage} 并调整注入即可，
 * 业务侧只依赖 {@link ImageStorage} 接口。</p>
 *
 * @author kemovie
 */
@Component
@ConditionalOnProperty(prefix = "kemovie.image", name = "storage", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage
{
    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    /** 存放在 profile 下的一级子目录，便于与其它上传隔离 */
    private static final String ROOT = "kemovie";

    @Override
    public String store(String relativePath, byte[] bytes)
    {
        if (bytes == null || bytes.length == 0 || StringUtils.isEmpty(relativePath)) { return null; }
        try
        {
            String rel = ROOT + "/" + relativePath.replaceFirst("^/+", "");
            Path target = Paths.get(RuoYiConfig.getProfile(), rel);
            File dir = target.getParent().toFile();
            if (!dir.exists() && !dir.mkdirs())
            {
                log.warn("创建图片目录失败 {}", dir.getAbsolutePath());
                return null;
            }
            Files.write(target, bytes);
            // 对外 URL：/profile/kemovie/xxx
            return Constants.RESOURCE_PREFIX + "/" + rel;
        }
        catch (Exception e)
        {
            log.warn("本地保存图片失败 path={} err={}", relativePath, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isManaged(String url)
    {
        return StringUtils.isNotEmpty(url) && url.startsWith(Constants.RESOURCE_PREFIX + "/" + ROOT + "/");
    }
}
