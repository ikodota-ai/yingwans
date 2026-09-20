package com.ruoyi.kemovie.client;

import java.util.ArrayList;
import java.util.List;
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
 * MyDramaList 抓取客户端。
 * MDL 使用 Cloudflare 托管 JS 挑战，普通 HTTP 会 403，
 * 因此调用本地已安装的 Chrome 无头模式 --dump-dom 渲染后再用 Jsoup 解析。
 *
 * @author kemovie
 */
@Component
public class MdlScraper
{
    private static final Logger log = LoggerFactory.getLogger(MdlScraper.class);

    private final KemovieProperties props;
    private final HeadlessChrome chrome;

    public MdlScraper(KemovieProperties props, HeadlessChrome chrome)
    {
        this.props = props;
        this.chrome = chrome;
    }

    /** 抓取的单条影片信息 */
    public static class MdlItem
    {
        public String mdlId;        // mdl-11183
        public String slug;         // 11183-the-handmaiden
        public String title;        // The Handmaiden
        public String posterUrl;    // https://i.mydramalist.com/xxx.jpg
        public String typeYear;     // Korean Movie - 2016
        public Integer year;
        public String type;

        @Override public String toString() { return mdlId + " | " + title + " | " + typeYear; }
    }

    /** 抓取指定 list 的某一页，返回影片列表 */
    public List<MdlItem> fetchListPage(String listId, int page)
    {
        String url = props.getMdl().getBaseUrl() + "/list/" + listId + (page > 1 ? "?page=" + page : "");
        String html = renderWithChrome(url);
        if (StringUtils.isEmpty(html))
        {
            log.warn("MDL抓取空内容 url={}", url);
            return new ArrayList<>();
        }
        if (chrome.isCloudflareChallenge(html))
        {
            log.warn("MDL仍处于Cloudflare挑战页 url={}", url);
            return new ArrayList<>();
        }
        return parseList(html);
    }

    /** 解析列表 HTML */
    public List<MdlItem> parseList(String html)
    {
        List<MdlItem> items = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Elements lis = doc.select("li.list-group-item[id^=mdl-]");
        for (Element li : lis)
        {
            MdlItem it = new MdlItem();
            it.mdlId = li.id(); // mdl-11183
            Element titleLink = li.selectFirst("h2.title a[href^=/], h2 a[title]");
            if (titleLink == null) { titleLink = li.selectFirst("a.film-cover"); }
            if (titleLink != null)
            {
                String href = titleLink.attr("href");
                it.slug = href.startsWith("/") ? href.substring(1) : href;
                String t = titleLink.hasAttr("title") ? titleLink.attr("title") : titleLink.text();
                it.title = StringUtils.trimToNull(t);
            }
            Element img = li.selectFirst("img");
            if (img != null)
            {
                String src = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
                it.posterUrl = StringUtils.trimToNull(src);
                if (it.title == null && img.hasAttr("alt")) { it.title = img.attr("alt"); }
            }
            Element meta = li.selectFirst("p.text-muted");
            if (meta != null)
            {
                it.typeYear = meta.text();
                parseTypeYear(it);
            }
            if (it.mdlId != null && it.title != null)
            {
                items.add(it);
            }
        }
        return items;
    }

    private void parseTypeYear(MdlItem it)
    {
        if (StringUtils.isEmpty(it.typeYear)) { return; }
        // e.g. "Korean Movie - 2016"
        String[] parts = it.typeYear.split("-");
        if (parts.length >= 1) { it.type = parts[0].trim(); }
        if (parts.length >= 2)
        {
            String y = parts[parts.length - 1].replaceAll("[^0-9]", "");
            if (y.length() >= 4)
            {
                try { it.year = Integer.parseInt(y.substring(0, 4)); } catch (Exception ignore) {}
            }
        }
    }

    /** 用无头 Chrome 渲染页面并输出 DOM */
    private String renderWithChrome(String url)
    {
        return chrome.render(url, props.getMdl().getVirtualTimeBudget(), props.getMdl().getRenderTimeout());
    }
}
