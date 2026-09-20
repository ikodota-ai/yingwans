package com.ruoyi.kemovie.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 国家/地区目录：库存 ISO 两字母码，显示补全中文名。
 * toCode 兼容各来源写法（码/中文别名/中文规范名）统一转码存储；
 * label 用于筛选选项与详情展示时补全中文。
 *
 * @author kemovie
 */
public class KmRegionCatalog
{
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, String> TO_CODE = new HashMap<>();
    static
    {
        String[][] rn = {
            {"CN","中国大陆"},{"HK","中国香港"},{"TW","中国台湾"},{"MO","中国澳门"},
            {"KR","韩国"},{"JP","日本"},{"TH","泰国"},{"US","美国"},{"GB","英国"},
            {"FR","法国"},{"DE","德国"},{"IT","意大利"},{"ES","西班牙"},{"RU","俄罗斯"},
            {"IN","印度"},{"PH","菲律宾"},{"MY","马来西亚"},{"SG","新加坡"},{"ID","印度尼西亚"},
            {"VN","越南"},{"CA","加拿大"},{"AU","澳大利亚"},{"NZ","新西兰"},{"BR","巴西"},
            {"MX","墨西哥"},{"AR","阿根廷"},{"NL","荷兰"},{"BE","比利时"},{"SE","瑞典"},
            {"NO","挪威"},{"DK","丹麦"},{"FI","芬兰"},{"PL","波兰"},{"TR","土耳其"},
            {"IE","爱尔兰"},{"CH","瑞士"},{"AT","奥地利"},{"PT","葡萄牙"},{"GR","希腊"},
            {"IS","冰岛"},{"LV","拉脱维亚"},{"CZ","捷克"},{"HU","匈牙利"},{"RO","罗马尼亚"},
            {"ZA","南非"},{"EG","埃及"},{"IL","以色列"},{"SA","沙特"},{"AE","阿联酋"},
            {"UA","乌克兰"},{"CL","智利"},{"CO","哥伦比亚"},{"PE","秘鲁"},{"KP","朝鲜"},
            {"TN","突尼斯"},{"CY","塞浦路斯"},{"AN","荷属安的列斯"},{"MM","缅甸"},
            {"KH","柬埔寨"},{"LA","老挝"},{"MN","蒙古"},{"PK","巴基斯坦"},{"BD","孟加拉国"},
            {"LK","斯里兰卡"},{"NP","尼泊尔"},{"IR","伊朗"},{"IQ","伊拉克"},{"NG","尼日利亚"},
            {"KE","肯尼亚"},{"MA","摩洛哥"},{"DZ","阿尔及利亚"},{"LU","卢森堡"},
            {"SK","斯洛伐克"},{"SI","斯洛文尼亚"},{"BG","保加利亚"},{"EE","爱沙尼亚"},
            {"LT","立陶宛"},{"HR","克罗地亚"},{"RS","塞尔维亚"},{"MT","马耳他"},
            {"PR","波多黎各"},{"CU","古巴"},{"DO","多米尼加"},{"VE","委内瑞拉"},
            {"UY","乌拉圭"},{"EC","厄瓜多尔"},{"BO","玻利维亚"},{"PY","巴拉圭"},
            {"BA","波黑"},{"BS","巴哈马"},{"CR","哥斯达黎加"},{"GH","加纳"},
            {"JM","牙买加"},{"JO","约旦"},{"LB","黎巴嫩"},{"KW","科威特"},
            {"QA","卡塔尔"},{"BH","巴林"},{"OM","阿曼"},{"PS","巴勒斯坦"},
            {"SY","叙利亚"},{"YE","也门"},{"AF","阿富汗"},{"KZ","哈萨克斯坦"},
            {"UZ","乌兹别克斯坦"},{"GE","格鲁吉亚"},{"AM","亚美尼亚"},{"AZ","阿塞拜疆"},
            {"AL","阿尔巴尼亚"},{"MK","北马其顿"},{"ME","黑山"},{"XK","科索沃"},
            {"MD","摩尔多瓦"},{"BY","白俄罗斯"},{"PA","巴拿马"},{"GT","危地马拉"},
            {"HN","洪都拉斯"},{"SV","萨尔瓦多"},{"NI","尼加拉瓜"},{"HT","海地"},
            {"TT","特立尼达和多巴哥"},{"SN","塞内加尔"},{"CI","科特迪瓦"},{"CM","喀麦隆"},
            {"ET","埃塞俄比亚"},{"UG","乌干达"},{"TZ","坦桑尼亚"},{"ZM","赞比亚"},
            {"ZW","津巴布韦"},{"MG","马达加斯加"},{"MU","毛里求斯"},{"NA","纳米比亚"},
            {"BW","博茨瓦纳"},{"MZ","莫桑比克"},{"AO","安哥拉"},{"CD","刚果(金)"},
            {"LY","利比亚"},{"SD","苏丹"},{"RW","卢旺达"},{"FJ","斐济"},
            {"PG","巴布亚新几内亚"},{"MC","摩纳哥"},{"LI","列支敦士登"},
            {"SM","圣马力诺"},{"VA","梵蒂冈"}
        };
        for (String[] r : rn)
        {
            NAMES.put(r[0], r[1]);
            TO_CODE.put(r[0], r[0]);
            TO_CODE.put(r[1], r[0]);
        }
        // 中文别名 -> 字母码（旧站等来源的写法偏差）
        TO_CODE.put("台湾", "TW");
        TO_CODE.put("台灣", "TW");
        TO_CODE.put("香港", "HK");
        TO_CODE.put("澳门", "MO");
        TO_CODE.put("澳門", "MO");
        TO_CODE.put("大陆", "CN");
        TO_CODE.put("内地", "CN");
        TO_CODE.put("中国", "CN");
        TO_CODE.put("南韩", "KR");
        TO_CODE.put("南韓", "KR");
        TO_CODE.put("北韩", "KP");
        TO_CODE.put("北韓", "KP");
    }

    /** 任意写法转字母码（已是码则大写返回）；无法识别返回原值（trim 后），空返回 null */
    public static String toCode(String raw)
    {
        if (raw == null) { return null; }
        String t = raw.trim();
        if (t.isEmpty()) { return null; }
        String code = TO_CODE.get(t);
        if (code != null) { return code; }
        code = TO_CODE.get(t.toUpperCase());
        return code != null ? code : t.toUpperCase();
    }

    /** 字母码补全中文名；未收录的码原样返回 */
    public static String label(String code)
    {
        if (code == null) { return ""; }
        return NAMES.getOrDefault(code.trim().toUpperCase(), code.trim());
    }

    /** region 字段归一为字母码串（逗号/斜杠多值）：逐段转码 + 去重保序；空返回 null */
    public static String normalizeCodes(String region)
    {
        if (region == null || region.trim().isEmpty()) { return null; }
        List<String> out = new ArrayList<>();
        for (String raw : region.split("[,/、，]"))
        {
            String c = toCode(raw);
            if (c != null && !out.contains(c)) { out.add(c); }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    /** region 字段转中文显示串（多值逐段补全） */
    public static String display(String region)
    {
        if (region == null || region.trim().isEmpty()) { return ""; }
        List<String> out = new ArrayList<>();
        for (String raw : region.split("[,/、，]"))
        {
            String t = raw.trim();
            if (!t.isEmpty()) { out.add(label(t)); }
        }
        return String.join("、", out);
    }
}
