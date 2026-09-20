package com.ruoyi.kemovie.client;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * 无头 Chrome 页面渲染器（绕过 Cloudflare JS 挑战），MDL / Letterboxd 共用。
 *
 * @author kemovie
 */
@Component
public class HeadlessChrome
{
    private static final Logger log = LoggerFactory.getLogger(HeadlessChrome.class);
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final KemovieProperties props;

    public HeadlessChrome(KemovieProperties props)
    {
        this.props = props;
    }

    /** 渲染页面并输出 DOM（超时后强制结束并读取已渲染内容） */
    public String render(String url, int virtualTimeBudget, long timeoutMs)
    {
        String chrome = props.getMdl().getChromePath();
        if (StringUtils.isEmpty(chrome) || !new File(chrome).exists())
        {
            log.error("未配置或找不到 Chrome 可执行文件: {}", chrome);
            return null;
        }
        File tmp = null;
        File outFile = null;
        Process p = null;
        try
        {
            tmp = Files.createTempDirectory("kemovie-profile-").toFile();
            outFile = File.createTempFile("kemovie-dom-", ".html");

            List<String> cmd = new ArrayList<>();
            cmd.add(chrome);
            cmd.add("--headless=new");
            cmd.add("--disable-gpu");
            cmd.add("--no-sandbox");
            cmd.add("--disable-dev-shm-usage");
            cmd.add("--no-first-run");
            cmd.add("--no-default-browser-check");
            cmd.add("--disable-extensions");
            cmd.add("--disable-background-networking");
            cmd.add("--disable-sync");
            cmd.add("--disable-crash-reporter");
            cmd.add("--user-data-dir=" + tmp.getAbsolutePath());
            cmd.add("--user-agent=" + UA);
            cmd.add("--virtual-time-budget=" + virtualTimeBudget);
            if (StringUtils.isNotEmpty(props.getChromeProxy()))
            {
                cmd.add("--proxy-server=" + props.getChromeProxy().trim());
            }
            else if (props.getProxy().isEnabled())
            {
                cmd.add("--proxy-server=http://" + props.getProxy().getHost() + ":" + props.getProxy().getPort());
            }
            cmd.add("--dump-dom");
            cmd.add(url);

            // 关键：将 stdout 重定向到临时文件，主线程只需 waitFor，
            // 避免 readAllBytes() 在 Chrome 不退出时无限阻塞（早期版本死锁根因）。
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(outFile);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            log.debug("Chrome渲染命令: {}", String.join(" ", cmd));
            p = pb.start();
            p.getOutputStream().close();

            // --dump-dom 的进程往往不退出，但输出是流式写完的：
            // 轮询输出文件直至出现 </html>（文档闭合）即视为完成，避免固定等待后读到截断内容。
            long deadline = System.currentTimeMillis() + timeoutMs;
            boolean complete = false;
            while (System.currentTimeMillis() < deadline)
            {
                if (fileEndsWithHtml(outFile))
                {
                    complete = true;
                    break;
                }
                if (!p.isAlive()) { break; }
                try { Thread.sleep(500); } catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!complete)
            {
                log.warn("Chrome 渲染超时({}ms) url={}，读取已渲染内容", timeoutMs, url);
            }
            p.destroyForcibly();
            p.waitFor(5, TimeUnit.SECONDS);
            byte[] out = Files.readAllBytes(outFile.toPath());
            return new String(out, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            log.error("Chrome 渲染异常 url={}", url, e);
            if (p != null) { try { p.destroyForcibly(); } catch (Exception ignore) {} }
            return null;
        }
        finally
        {
            if (tmp != null) { deleteDir(tmp); }
            if (outFile != null) { try { outFile.delete(); } catch (Exception ignore) {} }
        }
    }

    /** 渲染结果是否仍停留在 Cloudflare 挑战页 */
    public boolean isCloudflareChallenge(String html)
    {
        // 注意：Cloudflare 会给正常页面也注入 /cdn-cgi/challenge-platform/scripts/jsd/main.js，
        // 因此 "challenge-platform" 不能作为挑战页判据；真正的挑战页才有 _cf_chl_opt / 标题 Just a moment
        return html != null && (html.contains("_cf_chl_opt") || html.contains("<title>Just a moment"));
    }

    private void deleteDir(File dir)
    {
        try
        {
            File[] files = dir.listFiles();
            if (files != null) { for (File f : files) { if (f.isDirectory()) { deleteDir(f); } else { f.delete(); } } }
            dir.delete();
        }
        catch (Exception ignore) {}
    }

    /** 输出文件是否已包含 </html>（仅读尾部 8KB） */
    private boolean fileEndsWithHtml(File outFile)
    {
        try
        {
            long size = outFile.length();
            if (size < 16) { return false; }
            byte[] tail = new byte[(int) Math.min(size, 8192)];
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(outFile, "r"))
            {
                raf.seek(size - tail.length);
                raf.readFully(tail);
            }
            return new String(tail, StandardCharsets.UTF_8).contains("</html>");
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
