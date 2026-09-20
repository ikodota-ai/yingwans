import axios from 'axios'
import { Message } from 'element-ui'
import router from '@/router'
import { tansParams } from '@/utils/ruoyi'
import errorCode from '@/utils/errorCode'
import { getPortalToken, removePortalToken } from './portalAuth'

// 前台会员专用请求实例：携带 Portal-Token；401 时引导到前台登录页
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  const token = getPortalToken()
  if (token && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  return config
}, error => {
  return Promise.reject(error)
})

service.interceptors.response.use(res => {
  const code = res.data.code || 200
  const msg = errorCode[code] || res.data.msg || errorCode['default']
  if (code === 401) {
    removePortalToken()
    const cur = router.currentRoute.fullPath
    if (cur.indexOf('/portal-login') === -1) {
      router.push('/portal-login?redirect=' + encodeURIComponent(cur)).catch(() => {})
    }
    return Promise.reject('会员登录已过期，请重新登录')
  } else if (code !== 200) {
    Message({ message: msg, type: 'error' })
    return Promise.reject(new Error(msg))
  }
  return res.data
}, error => {
  let { message } = error
  if (message === 'Network Error') {
    message = '后端接口连接异常'
  } else if (message && message.includes('timeout')) {
    message = '系统接口请求超时'
  }
  Message({ message: message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

export default service
