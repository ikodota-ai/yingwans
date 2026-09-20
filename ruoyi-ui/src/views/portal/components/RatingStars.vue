<template>
  <div class="rating-stars" :class="{ readonly }">
    <span
      v-for="n in 5"
      :key="n"
      class="star"
      @mousemove="!readonly && onHover($event, n)"
      @mouseleave="!readonly && (hoverVal = 0)"
      @click="!readonly && onClick($event, n)"
    >
      <span class="star-bg">★</span>
      <span class="star-fill" :style="{ width: fillWidth(n) }">★</span>
    </span>
    <span v-if="!readonly && current > 0" class="clear" @click="clear">清除</span>
    <span v-if="showValue && current > 0" class="value">{{ current.toFixed(1) }}</span>
  </div>
</template>

<script>
export default {
  name: 'RatingStars',
  props: {
    // 0-10 分制，展示为 5 星（每星 2 分）
    value: { type: Number, default: 0 },
    readonly: { type: Boolean, default: false },
    showValue: { type: Boolean, default: true }
  },
  data() {
    return { hoverVal: 0 }
  },
  computed: {
    current() { return this.hoverVal || this.value || 0 }
  },
  methods: {
    fillWidth(n) {
      const starScore = this.current / 2 // 0-5
      const filled = Math.min(1, Math.max(0, starScore - (n - 1)))
      return (filled * 100) + '%'
    },
    onHover(e, n) {
      const half = this.isHalf(e)
      this.hoverVal = (n - 1 + (half ? 0.5 : 1)) * 2
    },
    onClick(e, n) {
      const half = this.isHalf(e)
      const v = (n - 1 + (half ? 0.5 : 1)) * 2
      this.$emit('input', v)
      this.$emit('change', v)
    },
    isHalf(e) {
      const rect = e.target.getBoundingClientRect()
      return (e.clientX - rect.left) < rect.width / 2
    },
    clear() {
      this.$emit('input', 0)
      this.$emit('change', null)
    }
  }
}
</script>

<style scoped>
.rating-stars { display: inline-flex; align-items: center; gap: 2px; }
.star { position: relative; font-size: 22px; line-height: 1; cursor: pointer; }
.readonly .star { cursor: default; }
.star-bg { color: var(--border); }
.star-fill {
  position: absolute; left: 0; top: 0; overflow: hidden;
  color: var(--brand); white-space: nowrap;
}
.clear, .value { margin-left: 8px; font-size: 12px; color: var(--text-secondary); cursor: pointer; }
.value { font-weight: 700; color: var(--brand); }
</style>
