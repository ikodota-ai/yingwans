<template>
  <div v-if="media" class="film-detail">
    <div class="backdrop" :style="backdropStyle"></div>
    <div class="portal-container detail-body">
      <div class="poster-col">
        <img :src="poster" class="poster" @error="onErr" />
      </div>
      <div class="info-col">
        <h1>{{ media.title }}</h1>
        <div class="original" v-if="media.originalTitle && media.originalTitle !== media.title">{{ media.originalTitle }}</div>
        <div class="meta-row">
          <span v-if="year" class="pill">{{ year }}</span>
          <span v-if="regionText" class="pill">{{ regionText }}</span>
          <span v-if="media.runtime" class="pill">{{ media.runtime }} 分钟</span>
          <span v-for="g in genreList" :key="g" class="pill">{{ g }}</span>
        </div>

        <div class="scores">
          <div class="score-item">
            <div class="score-num" :style="{ color: scoreColor(media.ratingAvg) }">
              {{ media.ratingAvg > 0 ? Number(media.ratingAvg).toFixed(1) : '—' }}
            </div>
            <div class="score-label">站内 · {{ media.ratingCount || 0 }} 人</div>
          </div>
          <div class="score-item">
            <div class="score-num" :style="{ color: scoreColor(media.voteAverage) }">
              {{ media.voteAverage > 0 ? Number(media.voteAverage).toFixed(1) : '—' }}
            </div>
            <div class="score-label">TMDB</div>
          </div>
        </div>

        <p class="overview">{{ media.overview || '暂无简介' }}</p>

        <!-- 行为条 -->
        <div class="action-bar">
          <button :class="{ active: isAct('like') }" @click="act('like')"><span>👍</span>点赞 {{ media.likeCount || 0 }}</button>
          <button :class="{ active: isAct('wish') }" @click="act('wish')"><span>♡</span>想看</button>
          <button :class="{ active: isAct('watched') }" @click="act('watched')"><span>✓</span>看过</button>
          <button :class="{ active: media.subscribed }" @click="act('subscribe')"><span>🔔</span>{{ media.subscribed ? '已订阅' : '订阅' }}</button>
          <button @click="openCollectionDialog"><span>＋</span>加入片单</button>
        </div>

        <div class="rate-row">
          <span class="rate-label">我的评分</span>
          <rating-stars v-model="myRating" @change="onRate" />
        </div>

        <!-- 上线平台（分组：在线观看 / 租赁购买 / 即将上线） -->
        <div v-for="g in releaseGroups" :key="g.key" class="releases">
          <div class="section-title"><span>{{ g.title }}</span></div>
          <div class="release-list">
            <platform-badge v-for="r in g.items" :key="r.releaseId" :release="r" />
          </div>
        </div>

        <!-- 下载资源不再公开：订阅后通过站内通知发送文档链接 -->
        <div class="releases">
          <div class="section-title"><span>📥 下载资源</span></div>
          <div class="doc-sub-tip">
            <template v-if="media.subscribed">已订阅：资源链接将通过站内通知发送，请留意右上角通知</template>
            <template v-else>登录并订阅本片后，资源链接将通过站内通知发送给你</template>
          </div>
        </div>
      </div>
    </div>

    <!-- 演职人员 -->
    <div v-if="castList.length" class="portal-container cast-section">
      <div class="section-title"><span>🎭 演职人员</span></div>
      <div class="cast-rail">
        <div v-for="c in castList" :key="c.id" class="cast-card" @click="goPerson(c.personId)">
          <div class="cast-avatar"><img :src="avatar(c)" @error="onAvatarErr" /></div>
          <div class="cast-name">{{ c.personName }}</div>
          <div class="cast-role">{{ c.creditType === 'cast' ? (c.character || '演员') : (jobLabel(c.job) || '剧组') }}</div>
        </div>
      </div>
    </div>

    <!-- 详情底部运营位 -->
    <div v-if="footerOps.length" class="portal-container ops-footer">
      <ops-block v-for="op in footerOps" :key="op.blockId" :block="op" />
    </div>

    <!-- 加入片单弹窗 -->
    <el-dialog title="加入我的片单" :visible.sync="collectionDialog" width="420px" append-to-body>
      <div v-if="myCollectionList.length" class="col-list">
        <div v-for="c in myCollectionList" :key="c.collectionId" class="col-item" @click="addTo(c)">
          <span>{{ c.name }}</span><span class="count">{{ c.itemCount || 0 }} 部</span>
        </div>
      </div>
      <el-empty v-else description="还没有片单，去我的片库创建" :image-size="80" />
    </el-dialog>
  </div>
  <div v-else class="empty-state">加载中…</div>
</template>

<script>
import { getMedia } from '@/api/kemovie/portal'
import { rate, myCollections, addToCollection } from '@/api/kemovie/me'
import RatingStars from './components/RatingStars.vue'
import PlatformBadge from './components/PlatformBadge.vue'
import OpsBlock from './components/OpsBlock.vue'
import { posterUrl, backdropUrl, ratingColor, POSTER_PLACEHOLDER, profileUrl, AVATAR_PLACEHOLDER, regionLabel } from './utils'
import actionMixin from './mixins/actionMixin'

export default {
  name: 'PortalFilmDetail',
  components: { RatingStars, PlatformBadge, OpsBlock },
  mixins: [actionMixin],
  data() {
    return {
      media: null, footerOps: [], myRating: 0,
      collectionDialog: false, myCollectionList: []
    }
  },
  computed: {
    poster() { return posterUrl(this.media, 'w500') },
    backdropStyle() {
      const bg = backdropUrl(this.media)
      return bg ? { backgroundImage: `linear-gradient(to bottom, rgba(0,0,0,.3), var(--bg-base)), url(${bg})` } : {}
    },
    year() { return this.media && this.media.releaseDate ? String(this.media.releaseDate).substring(0, 4) : '' },
    regionText() { return this.media ? regionLabel(this.media.region) : '' },
    genreList() { return this.media && this.media.genres ? this.media.genres.split(/[,，、]/).filter(Boolean) : [] },
    castList() { return (this.media && this.media.cast) ? this.media.cast : [] },
    releaseGroups() {
      const rs = (this.media && this.media.releases) || []
      const watch = [], pay = [], upcoming = []
      for (const r of rs) {
        if (r.status === 'upcoming' || r.platform === '待定') { upcoming.push(r) }
        else if (r.offerType === 'rent' || r.offerType === 'buy') { pay.push(r) }
        else { watch.push(r) }
      }
      return [
        { key: 'watch', title: '📺 在线观看', items: watch },
        { key: 'pay', title: '💳 租赁 / 购买', items: pay },
        { key: 'upcoming', title: '🕒 即将上线', items: upcoming }
      ].filter(g => g.items.length)
    }
  },
  watch: {
    '$route.params.mediaId'() { this.load() }
  },
  created() { this.load() },
  methods: {
    scoreColor(s) { return ratingColor(s) },
    onErr(e) { e.target.src = POSTER_PLACEHOLDER },
    avatar(c) { return profileUrl(c) },
    onAvatarErr(e) { e.target.src = AVATAR_PLACEHOLDER },
    goPerson(personId) { if (personId) this.$router.push('/person/' + personId) },
    jobLabel(job) {
      const m = { Director: '导演', Writer: '编剧', Screenplay: '编剧', Novel: '原著', Story: '故事', Creator: '主创', Producer: '制片', 'Executive Producer': '监制', 'Original Music Composer': '配乐' }
      return m[job] || job
    },
    load() {
      getMedia(this.$route.params.mediaId).then(res => {
        this.media = res.data
        this.footerOps = res.footerOps || []
        this.myRating = this.media.myAction && this.media.myAction.rating ? Number(this.media.myAction.rating) : 0
      })
    },
    isAct(key) {
      const a = this.media.myAction
      if (!a) return false
      const field = { like: 'liked', wish: 'wished', watched: 'watched' }[key] || key
      return a[field] === '1' || a[field] === 1
    },
    act(type) { this.handleAction({ type, media: this.media }) },
    onRate(val) {
      if (!this.ensureLogin()) return
      rate(this.media.mediaId, val).then(() => {
        this.$modal.msgSuccess(val ? '评分成功' : '已清除评分')
        this.load()
      })
    },
    openCollectionDialog() {
      if (!this.ensureLogin()) return
      myCollections().then(res => {
        this.myCollectionList = res.data || []
        this.collectionDialog = true
      })
    },
    addTo(c) {
      addToCollection(c.collectionId, this.media.mediaId).then(() => {
        this.$modal.msgSuccess('已加入「' + c.name + '」')
        this.collectionDialog = false
      })
    }
  }
}
</script>

<style scoped>
.film-detail { position: relative; }
.backdrop {
  position: absolute; top: 0; left: 0; right: 0; height: 380px;
  background-size: cover; background-position: center top; z-index: 0;
}
.detail-body { position: relative; z-index: 1; display: flex; gap: 32px; padding-top: 60px; }
.poster-col { flex: 0 0 260px; }
.poster { width: 260px; border-radius: 12px; box-shadow: var(--shadow-hover); }
.info-col { flex: 1; }
.info-col h1 { font-size: 30px; margin: 0 0 6px; }
.original { color: var(--text-secondary); margin-bottom: 12px; }
.meta-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 18px; }
.scores { display: flex; gap: 28px; margin-bottom: 18px; }
.score-num { font-size: 30px; font-weight: 700; }
.score-label { font-size: 12px; color: var(--text-secondary); }
.overview { line-height: 1.7; color: var(--text-secondary); margin-bottom: 22px; max-width: 700px; }
.action-bar { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 18px; }
.action-bar button {
  display: flex; align-items: center; gap: 6px;
  background: var(--bg-surface); border: 1px solid var(--border);
  color: var(--text-primary); border-radius: 10px; padding: 8px 16px; cursor: pointer;
  transition: all .2s;
}
.action-bar button span { font-size: 15px; }
.action-bar button:hover { border-color: var(--brand); }
.action-bar button.active { background: var(--brand); color: #1a1200; border-color: var(--brand); }
.rate-row { display: flex; align-items: center; gap: 14px; margin-bottom: 26px; }
.rate-label { color: var(--text-secondary); }
.release-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
@media (max-width: 720px) {
  .detail-body { flex-direction: column; }
  .poster-col { flex: none; }
  .poster { width: 160px; }
  .release-list { grid-template-columns: 1fr; }
}
.ops-footer { display: flex; gap: 16px; flex-wrap: wrap; margin-top: 40px; position: relative; z-index: 1; }
.col-list .col-item {
  display: flex; justify-content: space-between; padding: 12px 14px;
  border: 1px solid var(--border); border-radius: 8px; margin-bottom: 8px; cursor: pointer;
}
.col-list .col-item:hover { border-color: var(--brand); }
.col-item .count { color: var(--text-secondary); font-size: 12px; }

.cast-section { margin-top: 32px; }
.cast-rail { display: flex; gap: 16px; overflow-x: auto; padding-bottom: 10px; }
.cast-rail::-webkit-scrollbar { height: 6px; }
.cast-rail::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }
.cast-card { flex: 0 0 auto; width: 104px; cursor: pointer; text-align: center; }
.cast-avatar { width: 104px; height: 140px; border-radius: 10px; overflow: hidden; background: var(--bg-elevated); }
.cast-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .2s; }
.cast-card:hover .cast-avatar img { transform: scale(1.05); }
.cast-name { font-size: 13px; font-weight: 600; margin-top: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cast-role { font-size: 12px; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.doc-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
@media (max-width: 720px) { .doc-cards { grid-template-columns: 1fr; } }
.doc-card {
  display: flex; align-items: center; gap: 12px; text-decoration: none;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 10px; padding: 14px 16px; transition: border-color .2s;
}
.doc-card:hover { border-color: var(--brand); }
.doc-icon { font-size: 26px; }
.doc-info { flex: 1; display: flex; flex-direction: column; }
.doc-name { font-weight: 600; color: var(--text-primary); }
.doc-desc { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.doc-go { font-size: 13px; color: var(--brand); white-space: nowrap; }
</style>
