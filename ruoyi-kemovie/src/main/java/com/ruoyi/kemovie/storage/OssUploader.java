package com.ruoyi.kemovie.storage;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * 阿里云 OSS 上传器：仅在 {@code kemovie.image.storage=oss} 时装配。
 *
 * <p>同时服务两类图片：</p>
 * <ul>
 *   <li>影弯自动本地化的图片（海报/剧照/人物头像/平台 logo），通过 {@link OssImageStorage} 调用；</li>
 *   <li>后台/用户直接上传的文件（通用上传、头像、运营位二维码等）。</li>
 * </ul>
 *
 * @author kemovie
 */
@Component
@ConditionalOnProperty(prefix = "kemovie.image", name = "storage", havingValue = "oss")
public class OssUploader
{
    private static final Logger log = LoggerFactory.getLogger(OssUploader.class);

    @Autowired private KemovieProperties props;

    private OSS ossClient;
    private String bucket;
    private String rootDir;
    private String publicBase;

    @PostConstruct
    public void init()
    {
        KemovieProperties.Oss cfg = props.getImage().getOss();
        if (StringUtils.isEmpty(cfg.getEndpoint()) || StringUtils.isEmpty(cfg.getAccessKeyId())
                || StringUtils.isEmpty(cfg.getAccessKeySecret()) || StringUtils.isEmpty(cfg.getBucketName()))
        {
            throw new ServiceException("已启用 OSS 存储(kemovie.image.storage=oss)，但 endpoint/accessKeyId/accessKeySecret/bucketName 未配置完整");
        }
        String endpoint = cfg.getEndpoint().trim().replaceFirst("^https?://", "");
        this.bucket = cfg.getBucketName().trim();
        this.rootDir = normalizeKey(cfg.getDir());
        String domain = cfg.getDomain();

        ClientBuilderConfiguration clientCfg = new ClientBuilderConfiguration();
        String clientHost;
        if (StringUtils.isNotEmpty(domain))
        {
            // 绑定了自定义域名/CDN：SDK 以该域名为 Host 并开启 CNAME（读写均走该域名，
            // 形如 https://image.example.com/key），避免依赖 bucket 三级子域名解析。
            String d = domain.trim().replaceAll("/+$", "");
            this.publicBase = d;
            clientHost = d.replaceFirst("^https?://", "");
            clientCfg.setSupportCname(true);
        }
        else
        {
            // 未配置自定义域名：走默认三级子域名 https://{bucket}.{endpoint}
            this.publicBase = "https://" + bucket + "." + endpoint;
            clientHost = endpoint;
        }
        this.ossClient = new OSSClientBuilder().build("https://" + clientHost, cfg.getAccessKeyId(), cfg.getAccessKeySecret(), clientCfg);
        log.info("阿里云 OSS 存储已初始化 bucket={} rootDir={} clientHost={} cname={}",
                bucket, rootDir, clientHost, StringUtils.isNotEmpty(domain));
    }

    @PreDestroy
    public void destroy()
    {
        if (ossClient != null) { ossClient.shutdown(); }
    }

    /**
     * 上传字节到 OSS。
     *
     * @param relativePath 相对路径（不含根目录），如 {@code poster/movie/3_w500.jpg}
     * @param bytes        内容
     * @param contentType  MIME 类型，为空时按扩展名推断
     * @return 可公网访问的 URL
     */
    public String putObject(String relativePath, byte[] bytes, String contentType)
    {
        String key = buildKey(relativePath);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(bytes.length);
        meta.setContentType(StringUtils.isNotEmpty(contentType) ? contentType : contentTypeOf(key));
        ossClient.putObject(bucket, key, new ByteArrayInputStream(bytes), meta);
        if (props.getImage().getOss().isPublicRead())
        {
            ossClient.setObjectAcl(bucket, key, CannedAccessControlList.PublicRead);
        }
        return publicUrl(key);
    }

    /**
     * 上传 MultipartFile（后台通用上传/头像用）。
     *
     * @param file    文件
     * @param baseDir 业务目录，如 {@code upload}、{@code avatar}
     * @return 可公网访问的 URL
     */
    public String uploadMultipart(MultipartFile file, String baseDir)
    {
        try
        {
            String ext = extOf(file.getOriginalFilename());
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String name = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ext;
            String rel = stripSlashes(baseDir) + "/" + datePath + "/" + name;
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            return putObject(rel, bytes, contentType);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("上传 OSS 失败：" + e.getMessage());
        }
    }

    /** 判断 URL 是否由当前 OSS 托管 */
    public boolean isOssUrl(String url)
    {
        return StringUtils.isNotEmpty(url) && url.startsWith(publicBase + "/");
    }

    /** 由对象 key 拼对外 URL */
    public String publicUrl(String key)
    {
        return publicBase + "/" + key;
    }

    private String buildKey(String relativePath)
    {
        String rel = stripSlashes(relativePath);
        return StringUtils.isEmpty(rootDir) ? rel : rootDir + "/" + rel;
    }

    private static String stripSlashes(String s)
    {
        if (s == null) { return ""; }
        return s.replaceFirst("^/+", "").replaceFirst("/+$", "");
    }

    private static String normalizeKey(String dir)
    {
        return stripSlashes(dir);
    }

    private static String extOf(String filename)
    {
        if (StringUtils.isEmpty(filename)) { return ""; }
        int dot = filename.lastIndexOf('.');
        if (dot > -1 && dot < filename.length() - 1)
        {
            String ext = filename.substring(dot).toLowerCase();
            return ext.length() <= 8 ? ext : "";
        }
        return "";
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
