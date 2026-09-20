<template>
  <div class="collection-card" @click="go">
    <div class="cover" :class="'layout-' + layout">
      <template v-if="collection.coverUrl">
        <img class="single" :src="collection.coverUrl" loading="lazy" @error="onErr" />
      </template>
      <template v-else>
        <div class="tiles" :class="'tiles-' + layout">
          <div v-for="(p, i) in tiles" :key="i" class="tile">
            <img :src="p" loading="lazy" @error="onErr" />
          </div>
        </div>
      </template>
      <div class="cover-shade"></div>
      <span class="source-tag">{{ sourceText }}</span>
      <span class="count-badge">{{ collection.itemCount || 0 }} 部</span>
    </div>
    <div class="meta">
      <div class="name">{{ collection.name }}</div>
      <div class="desc" v-if="collection.description">{{ collection.description }}</div>
    </div>
  </div>
</template>

<script>
import { sourceLabel, posterFromPath, POSTER_PLACEHOLDER } from '../utils'

export default {
  name: 'CollectionCard',
  props: { collection: { type: Object, required: true } },
  computed: {
    sourceText() { return sourceLabel(this.collection.source) },
    posters() {
      return (this.collection.coverPosters || [])
        .filter(Boolean)
        .map(p => posterFromPath(p))
    },
    layout() {
      const n = this.posters.length
      if (n <= 1) return 1
      if (n === 2) return 2
      if (n === 3) return 3
      return 4
    },
    tiles() {
      const arr = this.posters.slice(0, this.layout)
      while (arr.length < this.layout) arr.push(POSTER_PLACEHOLDER)
      return arr
    }
  },
  methods: {
    go() { this.$router.push('/collection/' + this.collection.collectionId) },
    onErr(e) { e.target.src = POSTER_PLACEHOLDER }
  }
}
</script>

<style scoped>
.collection-card { cursor: pointer; }
.cover {
  position: relative; border-radius: 12px; overflow: hidden;
  aspect-ratio: 3 / 2; background: var(--bg-elevated);
  transition: transform .2s, box-shadow .2s;
}
.collection-card:hover .cover { transform: translateY(-4px); box-shadow: var(--shadow-hover); }
.cover .single { width: 100%; height: 100%; object-fit: cover; }
.cover-shade {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,.55) 0%, rgba(0,0,0,0) 45%);
  pointer-events: none;
}
.tiles { display: grid; height: 100%; gap: 2px; }
.tile { overflow: hidden; }
.tile img { width: 100%; height: 100%; object-fit: cover; display: block; }
/* 2 张：左右并排 */
.tiles-2 { grid-template-columns: 1fr 1fr; }
/* 3 张：左侧大图 + 右侧两张堆叠 */
.tiles-3 {
  grid-template-columns: 1.4fr 1fr;
  grid-template-rows: 1fr 1fr;
}
.tiles-3 .tile:nth-child(1) { grid-row: 1 / span 2; }
/* 4 张：2x2 */
.tiles-4 { grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; }
.source-tag {
  position: absolute; top: 8px; left: 8px;
  background: rgba(0,0,0,.7); color: var(--brand);
  font-size: 11px; padding: 2px 8px; border-radius: 4px;
}
.count-badge {
  position: absolute; bottom: 8px; right: 8px;
  background: rgba(0,0,0,.6); color: #fff;
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
}
.meta { margin-top: 8px; }
.name { font-size: 15px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.desc {
  font-size: 12px; color: var(--text-secondary); margin-top: 2px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
