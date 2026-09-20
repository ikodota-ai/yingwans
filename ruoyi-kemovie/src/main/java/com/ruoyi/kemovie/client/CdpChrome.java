package com.ruoyi.kemovie.client;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.kemovie.config.KemovieProperties;

/**
 * 基于 CDP（Chrome DevTools Protocol）的页面渲染器。
 * 与 --dump-dom（虚拟时钟竞态大）不同：导航后按真实时间轮询 DOM，
 * 直到目标选择器出现（Cloudflare 挑战已通过）或超时，再取 outerHTML。
 * Java 内置 java.net.http.WebSocket 实现，无额外依赖。
 *
 * @author kemovie
 */
@Component
public class CdpChrome
{
    private static final Logger log = LoggerFactory.getLogger(CdpChrome.class);
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final KemovieProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public CdpChrome(KemovieProperties props)
    {
        this.props = props;
    }

    /**
     * 渲染页面：等待 readySelector 出现或超时后返回 outerHTML。
     *
     * @param url           目标地址
     * @param readySelector 就绪标志 CSS 选择器（出现即认为内容已加载）
     * @param timeoutMs     最长等待（含 Cloudflare 挑战时间）
     */
    public String renderAndWait(String url, String readySelector, long timeoutMs)
    {
        String chrome = props.getMdl().getChromePath();
        if (StringUtils.isEmpty(chrome) || !new File(chrome).exists())
        {
            log.error("未配置或找不到 Chrome 可执行文件: {}", chrome);
            return null;
        }
        File profileDir = null;
        Process process = null;
        CdpSocket socket = null;
        try
        {
            profileDir = Files.createTempDirectory("kemovie-cdp-").toFile();
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
            cmd.add("--remote-debugging-port=0");
            cmd.add("--user-data-dir=" + profileDir.getAbsolutePath());
            cmd.add("--user-agent=" + UA);
            if (StringUtils.isNotEmpty(props.getChromeProxy()))
            {
                cmd.add("--proxy-server=" + props.getChromeProxy().trim());
            }
            else if (props.getProxy().isEnabled())
            {
                cmd.add("--proxy-server=http://" + props.getProxy().getHost() + ":" + props.getProxy().getPort());
            }
            cmd.add("about:blank");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            process = pb.start();
            process.getOutputStream().close();

            int port = awaitDevToolsPort(profileDir, 15000);
            if (port <= 0)
            {
                log.warn("CDP 端口就绪超时 url={}", url);
                return null;
            }
            socket = new CdpSocket(browserWsUrl(port), mapper);
            socket.connect();

            ObjectNode createParams = mapper.createObjectNode();
            createParams.put("url", url);
            String targetId = socket.call("Target.createTarget", createParams, null)
                    .path("result").path("targetId").asText(null);
            if (targetId == null)
            {
                log.warn("CDP 创建标签页失败 url={}", url);
                return null;
            }
            ObjectNode attachParams = mapper.createObjectNode();
            attachParams.put("targetId", targetId);
            attachParams.put("flatten", true);
            String sessionId = socket.call("Target.attachToTarget", attachParams, null)
                    .path("result").path("sessionId").asText(null);
            if (sessionId == null)
            {
                log.warn("CDP 附加会话失败 url={}", url);
                return null;
            }

            long deadline = System.currentTimeMillis() + timeoutMs;
            boolean ready = false;
            int poll = 0;
            while (System.currentTimeMillis() < deadline)
            {
                Integer count = evalInt(socket, sessionId,
                        "document.querySelectorAll(\"" + readySelector + "\").length");
                if (poll++ < 15 || poll % 10 == 0)
                {
                    String title = evalString(socket, sessionId, "document.title");
                    log.debug("CDP poll#{} count={} title={} url={}", poll, count, title, url);
                }
                if (count != null && count > 0)
                {
                    ready = true;
                    break;
                }
                try { Thread.sleep(1200); } catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!ready)
            {
                log.warn("CDP 等待就绪超时({}ms) url={} selector={}", timeoutMs, url, readySelector);
            }
            return evalString(socket, sessionId, "document.documentElement.outerHTML");
        }
        catch (Exception e)
        {
            log.error("CDP 渲染异常 url={} err={}", url, e.getMessage());
            return null;
        }
        finally
        {
            if (socket != null) { socket.close(); }
            if (process != null)
            {
                try { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); }
                catch (Exception ignore) { }
            }
            if (profileDir != null) { deleteDir(profileDir); }
        }
    }

    private Integer evalInt(CdpSocket socket, String sessionId, String expression)
    {
        try
        {
            JsonNode value = eval(socket, sessionId, expression);
            return value != null && value.isNumber() ? value.asInt() : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String evalString(CdpSocket socket, String sessionId, String expression)
    {
        try
        {
            JsonNode value = eval(socket, sessionId, expression);
            return value != null && value.isTextual() ? value.asText() : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private JsonNode eval(CdpSocket socket, String sessionId, String expression) throws Exception
    {
        ObjectNode params = mapper.createObjectNode();
        params.put("expression", expression);
        params.put("returnByValue", true);
        JsonNode resp = socket.call("Runtime.evaluate", params, sessionId);
        return resp.path("result").path("result").path("value");
    }

    /** 等待 DevToolsActivePort 文件出现并读取端口 */
    private int awaitDevToolsPort(File profileDir, long timeoutMs)
    {
        File portFile = new File(profileDir, "DevToolsActivePort");
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline)
        {
            try
            {
                if (portFile.exists())
                {
                    List<String> lines = Files.readAllLines(portFile.toPath(), StandardCharsets.UTF_8);
                    if (!lines.isEmpty())
                    {
                        return Integer.parseInt(lines.get(0).trim());
                    }
                }
            }
            catch (Exception ignore) { }
            try { Thread.sleep(200); } catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        return -1;
    }

    private String browserWsUrl(int port) throws Exception
    {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/json/version"))
                .timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode json = mapper.readTree(resp.body());
        return json.path("webSocketDebuggerUrl").asText(null);
    }

    private void deleteDir(File dir)
    {
        try
        {
            File[] files = dir.listFiles();
            if (files != null) { for (File f : files) { if (f.isDirectory()) { deleteDir(f); } else { f.delete(); } } }
            dir.delete();
        }
        catch (Exception ignore) { }
    }

    /** 最小 CDP WebSocket 客户端：JSON-RPC 请求/响应匹配，事件忽略 */
    private static class CdpSocket implements WebSocket.Listener
    {
        private final String wsUrl;
        private final ObjectMapper mapper;
        private final Map<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
        private final StringBuilder textBuf = new StringBuilder();
        private WebSocket webSocket;
        private int nextId = 0;

        CdpSocket(String wsUrl, ObjectMapper mapper)
        {
            this.wsUrl = wsUrl;
            this.mapper = mapper;
        }

        void connect() throws Exception
        {
            this.webSocket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), this)
                    .get(15, TimeUnit.SECONDS);
        }

        JsonNode call(String method, ObjectNode params, String sessionId) throws Exception
        {
            int id;
            synchronized (this) { id = ++nextId; }
            ObjectNode msg = mapper.createObjectNode();
            msg.put("id", id);
            msg.put("method", method);
            if (params != null) { msg.set("params", params); }
            if (sessionId != null) { msg.put("sessionId", sessionId); }
            CompletableFuture<JsonNode> future = new CompletableFuture<>();
            pending.put(id, future);
            webSocket.sendText(msg.toString(), true).get(10, TimeUnit.SECONDS);
            JsonNode resp = future.get(30, TimeUnit.SECONDS);
            if (resp.has("error"))
            {
                throw new RuntimeException("CDP错误 " + method + ": " + resp.path("error").path("message").asText());
            }
            return resp;
        }

        void close()
        {
            try { if (webSocket != null) { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, ""); } }
            catch (Exception ignore) { }
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last)
        {
            textBuf.append(data);
            if (last)
            {
                String full = textBuf.toString();
                textBuf.setLength(0);
                try
                {
                    JsonNode node = mapper.readTree(full);
                    if (node.has("id"))
                    {
                        CompletableFuture<JsonNode> future = pending.remove(node.path("id").asInt());
                        if (future != null) { future.complete(node); }
                    }
                }
                catch (Exception ignore) { }
            }
            return WebSocket.Listener.super.onText(ws, data, last);
        }
    }
}
