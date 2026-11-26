import { downloadAuthenticated, request } from './http'
import type { FileAttachmentInfo } from '../types/notice'

export const fileApi = {
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<FileAttachmentInfo>({
      method: 'post',
      url: '/files',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  deleteTemp(attachmentId: number) {
    return request<void>({
      method: 'delete',
      url: `/files/${attachmentId}`,
    })
  },

  download(attachment: FileAttachmentInfo) {
    return downloadAuthenticated(
      `/files/${attachment.id}/download`,
      attachment.originalName,
    )
  },
}

