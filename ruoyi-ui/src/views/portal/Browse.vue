<template>
  <div class="portal-container browse">
    <portal-breadcrumb :items="[{ text: '首页', to: '/discover' }, { text: '影片' }]" />
    <div class="section-title"><span>🔎 影片筛选</span></div>

    <provider-row v-model="filters.platform" :providers="opts.providers" @change="onFilterChange" />
    <filter-bar v-model="filters" :opts="opts" @change="onFilterChange" />

    <div v-if="list.length" class="poster-grid">
      <poster-card v-for="m in list" :key="m.mediaId" :media="m" :sort-by="filters.sortBy" @action="handleAction" />
    </div>
    <div v-else-if="!loading" class="empty-state">没有符合条件的影片</div>

    <div ref="sentinel" class="scroll-sentinel"></div>
    <div class="load-more" v-if="hasMore">
      <button class="btn-ghost" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
    </div>
    <div v-else-if="list.length && !loading" class="list-end">— 没有更多了 —</div>
  </div>
</template>

<script>
import { listMedia, getFilters } from '@/api/kemovie/portal'
import PosterCard from './components/PosterCard.vue'
import FilterBar from './components/FilterBar.vue'
import ProviderRow from './components/ProviderRow.vue'
import PortalBreadcrumb from './components/PortalBreadcrumb.vue'
import actionMixin from './mixins/actionMixin'
import infiniteScrollMixin from './mixins/infiniteScrollMixin'

export default {
  name: 'PortalBrowse',
  components: { PosterCard, FilterBar, ProviderRow, PortalBreadcrumb },
  mixins: [actionMixin, infiniteScrollMixin],
  data() {
    return {
      pageSize: 24,
      filters: { year: null, region: null, minRating: null, ratingSource: null, sortBy: null, genres: null, platform: null, offerType: null, mediaType: null, phase: null },
      opts: { years: [], platforms: [], genres: [], mediaTypes: [], providers: [], offerTypes: [], countries: [] }
    }
  },
  created() {
    if (this.$route.query.sort) this.filters.sortBy = this.$route.query.sort
    if (this.$route.query.genre) this.filters.genres = this.$route.query.genre
    if (this.$route.query.phase) this.filters.phase = this.$route.query.phase
    getFilters().then(res => {
      this.opts = {
        years: res.years || [], platforms: res.platforms || [],
        genres: res.genres || [], mediaTypes: res.mediaTypes || [],
        providers: res.providers || [], offerTypes: res.offerTypes || [],
        countries: res.countries || []
      }
    })
    this.resetAndFetch()
  },
  methods: {
    onFilterChange() { this.resetAndFetch() },
    buildParams() {
      const p = { pageNum: this.pageNum, pageSize: this.pageSize }
      Object.keys(this.filters).forEach(k => {
        if (this.filters[k] != null && this.filters[k] !== '') p[k] = this.filters[k]
      })
      return p
    },
    fetchPage() {
      return listMedia(this.buildParams()).then(res => {
        const rows = res.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = res.total || 0
      })
    }
  }
}
</script>

<style scoped>
.load-more { text-align: center; margin: 28px 0; }
.scroll-sentinel { height: 1px; }
.list-end { text-align: center; color: var(--text-muted); font-size: 13px; margin: 24px 0; }
</style>
