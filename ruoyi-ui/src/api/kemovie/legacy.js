import request from '@/utils/request'

// 上传旧站导出文件（multipart）
export function uploadLegacyFile(data) {
  return request({
    url: '/kemovie/legacy/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data
  })
}

// 启动导入/试跑
export function startLegacyImport(data) {
  return request({
    url: '/kemovie/legacy/start',
    method: 'post',
    data
  })
}

// 查询进度
export function legacyImportProgress(taskId) {
  return request({
    url: '/kemovie/legacy/progress/' + taskId,
    method: 'get'
  })
}
