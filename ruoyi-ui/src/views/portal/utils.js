// 影弯前台通用工具

const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p'
// 本地图（/profile 开头）需拼接后端 API 基址，因为前端 origin 不直接服务 /profile
const API_BASE = process.env.VUE_APP_BASE_API || ''
function withApiBase(url) {
  if (url && url.startsWith('/profile')) return API_BASE + url
  return url
}
// 占位图（本地资源缺失时的兜底，使用内联 SVG 避免额外请求）
export const POSTER_PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="450">' +
    '<rect width="100%" height="100%" fill="#1f232d"/>' +
    '<text x="50%" y="50%" fill="#6b7280" font-size="20" text-anchor="middle" dominant-baseline="middle">影弯</text>' +
    '</svg>'
  )

// 海报地址：优先本地化图，其次 TMDB poster_path，最后占位图
export function posterUrl(media, size = 'w342') {
  if (!media) return POSTER_PLACEHOLDER
  if (media.posterLocalUrl) return withApiBase(media.posterLocalUrl)
  if (media.posterPath) {
    if (media.posterPath.startsWith('http')) return media.posterPath
    return `${TMDB_IMAGE_BASE}/${size}${media.posterPath}`
  }
  return POSTER_PLACEHOLDER
}

// 由 TMDB poster_path/本地 URL 直接生成海报地址（用于片单封面拼贴等）
export function posterFromPath(path, size = 'w342') {
  if (!path) return POSTER_PLACEHOLDER
  if (path.startsWith('/profile')) return withApiBase(path)
  if (path.startsWith('http') || path.startsWith('data:')) return path
  if (path.startsWith('/')) return `${TMDB_IMAGE_BASE}/${size}${path}`
  return path
}

// 平台 logo 地址：本地化(/profile) 需拼接 API 基址，远程/占位原样返回
export function providerLogoUrl(logo) {
  if (!logo) return ''
  return withApiBase(logo)
}

// 背景大图
export function backdropUrl(media, size = 'w1280') {
  if (!media) return ''
  if (media.backdropLocalUrl) return withApiBase(media.backdropLocalUrl)
  if (!media.backdropPath) return ''
  if (media.backdropPath.startsWith('http')) return media.backdropPath
  return `${TMDB_IMAGE_BASE}/${size}${media.backdropPath}`
}

// 评分色阶：0-4 红、4-7 黄、7-10 绿
export function ratingColor(score) {
  const s = Number(score) || 0
  if (s >= 7) return '#3fb984'
  if (s >= 4) return '#e8a13a'
  return '#e5484d'
}

// 片单来源标签文案
export function sourceLabel(source) {
  return { system: '系统', tmdb: 'TMDB', mdl: 'MDL', manual: '手动' }[source] || '片单'
}

// 上线状态：根据 online_date 判断即将/已上线
export function releaseState(onlineDate) {
  if (!onlineDate) return { text: '待定', cls: 'muted' }
  const d = new Date(onlineDate.replace(/-/g, '/'))
  const now = new Date()
  if (d.getTime() <= now.getTime()) return { text: '已上线', cls: 'online' }
  return { text: '即将上线', cls: 'upcoming' }
}

// 人物头像：优先本地化图，其次 TMDB profile_path，最后头像占位
export const AVATAR_PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="185" height="278">' +
    '<rect width="100%" height="100%" fill="#1f232d"/>' +
    '<circle cx="92" cy="110" r="46" fill="#2a2e39"/>' +
    '<rect x="30" y="180" width="125" height="80" rx="40" fill="#2a2e39"/>' +
    '</svg>'
  )

export function profileUrl(person, size = 'w185') {
  if (!person) return AVATAR_PLACEHOLDER
  const local = person.profileLocalUrl
  const path = person.profilePath
  if (local) return withApiBase(local)
  if (path) {
    if (path.startsWith('http')) return path
    return `${TMDB_IMAGE_BASE}/${size}${path}`
  }
  return AVATAR_PLACEHOLDER
}

// 性别文案
export function genderLabel(g) {
  return { 1: '女', 2: '男' }[g] || '未知'
}

// 国家/地区代码 -> 中文名（用于详情页与筛选展示）
export const REGION_NAMES = {
  CN: '中国大陆', HK: '中国香港', TW: '中国台湾', MO: '中国澳门',
  KR: '韩国', JP: '日本', TH: '泰国', US: '美国', GB: '英国', UK: '英国',
  FR: '法国', DE: '德国', IT: '意大利', ES: '西班牙', RU: '俄罗斯',
  IN: '印度', PH: '菲律宾', MY: '马来西亚', SG: '新加坡', ID: '印度尼西亚',
  VN: '越南', CA: '加拿大', AU: '澳大利亚', NZ: '新西兰', BR: '巴西',
  MX: '墨西哥', AR: '阿根廷', NL: '荷兰', BE: '比利时', SE: '瑞典',
  NO: '挪威', DK: '丹麦', FI: '芬兰', PL: '波兰', TR: '土耳其',
  IE: '爱尔兰', CH: '瑞士', AT: '奥地利', PT: '葡萄牙', GR: '希腊',
  IS: '冰岛', LV: '拉脱维亚', CZ: '捷克', HU: '匈牙利', RO: '罗马尼亚',
  ZA: '南非', EG: '埃及', IL: '以色列', SA: '沙特', AE: '阿联酋',
  UA: '乌克兰', CL: '智利', CO: '哥伦比亚', PE: '秘鲁'
  , KP: '朝鲜', TN: '突尼斯', CY: '塞浦路斯', AN: '荷属安的列斯', MM: '缅甸',
  KH: '柬埔寨', LA: '老挝', MN: '蒙古', PK: '巴基斯坦', BD: '孟加拉国',
  LK: '斯里兰卡', NP: '尼泊尔', IR: '伊朗', IQ: '伊拉克', NG: '尼日利亚',
  KE: '肯尼亚', MA: '摩洛哥', DZ: '阿尔及利亚', LU: '卢森堡',
  SK: '斯洛伐克', SI: '斯洛文尼亚', BG: '保加利亚', EE: '爱沙尼亚',
  LT: '立陶宛', HR: '克罗地亚', RS: '塞尔维亚', MT: '马耳他',
  PR: '波多黎各', CU: '古巴', DO: '多米尼加', VE: '委内瑞拉',
  UY: '乌拉圭', EC: '厄瓜多尔', BO: '玻利维亚', PY: '巴拉圭',
  BA: '波黑', BS: '巴哈马', CR: '哥斯达黎加', GH: '加纳',
  JM: '牙买加', JO: '约旦', LB: '黎巴嫩', KW: '科威特',
  QA: '卡塔尔', BH: '巴林', OM: '阿曼', PS: '巴勒斯坦',
  SY: '叙利亚', YE: '也门', AF: '阿富汗', KZ: '哈萨克斯坦',
  UZ: '乌兹别克斯坦', GE: '格鲁吉亚', AM: '亚美尼亚', AZ: '阿塞拜疆',
  AL: '阿尔巴尼亚', MK: '北马其顿', ME: '黑山', XK: '科索沃',
  MD: '摩尔多瓦', BY: '白俄罗斯', PA: '巴拿马', GT: '危地马拉',
  HN: '洪都拉斯', SV: '萨尔瓦多', NI: '尼加拉瓜', HT: '海地',
  TT: '特立尼达和多巴哥', SN: '塞内加尔', CI: '科特迪瓦', CM: '喀麦隆',
  ET: '埃塞俄比亚', UG: '乌干达', TZ: '坦桑尼亚', ZM: '赞比亚',
  ZW: '津巴布韦', MG: '马达加斯加', MU: '毛里求斯', NA: '纳米比亚',
  BW: '博茨瓦纳', MZ: '莫桑比克', AO: '安哥拉', CD: '刚果(金)',
  LY: '利比亚', SD: '苏丹', RW: '卢旺达', FJ: '斐济',
  PG: '巴布亚新几内亚', MC: '摩纳哥', LI: '列支敦士登',
  SM: '圣马力诺', VA: '梵蒂冈'
}

// 将 region（可能是逗号分隔的多国代码）转为中文，未知代码保留原值
const REGION_ALIASES = {
  台湾: '中国台湾', 台灣: '中国台湾', 香港: '中国香港', 澳门: '中国澳门', 澳門: '中国澳门',
  大陆: '中国大陆', 内地: '中国大陆', 中国: '中国大陆', 南韩: '韩国', 南韓: '韩国',
  北韩: '朝鲜', 北韓: '朝鲜'
}

export function regionLabel(region) {
  if (!region) return ''
  return String(region)
    .split(/[,/、]/)
    .map(s => s.trim())
    .filter(Boolean)
    .map(code => REGION_NAMES[code.toUpperCase()] || REGION_ALIASES[code] || code)
    .join(' / ')
}
