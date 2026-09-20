<template>
  <div class="portal-root portal-auth" :data-portal-theme="theme">
    <div class="auth-card">
      <div class="brand"><span class="logo-mark">影</span><span class="logo-text">弯</span></div>
      <p class="subtitle">登录以收藏影片、创建片单、订阅上线提醒</p>
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" @submit.native.prevent>
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="账号" prefix-icon="el-icon-user" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">登 录</el-button>
      </el-form>
      <div class="foot">
        还没有账号？<router-link to="/portal-register">立即注册</router-link>
        <span class="back" @click="$router.push('/discover')">返回首页</span>
      </div>
    </div>
  </div>
</template>

<script>
import { memberLogin, getMemberInfo } from '@/api/kemovie/member'
import { setPortalToken, setPortalUser } from './portalAuth'

export default {
  name: 'PortalLogin',
  data() {
    return {
      theme: localStorage.getItem('portal-theme') || 'dark',
      loginForm: { username: '', password: '' },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入账号' }],
        password: [{ required: true, trigger: 'blur', message: '请输入密码' }]
      },
      loading: false,
      redirect: undefined
    }
  },
  watch: {
    $route: { handler(route) { this.redirect = route.query && route.query.redirect }, immediate: true }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        memberLogin(this.loginForm).then(res => {
          setPortalToken(res.token)
          return getMemberInfo()
        }).then(info => {
          if (info && info.member) setPortalUser(info.member)
          const target = this.redirect && this.redirect.indexOf('/portal-login') !== 0 ? this.redirect : '/discover'
          this.$router.push({ path: target }).catch(() => {})
        }).catch(() => {
          this.loading = false
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
@import './theme.scss';
.portal-auth {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base);
}
.auth-card {
  width: 380px; background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 14px; padding: 40px 34px; box-shadow: var(--shadow-hover);
}
.brand { text-align: center; font-size: 34px; font-weight: 700; margin-bottom: 6px; }
.logo-mark { color: var(--brand); }
.logo-text { color: var(--text-primary); }
.subtitle { text-align: center; color: var(--text-secondary); font-size: 13px; margin-bottom: 26px; }
.submit-btn { width: 100%; margin-top: 6px; }
.foot { margin-top: 18px; font-size: 13px; color: var(--text-secondary); text-align: center; }
.foot a { color: var(--brand); }
.foot .back { margin-left: 14px; cursor: pointer; }
.foot .back:hover { color: var(--brand); }
</style>
