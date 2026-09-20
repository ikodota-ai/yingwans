import request from '@/views/portal/portalRequest'

// ============ 登录用户接口 /me ============

// 点赞
export function like(mediaId) {
  return request({ url: '/me/like/' + mediaId, method: 'post' })
}
// 想看
export function wish(mediaId) {
  return request({ url: '/me/wish/' + mediaId, method: 'post' })
}
// 看过
export function watched(mediaId) {
  return request({ url: '/me/watched/' + mediaId, method: 'post' })
}
// 评分（rating 为空表示清除）
export function rate(mediaId, rating) {
  return request({ url: '/me/rate/' + mediaId, method: 'post', params: { rating } })
}
// 订阅/取消订阅
export function subscribe(mediaId) {
  return request({ url: '/me/subscribe/' + mediaId, method: 'post' })
}
// 我的订阅（含上线时间轴）
export function mySubscriptions() {
  return request({ url: '/me/subscriptions', method: 'get' })
}
// 我的片库（tab: wish/watched/rated/liked）
export function myLibrary(tab) {
  return request({ url: '/me/library', method: 'get', params: { tab } })
}
// 我创建的片单
export function myCollections() {
  return request({ url: '/me/collections', method: 'get' })
}
// 我关注的公共片单
export function myFollowedCollections() {
  return request({ url: '/me/collections/followed', method: 'get' })
}
// 关注片单
export function followCollection(collectionId) {
  return request({ url: '/me/collection/' + collectionId + '/follow', method: 'post' })
}
// 取消关注片单
export function unfollowCollection(collectionId) {
  return request({ url: '/me/collection/' + collectionId + '/follow', method: 'delete' })
}
// 创建片单
export function createCollection(data) {
  return request({ url: '/me/collection', method: 'post', data })
}
// 修改片单
export function updateMyCollection(data) {
  return request({ url: '/me/collection', method: 'put', data })
}
// 删除片单
export function deleteMyCollection(collectionId) {
  return request({ url: '/me/collection/' + collectionId, method: 'delete' })
}
// 把影片加入片单
export function addToCollection(collectionId, mediaId) {
  return request({ url: '/me/collection/' + collectionId + '/media/' + mediaId, method: 'post' })
}
// 从片单移除影片
export function removeFromCollection(collectionId, mediaId) {
  return request({ url: '/me/collection/' + collectionId + '/media/' + mediaId, method: 'delete' })
}
// 通知列表
export function myNotifications() {
  return request({ url: '/me/notifications', method: 'get' })
}
// 未读数
export function unreadCount() {
  return request({ url: '/me/notifications/unread', method: 'get' })
}
// 标记已读
export function readNotification(notifyId) {
  return request({ url: '/me/notifications/' + notifyId + '/read', method: 'put' })
}
// 全部已读
export function readAllNotifications() {
  return request({ url: '/me/notifications/read-all', method: 'put' })
}
