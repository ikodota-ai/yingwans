<template>
  <section class="browse-by">
    <ul class="bar-nav">
      <li v-for="menu in menus" :key="menu.key" class="smenu-wrapper" v-click-outside="() => close(menu.key)">
        <div class="smenu" :class="{ active: isMenuActive(menu) }" @click="toggle(menu.key)">
          <label>{{ labelOf(menu) }}<i class="arrow" :class="{ open: openKey === menu.key }"></i></label>
        </div>
        <div v-if="openKey === menu.key && menu.key === 'rating'" class="dropdown dropdown--rating">
          <div class="dd-item dd-item--all" :class="{ sel: value.minRating == null || value.minRating === '' }" @click="pickRating(null, null)">全部</div>
          <div class="rating-cols">
            <div v-for="col in ratingSources" :key="col.value" class="rating-col">
              <div class="rating-col__head">{{ col.label }}</div>
              <div
                v-for="r in ratingThresholds"
                :key="col.value + '-' + r"
                class="dd-item"
                :class="{ sel: isRatingSel(col.value, r) }"
                @click="pickRating(col.value, r)"
              >{{ r }}+</div>
            </div>
          </div>
        </div>
        <div v-else-if="openKey === menu.key" class="dropdown" :class="{ 'dropdown--cols2': menu.cols === 2 }">
          <div class="dd-item dd-item--all" :class="{ sel: value[menu.key] == null || value[menu.key] === '' }" @click="pick(menu.key, null)">全部</div>
          <div
            v-for="opt in menu.options"
            :key="String(opt.value)"
            class="dd-item"
            :class="{ sel: String(value[menu.key]) === String(opt.value), 'dd-item--provider': menu.key === 'platform' }"
            @click="pick(menu.key, opt.value)"
          >
            <img v-if="menu.key === 'platform' && opt.logo" class="dd-logo" :src="opt.logo" :alt="opt.label" loading="lazy" />
            <span>{{ opt.label }}</span>
          </div>
        </div>
      </li>
      <li v-if="hasActiveFilter" class="clear-li">
        <span class="clear-btn" @click="clearAll">清除筛选 ✕</span>
      </li>
    </ul>
  </section>
</template>

<script>
const clickOutside = {
  bind(el, binding) {
    el._handler = (e) => { if (!el.contains(e.target)) binding.value() }
    document.addEventListener('click', el._handler)
  },
  unbind(el) { document.removeEventListener('click', el._handler) }
}

export default {
  name: 'FilterBar',
  directives: { clickOutside },
  props: {
    // 当前筛选值对象（v-model）
    value: { type: Object, required: true },
    // 可选项：{ years, platforms, genres, mediaTypes }
    opts: { type: Object, default: () => ({ years: [], platforms: [], genres: [], mediaTypes: [], providers: [], offerTypes: [], countries: [] }) }
  },
  data() {
    return {
      openKey: '',
      ratingThresholds: [9, 8, 7, 6, 5],
      ratingSources: [
        { value: 'tmdb', label: 'TMDB' },
        { value: 'site', label: '站内' },
        { value: 'combined', label: '综合' }
      ]
    }
  },
  computed: {
    menus() {
      return [
        { key: 'year', name: '年份', options: (this.opts.years || []).map(y => ({ label: y + ' 年', value: y })) },
        { key: 'region', name: '国家/地区', options: (this.opts.countries || []).map(c => ({ label: c.label, value: c.value })) },
        { key: 'rating', name: '评分', options: [] },
        { key: 'genres', name: '类别', cols: 2, options: (this.opts.genres || []).map(g => ({ label: g, value: g })) },
        { key: 'offerType', name: '观看方式', options: (this.opts.offerTypes || []).map(o => ({ label: o.label, value: o.value })) },
        { key: 'mediaType', name: '类型', options: [{ label: '电影', value: 'movie' }, { label: '剧集', value: 'tv' }] },
        { key: 'phase', name: '状态', options: [
          { label: '即将上线', value: 'upcoming' },
          { label: '近期上线', value: 'airing' },
          { label: '已完结/经典', value: 'ended' }
        ] },
        { key: 'sortBy', name: '排序', options: [
          { label: '最热', value: 'hot' }, { label: '评分最高', value: 'rating' },
          { label: '最流行', value: 'popular' }, { label: '最新上映', value: 'recent' },
          { label: '最新收录', value: 'newest' }
        ] }
      ]
    },
    platformOptions() {
      const providers = this.opts.providers || []
      if (providers.length) {
        return providers.map(p => ({ label: p.label, value: p.key, logo: p.logo }))
      }
      return (this.opts.platforms || []).map(p => ({ label: p, value: p }))
    },
    hasActiveFilter() {
      return Object.keys(this.value).some(k => this.value[k] != null && this.value[k] !== '')
    }
  },
  methods: {
    labelOf(menu) {
      if (menu.key === 'rating') {
        const r = this.value.minRating
        if (r == null || r === '') return menu.name
        const src = this.ratingSources.find(s => s.value === (this.value.ratingSource || 'combined'))
        return (src ? src.label : '综合') + ' ' + r + '+'
      }
      const v = this.value[menu.key]
      if (v == null || v === '') return menu.name
      const opt = menu.options.find(o => String(o.value) === String(v))
      return opt ? opt.label : menu.name
    },
    isMenuActive(menu) {
      if (menu.key === 'rating') { return this.value.minRating != null && this.value.minRating !== '' }
      return this.value[menu.key] != null && this.value[menu.key] !== ''
    },
    isRatingSel(src, r) {
      return String(this.value.minRating) === String(r) && (this.value.ratingSource || 'combined') === src
    },
    pickRating(src, r) {
      const next = Object.assign({}, this.value, { minRating: r, ratingSource: r == null ? null : src })
      this.openKey = ''
      this.$emit('input', next)
      this.$emit('change', next)
    },
    toggle(key) { this.openKey = this.openKey === key ? '' : key },
    close(key) { if (this.openKey === key) this.openKey = '' },
    pick(key, val) {
      const next = Object.assign({}, this.value, { [key]: val })
      this.openKey = ''
      this.$emit('input', next)
      this.$emit('change', next)
    },
    clearAll() {
      const next = {}
      Object.keys(this.value).forEach(k => { next[k] = null })
      this.$emit('input', next)
      this.$emit('change', next)
    }
  }
}
</script>

<style scoped>
.browse-by { margin: 8px 0 24px; }
.bar-nav {
  list-style: none; display: flex; flex-wrap: wrap; gap: 10px; padding: 0; margin: 0;
}
.smenu-wrapper { position: relative; }
.smenu {
  cursor: pointer; user-select: none;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 20px; padding: 8px 16px; transition: border-color .2s, color .2s;
}
.smenu:hover { border-color: var(--brand); }
.smenu.active { border-color: var(--brand); color: var(--brand); }
.smenu label { cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: 14px; }
.arrow {
  display: inline-block; width: 0; height: 0;
  border-left: 4px solid transparent; border-right: 4px solid transparent;
  border-top: 5px solid currentColor; transition: transform .2s;
}
.arrow.open { transform: rotate(180deg); }
.dropdown {
  position: absolute; z-index: 30; top: calc(100% + 6px); left: 0; min-width: 150px;
  max-height: 300px; overflow-y: auto;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 10px; box-shadow: var(--shadow-hover); padding: 6px;
}
.dd-item {
  padding: 8px 12px; border-radius: 6px; cursor: pointer; font-size: 14px;
  white-space: nowrap;
}
.dropdown--cols2 { min-width: 260px; display: grid; grid-template-columns: 1fr 1fr; gap: 2px; }
.dropdown--cols2 .dd-item--all { grid-column: 1 / -1; }
.dropdown--rating { min-width: 240px; }
.rating-cols { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; margin-top: 4px; }
.rating-col__head {
  font-size: 12px; color: var(--text-secondary); text-align: center;
  padding: 4px 0 6px; border-bottom: 1px solid var(--border); margin-bottom: 4px;
}
.rating-col .dd-item { text-align: center; padding: 6px 4px; }
.dd-item:hover { background: var(--bg-elevated); }
.dd-item--provider { display: flex; align-items: center; gap: 10px; }
.dd-logo { width: 26px; height: 26px; border-radius: 6px; object-fit: cover; flex: 0 0 auto; }
.dd-item.sel { color: var(--brand); font-weight: 600; }
.clear-li { display: flex; align-items: center; }
.clear-btn { cursor: pointer; font-size: 13px; color: var(--text-secondary); }
.clear-btn:hover { color: var(--brand); }
</style>
