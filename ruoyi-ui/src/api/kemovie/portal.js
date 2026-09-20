import request from '@/views/portal/portalRequest'

// ============ 公开接口 /portal ============

// 首页聚合（热门榜/新上线/片单/运营位）
export function getHome() {
  return request({ url: '/portal/home', method: 'get' })
}

// 最近上新（JustWatch 各区域），params: { country, days, limit }
export function getNewReleases(params) {
  return request({ url: '/portal/newReleases', method: 'get', params })
}

// 最近收录（按本站收录时间倒序），params: { limit }
export function getRecentCollected(params) {
  return request({ url: '/portal/recentCollected', method: 'get', params })
}

// 发现列表（分页、可筛选）
export function listMedia(query) {
  return request({ url: '/portal/media/list', method: 'get', params: query })
}

// 热门榜
export function getRank(type, limit) {
  return request({ url: '/portal/rank', method: 'get', params: { type, limit } })
}

// 影片详情
export function getMedia(mediaId) {
  return request({ url: '/portal/media/' + mediaId, method: 'get' })
}

// 片单列表（公开）
export function listCollection(query) {
  return request({ url: '/portal/collection/list', method: 'get', params: query })
}

// 片单详情（元信息 + 关注态）
export function getCollection(collectionId) {
  return request({ url: '/portal/collection/' + collectionId, method: 'get' })
}

// 片单内影片：分页 + 筛选
export function listCollectionItems(collectionId, query) {
  return request({ url: '/portal/collection/' + collectionId + '/items', method: 'get', params: query })
}

// 运营位
export function getOps(blockKey) {
  return request({ url: '/portal/ops/' + blockKey, method: 'get' })
}

// 人物详情（含站内参演作品）
export function getPerson(personId) {
  return request({ url: '/portal/person/' + personId, method: 'get' })
}

// 发现页筛选可选项
export function getFilters() {
  return request({ url: '/portal/filters', method: 'get' })
}
