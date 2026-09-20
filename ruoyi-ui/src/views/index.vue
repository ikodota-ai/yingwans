<template>
  <div class="km-dashboard" v-loading="loading">
    <!-- 顶部欢迎横幅 -->
    <el-card shadow="never" class="hero-card">
      <div class="hero-inner">
        <div class="hero-text">
          <div class="hero-hello">{{ greeting }}，{{ name }}</div>
          <div class="hero-title">影弯 · 运营控制台</div>
          <div class="hero-sub">{{ today }} · 欢迎回到 Kemovie 影视库管理后台</div>
        </div>
        <div class="hero-actions">
          <el-button type="text" icon="el-icon-monitor" @click="goPortal">前往影院首页</el-button>
          <el-button type="primary" plain icon="el-icon-refresh" @click="loadData">刷新数据</el-button>
        </div>
      </div>
    </el-card>

    <!-- 核心指标 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in primaryCards" :key="card.label">
        <el-card shadow="hover" class="stat-card" @click.native="card.path && $router.push(card.path)">
          <div class="stat-flex">
            <div class="stat-icon" :style="{ background: card.bg }">
              <i :class="card.icon" />
            </div>
            <div class="stat-meta">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
          <div class="stat-foot" v-if="card.foot">{{ card.foot }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 次要指标 -->
    <div class="mini-grid">
      <el-card v-for="m in minorCards" :key="m.label" shadow="hover" class="mini-card" :body-style="{ padding: '14px 16px' }">
        <div class="mini-icon"><i :class="m.icon" :style="{ color: m.color }" /></div>
        <div class="mini-value">{{ m.value }}</div>
        <div class="mini-label">{{ m.label }}</div>
      </el-card>
    </div>

    <!-- 最近入库 / 即将上线 -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="panel">
          <div slot="header" class="panel-head">
            <span><i class="el-icon-film" /> 最近入库</span>
            <el-button type="text" @click="$router.push('/kemovie/media')">查看全部</el-button>
          </div>
          <div v-if="recent.length" class="recent-list">
            <div v-for="m in recent" :key="'r' + m.mediaId" class="recent-item" @click="openMedia(m)">
              <el-image :src="posterUrl(m)" class="recent-poster" fit="cover">
                <div slot="error" class="poster-fallback"><i class="el-icon-picture-outline" /></div>
              </el-image>
              <div class="recent-info">
                <div class="recent-title">
                  <el-tag size="mini" :type="m.mediaType === 'tv' ? 'success' : 'danger'" effect="plain">
                    {{ m.mediaType === 'tv' ? '剧集' : '电影' }}
                  </el-tag>
                  <span class="recent-name">{{ m.title }}</span>
                </div>
                <div class="recent-desc">
                  <span v-if="m.releaseDate"><i class="el-icon-date" />{{ fmtDate(m.releaseDate) }}</span>
                  <span v-if="m.voteAverage != null"><i class="el-icon-star-off" />{{ Number(m.voteAverage).toFixed(1) }}</span>
                  <span v-if="m.popularity != null"><i class="el-icon-data-line" />{{ Math.round(Number(m.popularity)) }}</span>
                </div>
              </div>
              <el-tag size="mini" :type="m.visible === '0' ? 'success' : 'info'">
                {{ m.visible === '0' ? '已上架' : '下架' }}
              </el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无影片" :image-size="80" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="panel">
          <div slot="header" class="panel-head">
            <span><i class="el-icon-bell" /> 即将上线</span>
            <el-tag size="small" type="warning" effect="plain">{{ overview.upcomingTotal || 0 }} 部</el-tag>
          </div>
          <div v-if="upcoming.length" class="up-list">
            <div v-for="m in upcoming" :key="'u' + m.mediaId" class="up-item" @click="openMedia(m)">
              <div class="up-date">
                <div class="up-day">{{ dayOf(m.onlineDate || m.releaseDate) }}</div>
                <div class="up-month">{{ monthOf(m.onlineDate || m.releaseDate) }}</div>
              </div>
              <div class="up-info">
                <div class="up-name">{{ m.title }}</div>
                <div class="up-meta">
                  <el-tag size="mini" :type="m.mediaType === 'tv' ? 'success' : 'danger'" effect="plain">
                    {{ m.mediaType === 'tv' ? '剧集' : '电影' }}
                  </el-tag>
                  <span class="up-status">{{ statusText(m.status) }}</span>
                  <span v-if="daysUntil(m.onlineDate) != null" class="up-countdown">{{ daysUntil(m.onlineDate) }} 天后</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无排期" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card shadow="never" class="panel">
      <div slot="header" class="panel-head"><span><i class="el-icon-menu" /> 快捷入口</span></div>
      <div class="quick-grid">
        <div v-for="q in quickLinks" :key="q.path" class="quick-item" @click="$router.push(q.path)">
          <i :class="q.icon" :style="{ color: q.color }" />
          <span>{{ q.label }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getDashboardOverview } from '@/api/kemovie/admin'

const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p'

const STATUS_MAP = {
  'Returning Series': '连载中',
  'Planned': '筹备中',
  'In Production': '制作中',
  'Post Production': '后期制作',
  'Released': '已上映',
  'Ended': '已完结',
  'Canceled': '已取消'
}

export default {
  name: 'AdminIndex',
  data() {
    return {
      loading: false,
      overview: {},
      recent: [],
      upcoming: [],
      quickLinks: [
        { label: '影视库', icon: 'el-icon-film', color: '#409eff', path: '/kemovie/media' },
        { label: '演职人员', icon: 'el-icon-user-solid', color: '#67c23a', path: '/kemovie/person' },
        { label: '片单管理', icon: 'el-icon-collection', color: '#e6a23c', path: '/kemovie/collection' },
        { label: '会员管理', icon: 'el-icon-s-custom', color: '#f56c6c', path: '/kemovie/member' },
        { label: 'MDL 抓取源', icon: 'el-icon-download', color: '#909399', path: '/kemovie/mdlSource' },
        { label: '运营位', icon: 'el-icon-picture-outline', color: '#9b59b6', path: '/kemovie/opsBlock' }
      ]
    }
  },
  computed: {
    name() {
      return this.$store.getters.nickName || this.$store.getters.name || '管理员'
    },
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '夜深了'
      if (h < 9) return '早上好'
      if (h < 12) return '上午好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
    today() {
      const d = new Date()
      const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
      return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 · 星期${week}`
    },
    primaryCards() {
      const o = this.overview
      return [
        {
          label: '影视库', icon: 'el-icon-film', bg: 'rgba(64,158,255,.12)', value: o.mediaTotal || 0,
          foot: `电影 ${o.movieTotal || 0} · 剧集 ${o.tvTotal || 0} · 已上架 ${o.visibleTotal || 0}`,
          path: '/kemovie/media'
        },
        {
          label: '演职人员', icon: 'el-icon-user-solid', bg: 'rgba(103,194,58,.12)', value: o.personTotal || 0,
          foot: '人物资料与头像', path: '/kemovie/person'
        },
        {
          label: '前台会员', icon: 'el-icon-s-custom', bg: 'rgba(245,108,108,.12)', value: o.memberTotal || 0,
          foot: '注册的影院会员', path: '/kemovie/member'
        },
        {
          label: '即将上线', icon: 'el-icon-bell', bg: 'rgba(230,162,60,.12)', value: o.upcomingTotal || 0,
          foot: '未来上线 / 制作中', path: '/kemovie/media'
        }
      ]
    },
    minorCards() {
      const o = this.overview
      return [
        { label: '片单', icon: 'el-icon-collection', color: '#e6a23c', value: o.collectionTotal || 0 },
        { label: 'MDL 抓取源', icon: 'el-icon-download', color: '#909399', value: o.mdlTotal || 0 },
        { label: '想看', icon: 'el-icon-star-off', color: '#f56c6c', value: o.likeTotal || 0 },
        { label: '期待', icon: 'el-icon-star-on', color: '#e6a23c', value: o.wishTotal || 0 },
        { label: '看过', icon: 'el-icon-circle-check', color: '#67c23a', value: o.watchTotal || 0 }
      ]
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    loadData() {
      this.loading = true
      getDashboardOverview().then(res => {
        this.overview = res.overview || {}
        this.recent = res.recent || []
        this.upcoming = res.upcoming || []
      }).finally(() => { this.loading = false })
    },
    posterUrl(m) {
      const u = m.posterLocalUrl
      if (u) {
        if (/^(https?:)?\/\//.test(u)) return u
        return (process.env.VUE_APP_BASE_API || '') + u
      }
      return m.posterPath ? TMDB_IMAGE_BASE + '/w92' + m.posterPath : ''
    },
    openMedia(m) {
      this.$router.push({ path: '/kemovie/media', query: {} })
    },
    goPortal() {
      this.$router.push('/discover')
    },
    fmtDate(v) {
      return v ? String(v).slice(0, 10) : ''
    },
    statusText(s) {
      return STATUS_MAP[s] || s || ''
    },
    dayOf(v) {
      return v ? String(v).slice(8, 10) : '--'
    },
    monthOf(v) {
      return v ? String(v).slice(0, 7).slice(5) + '月' : ''
    },
    daysUntil(v) {
      if (!v) return null
      const target = new Date(String(v).slice(0, 10) + 'T00:00:00')
      if (isNaN(target.getTime())) return null
      const diff = Math.round((target - new Date(new Date().toDateString())) / 86400000)
      return diff >= 0 ? diff : null
    }
  }
}
</script>

<style scoped>
.km-dashboard { padding: 4px 2px 16px; }

.hero-card {
  margin-bottom: 16px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(120deg, #1f2d3d 0%, #3a4f6b 55%, #409eff 130%);
  color: #fff;
}
.hero-card >>> .el-card__body { padding: 22px 26px; }
.hero-inner { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.hero-hello { font-size: 13px; opacity: .85; }
.hero-title { font-size: 24px; font-weight: 700; margin: 4px 0 6px; letter-spacing: 1px; }
.hero-sub { font-size: 13px; opacity: .8; }
.hero-actions .el-button--text { color: #fff; margin-right: 14px; }
.hero-actions .el-button--primary.is-plain { background: rgba(255,255,255,.12); border-color: rgba(255,255,255,.5); color: #fff; }
.hero-actions .el-button--primary.is-plain:hover { background: #fff; color: #1f2d3d; }

.stat-row { margin-bottom: 4px; }
.stat-card { margin: 8px 0; border: none; border-radius: 10px; cursor: pointer; }
.stat-flex { display: flex; align-items: center; gap: 14px; }
.stat-icon { width: 46px; height: 46px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.stat-icon i { font-size: 24px; color: #409eff; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.1; }
.stat-label { font-size: 13px; color: #909399; margin-top: 2px; }
.stat-foot { margin-top: 10px; font-size: 12px; color: #909399; }

.mini-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin: 4px 0 8px; }
.mini-card { border: none; border-radius: 10px; text-align: center; }
@media (max-width: 992px) { .mini-grid { grid-template-columns: repeat(3, 1fr); } }
.mini-icon i { font-size: 20px; }
.mini-value { font-size: 20px; font-weight: 700; margin: 4px 0 2px; }
.mini-label { font-size: 12px; color: #909399; }

.panel { margin-top: 12px; border: none; border-radius: 10px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.panel-head i { margin-right: 6px; color: #409eff; }

.recent-list .recent-item {
  display: flex; align-items: center; gap: 12px;
  padding: 9px 6px; border-bottom: 1px solid #f0f2f5; cursor: pointer;
}
.recent-list .recent-item:hover { background: #f7f9fc; }
.recent-list .recent-item:last-child { border-bottom: none; }
.recent-poster { width: 40px; height: 58px; border-radius: 6px; background: #f0f2f5; flex: none; }
.poster-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 18px; }
.recent-info { flex: 1; min-width: 0; }
.recent-title { display: flex; align-items: center; gap: 8px; }
.recent-name { font-size: 14px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.recent-desc { margin-top: 6px; font-size: 12px; color: #909399; display: flex; gap: 14px; }
.recent-desc i { margin-right: 3px; }

.up-list .up-item { display: flex; align-items: center; gap: 12px; padding: 10px 4px; border-bottom: 1px solid #f0f2f5; cursor: pointer; }
.up-list .up-item:hover { background: #f7f9fc; }
.up-list .up-item:last-child { border-bottom: none; }
.up-date {
  width: 46px; height: 46px; border-radius: 8px; flex: none;
  background: #fdf6ec; color: #e6a23c; text-align: center; display: flex; flex-direction: column; justify-content: center;
}
.up-day { font-size: 17px; font-weight: 700; line-height: 1; }
.up-month { font-size: 11px; margin-top: 2px; }
.up-info { flex: 1; min-width: 0; }
.up-name { font-size: 14px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.up-meta { margin-top: 6px; display: flex; align-items: center; gap: 8px; font-size: 12px; color: #909399; }
.up-countdown { color: #e6a23c; }

.quick-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.quick-item {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 18px 8px; border-radius: 10px; background: #f7f9fc; cursor: pointer; transition: background .2s;
}
.quick-item:hover { background: #ecf3fb; }
.quick-item i { font-size: 24px; }
.quick-item span { font-size: 13px; color: #606266; }
@media (max-width: 768px) { .quick-grid { grid-template-columns: repeat(3, 1fr); } }
</style>
