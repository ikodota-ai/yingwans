package com.ruoyi.kemovie.client;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaCast;
import com.ruoyi.kemovie.domain.KmMediaRelease;
import com.ruoyi.kemovie.domain.KmPerson;

/**
 * TMDB API 客户端（支持出网代理）
 *
 * @author kemovie
 */
@Component
public class TmdbClient
{
    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final KemovieProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile HttpClient httpClient;

    public TmdbClient(KemovieProperties props)
    {
        this.props = props;
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
                    if (props.getProxy().isEnabled())
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

    /** 通用 GET，返回 JSON 树 */
    public JsonNode get(String path, String query)
    {
        return get(path, query, null);
    }

    /** 通用 GET（可覆盖接口语言），返回 JSON 树 */
    public JsonNode get(String path, String query, String language)
    {
        try
        {
            String sep = path.contains("?") ? "&" : "?";
            String lang = StringUtils.isNotEmpty(language) ? language : props.getTmdb().getLanguage();
            String url = props.getTmdb().getBaseUrl() + path + sep
                    + "api_key=" + props.getTmdb().getApiKey()
                    + "&language=" + lang
                    + (StringUtils.isNotEmpty(query) ? "&" + query : "");
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = client().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300)
            {
                return mapper.readTree(resp.body());
            }
            log.warn("TMDB请求失败 {} status={}", path, resp.statusCode());
        }
        catch (Exception e)
        {
            log.error("TMDB请求异常 path={}", path, e);
        }
        return null;
    }

    /** 搜索影视 */
    public List<KmMedia> search(String keyword, String type)
    {
        List<KmMedia> list = new ArrayList<>();
        String q = "query=" + enc(keyword) + "&include_adult=false&region=" + props.getTmdb().getRegion();
        String path = "tv".equals(type) ? "/search/tv" : "movie".equals(type) ? "/search/movie" : "/search/multi";
        JsonNode root = get(path, q, null);
        return collectSearch(list, root, type);
    }

    /** 搜索影视（指定接口语言；英文片名在 zh 索引中可能查不到，需补 en-US） */
    public List<KmMedia> search(String keyword, String type, String language)
    {
        List<KmMedia> list = new ArrayList<>();
        String q = "query=" + enc(keyword) + "&include_adult=false&region=" + props.getTmdb().getRegion();
        String path = "tv".equals(type) ? "/search/tv" : "movie".equals(type) ? "/search/movie" : "/search/multi";
        JsonNode root = get(path, q, language);
        return collectSearch(list, root, type);
    }

    private List<KmMedia> collectSearch(List<KmMedia> list, JsonNode root, String type)
    {
        if (root != null && root.has("results"))
        {
            for (JsonNode n : root.get("results"))
            {
                KmMedia m = fromSearchNode(n, type);
                if (m != null) { list.add(m); }
            }
        }
        return list;
    }

    /** IMDB 查找结果（tmdbId + mediaType） */
    public static class FindResult
    {
        public Long tmdbId;
        public String mediaType;
    }

    /** 按 IMDB id 精确查找（/find/{tt}，旧站迁移用） */
    public FindResult findByImdb(String imdbId)
    {
        if (StringUtils.isEmpty(imdbId)) { return null; }
        JsonNode root = get("/find/" + imdbId.trim(), "external_source=imdb_id");
        if (root == null) { return null; }
        FindResult r = pickFind(root, "movie_results", "movie");
        return r != null ? r : pickFind(root, "tv_results", "tv");
    }

    private FindResult pickFind(JsonNode root, String key, String type)
    {
        JsonNode arr = root.path(key);
        if (arr.isArray() && arr.size() > 0)
        {
            long id = arr.get(0).path("id").asLong(0);
            if (id > 0)
            {
                FindResult r = new FindResult();
                r.tmdbId = id;
                r.mediaType = type;
                return r;
            }
        }
        return null;
    }

    /** 拉取影视详情（含 external_ids） */
    public KmMedia fetchDetail(long tmdbId, String type)
    {
        String path = "/" + ("tv".equals(type) ? "tv" : "movie") + "/" + tmdbId;
        JsonNode n = get(path, "append_to_response=external_ids");
        if (n == null || !n.has("id")) { return null; }
        KmMedia m = new KmMedia();
        m.setTmdbId(n.path("id").asLong());
        m.setMediaType(type);
        boolean tv = "tv".equals(type);
        m.setTitle(text(n, tv ? "name" : "title"));
        m.setOriginalTitle(text(n, tv ? "original_name" : "original_title"));
        m.setOverview(text(n, "overview"));
        m.setPosterPath(text(n, "poster_path"));
        m.setBackdropPath(text(n, "backdrop_path"));
        String rd = text(n, tv ? "first_air_date" : "release_date");
        m.setReleaseDate(parseDate(rd));
        m.setVoteAverage(decimal(n, "vote_average"));
        m.setVoteCount(n.path("vote_count").asInt(0));
        m.setPopularity(decimal(n, "popularity"));
        m.setStatus(text(n, "status"));
        if (tv)
        {
            JsonNode eps = n.path("episode_run_time");
            if (eps.isArray() && eps.size() > 0) { m.setRuntime(eps.get(0).asInt()); }
        }
        else
        {
            m.setRuntime(n.path("runtime").asInt(0));
        }
        // genres
        List<String> genres = new ArrayList<>();
        for (JsonNode g : n.path("genres")) { genres.add(g.path("name").asText()); }
        m.setGenres(String.join(",", genres));
        // region
        List<String> regions = new ArrayList<>();
        for (JsonNode c : n.path("production_countries")) { regions.add(c.path("iso_3166_1").asText()); }
        if (regions.isEmpty())
        {
            for (JsonNode c : n.path("origin_country")) { regions.add(c.asText()); }
        }
        m.setRegion(String.join(",", regions));
        // imdb
        JsonNode ext = n.path("external_ids");
        m.setImdbId(text(ext.isMissingNode() ? n : ext, "imdb_id"));
        m.setLastSyncedAt(new Date());
        return m;
    }

    /** 拉取 CN 区上线平台（watch/providers），映射为 release 列表 */
    public List<KmMediaRelease> fetchWatchProviders(long tmdbId, String type)
    {
        List<KmMediaRelease> list = new ArrayList<>();
        String path = "/" + ("tv".equals(type) ? "tv" : "movie") + "/" + tmdbId + "/watch/providers";
        JsonNode root = get(path, null);
        if (root == null) { return list; }
        JsonNode cn = root.path("results").path(props.getTmdb().getRegion());
        if (cn.isMissingNode()) { cn = root.path("results").path("US"); }
        if (cn.isMissingNode()) { return list; }
        addOffers(list, cn.path("flatrate"), "flatrate");
        addOffers(list, cn.path("rent"), "rent");
        addOffers(list, cn.path("buy"), "buy");
        addOffers(list, cn.path("free"), "free");
        return list;
    }

    private void addOffers(List<KmMediaRelease> list, JsonNode arr, String offerType)
    {
        if (arr == null || !arr.isArray()) { return; }
        for (JsonNode p : arr)
        {
            KmMediaRelease r = new KmMediaRelease();
            r.setPlatform(p.path("provider_name").asText());
            String logo = text(p, "logo_path");
            if (StringUtils.isNotEmpty(logo)) { r.setPlatformLogo(props.getTmdb().getImageBase() + "/w92" + logo); }
            r.setCountry(props.getTmdb().getRegion());
            r.setOfferType(offerType);
            r.setStatus("available");
            list.add(r);
        }
    }

    /**
     * 拉取该影视的“上线日期”（尽量含未来排期），用于订阅上线提醒。
     * <p>电影：读取 /movie/{id}/release_dates，按地区(CN→US→任意)优先取
     * 数字发行(type=4)/TV(type=6)，兜底影院(type=3/2/1)。
     * 剧集：读取 /tv/{id} 的 next_episode_to_air（未来集）优先，兜底
     * last_episode_to_air / first_air_date。返回可能是未来日期。</p>
     *
     * @return 上线日期；无法获取时返回 null
     */
    public Date fetchOnlineDate(long tmdbId, String type)
    {
        try
        {
            if ("tv".equals(type))
            {
                JsonNode n = get("/tv/" + tmdbId, null);
                if (n == null) { return null; }
                Date next = parseDate(text(n.path("next_episode_to_air"), "air_date"));
                if (next != null) { return next; }
                Date last = parseDate(text(n.path("last_episode_to_air"), "air_date"));
                if (last != null) { return last; }
                return parseDate(text(n, "first_air_date"));
            }
            else
            {
                JsonNode root = get("/movie/" + tmdbId + "/release_dates", null);
                if (root == null) { return null; }
                JsonNode results = root.path("results");
                String region = props.getTmdb().getRegion();
                Date byRegion = pickReleaseDate(results, region);
                if (byRegion != null) { return byRegion; }
                Date byUs = pickReleaseDate(results, "US");
                if (byUs != null) { return byUs; }
                // 任意地区兜底
                for (JsonNode c : results)
                {
                    Date d = pickReleaseDateFromEntry(c);
                    if (d != null) { return d; }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("获取上线日期异常 tmdbId={} type={} err={}", tmdbId, type, e.getMessage());
        }
        return null;
    }

    /** 从 release_dates.results 中找到指定地区，取其上线日期 */
    private Date pickReleaseDate(JsonNode results, String region)
    {
        if (results == null || !results.isArray()) { return null; }
        for (JsonNode c : results)
        {
            if (region.equalsIgnoreCase(text(c, "iso_3166_1")))
            {
                return pickReleaseDateFromEntry(c);
            }
        }
        return null;
    }

    /** 单地区条目内按发行类型优先级取日期：数字(4)/TV(6) > 影院(3/2/1) > 实体(5) */
    private Date pickReleaseDateFromEntry(JsonNode entry)
    {
        JsonNode arr = entry.path("release_dates");
        if (!arr.isArray()) { return null; }
        Date digital = null;
        Date theatrical = null;
        Date other = null;
        for (JsonNode rd : arr)
        {
            int t = rd.path("type").asInt(0);
            Date d = parseDate(cutTime(text(rd, "release_date")));
            if (d == null) { continue; }
            if (t == 4 || t == 6) { if (digital == null) { digital = d; } }
            else if (t == 3 || t == 2 || t == 1) { if (theatrical == null) { theatrical = d; } }
            else { if (other == null) { other = d; } }
        }
        if (digital != null) { return digital; }
        if (theatrical != null) { return theatrical; }
        return other;
    }

    /** TMDB release_dates 的日期形如 2024-03-07T00:00:00.000Z，截取日期部分 */
    private static String cutTime(String s)
    {
        if (s == null) { return null; }
        int t = s.indexOf('T');
        return t > 0 ? s.substring(0, t) : s;
    }

    private KmMedia fromSearchNode(JsonNode n, String type)
    {
        String mt = type;
        if (StringUtils.isEmpty(mt) || "multi".equals(mt))
        {
            mt = n.path("media_type").asText("movie");
            if (!"movie".equals(mt) && !"tv".equals(mt)) { return null; }
        }
        boolean tv = "tv".equals(mt);
        KmMedia m = new KmMedia();
        m.setTmdbId(n.path("id").asLong());
        m.setMediaType(mt);
        m.setTitle(text(n, tv ? "name" : "title"));
        m.setOriginalTitle(text(n, tv ? "original_name" : "original_title"));
        m.setOverview(text(n, "overview"));
        m.setPosterPath(text(n, "poster_path"));
        m.setBackdropPath(text(n, "backdrop_path"));
        m.setReleaseDate(parseDate(text(n, tv ? "first_air_date" : "release_date")));
        m.setVoteAverage(decimal(n, "vote_average"));
        m.setVoteCount(n.path("vote_count").asInt(0));
        m.setPopularity(decimal(n, "popularity"));
        return m;
    }

    /**
     * 拉取某影视的演职人员（credits）。返回的 KmMediaCast 里 personTmdbId 为 TMDB 人物ID，
     * 上层需据此 upsert km_person 后回填 personId。
     */
    public List<KmMediaCast> fetchCredits(long tmdbId, String type)
    {
        List<KmMediaCast> list = new ArrayList<>();
        boolean isTv = "tv".equals(type);
        // 剧集用 aggregate_credits（结构不同：character 在 roles[]，job 在 jobs[]），电影用 credits
        String path = "/" + (isTv ? "tv" : "movie") + "/" + tmdbId + "/" + (isTv ? "aggregate_credits" : "credits");
        JsonNode root = get(path, null);
        if (root == null) { return list; }
        // 演员
        for (JsonNode c : root.path("cast"))
        {
            KmMediaCast cast = new KmMediaCast();
            cast.setCreditType("cast");
            cast.setDepartment("Acting");
            cast.setPersonTmdbId(c.path("id").asLong());
            cast.setPersonName(text(c, "name"));
            cast.setProfilePath(text(c, "profile_path"));
            cast.setCharacter(isTv ? firstRoleCharacter(c) : text(c, "character"));
            cast.setCastOrder(c.path("order").asInt(999));
            list.add(cast);
        }
        // 剧组（仅保留导演/编剧/原著/制片等核心职务）
        for (JsonNode c : root.path("crew"))
        {
            if (isTv)
            {
                // aggregate_credits: 一个 crew 成员可能有多个 jobs
                for (JsonNode j : c.path("jobs"))
                {
                    String job = text(j, "job");
                    if (!isKeyCrewJob(job)) { continue; }
                    KmMediaCast crew = new KmMediaCast();
                    crew.setCreditType("crew");
                    crew.setDepartment(text(c, "department"));
                    crew.setJob(job);
                    crew.setPersonTmdbId(c.path("id").asLong());
                    crew.setPersonName(text(c, "name"));
                    crew.setProfilePath(text(c, "profile_path"));
                    crew.setCastOrder(998);
                    list.add(crew);
                }
            }
            else
            {
                String job = text(c, "job");
                if (!isKeyCrewJob(job)) { continue; }
                KmMediaCast crew = new KmMediaCast();
                crew.setCreditType("crew");
                crew.setDepartment(text(c, "department"));
                crew.setJob(job);
                crew.setPersonTmdbId(c.path("id").asLong());
                crew.setPersonName(text(c, "name"));
                crew.setProfilePath(text(c, "profile_path"));
                crew.setCastOrder(998);
                list.add(crew);
            }
        }
        return list;
    }

    /** aggregate_credits 中演员角色名取 roles[0].character */
    private String firstRoleCharacter(JsonNode castNode)
    {
        JsonNode roles = castNode.path("roles");
        if (roles.isArray() && roles.size() > 0)
        {
            return text(roles.get(0), "character");
        }
        return null;
    }

    private boolean isKeyCrewJob(String job)
    {
        if (StringUtils.isEmpty(job)) { return false; }
        switch (job)
        {
            case "Director":
            case "Writer":
            case "Screenplay":
            case "Novel":
            case "Story":
            case "Creator":
            case "Producer":
            case "Executive Producer":
            case "Original Music Composer":
                return true;
            default:
                return false;
        }
    }

    /** 拉取人物详情（含 external_ids） */
    public KmPerson fetchPerson(long tmdbId)
    {
        JsonNode n = get("/person/" + tmdbId, "append_to_response=external_ids");
        if (n == null || !n.has("id")) { return null; }
        return personFromNode(n);
    }

    private KmPerson personFromNode(JsonNode n)
    {
        KmPerson p = new KmPerson();
        p.setTmdbId(n.path("id").asLong());
        p.setName(text(n, "name"));
        p.setOriginalName(text(n, "original_name"));
        // also_known_as
        List<String> aka = new ArrayList<>();
        for (JsonNode a : n.path("also_known_as")) { aka.add(a.asText()); }
        if (!aka.isEmpty()) { p.setAlsoKnownAs(String.join(",", aka)); }
        p.setGender(n.path("gender").asInt(0));
        p.setBirthday(parseDate(text(n, "birthday")));
        p.setDeathday(parseDate(text(n, "deathday")));
        p.setPlaceOfBirth(text(n, "place_of_birth"));
        p.setKnownFor(text(n, "known_for_department"));
        p.setBiography(text(n, "biography"));
        p.setProfilePath(text(n, "profile_path"));
        p.setPopularity(decimal(n, "popularity"));
        p.setHomepage(text(n, "homepage"));
        JsonNode ext = n.path("external_ids");
        p.setImdbId(text(ext.isMissingNode() ? n : ext, "imdb_id"));
        p.setLastSyncedAt(new Date());
        return p;
    }

    /**
     * 拉取人物参演作品（combined_credits）。返回 KmMediaCast，里面用 media 相关字段承载
     * TMDB 影视信息（mediaType/mediaTitle/mediaReleaseDate/mediaPosterPath），personTmdbId=人物。
     * 上层据 media 的 tmdbId 关联到站内已收录影片。
     */
    public List<KmMediaCast> fetchPersonCredits(long personTmdbId)
    {
        List<KmMediaCast> list = new ArrayList<>();
        JsonNode root = get("/person/" + personTmdbId + "/combined_credits", null);
        if (root == null) { return list; }
        collectPersonCredits(list, root.path("cast"), "cast", personTmdbId);
        collectPersonCredits(list, root.path("crew"), "crew", personTmdbId);
        return list;
    }

    private void collectPersonCredits(List<KmMediaCast> list, JsonNode arr, String creditType, long personTmdbId)
    {
        if (arr == null || !arr.isArray()) { return; }
        for (JsonNode c : arr)
        {
            String mt = c.path("media_type").asText("");
            if (!"movie".equals(mt) && !"tv".equals(mt)) { continue; }
            boolean tv = "tv".equals(mt);
            KmMediaCast cast = new KmMediaCast();
            cast.setCreditType(creditType);
            cast.setPersonTmdbId(personTmdbId);
            cast.setPersonId(c.path("id").asLong());  // 借用 personId 字段临时承载“影视 TMDB id”
            cast.setMediaType(mt);
            cast.setMediaTitle(text(c, tv ? "name" : "title"));
            cast.setMediaPosterPath(text(c, "poster_path"));
            cast.setMediaReleaseDate(parseDate(text(c, tv ? "first_air_date" : "release_date")));
            if ("cast".equals(creditType)) { cast.setCharacter(text(c, "character")); }
            else { cast.setJob(text(c, "job")); cast.setDepartment(text(c, "department")); }
            list.add(cast);
        }
    }

    public String imageUrl(String size, String path)
    {
        if (StringUtils.isEmpty(path)) { return null; }
        return props.getTmdb().getImageBase() + "/" + size + path;
    }

    /**
     * 下载任意图片 URL（走同一代理），返回字节；失败返回 null。
     * 用于将 TMDB/MDL 海报、头像本地化。
     */
    public byte[] downloadImage(String url)
    {
        if (StringUtils.isEmpty(url)) { return null; }
        try
        {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (kemovie image fetcher)")
                    .GET().build();
            HttpResponse<byte[]> resp = client().send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300)
            {
                return resp.body();
            }
            log.warn("图片下载失败 status={} url={}", resp.statusCode(), url);
        }
        catch (Exception e)
        {
            log.warn("图片下载异常 url={} err={}", url, e.getMessage());
        }
        return null;
    }

    private String enc(String s)
    {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode n, String field)
    {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static BigDecimal decimal(JsonNode n, String field)
    {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? BigDecimal.ZERO : new BigDecimal(v.asText("0"));
    }

    private static Date parseDate(String s)
    {
        if (StringUtils.isEmpty(s)) { return null; }
        try { return new SimpleDateFormat("yyyy-MM-dd").parse(s); }
        catch (Exception e) { return null; }
    }
}
