import type { FileAttachmentInfo } from './notice'

export interface AssignmentRow {
  id: number
  classId: number
  className: string
  gradeName: string
  teacherId: number
  teacherName: string
  subjectId: number
  subjectName: string
  title: string
  content: string
  deadline: string
  maxScore?: number
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED'
  publishedAt?: string
  studentCount?: number
  submittedCount?: number
  gradedCount?: number
  unsubmittedCount?: number
  submissionId?: number
  targetStudentId?: number
  submissionStatus?: 'UNSUBMITTED' | 'SUBMITTED' | 'GRADED' | 'RETURNED'
  submissionContent?: string
  attemptCount?: number
  submittedAt?: string
  score?: number
  teacherComment?: string
  gradedAt?: string
  returnReason?: string
  attachments?: FileAttachmentInfo[]
  submissionAttachments?: FileAttachmentInfo[]
}

export interface SubmissionRow {
  studentId: number
  studentName: string
  studentNo: string
  submissionId?: number
  status: 'UNSUBMITTED' | 'SUBMITTED' | 'GRADED' | 'RETURNED'
  attemptCount?: number
  content?: string
  submittedAt?: string
  score?: number
  comment?: string
  gradedAt?: string
  returnReason?: string
  attachments?: FileAttachmentInfo[]
}

export interface TeachingOption {
  classId: number
  className: string
  gradeId: number
  gradeName: string
  subjectId: number
  subjectName: string
  fullScore: number
}

export interface AssignmentOptions {
  teachings: TeachingOption[]
  children: { id: number; label: string; secondary?: string }[]
}

export interface AssignmentSavePayload {
  title: string
  content: string
  classId: number
  subjectId: number
  deadline: string
  maxScore?: number
  attachmentIds: number[]
}

