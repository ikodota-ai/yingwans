<template>
  <div v-if="person" class="person-page">
    <div class="portal-container detail-body">
      <div class="avatar-col">
        <img :src="avatar" class="avatar" @error="onErr" />
      </div>
      <div class="info-col">
        <h1>{{ person.name }}</h1>
        <div class="original" v-if="person.originalName && person.originalName !== person.name">{{ person.originalName }}</div>
        <div class="meta-row">
          <span v-if="person.knownFor" class="pill">{{ knownForLabel(person.knownFor) }}</span>
          <span class="pill">{{ genderText(person.gender) }}</span>
          <span v-if="person.birthday" class="pill">{{ person.birthday }}<template v-if="age"> · {{ age }}岁</template></span>
          <span v-if="person.placeOfBirth" class="pill">{{ person.placeOfBirth }}</span>
        </div>
        <p class="bio">{{ person.biography || '暂无人物简介' }}</p>
      </div>
    </div>

    <div class="portal-container works">
      <div class="section-title"><span>🎬 参演作品（{{ credits.length }}）</span></div>
      <div v-if="credits.length" class="poster-grid">
        <div v-for="c in credits" :key="c.id" class="work-card" @click="goFilm(c.mediaId)">
          <div class="poster-wrap"><img :src="poster(c)" @error="onPosterErr" /></div>
          <div class="w-title">{{ c.mediaTitle }}</div>
          <div class="w-role">{{ c.creditType === 'cast' ? (c.character || '演员') : (jobLabel(c.job) || '剧组') }}</div>
        </div>
      </div>
      <div v-else class="empty-state">暂无站内收录的参演作品</div>
    </div>
  </div>
  <div v-else class="empty-state">加载中…</div>
</template>

<script>
import { getPerson } from '@/api/kemovie/portal'
import { profileUrl, AVATAR_PLACEHOLDER, posterUrl, POSTER_PLACEHOLDER } from './utils'

export default {
  name: 'PortalPersonDetail',
  data() { return { person: null } },
  computed: {
    avatar() { return profileUrl(this.person, 'w342') },
    credits() { return (this.person && this.person.credits) ? this.person.credits : [] },
    age() {
      if (!this.person || !this.person.birthday) return ''
      const b = new Date(String(this.person.birthday).replace(/-/g, '/'))
      const end = this.person.deathday ? new Date(String(this.person.deathday).replace(/-/g, '/')) : new Date()
      let a = end.getFullYear() - b.getFullYear()
      const m = end.getMonth() - b.getMonth()
      if (m < 0 || (m === 0 && end.getDate() < b.getDate())) a--
      return a >= 0 ? a : ''
    }
  },
  watch: { '$route.params.personId'() { this.load() } },
  created() { this.load() },
  methods: {
    load() {
      getPerson(this.$route.params.personId).then(res => { this.person = res.data })
    },
    poster(c) { return posterUrl({ posterLocalUrl: c.mediaPosterLocalUrl, posterPath: c.mediaPosterPath }) },
    onErr(e) { e.target.src = AVATAR_PLACEHOLDER },
    onPosterErr(e) { e.target.src = POSTER_PLACEHOLDER },
    goFilm(mediaId) { if (mediaId) this.$router.push('/film/' + mediaId) },
    genderText(g) { return { 1: '女', 2: '男' }[g] || '未知' },
    knownForLabel(k) { return { Acting: '演员', Directing: '导演', Writing: '编剧', Production: '制片' }[k] || k },
    jobLabel(job) {
      const m = { Director: '导演', Writer: '编剧', Screenplay: '编剧', Novel: '原著', Story: '故事', Creator: '主创', Producer: '制片', 'Executive Producer': '监制', 'Original Music Composer': '配乐' }
      return m[job] || job
    }
  }
}
</script>

<style scoped>
.person-page { padding-top: 24px; }
.detail-body { display: flex; gap: 32px; }
.avatar-col { flex: 0 0 220px; }
.avatar { width: 220px; border-radius: 14px; display: block; background: var(--bg-elevated); }
.info-col h1 { font-size: 30px; margin: 0 0 4px; }
.original { color: var(--text-secondary); margin-bottom: 14px; }
.meta-row { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.bio { font-size: 14px; line-height: 1.8; color: var(--text-secondary); white-space: pre-line; }
.works { margin-top: 36px; }
.work-card { cursor: pointer; }
.work-card .poster-wrap { border-radius: 8px; overflow: hidden; aspect-ratio: 2/3; background: var(--bg-elevated); }
.work-card img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .2s; }
.work-card:hover img { transform: translateY(-4px); }
.w-title { font-size: 13px; font-weight: 600; margin-top: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.w-role { font-size: 12px; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 640px) { .detail-body { flex-direction: column; } .avatar-col { flex: none; } .avatar { width: 150px; } }
</style>
