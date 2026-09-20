# 影弯 Kemovie 运行说明（重要）

## JDK 版本要求：使用 JDK 21 / 24，**不要用 JDK 25**

**现象**：用 JDK 25 运行打包后的 `ruoyi-admin.jar` 时，任何携带 `Connection: close`
请求头的请求都会被服务端重置（返回空响应 / ECONNRESET）。前端 `npm run dev`
的 webpack dev-server 代理（底层 http-proxy）默认就带 `Connection: close`，因此访问
`/discover` 会报：

```
Proxy error: Could not proxy request /portal/home from localhost:1024 to http://localhost:8080 (ECONNRESET).
```

**根因**：Spring Boot 4.0.6 的可执行 fat-jar 在 **JDK 25** 上，`LaunchedClassLoader`
无法从内嵌 jar 中懒加载 Tomcat 的 `org.apache.tomcat.util.http.parser.TokenList`
（仅在解析 `Connection` 头时触发），抛 `NoClassDefFoundError`，导致该请求处理失败。
- Keep-alive 请求不带 `Connection: close`，不触发该类加载，所以浏览器直连/`curl` 默认能通。
- 该问题在 **JDK 24 / 21 上不存在**；用扁平 classpath（解压后运行）也不存在。
- 项目字节码目标为 Java 17，21/24 均兼容。

## 正确的后端启动方式

```bash
cd /Users/mac/dev/kemovie
# 使用 JDK 24（或 21 LTS），不要用 25
export JAVA_HOME=/Users/mac/Library/Java/JavaVirtualMachines/openjdk-24/Contents/Home
export http_proxy=http://127.0.0.1:10792; export https_proxy=http://127.0.0.1:10792
export no_proxy=localhost,127.0.0.1
$JAVA_HOME/bin/java -jar ruoyi-admin/target/ruoyi-admin.jar
```

编译同样建议用 JDK 21/24：
```bash
export JAVA_HOME=/Users/mac/Library/Java/JavaVirtualMachines/openjdk-24/Contents/Home
mvn -DskipTests package
```

## 前端本地开发
```bash
cd ruoyi-ui
npm install      # 首次
npm run dev      # 默认 80 端口，代理 /dev-api -> http://localhost:8080
```
- 若仍报代理 ECONNRESET，确认后端是用 JDK 24/21 启动（见上），且 8080 已监听。
- 生产构建：`npm run build:prod`

## 数据库 / Redis
- MySQL：`yingwan` 库，见 `sql/`（ry_*.sql + quartz.sql + kemovie*.sql）。
- Redis：localhost:6379。
