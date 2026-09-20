<template>
  <div v-if="blocks && blocks.length" class="ops-dock">
    <div
      v-for="op in blocks"
      :key="op.blockId"
      class="ops-dock__item"
    >
      <!-- 收起态：竖条 -->
      <div class="ops-dock__tab">
        <span class="ops-dock__tab-text">{{ tabText(op) }}</span>
      </div>
      <!-- 展开态：悬浮卡片 -->
      <div class="ops-dock__panel">
        <div v-if="op.title" class="ops-dock__title">{{ op.title }}</div>
        <img v-if="op.imageUrl" :src="imgUrl(op.imageUrl)" class="ops-dock__img" @click="open(op)" />
        <div v-if="op.content" class="ops-dock__content" v-text="op.content"></div>
        <a v-if="op.link" class="ops-dock__link" @click="open(op)">了解更多 ›</a>
      </div>
    </div>
  </div>
</template>

<script>
import { providerLogoUrl } from '../utils'

export default {
  name: 'OpsDock',
  props: { blocks: { type: Array, default: () => [] } },
  methods: {
    imgUrl(url) { return providerLogoUrl(url) },
    tabText(op) {
      const t = (op.title || '推荐').trim()
      return t.length > 5 ? t.slice(0, 5) : t
    },
    open(op) {
      if (op.link) {
        if (op.link.startsWith('/')) { this.$router.push(op.link) }
        else { window.open(op.link, '_blank') }
      }
    }
  }
}
</script>

<style scoped>
.ops-dock {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 900;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ops-dock__item { position: relative; }

/* 竖条：默认可见 */
.ops-dock__tab {
  width: 34px;
  min-height: 96px;
  padding: 12px 6px;
  background: var(--brand);
  color: #1a1200;
  border-radius: 10px 0 0 10px;
  box-shadow: var(--shadow-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity .2s, transform .2s;
}
.ops-dock__tab-text {
  writing-mode: vertical-rl;
  letter-spacing: 3px;
  font-size: 13px;
  font-weight: 700;
}

/* 悬浮卡片：默认隐藏，hover 时滑出 */
.ops-dock__panel {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translate(12px, -50%) scale(.96);
  transform-origin: right center;
  width: 200px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-hover);
  padding: 16px;
  text-align: center;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: opacity .2s, transform .2s, visibility .2s;
}
.ops-dock__item:hover .ops-dock__panel {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transform: translate(0, -50%) scale(1);
}
.ops-dock__item:hover .ops-dock__tab {
  opacity: 0;
  transform: translateX(8px);
}
.ops-dock__title { font-weight: 600; margin-bottom: 10px; }
.ops-dock__img { max-width: 150px; border-radius: 8px; cursor: pointer; }
.ops-dock__content { font-size: 13px; color: var(--text-secondary); margin-top: 8px; white-space: pre-wrap; }
.ops-dock__link { display: inline-block; margin-top: 8px; font-size: 13px; color: var(--brand); cursor: pointer; }

@media (max-width: 768px) {
  .ops-dock { display: none; }
}
</style>
