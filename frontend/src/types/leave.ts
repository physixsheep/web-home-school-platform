import type { FileAttachmentInfo } from './notice'

export interface LeaveRow {
  id: number
  studentId: number
  studentName: string
  studentNo: string
  classId: number
  className: string
  gradeName: string
  leaveType: 'SICK' | 'PERSONAL' | 'OTHER'
  startAt: string
  endAt: string
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  approverTeacherId?: number
  approverName?: string
  approvedAt?: string
  approvalOpinion?: string
  cancelledAt?: string
  version: number
  createdAt: string
  attachments?: FileAttachmentInfo[]
}

export interface LeaveCreatePayload {
  leaveType: 'SICK' | 'PERSONAL' | 'OTHER'
  startAt: string
  endAt: string
  reason: string
  attachmentIds: number[]
}

