package com.ruoyi.kemovie.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.kemovie.auth.MemberSecurityUtils;
import com.ruoyi.kemovie.domain.KmCollection;
import com.ruoyi.kemovie.domain.KmHotRank;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmOpsBlock;
import com.ruoyi.kemovie.service.IKmCollectionService;
import com.ruoyi.kemovie.service.IKmHotRankService;
import com.ruoyi.kemovie.service.IKmMediaService;
import com.ruoyi.kemovie.service.IKmOpsBlockService;
import com.ruoyi.kemovie.service.IKmPersonService;
import com.ruoyi.kemovie.domain.KmPerson;

/**
 * 前台公开接口（发现/详情/片单/运营位），无需登录。
 *
 * @author kemovie
 */
@Anonymous
@RestController
@RequestMapping("/portal")
public class KmPublicController extends BaseController
{
    @Autowired private IKmMediaService mediaService;
    @Autowired private IKmCollectionService collectionService;
    @Autowired private IKmHotRankService hotRankService;
    @Autowired private IKmOpsBlockService opsBlockService;
    @Autowired private IKmPersonService personService;
    @Autowired private com.ruoyi.kemovie.config.KemovieProperties kemovieProperties;

    /** 最近上新（JustWatch 各区域），可按区域过滤，默认近14天24条 */
    @GetMapping("/newReleases")
    public AjaxResult newReleases(@RequestParam(required = false) String country,
            @RequestParam(defaultValue = "14") int days,
            @RequestParam(defaultValue = "24") int limit)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("list", mediaService.selectNewReleases(country, Math.min(days, 60), Math.min(limit, 60)));
        ajax.put("countries", kemovieProperties.getJustwatch().getNewCountries());
        return ajax;
    }

    /** 首页聚合：热门榜 + 近期上线 + 精选片单 + 运营位 */
    @GetMapping("/home")
    public AjaxResult home()
    {
        AjaxResult ajax = AjaxResult.success();
        List<KmHotRank> hot = hotRankService.latest("week", 10);
        for (KmHotRank r : hot) { stripDocs(r.getMedia()); }
        ajax.put("hotRank", hot);
        List<KmMedia> recent = mediaService.selectRecentReleases(12);
        stripDocs(recent);
        ajax.put("recent", recent);
        KmCollection cq = new KmCollection();
        cq.setIsPublic("0");
        List<KmCollection> collections = collectionService.selectKmCollectionList(cq);
        ajax.put("collections", collections.size() > 8 ? collections.subList(0, 8) : collections);
        ajax.put("homeSide", opsBlockService.selectEnabledByKey("home_side"));
        return ajax;
    }

    /** 发现列表（可筛选、分页） */
    @GetMapping("/media/list")
    public TableDataInfo mediaList(KmMedia query)
    {
        applyPlatformBrand(query);
        startPage();
        List<KmMedia> list = mediaService.selectPublicMediaList(query);
        stripDocs(list);
        return getDataTable(list);
    }

    /** 最近收录：按本站收录时间倒序（含旧站导入片） */
    @GetMapping("/recentCollected")
    public AjaxResult recentCollected(@RequestParam(defaultValue = "24") int limit)
    {
        List<KmMedia> list = mediaService.selectRecentCollected(Math.min(limit, 60));
        stripDocs(list);
        return AjaxResult.success(list);
    }

    /** 把前端传入的平台品牌 key（如 netflix）解析为原始平台名集合，供 IN 查询 */
    private void applyPlatformBrand(KmMedia query)
    {
        if (query == null || query.getPlatform() == null || query.getPlatform().isEmpty()) { return; }
        // platform 支持逗号分隔的多个品牌 key（如 netflix,prime）。
        // 品牌为动态派生：把库中所有原始平台名派生成品牌 key，选出匹配请求 key 的原始名做精确 IN 查询。
        java.util.Set<String> wanted = new java.util.HashSet<>();
        for (String key : query.getPlatform().split(","))
        {
            String k = key.trim();
            if (!k.isEmpty()) { wanted.add(k.toLowerCase()); }
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String raw : mediaService.selectDistinctPlatforms())
        {
            String[] d = com.ruoyi.kemovie.domain.KmProviderCatalog.derive(raw);
            if (d != null && wanted.contains(d[0].toLowerCase())) { names.add(raw); }
        }
        if (!names.isEmpty())
        {
            query.setPlatformNames(names);
            query.setPlatform(null);
        }
    }

    /** 组装品牌供应商（含代表 logo，取该品牌下命中的任一平台 logo） */
    private java.util.List<java.util.Map<String, Object>> buildProviders()
    {
        java.util.List<java.util.Map<String, Object>> logos = mediaService.selectPlatformLogos();
        // 动态归并：库中每个平台名派生成品牌，同源变体合并，统计资源数并取代表 logo
        java.util.Map<String, java.util.Map<String, Object>> grouped = new java.util.LinkedHashMap<>();
        for (java.util.Map<String, Object> row : logos)
        {
            String platform = row.get("platform") == null ? "" : row.get("platform").toString();
            String[] d = com.ruoyi.kemovie.domain.KmProviderCatalog.derive(platform);
            if (d == null) { continue; }
            String key = d[0];
            String label = d[1];
            java.util.Map<String, Object> g = grouped.get(key);
            if (g == null)
            {
                g = new java.util.LinkedHashMap<>();
                g.put("key", key);
                g.put("label", label);
                g.put("logo", row.get("logo"));
                g.put("count", 0);
                g.put("priority", com.ruoyi.kemovie.domain.KmProviderCatalog.priorityOf(key));
                grouped.put(key, g);
            }
            g.put("count", ((Integer) g.get("count")) + 1);
            if (g.get("logo") == null && row.get("logo") != null) { g.put("logo", row.get("logo")); }
        }
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>(grouped.values());
        // 排序：优先级升序，其次资源数降序，最后名称
        result.sort((a, b) -> {
            int pa = (Integer) a.get("priority"), pb = (Integer) b.get("priority");
            if (pa != pb) { return Integer.compare(pa, pb); }
            int ca = (Integer) a.get("count"), cb = (Integer) b.get("count");
            if (ca != cb) { return Integer.compare(cb, ca); }
            return String.valueOf(a.get("label")).compareTo(String.valueOf(b.get("label")));
        });
        return result;
    }

    /** 组装国家/地区筛选项：拆分多国 region，按出现次数聚合，输出 {value,label,count} */
    private java.util.List<java.util.Map<String, Object>> buildCountries()
    {
        java.util.List<String> regions = mediaService.selectDistinctRegions();
        java.util.Map<String, Integer> counter = new java.util.LinkedHashMap<>();
        for (String region : regions)
        {
            if (region == null) { continue; }
            for (String raw : region.split("[,/、，]"))
            {
                // 统一转字母码聚合（中文名/别名同码合并），显示时补全中文
                String code = com.ruoyi.kemovie.util.KmRegionCatalog.toCode(raw);
                if (code == null) { continue; }
                counter.merge(code, 1, Integer::sum);
            }
        }
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : counter.entrySet())
        {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("value", e.getKey());
            m.put("label", com.ruoyi.kemovie.util.KmRegionCatalog.label(e.getKey()));
            m.put("count", e.getValue());
            result.add(m);
        }
        // 按名称字母顺序排列（中文按拼音）
        java.text.Collator collator = java.text.Collator.getInstance(java.util.Locale.CHINA);
        result.sort((a, b) -> collator.compare(String.valueOf(a.get("label")), String.valueOf(b.get("label"))));
        return result;
    }

    /** 发现页筛选可选项：年份、平台、类别、类型 */
    @GetMapping("/filters")
    public AjaxResult filters()
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("years", mediaService.selectDistinctYears());
        ajax.put("platforms", mediaService.selectDistinctPlatforms());
        ajax.put("providers", buildProviders());
        ajax.put("countries", buildCountries());
        ajax.put("offerTypes", com.ruoyi.kemovie.domain.KmProviderCatalog.offerTypes());
        ajax.put("genres", java.util.Arrays.asList(
            "剧情", "喜剧", "爱情", "动作", "惊悚", "悬疑", "犯罪", "恐怖",
            "科幻", "奇幻", "动画", "家庭", "冒险", "历史", "战争", "音乐",
            "纪录", "西部"));
        ajax.put("mediaTypes", java.util.Arrays.asList("movie", "tv"));
        return ajax;
    }

    /** 热门榜 */
    @GetMapping("/rank")
    public AjaxResult rank(@RequestParam(defaultValue = "week") String type,
                           @RequestParam(defaultValue = "20") int limit)
    {
        List<KmHotRank> list = hotRankService.latest(type, limit);
        for (KmHotRank r : list) { stripDocs(r.getMedia()); }
        return AjaxResult.success(list);
    }

    /** 影片详情（登录用户附带其行为/订阅态） */
    @GetMapping("/media/{mediaId}")
    public AjaxResult mediaDetail(@PathVariable Long mediaId)
    {
        Long userId = currentUserIdOrNull();
        KmMedia media = mediaService.selectDetail(mediaId, userId);
        if (media == null) { return AjaxResult.error("影片不存在"); }
        // 资源文档链接不公开：订阅后通过站内通知获取（后台编辑接口不受影响）
        media.setFeishuDocUrl(null);
        media.setTencentDocUrl(null);
        AjaxResult ajax = AjaxResult.success(media);
        ajax.put("footerOps", opsBlockService.selectEnabledByKey("detail_footer"));
        return ajax;
    }

    /** 片单列表（仅公开） */
    @GetMapping("/collection/list")
    public TableDataInfo collectionList(KmCollection query)
    {
        query.setIsPublic("0");
        startPage();
        List<KmCollection> list = collectionService.selectKmCollectionList(query);
        return getDataTable(list);
    }

    /** 片单详情（仅元信息 + 关注态；影片列表走分页接口 /collection/{id}/items） */
    @GetMapping("/collection/{collectionId}")
    public AjaxResult collectionDetail(@PathVariable Long collectionId)
    {
        KmCollection c = collectionService.selectDetail(collectionId);
        if (c == null) { return AjaxResult.error("片单不存在"); }
        Long memberId = currentUserIdOrNull();
        if (memberId != null) { c.setFollowed(collectionService.isFollowed(memberId, collectionId)); }
        return AjaxResult.success(c);
    }

    /** 片单内影片：分页 + 筛选（与影片页一致），供无限滚动使用 */
    @GetMapping("/collection/{collectionId}/items")
    public TableDataInfo collectionItems(@PathVariable Long collectionId, KmMedia query)
    {
        applyPlatformBrand(query);
        startPage();
        List<KmMedia> list = collectionService.selectMediaOfCollectionPaged(collectionId, query);
        stripDocs(list);
        return getDataTable(list);
    }

    /** 资源文档字段不下发前台（订阅后通过站内通知获取） */
    private void stripDocs(KmMedia m)
    {
        if (m == null) { return; }
        m.setFeishuDocToken(null);
        m.setFeishuDocUrl(null);
        m.setTencentDocUrl(null);
    }

    private void stripDocs(List<KmMedia> list)
    {
        if (list == null) { return; }
        for (KmMedia m : list) { stripDocs(m); }
    }

    /** 运营位（按位置） */
    @GetMapping("/ops/{blockKey}")
    public AjaxResult ops(@PathVariable String blockKey)
    {
        List<KmOpsBlock> list = opsBlockService.selectEnabledByKey(blockKey);
        return AjaxResult.success(list);
    }

    /** 人物详情（含站内参演作品），无需登录 */
    @GetMapping("/person/{personId}")
    public AjaxResult personDetail(@PathVariable Long personId)
    {
        KmPerson p = personService.selectDetail(personId);
        if (p == null) { return AjaxResult.error("人物不存在"); }
        return AjaxResult.success(p);
    }

    private Long currentUserIdOrNull()
    {
        return MemberSecurityUtils.getMemberIdOrNull();
    }
}
