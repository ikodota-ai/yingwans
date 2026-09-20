#!/usr/bin/env python3
"""
旧站(ywtrzm)数据导出工具：读取本地 MySQL 旧库，解析为 legacy_export.jsonl。
服务器端「旧站导入」功能读取该文件完成 TMDB 匹配与落库。

用法:
    python3 tools/export_legacy.py [输出文件=legacy_export.jsonl]

依赖: 本机 mysql 客户端可访问 ywtrzm 库（脚本内账号按需修改）。
"""
import html
import json
import re
import subprocess
import sys

MYSQL = "/usr/local/mysql/bin/mysql"
DB_ARGS = ["--default-character-set=utf8mb4", "-uroot", "-p133301", "ywtrzm"]

THEME = {"1": "百合", "2": "男男纯爱", "3": "双性恋", "4": "跨性别", "5": "酷儿", "6": "异性恋", "7": "其他"}
TV_TYPES = ("电视剧", "真人秀")
SLOT_PLATFORM = {1: "baidu", 3: "115", 5: "quark", 6: "aliyun", 7: "uc", 8: "xunlei", 9: "other"}
SLOT_PWD = {1: 2, 3: 4}

SPEC = re.compile(
    r"(1080[Pp]|720[Pp]|4[Kk]|2160[Pp]|WEB[- ]?DL|WEBRip|BluRay|BDRip|HDRip|HDTV|DVD|TC|TS|CAM|HD|"
    r"内封|内嵌|官中|官译|官方中字|中字|中英|繁转简|简繁|双语|国配|粤配|无字|生肉|熟肉|字幕|"
    r"修复版|导演剪辑|未删减|删减|完整版|加长版|重制|高清|"
    r"蓝光|原盘|中日字幕|中日|日字|韩字|泰字|"
    r"完结|全\d+集|\d+集全|第?\d+集|EP\d+(-\d+)?|E\d{1,3}|更新至?\d*|\d+-\d+集|全\d+|"
    r"番外|特别篇|SP|OVA|OAD|剧场版|电影版|"
    r"第[一二三四五六七八九十]+季|[一二三四五六七八九十]+季全|S\d{1,2}|Season\s*\d+|"
    r"（[^）]*）|\[[^\]]*\]|【[^】]*】)", re.IGNORECASE)
YEAR = re.compile(r"[\(（]((?:19|20)\d{2})[\)）]")


def query(sql):
    out = subprocess.run([MYSQL] + DB_ARGS + ["-N", "-B", "-e", sql],
                         capture_output=True, text=True).stdout
    # mysql 批模式转义 \n/\t 但不转义 \r，而 splitlines() 会按 \r 断行，必须只按 \n 切
    for line in out.split("\n"):
        if line:
            yield line.split("\t")


def unesc(s):
    if s == "NULL":
        return ""
    return s.replace("\\\\", "\x00").replace("\\n", "\n").replace("\\t", "\t") \
            .replace("\\0", "\x00").replace("\x00", "\\").replace("\r", "")


def php_unserialize(d):
    b = d.encode("utf-8", errors="replace")

    def pv(i):
        t = chr(b[i])
        if t == "i":
            m = re.match(rb"i:(-?\d+);", b[i:])
            return int(m.group(1)), i + m.end()
        if t == "s":
            m = re.match(rb"s:(\d+):\"", b[i:])
            ln = int(m.group(1))
            st = i + m.end()
            return b[st:st + ln].decode("utf-8", errors="replace"), st + ln + 2
        if t == "a":
            m = re.match(rb"a:(\d+):\{", b[i:])
            n = int(m.group(1))
            i += m.end()
            out = {}
            for _ in range(n):
                k, i = pv(i)
                v, i = pv(i)
                out[k] = v
            return out, i + 1
        if t == "N":
            return None, i + 2
        raise ValueError("bad type %r at %d" % (t, i))

    arr, _ = pv(0)
    return arr


def parse_resources(detail):
    groups = []
    if not detail or not detail.startswith("a:"):
        return groups
    try:
        arr = php_unserialize(detail)
    except Exception:
        return groups
    for _, slots in sorted(arr.items()):
        if not isinstance(slots, dict):
            continue
        label = (slots.get(0) or "").strip()
        items = []
        for sk, plat in SLOT_PLATFORM.items():
            url = (slots.get(sk) or "").strip()
            if not url.lower().startswith("http"):
                continue
            # 旧站在 115 等链接后追加 "&# 访问码" 等备注垃圾，截断并去空白
            url = url.split("&#")[0].split("&amp;")[0]
            url = re.sub(r"\s+", "", url)
            if not url.lower().startswith("http"):
                continue
            pwd = ""
            if sk in SLOT_PWD:
                pwd = (slots.get(SLOT_PWD[sk]) or "").strip()
            items.append({"platform": plat, "url": url, "pwd": pwd})
        if items:
            groups.append({"group": label, "items": items})
    return groups


def split_title(raw):
    t = raw.replace("\\'", "'").strip()
    m = YEAR.search(t)
    year = m.group(1) if m else ""
    t = YEAR.sub(" ", t)
    t = SPEC.sub(" ", t)
    t = re.sub(r"[,，、/／|｜~～\-—–_:.：;；!！?？\'\"’‘“”()（）]+", " ", t)
    t = re.sub(r"\s+", " ", t).strip()
    t = re.sub(r"\s*全$", "", t).strip()
    # 按首个空白切分：前段为中文名，后段为外文/原名（兼容日文原名等非拉丁标题）
    parts = t.split(" ", 1)
    first = parts[0]
    has_cjk = re.search(r"[一-鿿]", first)
    if not has_cjk:
        return "", t, year
    if len(parts) == 1:
        return first, "", year
    return first, parts[1].strip(), year


def strip_html(s):
    s = re.sub(r"<[^>]+>", " ", s or "")
    s = html.unescape(s)
    s = html.unescape(s)  # 旧站存在 &amp;nbsp; 双重转义
    return re.sub(r"\s+", " ", s).strip()


def main():
    out_path = sys.argv[1] if len(sys.argv) > 1 else "legacy_export.jsonl"
    # 文本字段在 SQL 侧压掉换行（该 mysql 客户端不转义换行，会冲断 TSV 行）；
    # detail 是 PHP 序列化串，字节长度敏感，不能动（经验证其内部无换行）
    txt = lambda c: "REPLACE(REPLACE(IFNULL(%s,''),'\\r',' '),'\\n',' ')" % c
    rows = list(query(
        "SELECT a.aid, a.cid," + ",".join(txt(c) for c in
        ["a.article_title", "a.IMDB", "a.director", "a.actor", "a.article_fuzhu",
         "a.a1", "a.a2", "a.a3", "a.a4", "a.cp", "a.small_image", "a.big_image"])
        + ", a.detail, a.pub_time, a.hits, a.likes," + txt("c.content")
        + " FROM mod_article a LEFT JOIN mod_article_content c ON c.aid = a.aid"
        " WHERE a.cid IN (1,2,3,4,5,6,7) AND a.display = 1 ORDER BY a.aid"))
    stats = {"total": 0, "imdb": 0, "no_year": 0, "no_res": 0, "res_groups": 0, "res_items": 0, "bad_detail": 0}
    with open(out_path, "w", encoding="utf-8") as f:
        for r in rows:
            if len(r) != 19:
                stats["bad_detail"] += 1
                continue
            (aid, cid, title, imdb, director, actor, fuzhu, a1, a2, a3, a4, cp,
             small_image, big_image, detail, pub_time, hits, likes, content) = \
                [unesc(c) if i != 13 else c for i, c in enumerate(r)]
            cn, en, year = split_title(title)
            if not year:
                stats["no_year"] += 1
            imdb_id = ""
            m = re.search(r"(tt\d{6,10})", imdb or "")
            if m:
                imdb_id = m.group(1)
                stats["imdb"] += 1
            if detail.startswith("a:"):
                try:
                    php_unserialize(detail)
                except Exception:
                    stats["bad_detail"] += 1
            groups = parse_resources(detail)
            if not groups:
                stats["no_res"] += 1
            stats["res_groups"] += len(groups)
            stats["res_items"] += sum(len(g["items"]) for g in groups)
            tags = [THEME.get(cid, "其他")]
            genres = [g.strip() for g in re.split(r"[,，]", a2 or "") if g.strip()]
            media_type = "tv" if any(t in (a3 or "") for t in TV_TYPES) else "movie"
            actors = actor.strip() or fuzhu.strip()
            rec = {
                "aid": int(aid), "title_raw": title.strip(), "cn_title": cn, "en_title": en,
                "year": year, "media_type": media_type, "imdb_id": imdb_id,
                "director": director.strip(), "actors": actors, "cp": cp.strip(),
                "region": a1.strip(), "genres": genres, "quality": a4.strip(),
                "overview": strip_html(content), "theme_tags": tags,
                "poster": small_image.strip(), "backdrop": big_image.strip(),
                "pub_time": int(pub_time or 0), "hits": int(hits or 0), "likes": int(likes or 0),
                "resource_groups": groups,
            }
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
            stats["total"] += 1
    print(json.dumps(stats, ensure_ascii=False, indent=2))
    print("输出:", out_path)


if __name__ == "__main__":
    main()
