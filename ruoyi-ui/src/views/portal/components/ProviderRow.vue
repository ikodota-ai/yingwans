<template>
  <section v-if="providers && providers.length" class="provider-row">
    <div class="pr-list">
      <button
        v-for="p in shownProviders"
        :key="p.key"
        class="pr-item"
        :class="{ active: isSelected(p.key) }"
        :title="p.label"
        @click="toggle(p.key)"
      >
        <span class="pr-thumb">
          <img v-if="p.logo" class="pr-logo" :src="logoUrl(p.logo)" :alt="p.label" :title="p.label" loading="lazy" />
          <span v-else class="pr-fallback">{{ p.label.charAt(0) }}</span>
          <i v-if="isSelected(p.key)" class="pr-check">✓</i>
        </span>
        <span class="pr-label">{{ p.label }}</span>
      </button>
      <button v-if="providers.length > limit" class="pr-toggle" @click="expanded = !expanded">
        {{ expanded ? '收起' : ('更多 +' + (providers.length - limit)) }}
      </button>
    </div>
    <span v-if="selectedKeys.length" class="pr-clear" @click="clear">清除 ✕</span>
  </section>
</template>

<script>
import { providerLogoUrl } from '../utils'

export default {
  name: 'ProviderRow',
  props: {
    // 逗号分隔的品牌 key 字符串（v-model）
    value: { type: String, default: '' },
    providers: { type: Array, default: () => [] },
    limit: { type: Number, default: 14 }
  },
  data() {
    return { expanded: false }
  },
  computed: {
    selectedKeys() {
      return (this.value || '').split(',').map(s => s.trim()).filter(Boolean)
    },
    shownProviders() {
      if (this.expanded) return this.providers
      return this.providers.slice(0, this.limit)
    }
  },
  methods: {
    logoUrl(logo) { return providerLogoUrl(logo) },
    isSelected(key) { return this.selectedKeys.indexOf(key) !== -1 },
    toggle(key) {
      const keys = this.selectedKeys.slice()
      const idx = keys.indexOf(key)
      if (idx === -1) { keys.push(key) } else { keys.splice(idx, 1) }
      const next = keys.join(',')
      this.$emit('input', next)
      this.$emit('change', next)
    },
    clear() {
      this.$emit('input', '')
      this.$emit('change', '')
    }
  }
}
</script>

<style scoped>
.provider-row { display: flex; align-items: flex-start; gap: 12px; margin: 4px 0 16px; }
.pr-list { display: flex; flex-wrap: wrap; gap: 12px 14px; flex: 1; }
.pr-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  width: 62px; padding: 0; border: none; background: transparent; cursor: pointer;
}
.pr-thumb {
  position: relative; width: 48px; height: 48px; border-radius: 12px;
  border: 2px solid transparent; background: var(--bg-elevated); overflow: hidden;
  transition: border-color .2s, transform .12s, opacity .2s; opacity: .7;
}
.pr-item:hover .pr-thumb { transform: translateY(-2px); opacity: 1; }
.pr-item.active .pr-thumb { border-color: var(--brand); opacity: 1; box-shadow: var(--shadow-hover); }
.pr-logo { width: 100%; height: 100%; object-fit: cover; display: block; }
.pr-fallback {
  display: flex; align-items: center; justify-content: center; width: 100%; height: 100%;
  font-weight: 700; color: var(--text-secondary);
}
.pr-check {
  position: absolute; right: 2px; bottom: 2px; width: 16px; height: 16px;
  background: var(--brand); color: #fff; border-radius: 50%;
  font-size: 11px; line-height: 16px; text-align: center; font-style: normal;
}
.pr-label {
  font-size: 11px; line-height: 1.2; color: var(--text-secondary); text-align: center;
  max-width: 62px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.pr-item.active .pr-label { color: var(--brand); }
.pr-toggle {
  align-self: center; background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 20px; padding: 6px 14px; cursor: pointer; font-size: 13px;
  color: var(--text-secondary); height: 34px;
}
.pr-toggle:hover { border-color: var(--brand); color: var(--brand); }
.pr-clear { cursor: pointer; font-size: 13px; color: var(--text-secondary); white-space: nowrap; padding-top: 14px; }
.pr-clear:hover { color: var(--brand); }
</style>
