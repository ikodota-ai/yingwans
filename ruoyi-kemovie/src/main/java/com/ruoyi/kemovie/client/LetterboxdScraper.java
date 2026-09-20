package com.ruoyi.kemovie.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * Letterboxd 片单抓取客户端。
 * Letterboxd 无公开 API 且托管于 Cloudflare，纯 HTTP 会 403，
 * 因此与 MDL 一样复用无头 Chrome 渲染后解析：
 *  - 列表页：解析 LazyPoster 组件（片名+年份+slug，约 100/页，/page/N/ 翻页）
 *  - 影片页：提取 themoviedb.org/(movie|tv)/(id) 得到精确 TMDB id（带进程内缓存）
 *
 * @author kemovie
 */
@Component
public class LetterboxdScraper
{
    private static final Logger log = LoggerFactory.getLogger(LetterboxdScraper.class);
    private static final Pattern TMDB_LINK = Pattern.compile("themoviedb\\.org/(movie|tv)/(\\d+)");
    private static final Pattern IMDB_LINK = Pattern.compile("imdb\\.com/title/(tt\\d+)");
    private static final Pattern TRAILING_YEAR = Pattern.compile("^(.*?)\\s*\\((\\d{4})\\)\\s*$");

    private final KemovieProperties props;
    private final HeadlessChrome chrome;
    private final CdpChrome cdp;
    /** slug -> 影片页解析结果（本次运行内缓存，避免重复渲染） */
    private final Map<String, LbxFilm> filmCache = new ConcurrentHashMap<>();

    public LetterboxdScraper(KemovieProperties props, HeadlessChrome chrome, CdpChrome cdp)
    {
        this.props = props;
        this.chrome = chrome;
        this.cdp = cdp;
    }

    /** 列表条目 */
    public static class LbxItem
    {
        public String slug;    // city-of-god
        public String title;   // City of God
        public Integer year;   // 2002

        @Override public String toString() { return slug + " | " + title + " | " + year; }
    }

    /** 影片页解析结果 */
    public static class LbxFilm
    {
        public Long tmdbId;
        public String mediaType;  // movie / tv
        public String imdbId;
    }

    /** 单页抓取结果 */
    public static class LbxPage
    {
        public List<LbxItem> items = new ArrayList<>();
        public int totalPages = 1;
    }

    /** 抓取片单某一页（page 从 1 开始） */
    public LbxPage fetchListPage(String listPath, int page)
    {
        String url = props.getLetterboxd().getBaseUrl() + "/" + listPath + "/"
                + (page > 1 ? "page/" + page + "/" : "");
        // Cloudflare 通过时间波动较大，首次解析为空时重试一次
        for (int attempt = 0; attempt < 2; attempt++)
        {
            String html = render(url, "[data-component-class=LazyPoster]");
            LbxPage result = new LbxPage();
            if (StringUtils.isEmpty(html) || chrome.isCloudflareChallenge(html))
            {
                log.warn("Letterboxd 抓取失败/挑战页 url={} attempt={}", url, attempt);
                continue;
            }
            Document doc = Jsoup.parse(html);
            result.items = parseItems(doc);
            result.totalPages = parseTotalPages(doc);
            if (!result.items.isEmpty()) { return result; }
            String title = doc.title();
            String errCode = "";
            java.util.regex.Matcher em = java.util.regex.Pattern
                    .compile("ERR_[A-Z_]+").matcher(html);
            if (em.find()) { errCode = em.group(); }
            log.warn("Letterboxd 列表页未解析到条目 url={} attempt={} len={} title={} err={}",
                    url, attempt, html.length(), title, errCode);
            try
            {
                java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/lbx-fail-" + attempt + ".html"),
                        html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            catch (Exception ignore) { }
        }
        LbxPage result = new LbxPage();
        return result;
    }

    /** 解析列表页条目：优先 LazyPoster 组件，兼容旧的 film-poster 结构 */
    private List<LbxItem> parseItems(Document doc)
    {
        // 条目位于 ul.poster-list 下的 li.posteritem > div.LazyPoster
        List<LbxItem> scoped = parseLazyPosters(doc.select("ul.poster-list div.react-component[data-component-class=LazyPoster]"));
        if (!scoped.isEmpty()) { return scoped; }
        List<LbxItem> items = new ArrayList<>();
        Elements posters = doc.select("div.react-component[data-component-class=LazyPoster]");
        items = parseLazyPosters(posters);
        if (!items.isEmpty()) { return items; }

        // 兜底：旧版静态结构 div.film-poster[data-target-link]
        for (Element poster : doc.select("div.film-poster[data-target-link^=/film/]"))
        {
            String slug = extractSlug(poster.attr("data-target-link"));
            if (slug == null) { continue; }
            LbxItem it = new LbxItem();
            it.slug = slug;
            Element img = poster.selectFirst("img[alt]");
            if (img != null) { applyName(it, img.attr("alt")); }
            if (it.title != null) { items.add(it); }
        }
        return items;
    }

    private List<LbxItem> parseLazyPosters(Elements posters)
    {
        List<LbxItem> items = new ArrayList<>();
        for (Element poster : posters)
        {
            String slug = StringUtils.trimToNull(poster.attr("data-item-slug"));
            if (slug == null)
            {
                String link = poster.attr("data-item-link");
                slug = extractSlug(link);
            }
            if (slug == null) { continue; }
            LbxItem it = new LbxItem();
            it.slug = slug;
            applyName(it, poster.attr("data-item-name"));
            if (it.title == null)
            {
                Element img = poster.selectFirst("img[alt]");
                if (img != null) { applyName(it, img.attr("alt")); }
            }
            if (it.title != null) { items.add(it); }
        }
        return items;
    }

    /** "City of God (2002)" -> title + year */
    private void applyName(LbxItem it, String name)
    {
        name = StringUtils.trimToNull(name);
        if (name == null) { return; }
        Matcher m = TRAILING_YEAR.matcher(name);
        if (m.matches())
        {
            it.title = StringUtils.trimToNull(m.group(1));
            try { it.year = Integer.parseInt(m.group(2)); } catch (NumberFormatException ignore) { }
        }
        else
        {
            it.title = name;
        }
    }

    private String extractSlug(String link)
    {
        link = StringUtils.trimToNull(link);
        if (link == null) { return null; }
        int i = link.indexOf("/film/");
        if (i < 0) { return null; }
        String rest = link.substring(i + "/film/".length());
        int slash = rest.indexOf('/');
        String slug = slash >= 0 ? rest.substring(0, slash) : rest;
        return StringUtils.trimToNull(slug);
    }

    private int parseTotalPages(Document doc)
    {
        int max = 1;
        for (Element a : doc.select("div.pagination a[href]"))
        {
            Matcher m = Pattern.compile("/page/(\\d+)/").matcher(a.attr("href"));
            if (m.find())
            {
                try { max = Math.max(max, Integer.parseInt(m.group(1))); } catch (NumberFormatException ignore) { }
            }
        }
        return max;
    }

    /** 渲染影片页并提取精确 TMDB id（带缓存），失败返回 null */
    public LbxFilm resolveFilm(String slug)
    {
        LbxFilm cached = filmCache.get(slug);
        if (cached != null) { return cached; }
        String url = props.getLetterboxd().getBaseUrl() + "/film/" + slug + "/";
        String html = render(url, "a[href*='themoviedb.org']");
        if (StringUtils.isEmpty(html) || chrome.isCloudflareChallenge(html))
        {
            log.warn("Letterboxd 影片页抓取失败 slug={}", slug);
            return null;
        }
        LbxFilm film = new LbxFilm();
        Matcher tm = TMDB_LINK.matcher(html);
        if (tm.find())
        {
            film.mediaType = "tv".equals(tm.group(1)) ? "tv" : "movie";
            try { film.tmdbId = Long.parseLong(tm.group(2)); } catch (NumberFormatException ignore) { }
        }
        Matcher im = IMDB_LINK.matcher(html);
        if (im.find()) { film.imdbId = im.group(1); }
        if (film.tmdbId == null)
        {
            log.warn("Letterboxd 影片页未找到 TMDB 链接 slug={}", slug);
            return null;
        }
        filmCache.put(slug, film);
        if (props.getLetterboxd().getRequestIntervalMs() > 0)
        {
            try { Thread.sleep(props.getLetterboxd().getRequestIntervalMs()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return film;
    }

    /** 优先 CDP 真实等待渲染；失败时回退 --dump-dom */
    private String render(String url, String readySelector)
    {
        try
        {
            String html = cdp.renderAndWait(url, readySelector, props.getLetterboxd().getRenderTimeout());
            if (StringUtils.isNotEmpty(html) && !chrome.isCloudflareChallenge(html))
            {
                return html;
            }
        }
        catch (Exception e)
        {
            log.warn("CDP 渲染失败，回退 dump-dom url={} err={}", url, e.getMessage());
        }
        return chrome.render(url, props.getLetterboxd().getVirtualTimeBudget(),
                props.getLetterboxd().getRenderTimeout());
    }
}
