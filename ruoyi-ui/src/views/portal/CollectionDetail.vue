<template>
  <div v-if="collection" class="portal-container col-detail">
    <portal-breadcrumb :items="crumbs" />

    <div class="col-header">
      <div class="cover-tiles">
        <img v-for="(p, i) in coverTiles" :key="i" :src="p" loading="lazy" @error="onErr" />
      </div>
      <div class="col-info">
        <div class="source pill">{{ sourceText }}</div>
        <h1>{{ collection.name }}</h1>
        <div class="sub">{{ collection.itemCount || total }} 部<span v-if="collection.ownerName"> · {{ collection.ownerName }}</span></div>
        <p v-if="collection.description" class="desc">{{ collection.description }}</p>
        <div class="header-ops">
          <button
            v-if="isPublic"
            class="follow-btn"
            :class="{ following: followed }"
            @click="toggleFollow"
          >{{ followed ? '✓ 已关注' : '＋ 关注片单' }}</button>
        </div>
      </div>
    </div>

    <div class="section-title"><span>影片</span></div>
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
import { getCollection, listCollectionItems, getFilters } from '@/api/kemovie/portal'
import { followCollection, unfollowCollection } from '@/api/kemovie/me'
import PosterCard from './components/PosterCard.vue'
import FilterBar from './components/FilterBar.vue'
import ProviderRow from './components/ProviderRow.vue'
import PortalBreadcrumb from './components/PortalBreadcrumb.vue'
import { sourceLabel, posterFromPath, POSTER_PLACEHOLDER } from './utils'
import actionMixin from './mixins/actionMixin'
import infiniteScrollMixin from './mixins/infiniteScrollMixin'
import { getPortalToken } from './portalAuth'

export default {
  name: 'PortalCollectionDetail',
  components: { PosterCard, FilterBar, ProviderRow, PortalBreadcrumb },
  mixins: [actionMixin, infiniteScrollMixin],
  data() {
    return {
      collection: null,
      followed: false,
      pageSize: 24,
      filters: { year: null, region: null, minRating: null, ratingSource: null, sortBy: null, genres: null, platform: null, offerType: null, mediaType: null, phase: null },
      opts: { years: [], platforms: [], genres: [], mediaTypes: [], providers: [], offerTypes: [], countries: [] }
    }
  },
  computed: {
    sourceText() { return this.collection ? sourceLabel(this.collection.source) : '' },
    isPublic() { return this.collection && this.collection.isPublic === '0' },
    crumbs() {
      return [
        { text: '首页', to: '/discover' },
        { text: '片单', to: '/collections' },
        { text: this.collection ? this.collection.name : '' }
      ]
    },
    coverTiles() {
      const arr = (this.collection && this.collection.coverPosters || []).slice(0, 4).map(p => posterFromPath(p))
      while (arr.length < 4) arr.push(POSTER_PLACEHOLDER)
      return arr
    }
  },
  watch: {
    '$route.params.collectionId'() { this.load() }
  },
  created() {
    getFilters().then(res => {
      this.opts = {
        years: res.years || [], platforms: res.platforms || [],
        genres: res.genres || [], mediaTypes: res.mediaTypes || [],
        providers: res.providers || [], offerTypes: res.offerTypes || [],
        countries: res.countries || []
      }
    })
    this.load()
  },
  methods: {
    onErr(e) { e.target.src = POSTER_PLACEHOLDER },
    load() {
      getCollection(this.$route.params.collectionId).then(res => {
        this.collection = res.data
        this.followed = !!(res.data && res.data.followed)
        this.resetAndFetch()
      })
    },
    onFilterChange() { this.resetAndFetch() },
    buildParams() {
      const p = { pageNum: this.pageNum, pageSize: this.pageSize }
      Object.keys(this.filters).forEach(k => {
        if (this.filters[k] != null && this.filters[k] !== '') p[k] = this.filters[k]
      })
      return p
    },
    fetchPage() {
      const cid = this.$route.params.collectionId
      return listCollectionItems(cid, this.buildParams()).then(res => {
        const rows = res.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = res.total || 0
      })
    },
    toggleFollow() {
      if (!getPortalToken()) {
        this.$router.push('/portal-login?redirect=' + encodeURIComponent(this.$route.fullPath))
        return
      }
      const cid = this.collection.collectionId
      const fn = this.followed ? unfollowCollection : followCollection
      fn(cid).then(() => {
        this.followed = !this.followed
        this.$modal.msgSuccess(this.followed ? '已关注' : '已取消关注')
      }).catch(() => { this.$modal.msgError('操作失败') })
    }
  }
}
</script>

<style scoped>
.col-header { display: flex; gap: 24px; margin: 12px 0 24px; }
.cover-tiles {
  flex: 0 0 200px; height: 133px; display: grid;
  grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr;
  border-radius: 12px; overflow: hidden;
}
.cover-tiles img { width: 100%; height: 100%; object-fit: cover; }
.col-info h1 { font-size: 26px; margin: 8px 0 4px; }
.col-info .sub { color: var(--text-secondary); }
.col-info .desc { color: var(--text-secondary); margin-top: 10px; max-width: 700px; line-height: 1.6; }
.header-ops { margin-top: 14px; }
.follow-btn {
  background: var(--brand); color: #1a1200; border: none; border-radius: 20px;
  padding: 8px 22px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all .2s;
}
.follow-btn:hover { background: var(--brand-hover); }
.follow-btn.following { background: var(--bg-elevated); color: var(--text-secondary); border: 1px solid var(--border); }
.follow-btn.following:hover { color: var(--danger); border-color: var(--danger); }
.scroll-sentinel { height: 1px; }
.load-more { text-align: center; margin: 28px 0; }
.list-end { text-align: center; color: var(--text-muted); font-size: 13px; margin: 24px 0; }
</style>
