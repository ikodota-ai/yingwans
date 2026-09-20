package com.ruoyi.kemovie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 影弯业务配置
 *
 * @author kemovie
 */
@Component
@ConfigurationProperties(prefix = "kemovie")
public class KemovieProperties
{
    private Tmdb tmdb = new Tmdb();
    private Mdl mdl = new Mdl();
    private JustWatch justwatch = new JustWatch();
    private Letterboxd letterboxd = new Letterboxd();
    private Feishu feishu = new Feishu();
    private Wechat wechat = new Wechat();
    private Proxy proxy = new Proxy();
    private Image image = new Image();
    /** 仅无头 Chrome 使用的代理（可带 scheme，如 socks5://127.0.0.1:10795）；留空则跟随 proxy 配置 */
    private String chromeProxy;

    public Tmdb getTmdb() { return tmdb; }
    public void setTmdb(Tmdb tmdb) { this.tmdb = tmdb; }
    public Mdl getMdl() { return mdl; }
    public void setMdl(Mdl mdl) { this.mdl = mdl; }
    public JustWatch getJustwatch() { return justwatch; }
    public void setJustwatch(JustWatch justwatch) { this.justwatch = justwatch; }
    public Letterboxd getLetterboxd() { return letterboxd; }
    public void setLetterboxd(Letterboxd letterboxd) { this.letterboxd = letterboxd; }
    public Feishu getFeishu() { return feishu; }
    public void setFeishu(Feishu feishu) { this.feishu = feishu; }
    public Wechat getWechat() { return wechat; }
    public void setWechat(Wechat wechat) { this.wechat = wechat; }
    public Proxy getProxy() { return proxy; }
    public void setProxy(Proxy proxy) { this.proxy = proxy; }
    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }
    public String getChromeProxy() { return chromeProxy; }
    public void setChromeProxy(String chromeProxy) { this.chromeProxy = chromeProxy; }

    public static class Tmdb
    {
        private String apiKey;
        private String baseUrl = "https://api.themoviedb.org/3";
        private String imageBase = "https://image.tmdb.org/t/p";
        private String language = "zh-CN";
        private String region = "CN";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getImageBase() { return imageBase; }
        public void setImageBase(String imageBase) { this.imageBase = imageBase; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    public static class Mdl
    {
        private String baseUrl = "https://mydramalist.com";
        private String chromePath;
        private int virtualTimeBudget = 20000;
        private long renderTimeout = 60000;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getChromePath() { return chromePath; }
        public void setChromePath(String chromePath) { this.chromePath = chromePath; }
        public int getVirtualTimeBudget() { return virtualTimeBudget; }
        public void setVirtualTimeBudget(int virtualTimeBudget) { this.virtualTimeBudget = virtualTimeBudget; }
        public long getRenderTimeout() { return renderTimeout; }
        public void setRenderTimeout(long renderTimeout) { this.renderTimeout = renderTimeout; }
    }

    public static class JustWatch
    {
        /** GraphQL 接口地址（JustWatch 纯 JSON API，无需无头浏览器） */
        private String graphqlUrl = "https://apis.justwatch.com/graphql";
        /** 默认国家（ISO 两位大写），抓取源未填地区时使用 */
        private String country = "US";
        /** 默认内容语言 */
        private String language = "en";
        /** 每页条数（popularTitles 分页，最大 100） */
        private int pageSize = 100;
        /** 翻页请求间隔（毫秒），礼貌限速 */
        private long requestIntervalMs = 250L;
        /** 平台目录缓存毫秒数（packages 解析 slug->shortName 用） */
        private long packageCacheMs = 6L * 60L * 60L * 1000L;
        /** 是否走全局代理访问 JustWatch（默认否：JW 直连稳定，代理反而常被断握手） */
        private boolean useProxy = false;
        /** 区域上新抓取的国家列表（逗号分隔，ISO 两位大写） */
        private String newCountries = "HK,TW,US";
        /** 区域上新每次抓取的最大页数 */
        private int newMaxPages = 2;
        /** 区域上新每页条数（newTitles 分页） */
        private int newPageSize = 50;
        /** 每区域每次最多自动同步入库的新影片数（防失控） */
        private int newMediaSyncLimit = 40;

        public String getGraphqlUrl() { return graphqlUrl; }
        public void setGraphqlUrl(String graphqlUrl) { this.graphqlUrl = graphqlUrl; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public long getRequestIntervalMs() { return requestIntervalMs; }
        public void setRequestIntervalMs(long requestIntervalMs) { this.requestIntervalMs = requestIntervalMs; }
        public long getPackageCacheMs() { return packageCacheMs; }
        public void setPackageCacheMs(long packageCacheMs) { this.packageCacheMs = packageCacheMs; }
        public boolean isUseProxy() { return useProxy; }
        public void setUseProxy(boolean useProxy) { this.useProxy = useProxy; }
        public String getNewCountries() { return newCountries; }
        public void setNewCountries(String newCountries) { this.newCountries = newCountries; }
        public int getNewMaxPages() { return newMaxPages; }
        public void setNewMaxPages(int newMaxPages) { this.newMaxPages = newMaxPages; }
        public int getNewPageSize() { return newPageSize; }
        public void setNewPageSize(int newPageSize) { this.newPageSize = newPageSize; }
        public int getNewMediaSyncLimit() { return newMediaSyncLimit; }
        public void setNewMediaSyncLimit(int newMediaSyncLimit) { this.newMediaSyncLimit = newMediaSyncLimit; }
    }

    public static class Letterboxd
    {
        private String baseUrl = "https://letterboxd.com";
        /** 无头 Chrome 渲染预算（毫秒，需足够通过 Cloudflare 挑战） */
        private int virtualTimeBudget = 20000;
        private long renderTimeout = 90000;
        /** 影片页渲染间隔（毫秒），礼貌限速 */
        private long requestIntervalMs = 300L;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getVirtualTimeBudget() { return virtualTimeBudget; }
        public void setVirtualTimeBudget(int virtualTimeBudget) { this.virtualTimeBudget = virtualTimeBudget; }
        public long getRenderTimeout() { return renderTimeout; }
        public void setRenderTimeout(long renderTimeout) { this.renderTimeout = renderTimeout; }
        public long getRequestIntervalMs() { return requestIntervalMs; }
        public void setRequestIntervalMs(long requestIntervalMs) { this.requestIntervalMs = requestIntervalMs; }
    }

    public static class Proxy
    {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        private int port = 10792;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    /** 飞书自建应用（生成资源文档用；个人可免费创建飞书企业后建应用） */
    public static class Feishu
    {
        /** API 基址（国内 https://open.feishu.cn，海外 Lark https://open.larksuite.com） */
        private String baseUrl = "https://open.feishu.cn";
        private String appId;
        private String appSecret;
        /** 分享权限：anyone_readable 互联网上任何人可读 / tenant_readable 组织内可读 */
        private String linkShare = "anyone_readable";
        /** 是否走全局代理访问飞书（默认否） */
        private boolean useProxy = false;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getLinkShare() { return linkShare; }
        public void setLinkShare(String linkShare) { this.linkShare = linkShare; }
        public boolean isUseProxy() { return useProxy; }
        public void setUseProxy(boolean useProxy) { this.useProxy = useProxy; }
    }

    /** 微信公众号（接口测试号/服务号）模板消息推送 */
    public static class Wechat
    {
        private String appId;
        private String appSecret;
        /** 模板消息模板ID（测试号后台自定义） */
        private String templateId;
        /** 是否启用 wechat 渠道推送（未配置凭证时自动跳过） */
        private boolean enabled = true;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Image
    {
        private boolean localize = false;
        private String localDir;
        /** 存储方式：local 本地磁盘（默认）/ oss 阿里云 OSS */
        private String storage = "local";
        private Oss oss = new Oss();

        public boolean isLocalize() { return localize; }
        public void setLocalize(boolean localize) { this.localize = localize; }
        public String getLocalDir() { return localDir; }
        public void setLocalDir(String localDir) { this.localDir = localDir; }
        public String getStorage() { return storage; }
        public void setStorage(String storage) { this.storage = storage; }
        public Oss getOss() { return oss; }
        public void setOss(Oss oss) { this.oss = oss; }
    }

    /** 阿里云 OSS 配置（仅 storage=oss 时生效） */
    public static class Oss
    {
        /** Endpoint，如 oss-cn-hangzhou.aliyuncs.com（无需 https:// 前缀） */
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        /** 对象 Key 前缀，默认 kemovie */
        private String dir = "kemovie";
        /**
         * 对外访问域名（可选）：绑定 CDN/自定义域名时填写，如 https://cdn.example.com；
         * 留空则使用 https://{bucket}.{endpoint}
         */
        private String domain;
        /** 上传对象是否设为公共读（桶公共读关闭时可设 false，配合 CDN 回源） */
        private boolean publicRead = true;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isPublicRead() { return publicRead; }
        public void setPublicRead(boolean publicRead) { this.publicRead = publicRead; }
    }
}
