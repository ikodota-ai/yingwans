<template>
  <div class="portal-container subs">
    <div class="section-title">
      <span>订阅提醒</span>
      <div class="tab-switch">
        <button :class="{ active: view === 'timeline' }" @click="view = 'timeline'">上线时间轴</button>
        <button :class="{ active: view === 'notice' }" @click="loadNotices">通知记录</button>
      </div>
    </div>

    <!-- 时间轴 -->
    <template v-if="view === 'timeline'">
      <div v-if="groups.length">
        <div v-for="g in groups" :key="g.date" class="tl-group">
          <div class="tl-date">{{ g.date }}</div>
          <div class="tl-items">
            <div v-for="s in g.items" :key="s.subId" class="tl-item" @click="goDetail(s.media)">
              <img :src="poster(s.media)" class="tl-poster" @error="onErr" />
              <div class="tl-info">
                <div class="tl-title">{{ s.media.title }}</div>
                <div class="tl-meta">
                  <span class="pill">{{ s.media.region || '—' }}</span>
                  <span :class="['state', releaseCls(s.media.onlineDate || s.media.releaseDate)]">{{ releaseText(s.media.onlineDate || s.media.releaseDate) }}</span>
                </div>
              </div>
              <button class="btn-ghost" @click.stop="unsub(s)">取消订阅</button>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">还没有订阅任何影片，去影片详情点击 🔔 订阅上线提醒</div>
    </template>

    <!-- 通知记录 -->
    <template v-else>
      <div class="notice-head" v-if="notices.length">
        <button class="btn-ghost" @click="markAll">全部已读</button>
      </div>
      <div v-if="notices.length" class="notice-list">
        <div v-for="n in notices" :key="n.notifyId" :class="['notice', { unread: n.isRead !== '1' }]" @click="readOne(n)">
          <div class="notice-title">{{ n.title }}</div>
          <div class="notice-content">{{ n.content }}</div>
          <div class="notice-time">{{ n.createTime }}</div>
        </div>
      </div>
      <div v-else class="empty-state">暂无通知</div>
    </template>
  </div>
</template>

<script>
import { mySubscriptions, subscribe, myNotifications, readNotification, readAllNotifications } from '@/api/kemovie/me'
import { posterUrl, releaseState, POSTER_PLACEHOLDER } from './utils'

export default {
  name: 'PortalSubscriptions',
  data() {
    return { view: 'timeline', subs: [], notices: [] }
  },
  computed: {
    groups() {
      const map = {}
      this.subs.forEach(s => {
        const src = s.media && (s.media.onlineDate || s.media.releaseDate)
        const d = src ? String(src).substring(0, 10) : '待定'
        if (!map[d]) map[d] = []
        map[d].push(s)
      })
      return Object.keys(map).sort().map(date => ({ date, items: map[date] }))
    }
  },
  created() { this.loadTimeline() },
  methods: {
    poster(m) { return posterUrl(m) },
    onErr(e) { e.target.src = POSTER_PLACEHOLDER },
    releaseCls(d) { return releaseState(d).cls },
    releaseText(d) { return releaseState(d).text },
    goDetail(m) { this.$router.push('/film/' + m.mediaId) },
    loadTimeline() {
      this.view = 'timeline'
      mySubscriptions().then(res => { this.subs = res.data || [] })
    },
    loadNotices() {
      this.view = 'notice'
      myNotifications().then(res => { this.notices = res.data || [] })
    },
    unsub(s) {
      subscribe(s.media.mediaId).then(() => {
        this.$modal.msgSuccess('已取消订阅')
        this.loadTimeline()
      })
    },
    readOne(n) {
      if (n.isRead === '1') return
      readNotification(n.notifyId).then(() => { n.isRead = '1' })
    },
    markAll() {
      readAllNotifications().then(() => {
        this.$modal.msgSuccess('已全部标记为已读')
        this.notices.forEach(n => { n.isRead = '1' })
      })
    }
  }
}
</script>

<style scoped>
.tab-switch button {
  background: transparent; border: none; color: var(--text-secondary);
  cursor: pointer; padding: 4px 12px; font-size: 14px;
}
.tab-switch button.active { color: var(--brand); font-weight: 600; }
.tl-group { margin-bottom: 24px; }
.tl-date {
  font-size: 14px; color: var(--brand); font-weight: 600;
  padding-bottom: 8px; border-bottom: 1px solid var(--border); margin-bottom: 12px;
}
.tl-item {
  display: flex; align-items: center; gap: 14px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 10px; padding: 10px 14px; margin-bottom: 10px; cursor: pointer;
}
.tl-item:hover { border-color: var(--brand); }
.tl-poster { width: 46px; height: 68px; object-fit: cover; border-radius: 6px; }
.tl-info { flex: 1; }
.tl-title { font-weight: 600; margin-bottom: 6px; }
.tl-meta { display: flex; gap: 8px; align-items: center; }
.state { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.state.online { background: rgba(63,185,132,.15); color: var(--success); }
.state.upcoming { background: rgba(232,161,58,.15); color: var(--warning); }
.state.muted { background: var(--bg-elevated); color: var(--text-muted); }
.notice-head { margin-bottom: 12px; text-align: right; }
.notice {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 10px; padding: 14px 16px; margin-bottom: 10px; cursor: pointer;
}
.notice.unread { border-left: 3px solid var(--brand); }
.notice-title { font-weight: 600; margin-bottom: 4px; }
.notice-content { color: var(--text-secondary); font-size: 14px; }
.notice-time { color: var(--text-muted); font-size: 12px; margin-top: 6px; }
</style>
