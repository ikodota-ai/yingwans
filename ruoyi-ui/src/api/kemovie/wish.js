import request from '@/utils/request'

// 想看影片聚合列表
export function listWishMedia(query) {
  return request({
    url: '/kemovie/wish/list',
    method: 'get',
    params: query
  })
}

// 某片想看用户明细
export function listWishUsers(mediaId) {
  return request({
    url: '/kemovie/wish/users/' + mediaId,
    method: 'get'
  })
}
