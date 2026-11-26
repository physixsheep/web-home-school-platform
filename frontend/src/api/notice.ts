import { downloadAuthenticated, request } from './http'
import type {
  FileAttachmentInfo,
  NoticeOptions,
  NoticeReader,
  NoticeRow,
  NoticeSavePayload,
} from '../types/notice'
import type { PageResult } from '../types/organization'

export const noticeApi = {
  readable(params: {
    page: number
    size: number
    keyword?: string
  }) {
    return request<PageResult<NoticeRow>>({
      method: 'get',
      url: '/notices',
      params,
    })
  },

  manage(params: {
    page: number
    size: number
    keyword?: string
    status?: string
    scopeType?: string
  }) {
    return request<PageResult<NoticeRow>>({
      method: 'get',
      url: '/notices/manage',
      params,
    })
  },

  options() {
    return request<NoticeOptions>({
      method: 'get',
      url: '/notices/options',
    })
  },

  detail(noticeId: number) {
    return request<NoticeRow>({
      method: 'get',
      url: `/notices/${noticeId}`,
    })
  },

  create(data: NoticeSavePayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/notices',
      data,
    })
  },

  update(noticeId: number, data: NoticeSavePayload) {
    return request<void>({
      method: 'put',
      url: `/notices/${noticeId}`,
      data,
    })
  },

  publish(noticeId: number) {
    return request<void>({
      method: 'post',
      url: `/notices/${noticeId}/publish`,
    })
  },

  withdraw(noticeId: number) {
    return request<void>({
      method: 'post',
      url: `/notices/${noticeId}/withdraw`,
    })
  },

  deleteDraft(noticeId: number) {
    return request<void>({
      method: 'delete',
      url: `/notices/${noticeId}`,
    })
  },

  markRead(noticeId: number) {
    return request<void>({
      method: 'post',
      url: `/notices/${noticeId}/read`,
    })
  },

  readers(noticeId: number, page = 1, size = 20) {
    return request<PageResult<NoticeReader>>({
      method: 'get',
      url: `/notices/${noticeId}/readers`,
      params: { page, size },
    })
  },

  uploadAttachment(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<FileAttachmentInfo>({
      method: 'post',
      url: '/files',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  deleteTempAttachment(attachmentId: number) {
    return request<void>({
      method: 'delete',
      url: `/files/${attachmentId}`,
    })
  },

  downloadAttachment(attachment: FileAttachmentInfo) {
    return downloadAuthenticated(
      `/files/${attachment.id}/download`,
      attachment.originalName,
    )
  },
}

