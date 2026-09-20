// 无限滚动：IntersectionObserver 触底自动加载 + 按钮兜底
// 使用方：
//  - data 中定义 loading/list/pageNum/pageSize/total
//  - 实现 fetchPage() 返回 Promise，内部使用 this.pageNum 请求并 concat 到 this.list、更新 this.total
//  - 模板中放置 <div ref="sentinel"></div> 作为触底哨兵
//  - 触发首屏用 this.resetAndFetch()
export default {
  data() {
    return {
      loading: false,
      list: [],
      pageNum: 1,
      pageSize: 24,
      total: 0,
      _io: null
    }
  },
  computed: {
    hasMore() { return this.list.length < this.total }
  },
  mounted() {
    this.$nextTick(this.setupObserver)
  },
  beforeDestroy() {
    if (this._io) { this._io.disconnect(); this._io = null }
  },
  methods: {
    setupObserver() {
      const el = this.$refs.sentinel
      if (!el || typeof IntersectionObserver === 'undefined') return
      if (this._io) { this._io.disconnect() }
      this._io = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && this.hasMore && !this.loading) {
          this.loadMore()
        }
      }, { rootMargin: '300px' })
      this._io.observe(el)
    },
    resetAndFetch() {
      this.pageNum = 1
      this.list = []
      this.total = 0
      return this.fetchWrap()
    },
    loadMore() {
      if (this.loading || !this.hasMore) return
      this.pageNum += 1
      this.fetchWrap()
    },
    fetchWrap() {
      this.loading = true
      return Promise.resolve(this.fetchPage()).finally(() => {
        this.loading = false
        this.$nextTick(this.setupObserver)
      })
    }
  }
}
