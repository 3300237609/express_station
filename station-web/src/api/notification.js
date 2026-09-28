import request from './request'

/**
 * 消息通知接口模块（站内消息）
 * 场景：入库/取件通知、超期提醒、异常件通知、取件成功、系统公告
 */

/**
 * 分页查询我的消息
 * @param params { pageNum, pageSize, isRead, type }
 */
export function listNotification(params) {
  return request.get('/notification/list', { params })
}

/** 查询未读消息数（顶栏红点） */
export function getUnreadCount() {
  return request.get('/notification/unreadCount')
}

/**
 * 标记单条消息已读
 * @param id 消息ID
 */
export function readNotification(id) {
  return request.put(`/notification/read/${id}`)
}

/** 全部标记已读 */
export function readAllNotification() {
  return request.put('/notification/readAll')
}

/**
 * 发布系统公告（管理员）
 * @param data { title, content }
 */
export function publishNotice(data) {
  return request.post('/notification/publish', data)
}

/**
 * 分页查询全部消息记录（仅管理员，可按接收用户/类型筛选）
 * @param params { pageNum, pageSize, userId（接收用户ID，可选）, type（消息类型，可选） }
 */
export function listNotificationManage(params) {
  return request.get('/notification/manage', { params })
}
