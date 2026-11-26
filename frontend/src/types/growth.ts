import type { FileAttachmentInfo } from './notice'

export type GrowthCategory =
  | 'STUDY'
  | 'MORAL'
  | 'ACTIVITY'
  | 'HONOR'
  | 'COMPREHENSIVE'

export interface GrowthRow {
  id: number
  studentId: number
  studentName: string
  studentNo: string
  classId: number
  className: string
  gradeName: string
  category: GrowthCategory
  title: string
  content: string
  occurredAt: string
  recorderTeacherId: number
  recorderName: string
  visibleToParent: number
  createdAt: string
  updatedAt: string
  attachments?: FileAttachmentInfo[]
}

export interface GrowthSavePayload {
  studentId: number
  category: GrowthCategory
  title: string
  content: string
  occurredAt: string
  visibleToParent: boolean
  attachmentIds: number[]
}

