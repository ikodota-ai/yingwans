<template>
  <div class="portal-container library">
    <portal-breadcrumb :items="[{ text: '首页', to: '/discover' }, { text: '我的片库' }]" />
    <div class="section-title"><span>我的片库</span></div>
    <div class="tabs">
      <button v-for="t in tabs" :key="t.key" :class="{ active: tab === t.key }" @click="switchTab(t.key)">{{ t.label }}</button>
    </div>

    <div v-if="list.length" class="poster-grid">
      <poster-card v-for="m in list" :key="m.mediaId" :media="m" @action="handleAction" />
    </div>
    <div v-else class="empty-state">这里还什么都没有，去发现页收藏几部吧</div>

    <div class="lib-tip">
      想管理你的片单？前往 <router-link class="link" to="/collections?tab=mine">片单 · 我的片单</router-link>
    </div>
  </div>
</template>

<script>
import { myLibrary } from '@/api/kemovie/me'
import PosterCard from './components/PosterCard.vue'
import PortalBreadcrumb from './components/PortalBreadcrumb.vue'
import actionMixin from './mixins/actionMixin'

export default {
  name: 'PortalLibrary',
  components: { PosterCard, PortalBreadcrumb },
  mixins: [actionMixin],
  data() {
    return {
      tab: 'wish',
      tabs: [
        { key: 'wish', label: '想看' },
        { key: 'watched', label: '看过' },
        { key: 'rated', label: '我的评分' },
        { key: 'liked', label: '我的点赞' }
      ],
      list: []
    }
  },
  created() { this.switchTab('wish') },
  methods: {
    switchTab(key) {
      this.tab = key
      this.loadList()
    },
    loadList() {
      myLibrary(this.tab).then(res => { this.list = res.rows || [] })
    }
  }
}
</script>

<style scoped>
.tabs { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.tabs button {
  background: var(--bg-surface); border: 1px solid var(--border);
  color: var(--text-secondary); border-radius: 20px; padding: 6px 18px; cursor: pointer;
}
.tabs button.active { background: var(--brand); color: #1a1200; border-color: var(--brand); }
.lib-tip { margin-top: 26px; font-size: 13px; color: var(--text-muted); }
.lib-tip .link { color: var(--brand); }
</style>
