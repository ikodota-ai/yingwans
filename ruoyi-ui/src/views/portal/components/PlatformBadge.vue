<template>
  <div class="platform-badge" :class="state.cls">
    <img v-if="release.platformLogo" :src="logoUrl(release.platformLogo)" class="logo" :title="release.platform" @error="hideLogo" />
    <div class="info">
      <div class="line1">
        <span class="platform">{{ release.platform }}</span>
        <span v-if="countryText" class="cc">{{ countryText }}</span>
        <span v-if="offer" class="offer">{{ offer }}</span>
      </div>
      <div class="line2">
        <span class="state-dot"></span>
        <span>{{ state.text }}</span>
        <span v-if="release.onlineDate" class="date">{{ dateText }}</span>
      </div>
    </div>
    <a v-if="release.link" :href="release.link" target="_blank" class="go" @click.stop>去观看 ›</a>
  </div>
</template>

<script>
import { releaseState, providerLogoUrl, regionLabel } from '../utils'

export default {
  name: 'PlatformBadge',
  props: { release: { type: Object, required: true } },
  computed: {
    state() { return releaseState(this.release.onlineDate) },
    offer() {
      if (this.release.source === 'custom') return '在线'
      return { flatrate: '会员', rent: '租', buy: '购', free: '免费', ads: '广告' }[this.release.offerType] || this.release.offerType
    },
    dateText() { return String(this.release.onlineDate).substring(0, 10) },
    countryText() { return regionLabel(this.release.country) }
  },
  methods: {
    logoUrl(logo) { return providerLogoUrl(logo) },
    hideLogo(e) { e.target.style.display = 'none' }
  }
}
</script>

<style scoped>
.platform-badge {
  display: flex; align-items: center; gap: 12px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 10px; padding: 10px 14px;
}
.logo { width: 36px; height: 36px; border-radius: 8px; object-fit: cover; }
.info { flex: 1; }
.line1 { display: flex; align-items: center; gap: 8px; }
.platform { font-weight: 600; }
.offer { font-size: 12px; padding: 1px 6px; border-radius: 4px; background: var(--bg-elevated); color: var(--text-secondary); }
.cc { font-size: 11px; padding: 1px 6px; border-radius: 4px; border: 1px solid var(--border); color: var(--text-muted); }
.line2 { font-size: 12px; color: var(--text-secondary); margin-top: 2px; display: flex; align-items: center; gap: 6px; }
.date { color: var(--text-muted); }
.state-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-muted); }
.online .state-dot { background: var(--success); }
.upcoming .state-dot { background: var(--warning); }
.go { font-size: 13px; color: var(--brand); white-space: nowrap; }
</style>
