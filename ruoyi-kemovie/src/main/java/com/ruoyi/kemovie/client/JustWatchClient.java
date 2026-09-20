package com.ruoyi.kemovie.client;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * JustWatch GraphQL 客户端。
 *
 * JustWatch 提供纯 JSON 的 GraphQL 接口 {@code https://apis.justwatch.com/graphql}，
 * 普通 UA + POST 即可访问，无需无头浏览器。通过 popularTitles + packages 两个查询，
 * 可以按“国家 + 播放平台”分页拉取全站片单，且条目直接带 tmdbId / imdbId，可精确入库。
 *
 * @author kemovie
 */
@Component
public class JustWatchClient
{
    private static final Logger log = LoggerFactory.getLogger(JustWatchClient.class);
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    /** 平台目录：packageId / technicalName / shortName（过滤用） / slug（URL 用） */
    private static final String Q_PACKAGES =
            "query($c:Country!,$p:Platform!){ packages(country:$c,platform:$p)"
            + "{ packageId technicalName shortName slug clearName monetizationTypes } }";

    /** 热门/平台片单（游标分页），条目直接含 externalIds.tmdbId */
    private static final String Q_POPULAR =
            "query($c:Country!,$l:Language!,$first:Int!,$after:String,$f:TitleFilter){"
            + " popularTitles(country:$c,language:$l,first:$first,after:$after,filter:$f){"
            + " totalCount edges{ node{ id objectType content(country:$c,language:$l){"
            + " title originalReleaseYear externalIds{ tmdbId imdbId } } } }"
            + " pageInfo{ endCursor hasNextPage } } }";

    /** 指定国家某日的最新上线（newTitles，含 offers：平台/观看方式/链接） */
    private static final String Q_NEW_TITLES =
            "query($c:Country!,$l:Language!,$d:Date!,$first:Int!,$after:String){"
            + " newTitles(country:$c,date:$d,first:$first,after:$after){"
            + " totalCount edges{ node{ id objectType content(country:$c,language:$l){"
            + " title originalReleaseYear externalIds{ tmdbId imdbId } }"
            + " offers(country:$c,platform:WEB){ monetizationType presentationType"
            + " package{ packageId shortName clearName } standardWebURL } } }"
            + " pageInfo{ endCursor hasNextPage } } }";

    private final KemovieProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile HttpClient httpClient;
    private final Map<String, CacheEntry> packageCache = new ConcurrentHashMap<>();

    public JustWatchClient(KemovieProperties props)
    {
        this.props = props;
    }

    /** JustWatch 播放平台包信息 */
    public static class JwPackage
    {
        private int packageId;
        private String technicalName;
        private String shortName;
        private String slug;
        private String clearName;
        private List<String> monetizationTypes = new ArrayList<>();

        public int getPackageId() { return packageId; }
        public void setPackageId(int packageId) { this.packageId = packageId; }
        public String getTechnicalName() { return technicalName; }
        public void setTechnicalName(String technicalName) { this.technicalName = technicalName; }
        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getClearName() { return clearName; }
        public void setClearName(String clearName) { this.clearName = clearName; }
        public List<String> getMonetizationTypes() { return monetizationTypes; }
        public void setMonetizationTypes(List<String> monetizationTypes) { this.monetizationTypes = monetizationTypes; }
    }

    /** 单个片单条目（已转成本站口径 movie/tv + tmdbId） */
    public static class JwTitle
    {
        private String jwId;
        private String mediaType;
        private String title;
        private Integer year;
        private Long tmdbId;
        private String imdbId;

        public String getJwId() { return jwId; }
        public void setJwId(String jwId) { this.jwId = jwId; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String mediaType) { this.mediaType = mediaType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Long getTmdbId() { return tmdbId; }
        public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
        public String getImdbId() { return imdbId; }
        public void setImdbId(String imdbId) { this.imdbId = imdbId; }
    }

    /** 分页结果 */
    public static class JwPage
    {
        private List<JwTitle> titles = new ArrayList<>();
        private String endCursor;
        private boolean hasNext;
        private int total;

        public List<JwTitle> getTitles() { return titles; }
        public void setTitles(List<JwTitle> titles) { this.titles = titles; }
        public String getEndCursor() { return endCursor; }
        public void setEndCursor(String endCursor) { this.endCursor = endCursor; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
    }

    /** 上新条目中的单个观看方式（平台套餐 + 货币化类型 + 直达链接） */
    public static class JwNewOffer
    {
        private String monetizationType;
        private int packageId;
        private String packageShort;
        private String packageName;
        private String url;

        public String getMonetizationType() { return monetizationType; }
        public void setMonetizationType(String monetizationType) { this.monetizationType = monetizationType; }
        public int getPackageId() { return packageId; }
        public void setPackageId(int packageId) { this.packageId = packageId; }
        public String getPackageShort() { return packageShort; }
        public void setPackageShort(String packageShort) { this.packageShort = packageShort; }
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    /** 上新条目（tmdbId 已截断为纯 show/movie id，offers 按 平台+观看方式 去重） */
    public static class JwNewTitle
    {
        private String jwId;
        private String mediaType;
        private String title;
        private Long tmdbId;
        private List<JwNewOffer> offers = new ArrayList<>();

        public String getJwId() { return jwId; }
        public void setJwId(String jwId) { this.jwId = jwId; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String mediaType) { this.mediaType = mediaType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Long getTmdbId() { return tmdbId; }
        public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
        public List<JwNewOffer> getOffers() { return offers; }
        public void setOffers(List<JwNewOffer> offers) { this.offers = offers; }
    }

    /** 上新分页结果 */
    public static class JwNewPage
    {
        private List<JwNewTitle> titles = new ArrayList<>();
        private String endCursor;
        private boolean hasNext;
        private int total;

        public List<JwNewTitle> getTitles() { return titles; }
        public void setTitles(List<JwNewTitle> titles) { this.titles = titles; }
        public String getEndCursor() { return endCursor; }
        public void setEndCursor(String endCursor) { this.endCursor = endCursor; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
    }

    private HttpClient client()
    {
        if (httpClient == null)
        {
            synchronized (this)
            {
                if (httpClient == null)
                {
                    HttpClient.Builder b = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(15))
                            .followRedirects(HttpClient.Redirect.NORMAL);
                    if (props.getProxy().isEnabled() && props.getJustwatch().isUseProxy())
                    {
                        b.proxy(ProxySelector.of(new InetSocketAddress(
                                props.getProxy().getHost(), props.getProxy().getPort())));
                    }
                    httpClient = b.build();
                }
            }
        }
        return httpClient;
    }

    /** 拉取指定国家的全部播放平台包（WEB 平台），带缓存 */
    public List<JwPackage> packages(String country)
    {
        String cc = normalizeCountry(country);
        CacheEntry cached = packageCache.get(cc);
        if (cached != null && !cached.expired(props.getJustwatch().getPackageCacheMs()))
        {
            return cached.packages;
        }
        ObjectNode vars = mapper.createObjectNode();
        vars.put("c", cc);
        vars.put("p", "WEB");
        JsonNode data = postGraphql(Q_PACKAGES, vars);
        List<JwPackage> list = new ArrayList<>();
        JsonNode arr = data.path("packages");
        if (arr.isArray())
        {
            for (JsonNode n : arr)
            {
                JwPackage p = new JwPackage();
                p.setPackageId(n.path("packageId").asInt(0));
                p.setTechnicalName(n.path("technicalName").asText(null));
                p.setShortName(n.path("shortName").asText(null));
                p.setSlug(n.path("slug").asText(null));
                p.setClearName(n.path("clearName").asText(null));
                JsonNode mt = n.path("monetizationTypes");
                if (mt.isArray())
                {
                    mt.forEach(x -> p.getMonetizationTypes().add(x.asText()));
                }
                list.add(p);
            }
        }
        packageCache.put(cc, new CacheEntry(list));
        return list;
    }

    /**
     * 将用户填写的任意平台标识解析为平台包：支持完整 provider URL、slug(netflix)、
     * shortName(nfx)、technicalName(amazonprime) 或 packageId(8)。无法识别返回 null。
     */
    public JwPackage resolvePackage(String country, String token)
    {
        String key = stripProviderToken(token);
        if (StringUtils.isEmpty(key)) { return null; }
        key = key.toLowerCase(Locale.ROOT);
        for (JwPackage p : packages(country))
        {
            if (key.equals(p.getSlug()) || key.equals(p.getShortName())
                    || key.equals(p.getTechnicalName()) || key.equals(String.valueOf(p.getPackageId())))
            {
                return p;
            }
        }
        return null;
    }

    /**
     * 拉取一页热门/平台片单。
     *
     * @param packageShort 平台 shortName（如 nfx）；null 表示全国热门
     * @param mediaType    movie / tv；null 表示混合
     */
    public JwPage fetchPopular(String country, int first, String after, String packageShort, String mediaType)
    {
        String cc = normalizeCountry(country);
        ObjectNode vars = mapper.createObjectNode();
        vars.put("c", cc);
        vars.put("l", StringUtils.isNotEmpty(props.getJustwatch().getLanguage())
                ? props.getJustwatch().getLanguage() : "en");
        vars.put("first", first > 0 ? first : props.getJustwatch().getPageSize());
        if (StringUtils.isNotEmpty(after)) { vars.put("after", after); }

        boolean hasFilter = false;
        ObjectNode filter = mapper.createObjectNode();
        if (StringUtils.isNotEmpty(packageShort))
        {
            filter.putArray("packages").add(packageShort);
            hasFilter = true;
        }
        if ("movie".equalsIgnoreCase(mediaType) || "tv".equalsIgnoreCase(mediaType))
        {
            filter.putArray("objectTypes").add("tv".equalsIgnoreCase(mediaType) ? "SHOW" : "MOVIE");
            hasFilter = true;
        }
        if (hasFilter) { vars.set("f", filter); }

        JsonNode data = postGraphql(Q_POPULAR, vars);
        JsonNode pt = data.path("popularTitles");
        JwPage page = new JwPage();
        page.setTotal(pt.path("totalCount").asInt(0));
        JsonNode edges = pt.path("edges");
        if (edges.isArray())
        {
            for (JsonNode edge : edges)
            {
                JsonNode node = edge.path("node");
                JsonNode content = node.path("content");
                JsonNode ext = content.path("externalIds");
                JwTitle t = new JwTitle();
                t.setJwId(node.path("id").asText(null));
                t.setTitle(content.path("title").asText(null));
                if (content.has("originalReleaseYear") && !content.path("originalReleaseYear").isNull())
                {
                    t.setYear(content.path("originalReleaseYear").asInt());
                }
                t.setMediaType("SHOW".equalsIgnoreCase(node.path("objectType").asText()) ? "tv" : "movie");
                String tmdb = ext.path("tmdbId").asText(null);
                if (StringUtils.isNotEmpty(tmdb))
                {
                    try { t.setTmdbId(Long.parseLong(tmdb.trim())); } catch (NumberFormatException ignore) { }
                }
                t.setImdbId(ext.path("imdbId").asText(null));
                page.getTitles().add(t);
            }
        }
        JsonNode pageInfo = pt.path("pageInfo");
        page.setEndCursor(pageInfo.path("endCursor").asText(null));
        page.setHasNext(pageInfo.path("hasNextPage").asBoolean(false));
        return page;
    }

    /**
     * 拉取指定国家某日的最新上线（含观看方式），游标分页。
     * SHOW_SEASON 的复合 tmdbId（showId:season）在此截断为 show id，类型归为 tv。
     */
    public JwNewPage fetchNewTitles(String country, String date, int first, String after)
    {
        String cc = normalizeCountry(country);
        ObjectNode vars = mapper.createObjectNode();
        vars.put("c", cc);
        vars.put("l", props.getJustwatch().getLanguage());
        vars.put("d", date);
        vars.put("first", first);
        if (StringUtils.isNotEmpty(after)) { vars.put("after", after); }

        JsonNode data = postGraphql(Q_NEW_TITLES, vars);
        JsonNode nt = data.path("newTitles");
        JwNewPage page = new JwNewPage();
        page.setTotal(nt.path("totalCount").asInt(0));
        JsonNode edges = nt.path("edges");
        if (edges.isArray())
        {
            for (JsonNode edge : edges)
            {
                JsonNode node = edge.path("node");
                JsonNode content = node.path("content");
                JsonNode ext = content.path("externalIds");
                JwNewTitle t = new JwNewTitle();
                t.setJwId(node.path("id").asText(null));
                t.setTitle(content.path("title").asText(null));
                String objectType = node.path("objectType").asText("");
                t.setMediaType(objectType.toUpperCase(Locale.ROOT).contains("SHOW") ? "tv" : "movie");
                String tmdb = ext.path("tmdbId").asText(null);
                if (StringUtils.isNotEmpty(tmdb))
                {
                    int colon = tmdb.indexOf(':');
                    if (colon > 0) { tmdb = tmdb.substring(0, colon); }
                    try { t.setTmdbId(Long.parseLong(tmdb.trim())); } catch (NumberFormatException ignore) { }
                }
                java.util.Set<String> seen = new java.util.HashSet<>();
                JsonNode offers = node.path("offers");
                if (offers.isArray())
                {
                    for (JsonNode o : offers)
                    {
                        JsonNode pkg = o.path("package");
                        int packageId = pkg.path("packageId").asInt(0);
                        String mono = o.path("monetizationType").asText("");
                        if (packageId <= 0 || !seen.add(packageId + "|" + mono)) { continue; }
                        JwNewOffer offer = new JwNewOffer();
                        offer.setMonetizationType(mono);
                        offer.setPackageId(packageId);
                        offer.setPackageShort(pkg.path("shortName").asText(null));
                        offer.setPackageName(pkg.path("clearName").asText(null));
                        offer.setUrl(o.path("standardWebURL").asText(null));
                        t.getOffers().add(offer);
                    }
                }
                page.getTitles().add(t);
            }
        }
        JsonNode pageInfo = nt.path("pageInfo");
        page.setEndCursor(pageInfo.path("endCursor").asText(null));
        page.setHasNext(pageInfo.path("hasNextPage").asBoolean(false));
        return page;
    }

    /** 从完整 provider URL 中提取 slug，否则原样返回（去空白/小写） */
    private String stripProviderToken(String token)
    {
        if (token == null) { return null; }
        String t = token.trim();
        int marker = t.indexOf("/provider/");
        if (marker >= 0)
        {
            t = t.substring(marker + "/provider/".length());
            int slash = t.indexOf('/');
            if (slash >= 0) { t = t.substring(0, slash); }
            int query = t.indexOf('?');
            if (query >= 0) { t = t.substring(0, query); }
        }
        return t.trim();
    }

    private String normalizeCountry(String country)
    {
        if (StringUtils.isEmpty(country)) { return props.getJustwatch().getCountry(); }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private JsonNode postGraphql(String query, ObjectNode variables)
    {
        try
        {
            ObjectNode body = mapper.createObjectNode();
            body.put("query", query);
            body.set("variables", variables);
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.getJustwatch().getGraphqlUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", UA)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = mapper.readTree(resp.body());
            JsonNode errors = root.path("errors");
            if (errors.isArray() && errors.size() > 0)
            {
                String msg = errors.get(0).path("message").asText("JustWatch GraphQL 错误");
                throw new RuntimeException(msg);
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull())
            {
                throw new RuntimeException("JustWatch 无数据返回 status=" + resp.statusCode());
            }
            return data;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("JustWatch 请求失败: " + e.getMessage(), e);
        }
    }

    /** 平台目录缓存条目 */
    private static class CacheEntry
    {
        final List<JwPackage> packages;
        final long createdAt = System.currentTimeMillis();

        CacheEntry(List<JwPackage> packages) { this.packages = packages; }

        boolean expired(long ttlMs) { return System.currentTimeMillis() - createdAt > ttlMs; }
    }
}
