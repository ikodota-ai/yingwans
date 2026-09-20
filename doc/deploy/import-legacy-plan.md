# 旧站（ywtrzm）数据导入计划

> 状态：**计划已确认，尚未执行导入**。导出工具与服务器端导入功能已就绪并实测。
> 目标库：**线上服务器数据库**（本地 ywtrzm 仅作为导出数据源，只读）。

## 一、旧站数据结构解读（已逐字段核对）

### mod_article（2440 行，2439 行 display=1）——影片主表
| 字段 | 实际含义 | 去向 |
|---|---|---|
| `aid` | 主键 | `km_media.legacy_aid`（幂等锚点） |
| `cid` | 题材七类：1百合/2男男纯爱/3双性恋/4跨性别/5酷儿/6异性恋/7其他（8关于我们、9小说不导入） | `km_media.tags` 第一段 |
| `article_title` | 全标题，含年份/集数/画质/字幕（如「克莱尔和贝尔 Clairebell (2025) 1-3集 1080P 中字」） | 解析出 中文名/外文名/年份 |
| `article_fuzhu` | 主演/卡司（如「大桥明代 / 纪伊加奈」） | 导出保留（actors，见「七、边界」） |
| `article_fuzhu1` | 上映日期 | 导出保留 |
| `article_fuzhu2` | 时长（如「59分钟」） | 导出保留 |
| `article_fuzhu3` | 其他译名/原名 | 导出保留 |
| `article_fuzhu4` | 连载状态（一季全/完结） | 导出保留 |
| `article_fuzhu5` | 字幕组/版本（亿万同人字幕组/网飞官中） | 导出保留 |
| `a1` | 国家/地区（逗号多值） | `km_media.region` |
| `a2` | 补充标签（剧情,同性…） | 能映射 TMDB 类型的进 `genres`，其余进 `tags` 补充段 |
| `a3` | 类型（电影/电视剧/短片/纪录片/真人秀/脱口秀/剪辑） | `media_type`（电视剧/真人秀→tv，其余→movie） |
| `a4` | 画质（1080p/720p） | 导出保留（不进库） |
| `IMDB` | IMDB 编号（765 条有值） | 精确匹配 TMDB 的第一优先级 |
| `small_image` | 海报 `/article/xxx.jpg` | OSS → `poster_local_url` |
| `big_image` | 剧照/横幅（465 条有值） | OSS → `backdrop_local_url` |
| `detail` | PHP 序列化资源数组（见下） | `km_media_resource` |
| `hits` / `likes` / `pub_time` | 点击量/点赞/发布时间 | 导出保留，暂不落库 |
| `mrate` / `cp` / `content1-5` | 评分全为 -1/0；cp 是角色配对非公司；内容字段全空 | 不导入 |

### detail 资源槽位（PHP 序列化，外层多行=季/版本分组）
| 槽位 | 含义 |
|---|---|
| 0 | 分组名（如「第一季」「姬线cut」，可空） |
| 1/2 | 百度网盘链接 / 提取码 |
| 3/4 | 115 链接（常带「&#备注」垃圾需截断）/ 访问码 |
| 5 | 夸克链接 |
| 6 | 阿里云盘链接 |
| 7 | UC 网盘链接 |
| 8 | 迅雷链接 |
| 9 | 其他 |

### 其余表
- `mod_article_content.content`：影片简介（731 条非空，含 HTML/双重转义已清洗）→ TMDB 未命中时作 `overview`
- `mod_article_class`：cid 1-7 即七类题材（见上）
- `mod_article2`：房产字段表，与本站无关，忽略
- `mod_like`（0 行）、`mod_article_special`（0 行）：忽略
- `mod_attachments`：文件登记表，图片直接按路径上传 OSS，不导表

## 二、导出实测统计（tools/export_legacy.py，2026-09-18 跑通）
- 导出 **2438** 条（cid∈1-7 且 display=1）
- 有 IMDB：**765** 条（精确命中 TMDB）
- 缺年份：**614** 条（标题匹配命中率会降低，未命中建 legacy 片）
- 无资源：24 条（仍导入为影片）
- 资源分组 2707 个 / 资源链接 **8206** 条；detail 解析失败 0

## 三、执行步骤

### 第 1 步：本地导出（只读旧库，可随时重跑）
```bash
python3 tools/export_legacy.py legacy_export.jsonl
```

### 第 2 步：海报/剧照上传 OSS（一次性）
```bash
ossutil cp -r ~/dev/ywtrzm/web/web/uploads/article/ oss://yingwans/article/ --update
```
- 3268 个文件约 2.1G；上传后 `https://image.yingwans.com/article/xxx.jpg` 可访问
- 后台导入页 posterBase 默认已是 `https://image.yingwans.com`，无需改动

### 第 3 步：服务器后台导入（写入线上库）
1. 后台「影弯管理 → 旧站导入」上传 `legacy_export.jsonl`
2. 先点 **试跑**（dryRun 不落库）：查看 IMDB 命中数 / 标题命中数 / 未命中清单
3. 确认后点 **正式导入**：每行间隔 120ms + TMDB 请求，全量约 1.5–3 小时（服务器香港直连 TMDB）
4. 落地优先级：`legacy_aid` 复用 → IMDB 精确 → 标题+年份（先外文后中文）→ 旧站数据建 legacy 片（source=legacy，正常可见）

### 第 4 步：验收
- 进度页核对 matched/created/failed/unmatched
- 抽查：门户详情页海报与背景图、题材标签（七类+补充）、后台「下载资源」区链接
- 资源只进后台/飞书文档，**前台接口不返回**（既有约束）

## 四、幂等与重跑
- `legacy_aid` 为锚点：重跑时已导入的片仅刷新 tags 与资源，不重复建片
- 资源按 (media_id, source=legacy) 全量替换，重导不产生重复链接
- IMDB 命中但 TMDB 同步失败的行记失败、不降级，可修复后重跑

## 五、导入后收尾（可选）
- TMDB 未命中的 legacy 片：后台「影视库」编辑录入 TMDB ID 同步即可升级为完整资料（海报/简介/演职员）
- 需要分享资源时：后台编辑页点「生成/更新飞书文档」（需配置 FEISHU_APP_ID/SECRET）

## 六、字段映射之外的决定
- 题材标签 = **七类 + a2 补充**（已确认）
- **不区分版权下架**：资源在第三方网盘，全部正常导入（已确认）
- 旧站 hits/likes 不迁移为新站计数（避免与 km_user_media_action 冗余不一致）

## 七、已知边界
1. 614 条缺年份：建 legacy 片概率高，属预期；后续可人工补 TMDB ID
2. 旧站演职员（article_fuzhu/actor）暂不进 km_person/km_media_cast——legacy 片的卡司只在导出文件里保留；如需展示可后续加导入
3. 连载状态/字幕组/画质等版本信息不入库（新站以 TMDB 资料为准）
4. `&amp;nbsp;` 双重转义已在导出侧清洗（2026-09-18 修复）
