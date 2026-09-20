<template>
  <div class="portal-container discover">
    <!-- 搜索结果模式 -->
    <template v-if="query">
      <div class="section-title"><span>搜索：“{{ query }}”</span></div>
      <div v-if="searchList.length" class="poster-grid">
        <poster-card v-for="m in searchList" :key="m.mediaId" :media="m" @action="handleAction" />
      </div>
      <div v-else class="empty-state">没有找到相关影片</div>
    </template>

    <template v-else>
      <!-- 浮动运营位（竖条，hover 展开），不占用主体宽度 -->
      <ops-dock :blocks="homeSide" />

      <!-- 本周热门 TOP5 轮播 -->
          <section v-if="topFive.length" class="hero-carousel">
            <div class="hero" :style="heroStyle">
              <div class="hero-overlay">
                <div class="hero-tag">本周热门 · TOP {{ activeIndex + 1 }}</div>
                <h1>{{ banner.title }}</h1>
                <div class="hero-meta">
                  <span v-if="banner.ratingAvg > 0" class="score" :style="{ color: scoreColor(banner.ratingAvg) }">
                    ★ {{ Number(banner.ratingAvg).toFixed(1) }}
                  </span>
                  <span v-if="bannerRegion">{{ bannerRegion }}</span>
                  <span v-if="bannerYear">{{ bannerYear }}</span>
                </div>
                <p class="hero-desc">{{ banner.overview }}</p>
                <div class="hero-btns">
                  <button class="btn-brand" @click="goDetail(banner)">查看详情</button>
                  <button class="btn-ghost" @click="handleAction({ type: 'wish', media: banner })">＋ 想看</button>
                </div>
              </div>
            </div>
            <div class="hero-dots">
              <span
                v-for="(m, idx) in topFive"
                :key="m.mediaId || idx"
                class="dot"
                :class="{ active: idx === activeIndex }"
                @click="selectHero(idx)"
              ></span>
            </div>
          </section>

          <!-- 上映状态快捷入口 -->
          <section class="phase-chips">
            <span class="chip" @click="goPhase('upcoming')">🕒 即将上线</span>
            <span class="chip" @click="goPhase('airing')">▶ 近期上线</span>
            <span class="chip" @click="goPhase('ended')">✔ 已完结 / 经典</span>
          </section>

          <!-- 热门排行榜 -->
          <section>
            <div class="section-title">
              <span>🔥 热门排行榜</span>
              <span class="more" @click="$router.push('/browse?sort=hot')">查看全部 ›</span>
            </div>
            <div class="rail-2">
              <poster-card
                v-for="(item, idx) in hotRank"
                :key="item.mediaId || idx"
                :media="item.media || item"
                :rank="idx + 1"
                @action="handleAction"
              />
            </div>
          </section>

          <!-- 最近收录（按本站收录时间，含旧站导入） -->
          <section v-if="recentCollected.length">
            <div class="section-title">
              <span>📥 最近收录</span>
              <span class="more" @click="$router.push('/browse?sort=newest')">查看全部 ›</span>
            </div>
            <div class="nr-row">
              <div
                v-for="it in recentCollected"
                :key="it.mediaId"
                class="nr-card"
                @click="goDetail(it)"
              >
                <div class="nr-poster">
                  <img :src="posterOf(it)" :alt="it.title" loading="lazy" @error="onImgErr" />
                  <span class="nr-date">{{ yearLabel(it.releaseDate) }}</span>
                </div>
                <div class="nr-name">{{ it.title }}</div>
                <div class="nr-platforms">{{ collectedLabel(it.createTime) }}</div>
              </div>
            </div>
          </section>

          <!-- 最新上映 -->
          <section>
            <div class="section-title">
              <span>🆕 最新上映</span>
              <span class="more" @click="$router.push('/browse?sort=recent')">查看全部 ›</span>
            </div>
            <div class="rail-2">
              <poster-card v-for="m in recent" :key="'r' + m.mediaId" :media="m" @action="handleAction" />
            </div>
          </section>

          <!-- 片单精选 -->
      <section v-if="collections.length">
        <div class="section-title">
          <span>🎬 片单精选</span>
          <span class="more" @click="$router.push('/collections')">查看全部 ›</span>
        </div>
        <div class="collection-row">
          <collection-card v-for="c in collections" :key="c.collectionId" :collection="c" />
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import { getHome, listMedia, getRecentCollected } from '@/api/kemovie/portal'
import PosterCard from './components/PosterCard.vue'
import CollectionCard from './components/CollectionCard.vue'
import OpsDock from './components/OpsDock.vue'
import { backdropUrl, ratingColor, regionLabel, posterUrl, POSTER_PLACEHOLDER } from './utils'
import actionMixin from './mixins/actionMixin'

export default {
  name: 'PortalDiscover',
  components: { PosterCard, CollectionCard, OpsDock },
  mixins: [actionMixin],
  data() {
    return {
      hotRank: [], recent: [], collections: [], homeSide: [],
      searchList: [], activeIndex: 0, heroTimer: null,
      recentCollected: []
    }
  },
  computed: {
    query() { return this.$route.query.q || '' },
    topFive() {
      const rows = (this.hotRank || []).slice(0, 5).map(t => t.media || t)
      if (rows.length) return rows
      return this.recent && this.recent.length ? [this.recent[0]] : []
    },
    banner() { return this.topFive[this.activeIndex] || this.topFive[0] || null },
    bannerYear() {
      return this.banner && this.banner.releaseDate ? String(this.banner.releaseDate).substring(0, 4) : ''
    },
    bannerRegion() { return this.banner ? regionLabel(this.banner.region) : '' },
    heroStyle() {
      const bg = backdropUrl(this.banner)
      return bg ? { backgroundImage: `linear-gradient(to right, rgba(0,0,0,.9), rgba(0,0,0,.3)), url(${bg})` } : {}
    },
  },
  watch: {
    query() { this.query ? this.doSearch() : this.loadHome() }
  },
  created() {
    this.query ? this.doSearch() : this.loadHome()
  },
  beforeDestroy() { this.stopHeroTimer() },
  methods: {
    scoreColor(s) { return ratingColor(s) },
    goDetail(m) { this.$router.push('/film/' + m.mediaId) },
    goPhase(phase) { this.$router.push('/browse?phase=' + phase) },
    selectHero(idx) { this.activeIndex = idx; this.startHeroTimer() },
    startHeroTimer() {
      this.stopHeroTimer()
      if (this.topFive.length <= 1) return
      this.heroTimer = setInterval(() => {
        this.activeIndex = (this.activeIndex + 1) % this.topFive.length
      }, 6000)
    },
    stopHeroTimer() { if (this.heroTimer) { clearInterval(this.heroTimer); this.heroTimer = null } },
    loadHome() {
      getHome().then(res => {
        this.hotRank = res.hotRank || []
        this.recent = res.recent || []
        this.collections = res.collections || []
        this.homeSide = res.homeSide || []
        this.activeIndex = 0
        this.startHeroTimer()
      })
      this.loadRecentCollected()
    },
    loadRecentCollected() {
      getRecentCollected({ limit: 24 }).then(res => {
        this.recentCollected = res.data || []
      }).catch(() => {})
    },
    posterOf(m) { return posterUrl(m) },
    onImgErr(e) { e.target.src = POSTER_PLACEHOLDER },
    yearLabel(d) { return d ? String(d).substring(0, 4) : '未知' },
    collectedLabel(t) {
      if (!t) return ''
      const day = new Date(String(t).replace(' ', 'T'))
      const diff = Math.floor((Date.now() - day.getTime()) / 86400000)
      if (diff <= 0) return '今天收录'
      if (diff === 1) return '昨天收录'
      if (diff < 30) return diff + ' 天前收录'
      return String(t).substring(5, 10).replace('-', '月') + '日收录'
    },
    doSearch() {
      listMedia({ title: this.query, pageNum: 1, pageSize: 30 }).then(res => {
        this.searchList = res.rows || []
      })
    }
  }
}
</script>

<style scoped>
.phase-chips {
  display: flex; flex-wrap: wrap; gap: 12px; margin: 20px 0 4px;
}
.phase-chips .chip {
  cursor: pointer; user-select: none;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-secondary); border-radius: 22px; padding: 9px 20px;
  font-size: 14px; transition: all .2s;
}
.phase-chips .chip:hover {
  border-color: var(--brand); color: var(--brand); transform: translateY(-2px);
}
.hero-carousel { margin-top: 24px; }
.hero {
  height: 340px; border-radius: 14px;
  background-size: cover; background-position: center;
  display: flex; align-items: flex-end; overflow: hidden;
  background-color: var(--bg-surface);
}
.hero-overlay { padding: 32px; max-width: 620px; color: #fff; }
.hero-tag { font-size: 12px; color: var(--brand); letter-spacing: 1px; margin-bottom: 8px; }
.hero-overlay h1 { font-size: 32px; margin: 0 0 10px; }
.hero-meta { display: flex; gap: 14px; align-items: center; font-size: 14px; opacity: .9; margin-bottom: 10px; }
.hero-meta .score { font-weight: 700; }
.hero-desc {
  font-size: 14px; line-height: 1.6; opacity: .85; margin-bottom: 18px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.hero-btns { display: flex; gap: 12px; }
.hero-dots { display: flex; justify-content: center; gap: 8px; margin-top: 12px; }
.hero-dots .dot {
  width: 8px; height: 8px; border-radius: 50%; background: var(--border);
  cursor: pointer; transition: all .2s;
}
.hero-dots .dot.active { background: var(--brand); width: 22px; border-radius: 4px; }
.collection-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 900px) { .collection-row { grid-template-columns: repeat(2, 1fr); } }

.nr-tabs { display: inline-flex; gap: 6px; margin-left: 14px; }
.nr-tab {
  cursor: pointer; font-size: 12px; padding: 3px 12px; border-radius: 14px;
  color: var(--text-secondary); border: 1px solid var(--border);
  transition: all .2s;
}
.nr-tab:hover { color: var(--brand); border-color: var(--brand); }
.nr-tab.active { color: #fff; background: var(--brand); border-color: var(--brand); }
.nr-row {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px;
}
@media (max-width: 1200px) { .nr-row { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 700px) { .nr-row { grid-template-columns: repeat(3, 1fr); } }
.nr-card { cursor: pointer; min-width: 0; }
.nr-poster { position: relative; border-radius: 10px; overflow: hidden; aspect-ratio: 2/3; background: var(--bg-surface); }
.nr-poster img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s; }
.nr-card:hover .nr-poster img { transform: scale(1.05); }
.nr-date {
  position: absolute; left: 6px; bottom: 6px;
  background: rgba(0, 0, 0, .72); color: #fff; font-size: 11px;
  padding: 2px 8px; border-radius: 10px;
}
.nr-name {
  margin-top: 6px; font-size: 13px; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.nr-platforms {
  font-size: 11px; color: var(--text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
</style>
