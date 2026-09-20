<template>
  <nav class="pt-breadcrumb" aria-label="breadcrumb">
    <template v-for="(item, i) in items">
      <router-link
        v-if="item.to && i < items.length - 1"
        :key="'l' + i"
        :to="item.to"
        class="crumb link"
      >{{ item.text }}</router-link>
      <span v-else :key="'s' + i" class="crumb current">{{ item.text }}</span>
      <span v-if="i < items.length - 1" :key="'sep' + i" class="sep">›</span>
    </template>
  </nav>
</template>

<script>
export default {
  name: 'PortalBreadcrumb',
  props: {
    // [{ text: '片单', to: '/collections' }, { text: '片单名' }]
    items: { type: Array, default: () => [] }
  }
}
</script>

<style scoped>
.pt-breadcrumb {
  display: flex; align-items: center; flex-wrap: wrap;
  gap: 6px; margin: 18px 0 6px; font-size: 13px;
}
.crumb { color: var(--text-secondary); }
.crumb.link { cursor: pointer; text-decoration: none; }
.crumb.link:hover { color: var(--brand); }
.crumb.current { color: var(--text-primary); font-weight: 600; }
.sep { color: var(--text-muted); }
</style>
