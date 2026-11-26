import { request } from './http'
import type {
  ExamPayload,
  ExamRow,
  ScoreEntryRow,
  ScoreReport,
  ScoreStats,
  ScoreWorkspaceOptions,
  SubjectPayload,
  SubjectRow,
} from '../types/score'
import type { PageResult } from '../types/organization'

export const scoreApi = {
  exams(params: {
    page: number
    size: number
    keyword?: string
    status?: string
    academicYear?: string
  }) {
    return request<PageResult<ExamRow>>({
      method: 'get',
      url: '/admin/exams',
      params,
    })
  },

  createExam(data: ExamPayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/exams',
      data,
    })
  },

  updateExam(examId: number, data: ExamPayload) {
    return request<void>({
      method: 'put',
      url: `/admin/exams/${examId}`,
      data,
    })
  },

  deleteExam(examId: number) {
    return request<void>({
      method: 'delete',
      url: `/admin/exams/${examId}`,
    })
  },

  subjects(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<SubjectRow>>({
      method: 'get',
      url: '/admin/subjects',
      params,
    })
  },

  createSubject(data: SubjectPayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/subjects',
      data,
    })
  },

  updateSubject(subjectId: number, data: SubjectPayload) {
    return request<void>({
      method: 'put',
      url: `/admin/subjects/${subjectId}`,
      data,
    })
  },

  deleteSubject(subjectId: number) {
    return request<void>({
      method: 'delete',
      url: `/admin/subjects/${subjectId}`,
    })
  },

  workspaceOptions() {
    return request<ScoreWorkspaceOptions>({
      method: 'get',
      url: '/scores/workspace-options',
    })
  },

  entryRows(examId: number, classId: number, subjectId: number) {
    return request<ScoreEntryRow[]>({
      method: 'get',
      url: '/scores/entry',
      params: { examId, classId, subjectId },
    })
  },

  batchSave(data: {
    examId: number
    classId: number
    subjectId: number
    records: {
      studentId: number
      score?: number
      absent: boolean
      remark?: string
    }[]
  }) {
    return request<void>({
      method: 'put',
      url: '/scores/batch',
      data,
    })
  },

  statistics(examId: number, classId: number, subjectId: number) {
    return request<ScoreStats>({
      method: 'get',
      url: '/scores/statistics',
      params: { examId, classId, subjectId },
    })
  },

  report(studentId?: number) {
    return request<ScoreReport>({
      method: 'get',
      url: '/scores/report',
      params: { studentId },
    })
  },
}

