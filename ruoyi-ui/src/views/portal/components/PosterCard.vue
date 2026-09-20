<template>
  <div class="poster-card" @click="goDetail">
    <div class="poster-wrap">
      <span v-if="rank" class="rank-badge">{{ rank }}</span>
      <img :src="poster" :alt="media.title" loading="lazy" @error="onImgError" />
      <div class="poster-mask">
        <div class="title">{{ media.title }}</div>
        <div class="sub">{{ year }}<span v-if="regionText"> · {{ regionText }}</span></div>
      </div>
      <div v-if="media.ratingAvg > 0" class="score" :style="{ color: scoreColor }">
        {{ Number(media.ratingAvg).toFixed(1) }}
      </div>
      <div v-if="metric" class="metric" :title="metric.label">{{ metric.icon }} {{ metric.value }}</div>
      <div v-if="showActions" class="quick-actions" @click.stop>
        <button title="想看" :class="{ active: actName('wish') }" @click="emitAction('wish')">♡</button>
        <button title="看过" :class="{ active: actName('watched') }" @click="emitAction('watched')">✓</button>
        <button title="订阅" :class="{ active: media.subscribed }" @click="emitAction('subscribe')">🔔</button>
      </div>
    </div>
  </div>
</template>

<script>
import { posterUrl, ratingColor, POSTER_PLACEHOLDER, regionLabel } from '../utils'

export default {
  name: 'PosterCard',
  props: {
    media: { type: Object, required: true },
    rank: { type: Number, default: 0 },
    showActions: { type: Boolean, default: true },
    // 当前排序方式，用于在封面展示对应指标（hot/popular）
    sortBy: { type: String, default: '' }
  },
  computed: {
    poster() { return posterUrl(this.media) },
    scoreColor() { return ratingColor(this.media.ratingAvg) },
    year() {
      const d = this.media.releaseDate
      return d ? String(d).substring(0, 4) : ''
    },
    regionText() { return regionLabel(this.media.region) },
    metric() {
      if (this.sortBy === 'hot') {
        const v = Number(this.media.hotScore || 0)
        if (v <= 0) return null
        return { icon: '🔥', value: v.toFixed(1), label: '热度' }
      }
      if (this.sortBy === 'popular') {
        const v = Number(this.media.popularity || 0)
        if (v <= 0) return null
        return { icon: '📈', value: v.toFixed(1), label: '流行度' }
      }
      return null
    }
  },
  methods: {
    actName(name) {
      const a = this.media.myAction
      if (!a) return false
      if (name === 'wish') return a.wished === '1' || a.wished === 1
      if (name === 'watched') return a.watched === '1' || a.watched === 1
      return false
    },
    goDetail() {
      this.$router.push('/film/' + this.media.mediaId)
    },
    emitAction(type) {
      this.$emit('action', { type, media: this.media })
    },
    onImgError(e) { e.target.src = POSTER_PLACEHOLDER }
  }
}
</script>

<style scoped>
.poster-card { cursor: pointer; }
.poster-wrap {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 2 / 3;
  background: var(--bg-elevated);
  transition: transform .2s, box-shadow .2s;
}
.poster-card:hover .poster-wrap {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}
.poster-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.poster-mask {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  padding: 20px 10px 8px;
  background: linear-gradient(to top, rgba(0,0,0,.85), transparent);
  color: #fff;
}
.poster-mask .title {
  font-size: 14px; font-weight: 600;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.poster-mask .sub { font-size: 12px; opacity: .8; }
.score {
  position: absolute; top: 8px; right: 8px;
  background: rgba(0,0,0,.7); border-radius: 6px;
  padding: 2px 6px; font-size: 12px; font-weight: 700;
}
.metric {
  position: absolute; top: 8px; left: 8px; z-index: 2;
  background: rgba(0,0,0,.7); color: #fff; border-radius: 6px;
  padding: 2px 6px; font-size: 11px; font-weight: 600;
}
/* 有排名徽标时，指标下移避免与徽标重叠 */
.rank-badge + img + .poster-mask + .score + .metric,
.rank-badge ~ .metric { top: 34px; }
.rank-badge {
  position: absolute; top: 6px; left: 6px; z-index: 2;
  min-width: 22px; height: 22px; line-height: 22px; text-align: center;
  background: var(--brand); color: #1a1200; font-weight: 700;
  border-radius: 6px; font-size: 13px; padding: 0 4px;
}
.quick-actions {
  position: absolute; top: 8px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 6px; opacity: 0; transition: opacity .2s;
}
.poster-card:hover .quick-actions { opacity: 1; }
.quick-actions button {
  width: 30px; height: 30px; border-radius: 50%;
  border: none; cursor: pointer;
  background: rgba(0,0,0,.65); color: #fff; font-size: 14px;
}
.quick-actions button.active { background: var(--brand); color: #1a1200; }
.quick-actions button:hover { background: var(--brand-hover); color: #1a1200; }
</style>
