import type { TeachingOption } from './assignment'

export interface ExamRow {
  id: number
  name: string
  examType: string
  gradeId: number
  gradeName: string
  classId?: number
  className?: string
  academicYear: string
  term: string
  examDate: string
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  createdAt: string
}

export interface ExamPayload {
  name: string
  examType: string
  gradeId: number
  classId?: number
  academicYear: string
  term: string
  examDate: string
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
}

export interface SubjectRow {
  id: number
  subjectCode: string
  subjectName: string
  defaultFullScore: number
  sortOrder: number
  status: 'ENABLED' | 'DISABLED'
  createdAt: string
}

export interface SubjectPayload {
  subjectCode: string
  subjectName: string
  defaultFullScore: number
  sortOrder: number
  status: 'ENABLED' | 'DISABLED'
}

export interface ScoreEntryRow {
  studentId: number
  studentName: string
  studentNo: string
  score?: number
  isAbsent: number
  remark?: string
}

export interface ExamOption {
  id: number
  name: string
  examDate: string
  gradeId: number
  classId?: number
  gradeName: string
  className?: string
}

export interface ScoreWorkspaceOptions {
  exams: ExamOption[]
  teachings: TeachingOption[]
}

export interface ScoreItem {
  examId: number
  examName: string
  examType: string
  examDate: string
  academicYear: string
  term: string
  classId: number
  className: string
  subjectId: number
  subjectName: string
  score?: number
  fullScore: number
  isAbsent: number
  remark?: string
}

export interface ScoreTrend {
  examId: number
  examName: string
  examDate: string
  academicYear: string
  term: string
  totalScore: number
  averageScore: number
  subjectCount: number
}

export interface ScoreReport {
  studentId: number
  studentName: string
  studentNo: string
  items: ScoreItem[]
  trends: ScoreTrend[]
}

export interface ScoreStats {
  enteredCount: number
  absentCount: number
  averageScore?: number
  highestScore?: number
  lowestScore?: number
}

