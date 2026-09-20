import request from '@/utils/request'

// ============ 首页运营概览 ============
export function getDashboardOverview() {
  return request({ url: '/kemovie/dashboard/overview', method: 'get' })
}

// ============ 影片管理 ============
export function listAdminMedia(query) {
  return request({ url: '/kemovie/media/list', method: 'get', params: query })
}
export function getAdminMedia(mediaId) {
  return request({ url: '/kemovie/media/' + mediaId, method: 'get' })
}
export function updateAdminMedia(data) {
  return request({ url: '/kemovie/media', method: 'put', data })
}
export function delAdminMedia(mediaIds) {
  return request({ url: '/kemovie/media/' + mediaIds, method: 'delete' })
}
export function syncTmdb(tmdbId, type) {
  return request({ url: '/kemovie/media/sync', method: 'post', params: { tmdbId, type } })
}

// ============ 片单管理 ============
export function listAdminCollection(query) {
  return request({ url: '/kemovie/collection/list', method: 'get', params: query })
}
export function getAdminCollection(collectionId) {
  return request({ url: '/kemovie/collection/' + collectionId, method: 'get' })
}
export function addAdminCollection(data) {
  return request({ url: '/kemovie/collection', method: 'post', data })
}
export function updateAdminCollection(data) {
  return request({ url: '/kemovie/collection', method: 'put', data })
}
export function delAdminCollection(collectionIds) {
  return request({ url: '/kemovie/collection/' + collectionIds, method: 'delete' })
}
export function addCollectionMedia(collectionId, mediaId) {
  return request({ url: '/kemovie/collection/' + collectionId + '/media/' + mediaId, method: 'post' })
}
export function removeCollectionMedia(collectionId, mediaId) {
  return request({ url: '/kemovie/collection/' + collectionId + '/media/' + mediaId, method: 'delete' })
}

// ============ MDL 抓取源 ============
export function listMdlSource(query) {
  return request({ url: '/kemovie/mdlSource/list', method: 'get', params: query })
}
export function getMdlSource(sourceId) {
  return request({ url: '/kemovie/mdlSource/' + sourceId, method: 'get' })
}
export function addMdlSource(data) {
  return request({ url: '/kemovie/mdlSource', method: 'post', data })
}
export function updateMdlSource(data) {
  return request({ url: '/kemovie/mdlSource', method: 'put', data })
}
export function delMdlSource(sourceIds) {
  return request({ url: '/kemovie/mdlSource/' + sourceIds, method: 'delete' })
}
export function runMdlSource(sourceId) {
  return request({ url: '/kemovie/mdlSource/run/' + sourceId, method: 'post' })
}
export function runAllMdlSource() {
  return request({ url: '/kemovie/mdlSource/runAll', method: 'post' })
}
export function listJustWatchPackages(country) {
  return request({ url: '/kemovie/mdlSource/justwatchPackages', method: 'get', params: { country: country || 'US' } })
}

// ============ 运营位 ============
export function listOpsBlock(query) {
  return request({ url: '/kemovie/opsBlock/list', method: 'get', params: query })
}
export function getOpsBlock(blockId) {
  return request({ url: '/kemovie/opsBlock/' + blockId, method: 'get' })
}
export function addOpsBlock(data) {
  return request({ url: '/kemovie/opsBlock', method: 'post', data })
}
export function updateOpsBlock(data) {
  return request({ url: '/kemovie/opsBlock', method: 'put', data })
}
export function delOpsBlock(blockIds) {
  return request({ url: '/kemovie/opsBlock/' + blockIds, method: 'delete' })
}

// ============ 前台会员管理 ============
export function listMember(query) {
  return request({ url: '/kemovie/member/list', method: 'get', params: query })
}
export function getMember(memberId) {
  return request({ url: '/kemovie/member/' + memberId, method: 'get' })
}
export function updateMember(data) {
  return request({ url: '/kemovie/member', method: 'put', data })
}
export function changeMemberStatus(memberId, status) {
  return request({ url: '/kemovie/member/changeStatus', method: 'put', data: { memberId, status } })
}
export function resetMemberPwd(memberId, password) {
  return request({ url: '/kemovie/member/resetPwd', method: 'put', data: { memberId, password } })
}
export function delMember(memberIds) {
  return request({ url: '/kemovie/member/' + memberIds, method: 'delete' })
}

// ============ 演职人员管理 ============
export function listPerson(query) {
  return request({ url: '/kemovie/person/list', method: 'get', params: query })
}
export function getAdminPerson(personId) {
  return request({ url: '/kemovie/person/' + personId, method: 'get' })
}
export function refreshPerson(personId) {
  return request({ url: '/kemovie/person/' + personId + '/refresh', method: 'post' })
}
export function delPerson(personIds) {
  return request({ url: '/kemovie/person/' + personIds, method: 'delete' })
}
// 同步某影片的演职人员
export function syncMediaCast(mediaId) {
  return request({ url: '/kemovie/media/' + mediaId + '/syncCast', method: 'post' })
}

// ============ 自定义服务商 ============
export function listProvider(query) {
  return request({ url: '/kemovie/provider/list', method: 'get', params: query })
}
export function getProvider(providerId) {
  return request({ url: '/kemovie/provider/' + providerId, method: 'get' })
}
export function addProvider(data) {
  return request({ url: '/kemovie/provider', method: 'post', data })
}
export function updateProvider(data) {
  return request({ url: '/kemovie/provider', method: 'put', data })
}
export function delProvider(providerIds) {
  return request({ url: '/kemovie/provider/' + providerIds, method: 'delete' })
}
// 启用的服务商（影片编辑勾选框）
export function listEnabledProviders() {
  return request({ url: '/kemovie/provider/enabled', method: 'get' })
}
// 影片已勾选的服务商 ID
export function getMediaProviderIds(mediaId) {
  return request({ url: '/kemovie/provider/media/' + mediaId, method: 'get' })
}

// ============ 影片下载资源 / 资源文档 ============
export function listMediaResources(mediaId) {
  return request({ url: '/kemovie/media/' + mediaId + '/resources', method: 'get' })
}
export function addMediaResource(data) {
  return request({ url: '/kemovie/media/resource', method: 'post', data })
}
export function delMediaResource(resourceId) {
  return request({ url: '/kemovie/media/resource/' + resourceId, method: 'delete' })
}
export function updateMediaResource(data) {
  return request({ url: '/kemovie/media/resource', method: 'put', data })
}
export function generateFeishuDoc(mediaId) {
  return request({ url: '/kemovie/media/' + mediaId + '/doc/feishu', method: 'post' })
}
export function testFeishuConnection() {
  return request({ url: '/kemovie/media/doc/feishu/test', method: 'get' })
}
export function listSubscriptionSummary(query) {
  return request({ url: '/kemovie/subscription/summary', method: 'get', params: query })
}
export function listSubscriptionEvents(query) {
  return request({ url: '/kemovie/subscription/events', method: 'get', params: query })
}
