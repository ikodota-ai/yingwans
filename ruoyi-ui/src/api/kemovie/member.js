import request from '@/views/portal/portalRequest'

// 前台会员：登录/注册/信息/退出（独立于后台 sys_user）

export function memberLogin(data) {
  return request({ url: '/member/login', method: 'post', data, headers: { isToken: false } })
}

export function memberRegister(data) {
  return request({ url: '/member/register', method: 'post', data, headers: { isToken: false } })
}

export function getMemberInfo() {
  return request({ url: '/member/info', method: 'get' })
}

export function memberLogout() {
  return request({ url: '/member/logout', method: 'post' })
}

export function updateMemberProfile(data) {
  return request({ url: '/member/profile', method: 'post', data })
}
