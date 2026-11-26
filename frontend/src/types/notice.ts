export interface FileAttachmentInfo {
  id: number
  originalName: string
  extension: string
  mimeType: string
  fileSize: number
}

export interface NoticeRow {
  id: number
  publisherId: number
  publisherName: string
  scopeType: 'ALL' | 'GRADE' | 'CLASS'
  gradeId?: number
  gradeName?: string
  classId?: number
  className?: string
  title: string
  content: string
  status: 'DRAFT' | 'PUBLISHED' | 'WITHDRAWN'
  publishedAt?: string
  withdrawnAt?: string
  isRead?: boolean
  readAt?: string
  readCount: number
  attachments?: FileAttachmentInfo[]
  createdAt: string
  updatedAt: string
}

export interface NoticeOptions {
  grades: NoticeOption[]
  classes: NoticeOption[]
}

export interface NoticeOption {
  id: number
  label: string
  secondary?: string
}

export interface NoticeReader {
  userId: number
  username: string
  realName: string
  roleCode: string
  roleName: string
  readAt: string
}

export interface NoticeSavePayload {
  title: string
  content: string
  scopeType: 'ALL' | 'GRADE' | 'CLASS'
  gradeId?: number
  classId?: number
  attachmentIds: number[]
}

