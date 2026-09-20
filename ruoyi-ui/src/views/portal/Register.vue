<template>
  <div class="portal-root portal-auth" :data-portal-theme="theme">
    <div class="auth-card">
      <div class="brand"><span class="logo-mark">影</span><span class="logo-text">弯</span></div>
      <p class="subtitle">注册一个账号，开启你的私人影库</p>
      <el-form ref="registerForm" :model="registerForm" :rules="registerRules" @submit.native.prevent>
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="账号" prefix-icon="el-icon-user" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="el-icon-lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleRegister" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">注 册</el-button>
      </el-form>
      <div class="foot">
        已有账号？<router-link to="/portal-login">去登录</router-link>
        <span class="back" @click="$router.push('/discover')">返回首页</span>
      </div>
    </div>
  </div>
</template>

<script>
import { memberRegister } from '@/api/kemovie/member'

export default {
  name: 'PortalRegister',
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.registerForm.password !== value) { callback(new Error('两次输入的密码不一致')) }
      else { callback() }
    }
    return {
      theme: localStorage.getItem('portal-theme') || 'dark',
      registerForm: { username: '', password: '', confirmPassword: '' },
      registerRules: {
        username: [
          { required: true, trigger: 'blur', message: '请输入账号' },
          { min: 2, max: 20, message: '账号长度为 2 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, trigger: 'blur', message: '请输入密码' },
          { min: 5, max: 20, message: '密码长度为 5 到 20 个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, trigger: 'blur', message: '请再次输入密码' },
          { required: true, validator: equalToPassword, trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) return
        this.loading = true
        memberRegister(this.registerForm).then(() => {
          this.$alert("恭喜，账号 <b>" + this.registerForm.username + "</b> 注册成功！", '注册成功', {
            dangerouslyUseHTMLString: true
          }).then(() => {
            this.$router.push('/portal-login')
          }).catch(() => {})
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
