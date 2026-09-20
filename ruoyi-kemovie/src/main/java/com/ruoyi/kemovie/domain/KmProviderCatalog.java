package com.ruoyi.kemovie.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 播放平台品牌目录（JustWatch 式）。
 *
 * 不再使用固定白名单，而是对 TMDB 返回的任意平台名做“归一化分组”：
 * 剥离发行渠道后缀（含广告 / Amazon 频道 / Apple TV 频道 / Roku 频道 等），
 * 再映射到面向用户的品牌 key + 展示名。库中出现的所有平台都会成为可筛选项，
 * 同源变体自动合并。前端按品牌 key 传参，后端解析回原始平台名集合做精确 IN 查询。
 *
 * @author kemovie
 */
public final class KmProviderCatalog
{
    private KmProviderCatalog() {}

    /** 需要剥离的发行渠道后缀（小写匹配） */
    private static final String[] SUFFIXES = {
        " standard with ads", " with ads",
        " amazon channel", " apple tv channel", " roku premium channel",
        " premium channel", " channel"
    };

    /** 归一化基名 -> "key|展示名"（展示名优先用中文/规范名） */
    private static final Map<String, String> KNOWN = new LinkedHashMap<>();
    /** 品牌展示优先级（越靠前越先展示），未列出的按资源数量排序 */
    private static final List<String> PRIORITY = new ArrayList<>();
    static
    {
        put("netflix", "netflix", "Netflix");
        put("amazon prime video", "prime", "Prime Video");
        put("amazon video", "amazonstore", "Amazon 租买");
        put("apple tv", "appletv", "Apple TV");
        put("apple tv store", "appletv", "Apple TV");
        put("disney plus", "disney", "Disney+");
        put("disney+", "disney", "Disney+");
        put("hbo max", "hbomax", "HBO Max");
        put("max", "hbomax", "HBO Max");
        put("hulu", "hulu", "Hulu");
        put("iqiyi", "iqiyi", "爱奇艺");
        put("rakuten viki", "viki", "Rakuten Viki");
        put("viki", "viki", "Rakuten Viki");
        put("crunchyroll", "crunchyroll", "Crunchyroll");
        put("wetv", "wetv", "WeTV");
        put("kocowa", "kocowa", "Kocowa");
        put("viu", "viu", "Viu");
        put("paramount plus", "paramount", "Paramount+");
        put("paramount+", "paramount", "Paramount+");
        put("peacock", "peacock", "Peacock");
        put("peacock premium", "peacock", "Peacock");
        put("peacock premium plus", "peacock", "Peacock");
        put("youtube", "youtube", "YouTube");
        put("youtube premium", "youtube", "YouTube");
        put("youtube tv", "youtubetv", "YouTube TV");
        put("mgm plus", "mgm", "MGM+");
        put("mgm+", "mgm", "MGM+");
        put("starz", "starz", "Starz");
        put("amc+", "amc", "AMC+");
        put("amc plus", "amc", "AMC+");
        put("mubi", "mubi", "MUBI");
        put("discovery", "discovery", "Discovery+");
        put("discovery +", "discovery", "Discovery+");
        put("discovery+", "discovery", "Discovery+");
        put("acorn tv", "acorn", "Acorn TV");
        put("acorntv", "acorn", "Acorn TV");
        put("acorn tv apple tv", "acorn", "Acorn TV");
        put("criterion channel", "criterion", "Criterion Channel");
        put("fandango at home", "fandango", "Fandango At Home");
        put("google play movies", "googleplay", "Google Play");
        put("hoopla", "hoopla", "Hoopla");
        put("kanopy", "kanopy", "Kanopy");
        put("plex", "plex", "Plex");
        put("pure flix", "pureflix", "Pure Flix");
        put("great american pure flix", "pureflix", "Pure Flix");
        put("the cw", "cw", "The CW");
        put("fubotv", "fubo", "fuboTV");
        put("philo", "philo", "Philo");
        put("spectrum on demand", "spectrum", "Spectrum On Demand");
        put("asiancrush", "asiancrush", "AsianCrush");
        put("ondemandkorea", "ondemandkorea", "OnDemandKorea");
        put("film movement plus", "filmmovement", "Film Movement Plus");
        put("fawesome", "fawesome", "Fawesome");
        put("flixfling", "flixfling", "FlixFling");
        put("ovid", "ovid", "OVID");
        put("hallmark tv", "hallmark", "Hallmark");
        put("hallmark+", "hallmark", "Hallmark+");
        put("shout! factory", "shoutfactory", "Shout! Factory");
        put("indieflix shorts", "indieflix", "IndieFlix");
        put("cineverse", "cineverse", "Cineverse");
        put("fandor", "fandor", "Fandor");
        put("moviesphere+", "moviesphere", "MovieSphere+");
        put("pinoy box office", "pinoy", "Pinoy Box Office");
        put("darkroom", "darkroom", "Darkroom");
        put("justwatch tv", "justwatchtv", "JustWatch TV");
        put("yow.tv", "yowtv", "YOW.tv");

        // 展示优先级：主流平台靠前
        PRIORITY.add("netflix"); PRIORITY.add("prime"); PRIORITY.add("appletv");
        PRIORITY.add("disney"); PRIORITY.add("hbomax"); PRIORITY.add("hulu");
        PRIORITY.add("iqiyi"); PRIORITY.add("viki"); PRIORITY.add("crunchyroll");
        PRIORITY.add("wetv"); PRIORITY.add("kocowa"); PRIORITY.add("viu");
        PRIORITY.add("paramount"); PRIORITY.add("peacock"); PRIORITY.add("mgm");
        PRIORITY.add("starz"); PRIORITY.add("amc"); PRIORITY.add("mubi");
        PRIORITY.add("youtube"); PRIORITY.add("amazonstore"); PRIORITY.add("googleplay");
        PRIORITY.add("fandango");
    }

    private static void put(String base, String key, String label)
    {
        KNOWN.put(base, key + "|" + label);
    }

    /** 展示优先级序号；未列出返回较大值 */
    public static int priorityOf(String key)
    {
        int idx = PRIORITY.indexOf(key);
        return idx < 0 ? 999 : idx;
    }

    /** 观看方式定义 */
    public static List<Map<String, String>> offerTypes()
    {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(offer("flatrate", "订阅会员"));
        list.add(offer("free", "免费"));
        list.add(offer("rent", "租赁"));
        list.add(offer("buy", "购买"));
        return list;
    }

    private static Map<String, String> offer(String v, String label)
    {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("value", v);
        m.put("label", label);
        return m;
    }

    /**
     * 由原始平台名派生品牌 {key, label}。占位/空返回 null。
     */
    public static String[] derive(String rawPlatform)
    {
        if (rawPlatform == null) { return null; }
        String raw = rawPlatform.trim();
        if (raw.isEmpty() || "待定".equals(raw)) { return null; }
        String base = raw.toLowerCase();
        // 剥离发行渠道后缀（可能叠加，循环剥离）
        boolean changed = true;
        while (changed)
        {
            changed = false;
            for (String suf : SUFFIXES)
            {
                if (base.endsWith(suf))
                {
                    base = base.substring(0, base.length() - suf.length()).trim();
                    changed = true;
                }
            }
        }
        String hit = KNOWN.get(base);
        if (hit != null)
        {
            int p = hit.indexOf('|');
            return new String[] { hit.substring(0, p), hit.substring(p + 1) };
        }
        // 未知平台：用清洗后的基名生成 key + 展示名（首字母大写沿用原名）
        String key = slug(base);
        String label = cleanLabel(raw);
        return new String[] { key, label };
    }

    /** 展示名：去掉发行渠道后缀，保留原始大小写 */
    private static String cleanLabel(String raw)
    {
        String label = raw.trim();
        String lower = label.toLowerCase();
        boolean changed = true;
        while (changed)
        {
            changed = false;
            for (String suf : SUFFIXES)
            {
                if (lower.endsWith(suf))
                {
                    label = label.substring(0, label.length() - suf.length()).trim();
                    lower = label.toLowerCase();
                    changed = true;
                }
            }
        }
        return label;
    }

    private static String slug(String base)
    {
        StringBuilder sb = new StringBuilder();
        for (char c : base.toCharArray())
        {
            if (Character.isLetterOrDigit(c)) { sb.append(Character.toLowerCase(c)); }
        }
        String s = sb.toString();
        return s.isEmpty() ? "other" : s;
    }
}
