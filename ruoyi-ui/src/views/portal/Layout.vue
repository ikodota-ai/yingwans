<template>
  <div class="portal-root" :data-portal-theme="theme">
    <header class="portal-header">
      <div class="portal-container header-inner">
        <div class="brand" @click="go('/discover')">
          <span class="logo-mark">影</span><span class="logo-text">弯</span>
        </div>
        <nav class="nav">
          <router-link to="/discover">发现</router-link>
          <router-link to="/browse">影片</router-link>
          <router-link to="/collections">片单</router-link>
          <router-link to="/library">我的片库</router-link>
          <router-link to="/subscriptions" class="sub-link">
            订阅提醒
            <span v-if="unread > 0" class="badge">{{ unread }}</span>
          </router-link>
        </nav>
        <div class="actions">
          <div class="search-box" v-click-outside="closeSuggest">
            <input
              v-model="keyword"
              placeholder="搜索影片…"
              @input="onInput"
              @keyup.enter="search"
              @focus="onInput"
            />
            <div v-if="showSuggest && suggestList.length" class="suggest">
              <div
                v-for="m in suggestList"
                :key="m.mediaId"
                class="suggest-item"
                @click="goFilm(m)"
              >
                <img :src="poster(m)" @error="onImgErr" />
                <div class="s-meta">
                  <div class="s-title">{{ m.title }}</div>
                  <div class="s-sub">
                    <span v-if="m.releaseDate">{{ String(m.releaseDate).substring(0,4) }}</span>
                    <span v-if="m.region"> · {{ m.region }}</span>
                    <span v-if="m.ratingAvg > 0" class="s-score">★ {{ Number(m.ratingAvg).toFixed(1) }}</span>
                  </div>
                </div>
              </div>
              <div class="suggest-all" @click="search">查看全部结果 ›</div>
            </div>
          </div>
          <button class="theme-toggle" :title="theme === 'dark' ? '切换浅色' : '切换深色'" @click="toggleTheme">
            {{ theme === 'dark' ? '☀' : '☾' }}
          </button>
          <router-link v-if="!logged" to="/portal-login" class="login-btn">登录</router-link>
          <el-dropdown v-else trigger="click" @command="onUserCommand">
            <div class="avatar">{{ avatarText }}</div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="library">我的片库</el-dropdown-item>
              <el-dropdown-item command="subscriptions">订阅提醒</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view :key="$route.fullPath" />
    </main>

    <footer class="portal-footer">
      <div class="portal-container">影弯 Kemovie · 私人影库与流媒体上线追踪</div>
    </footer>
  </div>
</template>

<script>
import { unreadCount } from '@/api/kemovie/me'
import { listMedia } from '@/api/kemovie/portal'
import { posterUrl, POSTER_PLACEHOLDER } from './utils'
import { memberLogout } from '@/api/kemovie/member'
import { getPortalToken, getPortalUser, removePortalToken } from './portalAuth'

export default {
  name: 'PortalLayout',
  data() {
    return {
      theme: localStorage.getItem('portal-theme') || 'dark',
      keyword: '',
      unread: 0,
      suggestList: [],
      showSuggest: false,
      suggestTimer: null
    }
  },
  directives: {
    clickOutside: {
      bind(el, binding) {
        el._handler = (e) => { if (!el.contains(e.target)) binding.value() }
        document.addEventListener('click', el._handler)
      },
      unbind(el) { document.removeEventListener('click', el._handler) }
    }
  },
  computed: {
    logged() { return !!getPortalToken() },
    avatarText() {
      const u = getPortalUser()
      const n = (u && (u.nickname || u.username)) || 'U'
      return n.substring(0, 1).toUpperCase()
    }
  },
  created() {
    if (this.logged) this.loadUnread()
  },
  methods: {
    toggleTheme() {
      this.theme = this.theme === 'dark' ? 'light' : 'dark'
      localStorage.setItem('portal-theme', this.theme)
    },
    go(path) { this.$router.push(path) },
    search() {
      if (!this.keyword.trim()) return
      this.showSuggest = false
      this.$router.push({ path: '/discover', query: { q: this.keyword.trim() } })
    },
    poster(m) { return posterUrl(m, 'w92') },
    onImgErr(e) { e.target.src = POSTER_PLACEHOLDER },
    onInput() {
      const kw = this.keyword.trim()
      if (this.suggestTimer) clearTimeout(this.suggestTimer)
      if (!kw) { this.suggestList = []; this.showSuggest = false; return }
      this.suggestTimer = setTimeout(() => {
        listMedia({ title: kw, pageNum: 1, pageSize: 6 }).then(res => {
          this.suggestList = res.rows || []
          this.showSuggest = true
        }).catch(() => {})
      }, 250)
    },
    goFilm(m) {
      this.showSuggest = false
      this.keyword = ''
      this.$router.push('/film/' + m.mediaId)
    },
    closeSuggest() { this.showSuggest = false },
    loadUnread() {
      unreadCount().then(res => { this.unread = res.data || 0 }).catch(() => {})
    },
    onUserCommand(cmd) {
      if (cmd === 'library') { this.go('/library') }
      else if (cmd === 'subscriptions') { this.go('/subscriptions') }
      else if (cmd === 'logout') {
        memberLogout().catch(() => {}).then(() => {
          removePortalToken()
          this.unread = 0
          this.$router.push('/discover').catch(() => {})
        })
      }
    }
  }
}
</script>

<style lang="scss">
/* 前台主题与公共布局样式（含 .rail/.poster-grid 等），
   通过 .portal-root 命名空间隔离，不影响后台 UI；不能放在 scoped 里，
   否则子路由组件的元素无法命中这些布局类。 */
@import './theme.scss';
</style>

<style lang="scss" scoped>
.portal-header {
  position: sticky; top: 0; z-index: 100;
  background: rgba(13, 14, 18, .92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border);
}
.portal-root[data-portal-theme="light"] .portal-header {
  background: rgba(255, 255, 255, .92);
}
.header-inner { display: flex; align-items: center; height: 60px; gap: 24px; }
.brand { display: flex; align-items: center; cursor: pointer; font-size: 22px; font-weight: 700; }
.logo-mark { color: var(--brand); }
.logo-text { color: var(--text-primary); }
.nav { display: flex; gap: 22px; flex: 1; }
.nav a {
  color: var(--text-secondary); font-size: 15px; position: relative; padding: 4px 0;
}
.nav a:hover, .nav a.router-link-active { color: var(--text-primary); }
.nav a.router-link-active::after {
  content: ''; position: absolute; left: 0; right: 0; bottom: -6px;
  height: 2px; background: var(--brand); border-radius: 2px;
}
.sub-link .badge {
  position: absolute; top: -6px; right: -14px;
  background: var(--danger); color: #fff; font-size: 10px;
  border-radius: 8px; padding: 0 5px; line-height: 15px;
}
.actions { display: flex; align-items: center; gap: 14px; }
.search-box { position: relative; }
.search-box input {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 20px; padding: 6px 14px; color: var(--text-primary);
  outline: none; width: 180px; transition: border-color .2s;
}
.search-box input:focus { border-color: var(--brand); }
.suggest {
  position: absolute; top: calc(100% + 8px); right: 0; width: 320px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 12px; box-shadow: var(--shadow-hover); padding: 6px; z-index: 200;
}
.suggest-item {
  display: flex; gap: 10px; align-items: center; padding: 6px 8px;
  border-radius: 8px; cursor: pointer;
}
.suggest-item:hover { background: var(--bg-elevated); }
.suggest-item img { width: 34px; height: 50px; object-fit: cover; border-radius: 4px; flex: 0 0 auto; }
.s-meta { overflow: hidden; }
.s-title { font-size: 14px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.s-sub { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.s-score { color: var(--brand); margin-left: 6px; }
.suggest-all {
  text-align: center; font-size: 13px; color: var(--text-secondary);
  padding: 8px; cursor: pointer; border-top: 1px solid var(--border); margin-top: 4px;
}
.suggest-all:hover { color: var(--brand); }
.theme-toggle {
  width: 34px; height: 34px; border-radius: 50%;
  border: 1px solid var(--border); background: var(--bg-surface);
  color: var(--brand); cursor: pointer; font-size: 15px;
}
.login-btn { color: var(--brand); font-weight: 600; }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--brand); color: #1a1200;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; cursor: pointer;
}
.portal-main { min-height: calc(100vh - 60px - 80px); }
.portal-footer {
  border-top: 1px solid var(--border);
  color: var(--text-muted); text-align: center;
  padding: 28px 0; margin-top: 40px; font-size: 13px;
}
@media (max-width: 640px) {
  .search-box { display: none; }
  .nav { gap: 14px; font-size: 14px; }
}
</style>
