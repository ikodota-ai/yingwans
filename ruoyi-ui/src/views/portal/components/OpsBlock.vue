<template>
  <div class="ops-block">
    <div v-if="block.title" class="ops-title">{{ block.title }}</div>
    <img v-if="block.imageUrl" :src="block.imageUrl" class="ops-img" @click="open" />
    <div v-if="block.content" class="ops-content" v-text="block.content"></div>
    <a v-if="block.link" :href="block.link" target="_blank" class="ops-link">了解更多 ›</a>
  </div>
</template>

<script>
export default {
  name: 'OpsBlock',
  props: { block: { type: Object, required: true } },
  methods: {
    open() { if (this.block.link) window.open(this.block.link, '_blank') }
  }
}
</script>

<style scoped>
.ops-block {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 10px; padding: 16px; text-align: center;
  min-width: 200px; flex: 1;
}
.ops-title { font-weight: 600; margin-bottom: 10px; }
.ops-img { max-width: 160px; border-radius: 8px; cursor: pointer; }
.ops-content { font-size: 13px; color: var(--text-secondary); margin-top: 8px; white-space: pre-wrap; }
.ops-link { display: inline-block; margin-top: 8px; font-size: 13px; color: var(--brand); }
</style>
