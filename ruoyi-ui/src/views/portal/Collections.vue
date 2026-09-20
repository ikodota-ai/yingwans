<template>
  <div class="portal-container collections">
    <portal-breadcrumb :items="[{ text: '首页', to: '/discover' }, { text: '片单' }]" />
    <div class="section-title"><span>片单</span></div>

    <!-- 一级 Tab：公共片单 / 我的片单 -->
    <div class="tabs">
      <button :class="{ active: tab === 'public' }" @click="switchTab('public')">公共片单</button>
      <button :class="{ active: tab === 'mine' }" @click="switchTab('mine')">我的片单</button>
    </div>

    <!-- 公共片单：关注 / 浏览 -->
    <template v-if="tab === 'public'">
      <div class="subtabs">
        <span :class="{ active: pubTab === 'browse' }" @click="switchPub('browse')">浏览</span>
        <span :class="{ active: pubTab === 'followed' }" @click="switchPub('followed')">关注</span>
      </div>

      <!-- 浏览：分页无限滚动 -->
      <template v-if="pubTab === 'browse'">
        <div v-if="list.length" class="collection-row">
          <collection-card v-for="c in list" :key="c.collectionId" :collection="c" />
        </div>
        <div v-else-if="!loading" class="empty-state">暂无公开片单</div>
        <div ref="sentinel" class="scroll-sentinel"></div>
        <div class="load-more" v-if="hasMore">
          <button class="btn-ghost" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
        </div>
        <div v-else-if="list.length && !loading" class="list-end">— 没有更多了 —</div>
      </template>

      <!-- 关注 -->
      <template v-else>
        <div v-if="!logged" class="empty-state">登录后可关注公共片单 · <router-link class="link" :to="loginTo">去登录</router-link></div>
        <div v-else-if="followed.length" class="collection-row">
          <collection-card v-for="c in followed" :key="c.collectionId" :collection="c" />
        </div>
        <div v-else class="empty-state">还没有关注任何片单，去「浏览」看看吧</div>
      </template>
    </template>

    <!-- 我的片单 -->
    <template v-else>
      <div v-if="!logged" class="empty-state">登录后可创建和管理你的片单 · <router-link class="link" :to="loginTo">去登录</router-link></div>
      <template v-else>
        <div class="col-actions">
          <button class="btn-brand" @click="openCreate">＋ 新建片单</button>
        </div>
        <div v-if="myCols.length" class="collection-row">
          <div v-for="c in myCols" :key="c.collectionId" class="my-col">
            <collection-card :collection="c" />
            <div class="col-ops">
              <span @click="openEdit(c)">编辑</span>
              <span class="del" @click="removeCol(c)">删除</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">还没有创建片单，点上方「新建片单」开始吧</div>
      </template>
    </template>

    <!-- 新建/编辑片单弹窗（美化版） -->
    <el-dialog
      :visible.sync="dialog"
      :show-close="false"
      width="460px"
      custom-class="col-dialog"
      append-to-body
    >
      <div class="cd-head">
        <h3>{{ form.collectionId ? '编辑片单' : '新建片单' }}</h3>
        <p class="cd-sub">整理你喜欢的影片，公开后其他影迷也能发现</p>
      </div>
      <div class="cd-body">
        <div class="cd-field">
          <label>片单名</label>
          <input v-model="form.name" maxlength="50" placeholder="例如：年度必看韩剧" />
        </div>
        <div class="cd-field">
          <label>简介 <span class="opt">（选填）</span></label>
          <textarea v-model="form.description" rows="3" maxlength="200" placeholder="一句话介绍这个片单…"></textarea>
        </div>
        <div class="cd-field row">
          <div>
            <label>公开片单</label>
            <div class="cd-hint">公开后其他用户可在公共片单中浏览、关注</div>
          </div>
          <el-switch v-model="form.isPublic" active-value="0" inactive-value="1" active-color="#f5c518" />
        </div>
      </div>
      <div class="cd-foot">
        <button class="btn-ghost" @click="dialog = false">取消</button>
        <button class="btn-brand" @click="save">{{ form.collectionId ? '保存' : '创建' }}</button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCollection } from '@/api/kemovie/portal'
import { myCollections, myFollowedCollections, createCollection, updateMyCollection, deleteMyCollection } from '@/api/kemovie/me'
import CollectionCard from './components/CollectionCard.vue'
import PortalBreadcrumb from './components/PortalBreadcrumb.vue'
import infiniteScrollMixin from './mixins/infiniteScrollMixin'
import { getPortalToken } from './portalAuth'

export default {
  name: 'PortalCollections',
  components: { CollectionCard, PortalBreadcrumb },
  mixins: [infiniteScrollMixin],
  data() {
    return {
      tab: 'public',
      pubTab: 'browse',
      pageSize: 20,
      myCols: [],
      followed: [],
      dialog: false,
      form: { collectionId: null, name: '', description: '', isPublic: '1' }
    }
  },
  computed: {
    logged() { return !!getPortalToken() },
    loginTo() { return '/portal-login?redirect=' + encodeURIComponent(this.$route.fullPath) }
  },
  created() {
    if (this.$route.query.tab === 'mine') this.tab = 'mine'
    this.initTab()
  },
  methods: {
    initTab() {
      if (this.tab === 'public') {
        if (this.pubTab === 'browse') this.resetAndFetch()
        else this.loadFollowed()
      } else {
        this.loadMine()
      }
    },
    switchTab(t) {
      if (this.tab === t) return
      this.tab = t
      this.initTab()
    },
    switchPub(t) {
      if (this.pubTab === t) return
      this.pubTab = t
      if (t === 'browse') this.resetAndFetch()
      else this.loadFollowed()
    },
    fetchPage() {
      return listCollection({ pageNum: this.pageNum, pageSize: this.pageSize }).then(res => {
        const rows = res.rows || []
        this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
        this.total = res.total || 0
      })
    },
    loadFollowed() {
      if (!this.logged) { this.followed = []; return }
      myFollowedCollections().then(res => { this.followed = res.data || [] })
    },
    loadMine() {
      if (!this.logged) { this.myCols = []; return }
      myCollections().then(res => { this.myCols = res.data || [] })
    },
    openCreate() {
      this.form = { collectionId: null, name: '', description: '', isPublic: '1' }
      this.dialog = true
    },
    openEdit(c) {
      this.form = { collectionId: c.collectionId, name: c.name, description: c.description, isPublic: c.isPublic }
      this.dialog = true
    },
    save() {
      if (!this.form.name || !this.form.name.trim()) { this.$modal.msgWarning('请输入片单名'); return }
      const fn = this.form.collectionId ? updateMyCollection : createCollection
      fn(this.form).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.dialog = false
        this.loadMine()
      })
    },
    removeCol(c) {
      this.$modal.confirm('确认删除片单「' + c.name + '」？').then(() => {
        return deleteMyCollection(c.collectionId)
      }).then(() => {
        this.$modal.msgSuccess('已删除')
        this.loadMine()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.tabs { display: flex; gap: 10px; margin: 8px 0 16px; flex-wrap: wrap; }
.tabs button {
  background: var(--bg-surface); border: 1px solid var(--border);
  color: var(--text-secondary); border-radius: 20px; padding: 7px 20px; cursor: pointer;
  font-size: 14px; transition: all .2s;
}
.tabs button:hover { border-color: var(--brand); }
.tabs button.active { background: var(--brand); color: #1a1200; border-color: var(--brand); font-weight: 600; }
.subtabs { display: flex; gap: 18px; margin-bottom: 18px; }
.subtabs span {
  cursor: pointer; font-size: 14px; color: var(--text-secondary);
  padding-bottom: 4px; border-bottom: 2px solid transparent;
}
.subtabs span:hover { color: var(--text-primary); }
.subtabs span.active { color: var(--brand); border-bottom-color: var(--brand); font-weight: 600; }
.collection-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-top: 8px; }
@media (max-width: 900px) { .collection-row { grid-template-columns: repeat(2, 1fr); } }
.col-actions { margin-bottom: 16px; }
.col-ops { display: flex; gap: 14px; margin-top: 6px; font-size: 13px; }
.col-ops span { color: var(--text-secondary); cursor: pointer; }
.col-ops span:hover { color: var(--brand); }
.col-ops .del:hover { color: var(--danger); }
.scroll-sentinel { height: 1px; }
.load-more { text-align: center; margin: 28px 0; }
.list-end { text-align: center; color: var(--text-muted); font-size: 13px; margin: 24px 0; }
.link { color: var(--brand); }
</style>

<style>
/* 弹窗为 append-to-body，样式需非 scoped */
.col-dialog { border-radius: 16px; overflow: hidden; background: var(--bg-surface); }
.col-dialog .el-dialog__header { display: none; }
.col-dialog .el-dialog__body { padding: 0; }
.col-dialog .cd-head { padding: 22px 24px 4px; }
.col-dialog .cd-head h3 { margin: 0; font-size: 19px; color: var(--text-primary); }
.col-dialog .cd-head .cd-sub { margin: 6px 0 0; font-size: 13px; color: var(--text-muted); }
.col-dialog .cd-body { padding: 12px 24px 4px; }
.col-dialog .cd-field { margin-bottom: 18px; }
.col-dialog .cd-field > label { display: block; font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.col-dialog .cd-field .opt { color: var(--text-muted); }
.col-dialog .cd-field input,
.col-dialog .cd-field textarea {
  width: 100%; box-sizing: border-box; background: var(--bg-elevated);
  border: 1px solid var(--border); border-radius: 10px; padding: 10px 12px;
  color: var(--text-primary); font-size: 14px; outline: none; transition: border-color .2s;
  resize: vertical; font-family: inherit;
}
.col-dialog .cd-field input:focus,
.col-dialog .cd-field textarea:focus { border-color: var(--brand); }
.col-dialog .cd-field.row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.col-dialog .cd-field.row > div label { display: block; font-size: 14px; color: var(--text-primary); margin-bottom: 4px; }
.col-dialog .cd-hint { font-size: 12px; color: var(--text-muted); }
.col-dialog .cd-foot {
  display: flex; justify-content: flex-end; gap: 12px;
  padding: 8px 24px 22px;
}
</style>
