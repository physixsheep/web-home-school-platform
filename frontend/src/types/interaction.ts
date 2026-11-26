export interface MessageRow {
  id: number
  senderUserId: number
  senderName: string
  senderRole: 'STUDENT' | 'PARENT'
  studentId: number
  studentName: string
  studentNo: string
  className: string
  gradeName: string
  receiverTeacherId: number
  receiverName: string
  subject: string
  content: string
  status: 'UNREAD' | 'READ' | 'REPLIED' | 'CLOSED'
  lastReplyAt?: string
  closedAt?: string
  createdAt: string
  updatedAt: string
  replyCount: number
  lastReplyPreview?: string
}

export interface MessageReply {
  id: number
  replierUserId: number
  replierName: string
  replierRole: 'TEACHER' | 'STUDENT' | 'PARENT'
  content: string
  createdAt: string
}

export interface MessageDetail {
  message: MessageRow
  replies: MessageReply[]
}

