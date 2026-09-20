package com.ruoyi.kemovie.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.client.TmdbClient;
import com.ruoyi.kemovie.domain.KmMedia;
import com.ruoyi.kemovie.domain.KmMediaResource;
import com.ruoyi.kemovie.mapper.KmMediaMapper;
import com.ruoyi.kemovie.mapper.KmMediaResourceMapper;
import com.ruoyi.kemovie.service.IKmLegacyImportService;
import com.ruoyi.kemovie.service.IKmMediaSyncService;
import com.ruoyi.kemovie.util.KmRegionCatalog;

/**
 * 旧站数据导入实现：JSONL 逐行处理。
 * 落地优先级：legacy_aid 复用 -> IMDB 精确 -> 标题+年份 -> 旧站数据建 legacy 片；
 * 资源一律按 media 全量替换（source=legacy），保证重导幂等。
 *
 * @author kemovie
 */
@Service
public class KmLegacyImportServiceImpl implements IKmLegacyImportService
{
    private static final Logger log = LoggerFactory.getLogger(KmLegacyImportServiceImpl.class);
    private static final int MAX_LIST = 1000;
    /** 旧站 a2 标签 -> TMDB 中文类型名；不在表内的进题材 tags */
    private static final Map<String, String> GENRE_MAP = new LinkedHashMap<>();
    static
    {
        for (String g : new String[] {"剧情", "喜剧", "恐怖", "爱情", "悬疑", "奇幻", "惊悚", "犯罪",
                "科幻", "传记", "家庭", "历史", "战争", "动作", "冒险", "音乐", "歌舞", "西部"})
        {
            GENRE_MAP.put(g, g);
        }
        GENRE_MAP.put("戏剧", "剧情");
        GENRE_MAP.put("动漫", "动画");
        GENRE_MAP.put("动画", "动画");
        GENRE_MAP.put("纪录片", "纪录");
    }

    @Autowired private KmMediaMapper mediaMapper;
    @Autowired private KmMediaResourceMapper resourceMapper;
    @Autowired private TmdbClient tmdbClient;
    @Autowired private IKmMediaSyncService mediaSyncService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Map<String, ImportTask> tasks = new ConcurrentHashMap<>();

    /** 导入任务状态 */
    public static class ImportTask
    {
        public volatile boolean running = true;
        public volatile boolean dryRun;
        public volatile int total;
        public volatile int done;
        public volatile int reused;
        public volatile int matchedImdb;
        public volatile int matchedTitle;
        public volatile int created;
        public volatile int failed;
        public volatile long startedAt = System.currentTimeMillis();
        public volatile long finishedAt;
        public volatile String message = "";
        public final List<Map<String, Object>> failures = new ArrayList<>();
        public final List<Map<String, Object>> unmatched = new ArrayList<>();
    }

    @Override
    public String startImport(String filePath, boolean dryRun, int maxRows, String posterBase)
    {
        File f = new File(filePath);
        if (!f.exists())
        {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ImportTask task = new ImportTask();
        task.dryRun = dryRun;
        tasks.put(taskId, task);
        exec.submit(() -> run(task, f, maxRows, posterBase));
        return taskId;
    }

    @Override
    public Map<String, Object> progress(String taskId)
    {
        ImportTask t = tasks.get(taskId);
        if (t == null) { return null; }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("running", t.running);
        m.put("dryRun", t.dryRun);
        m.put("total", t.total);
        m.put("done", t.done);
        m.put("reused", t.reused);
        m.put("matchedImdb", t.matchedImdb);
        m.put("matchedTitle", t.matchedTitle);
        m.put("created", t.created);
        m.put("failed", t.failed);
        m.put("message", t.message);
        m.put("elapsedMs", (t.finishedAt > 0 ? t.finishedAt : System.currentTimeMillis()) - t.startedAt);
        m.put("failures", t.failures);
        m.put("unmatched", t.unmatched);
        return m;
    }

    private void run(ImportTask task, File file, int maxRows, String posterBase)
    {
        log.info("[旧站导入] 开始 file={} dryRun={} maxRows={}", file.getAbsolutePath(), task.dryRun, maxRows);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (StringUtils.isEmpty(line.trim())) { continue; }
                if (maxRows > 0 && task.done >= maxRows) { break; }
                task.total++;
                try
                {
                    process(task, mapper.readTree(line), posterBase);
                }
                catch (Exception e)
                {
                    task.failed++;
                    try
                    {
                        addRow(task.failures, mapper.readTree(line), "异常: " + e.getMessage());
                    }
                    catch (Exception ignore)
                    {
                        addRow(task.failures, null, "行解析失败: " + line.substring(0, Math.min(60, line.length())));
                    }
                    log.warn("[旧站导入] 行处理失败: {}", e.getMessage());
                }
                task.done++;
                sleep(120);
            }
            task.message = "完成";
        }
        catch (Exception e)
        {
            task.message = "任务失败: " + e.getMessage();
            log.error("[旧站导入] 任务异常", e);
        }
        finally
        {
            task.running = false;
            task.finishedAt = System.currentTimeMillis();
            log.info("[旧站导入] 结束 done={} reused={} imdb={} title={} created={} failed={}",
                    task.done, task.reused, task.matchedImdb, task.matchedTitle, task.created, task.failed);
        }
    }

    private void process(ImportTask task, JsonNode r, String posterBase)
    {
        long aid = r.path("aid").asLong();
        // 1) legacy_aid 复用：仅刷新资源与题材标签
        KmMedia exist = mediaMapper.selectKmMediaByLegacyAid(aid);
        if (exist != null)
        {
            if (!task.dryRun)
            {
                backfillTags(exist, r);
                replaceResources(exist.getMediaId(), r);
            }
            task.reused++;
            return;
        }
        // 2) IMDB 精确
        KmMedia media = null;
        boolean imdbHit = false;
        String imdbId = text(r, "imdb_id");
        if (StringUtils.isNotEmpty(imdbId))
        {
            TmdbClient.FindResult found = tmdbClient.findByImdb(imdbId);
            if (found != null && found.tmdbId != null)
            {
                imdbHit = true;
                media = task.dryRun ? stub(found.tmdbId, found.mediaType)
                        : mediaSyncService.syncFromTmdb(found.tmdbId, found.mediaType);
            }
        }
        // IMDB 已精确命中但同步失败（如 TMDB 详情为空）：不重试标题、不建 legacy，记失败
        if (imdbHit && media == null)
        {
            task.failed++;
            addRow(task.failures, r, "IMDB命中但同步失败");
            return;
        }
        // 3) 标题+年份（先外文后中文）
        Integer year = parseYear(text(r, "year"));
        String mediaType = StringUtils.isNotEmpty(text(r, "media_type")) ? text(r, "media_type") : "movie";
        if (media == null)
        {
            media = matchByTitle(task, text(r, "en_title"), year, mediaType);
        }
        if (media == null)
        {
            media = matchByTitle(task, text(r, "cn_title"), year, mediaType);
        }
        if (imdbHit) { task.matchedImdb++; }
        else if (media != null) { task.matchedTitle++; }
        // 4) 未命中：旧站数据建 legacy 片
        if (media == null)
        {
            task.created++;
            addRow(task.unmatched, r, "TMDB未命中");
            if (!task.dryRun)
            {
                media = createLegacy(r, year, mediaType, posterBase);
            }
        }
        // 5) 落地：锚点/标签/资源
        if (media != null && !task.dryRun)
        {
            KmMedia saved = mediaMapper.selectKmMediaByMediaId(media.getMediaId());
            if (saved != null)
            {
                backfillMeta(saved, r);
                replaceResources(saved.getMediaId(), r);
            }
        }
    }

    private KmMedia matchByTitle(ImportTask task, String title, Integer year, String mediaType)
    {
        if (StringUtils.isEmpty(title)) { return null; }
        return task.dryRun
                ? mediaSyncService.matchOnly(title, year, mediaType)
                : mediaSyncService.matchAndSyncByTitle(title, year, mediaType);
    }

    /** 命中 TMDB 的影片：补 legacy 锚点/题材标签/IMDB（不覆盖已有值） */
    private void backfillMeta(KmMedia media, JsonNode r)
    {
        KmMedia upd = new KmMedia();
        upd.setMediaId(media.getMediaId());
        boolean dirty = false;
        if (media.getLegacyAid() == null)
        {
            upd.setLegacyAid(r.path("aid").asLong());
            dirty = true;
        }
        if (StringUtils.isEmpty(media.getImdbId()) && StringUtils.isNotEmpty(text(r, "imdb_id")))
        {
            upd.setImdbId(text(r, "imdb_id"));
            dirty = true;
        }
        String tags = buildTags(r);
        if (StringUtils.isEmpty(media.getTags()) && StringUtils.isNotEmpty(tags))
        {
            upd.setTags(tags);
            dirty = true;
        }
        if (dirty)
        {
            upd.setUpdateBy("legacy-import");
            mediaMapper.updateKmMedia(upd);
        }
    }

    /** 复用旧片：仅补题材标签 */
    private void backfillTags(KmMedia media, JsonNode r)
    {
        String tags = buildTags(r);
        if (StringUtils.isEmpty(media.getTags()) && StringUtils.isNotEmpty(tags))
        {
            KmMedia upd = new KmMedia();
            upd.setMediaId(media.getMediaId());
            upd.setTags(tags);
            upd.setUpdateBy("legacy-import");
            mediaMapper.updateKmMedia(upd);
        }
    }

    private KmMedia createLegacy(JsonNode r, Integer year, String mediaType, String posterBase)
    {
        KmMedia m = new KmMedia();
        String cn = text(r, "cn_title");
        String en = text(r, "en_title");
        m.setTitle(StringUtils.isNotEmpty(cn) ? cn : en);
        m.setOriginalTitle(StringUtils.isNotEmpty(cn) ? en : null);
        if (StringUtils.isEmpty(m.getTitle()))
        {
            m.setTitle(text(r, "title_raw"));
        }
        m.setMediaType(mediaType);
        m.setImdbId(StringUtils.isNotEmpty(text(r, "imdb_id")) ? text(r, "imdb_id") : null);
        m.setLegacyAid(r.path("aid").asLong());
        m.setOverview(StringUtils.isNotEmpty(text(r, "overview")) ? text(r, "overview") : null);
        // 旧站中文地区名 -> 字母码存储（显示时补全中文）
        m.setRegion(KmRegionCatalog.normalizeCodes(text(r, "region")));
        m.setGenres(buildGenres(r));
        m.setTags(buildTags(r));
        if (year != null)
        {
            m.setReleaseDate(Date.valueOf(year + "-01-01"));
        }
        String poster = text(r, "poster");
        if (StringUtils.isNotEmpty(poster))
        {
            m.setPosterLocalUrl(StringUtils.isNotEmpty(posterBase) ? posterBase + poster : poster);
        }
        String backdrop = text(r, "backdrop");
        if (StringUtils.isNotEmpty(backdrop))
        {
            m.setBackdropLocalUrl(StringUtils.isNotEmpty(posterBase) ? posterBase + backdrop : backdrop);
        }
        m.setSource("legacy");
        m.setVisible("0");
        m.setCreateBy("legacy-import");
        m.setRemark("旧站aid=" + r.path("aid").asLong());
        mediaMapper.insertKmMedia(m);
        return m;
    }

    /** 资源全量替换（source=legacy 维度），重导幂等 */
    private void replaceResources(Long mediaId, JsonNode r)
    {
        resourceMapper.deleteByMediaAndSource(mediaId, "legacy");
        JsonNode groups = r.path("resource_groups");
        if (!groups.isArray()) { return; }
        int order = 0;
        for (JsonNode g : groups)
        {
            String groupName = StringUtils.trimToNull(g.path("group").asText(null));
            JsonNode items = g.path("items");
            if (!items.isArray()) { continue; }
            for (JsonNode it : items)
            {
                String url = StringUtils.trimToNull(it.path("url").asText(null));
                String platform = StringUtils.trimToNull(it.path("platform").asText(null));
                if (url == null || platform == null) { continue; }
                KmMediaResource res = new KmMediaResource();
                res.setMediaId(mediaId);
                res.setGroupName(groupName);
                res.setPlatform(platform);
                res.setUrl(url);
                res.setPwd(StringUtils.trimToNull(it.path("pwd").asText(null)));
                res.setGroupOrder(order);
                res.setSource("legacy");
                res.setCreateBy("legacy-import");
                resourceMapper.insertKmMediaResource(res);
            }
            order++;
        }
    }

    /** a2 标签 -> TMDB genres（映射不上的进题材 tags） */
    private String buildGenres(JsonNode r)
    {
        List<String> out = new ArrayList<>();
        for (JsonNode g : r.path("genres"))
        {
            String mapped = GENRE_MAP.get(g.asText());
            if (mapped != null && !out.contains(mapped)) { out.add(mapped); }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    /** 题材七类 + a2 中 TMDB 没有的补充标签 */
    private String buildTags(JsonNode r)
    {
        List<String> out = new ArrayList<>();
        for (JsonNode t : r.path("theme_tags"))
        {
            String tag = t.asText();
            if (StringUtils.isNotEmpty(tag) && !out.contains(tag)) { out.add(tag); }
        }
        for (JsonNode g : r.path("genres"))
        {
            String raw = g.asText();
            if (!GENRE_MAP.containsKey(raw) && StringUtils.isNotEmpty(raw) && !out.contains(raw))
            {
                out.add(raw);
            }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    private KmMedia stub(Long tmdbId, String mediaType)
    {
        KmMedia m = new KmMedia();
        m.setTmdbId(tmdbId);
        m.setMediaType(mediaType);
        return m;
    }

    private void addRow(List<Map<String, Object>> list, JsonNode r, String reason)
    {
        if (list.size() >= MAX_LIST) { return; }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("aid", r != null ? r.path("aid").asLong() : 0);
        row.put("title", r != null ? r.path("title_raw").asText("") : "");
        row.put("reason", reason);
        list.add(row);
    }

    private String text(JsonNode r, String field)
    {
        JsonNode n = r.path(field);
        return n.isTextual() ? StringUtils.trimToNull(n.asText()) : null;
    }

    private Integer parseYear(String y)
    {
        if (StringUtils.isEmpty(y)) { return null; }
        try { return Integer.parseInt(y.trim()); } catch (NumberFormatException e) { return null; }
    }

    private void sleep(long ms)
    {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
