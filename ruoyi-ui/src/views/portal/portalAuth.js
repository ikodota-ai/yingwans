import Cookies from 'js-cookie'

// 前台会员令牌，与后台 Admin-Token 完全独立
const PortalTokenKey = 'Portal-Token'
const PortalUserKey = 'Portal-User'

export function getPortalToken() {
  return Cookies.get(PortalTokenKey)
}

export function setPortalToken(token) {
  return Cookies.set(PortalTokenKey, token)
}

export function removePortalToken() {
  Cookies.remove(PortalUserKey)
  return Cookies.remove(PortalTokenKey)
}

export function getPortalUser() {
  const raw = Cookies.get(PortalUserKey)
  try { return raw ? JSON.parse(raw) : null } catch (e) { return null }
}

export function setPortalUser(user) {
  return Cookies.set(PortalUserKey, JSON.stringify(user || {}))
}
