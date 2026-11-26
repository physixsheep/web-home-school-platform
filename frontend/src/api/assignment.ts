import { request } from './http'
import type {
  AssignmentOptions,
  AssignmentRow,
  AssignmentSavePayload,
  SubmissionRow,
} from '../types/assignment'
import type { PageResult } from '../types/organization'

export const assignmentApi = {
  readable(params: {
    page: number
    size: number
    keyword?: string
    studentId?: number
  }) {
    return request<PageResult<AssignmentRow>>({
      method: 'get',
      url: '/assignments',
      params,
    })
  },

  manage(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<AssignmentRow>>({
      method: 'get',
      url: '/assignments/manage',
      params,
    })
  },

  options() {
    return request<AssignmentOptions>({
      method: 'get',
      url: '/assignments/options',
    })
  },

  detail(assignmentId: number, studentId?: number) {
    return request<AssignmentRow>({
      method: 'get',
      url: `/assignments/${assignmentId}`,
      params: { studentId },
    })
  },

  create(data: AssignmentSavePayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/assignments',
      data,
    })
  },

  update(assignmentId: number, data: AssignmentSavePayload) {
    return request<void>({
      method: 'put',
      url: `/assignments/${assignmentId}`,
      data,
    })
  },

  publish(assignmentId: number) {
    return request<void>({
      method: 'post',
      url: `/assignments/${assignmentId}/publish`,
    })
  },

  close(assignmentId: number) {
    return request<void>({
      method: 'post',
      url: `/assignments/${assignmentId}/close`,
    })
  },

  deleteDraft(assignmentId: number) {
    return request<void>({
      method: 'delete',
      url: `/assignments/${assignmentId}`,
    })
  },

  submissions(
    assignmentId: number,
    params: {
      page: number
      size: number
      keyword?: string
      status?: string
    },
  ) {
    return request<PageResult<SubmissionRow>>({
      method: 'get',
      url: `/assignments/${assignmentId}/submissions`,
      params,
    })
  },

  submit(
    assignmentId: number,
    data: { content?: string; attachmentIds: number[] },
  ) {
    return request<{ submissionId: number }>({
      method: 'post',
      url: `/assignments/${assignmentId}/submission`,
      data,
    })
  },

  grade(
    submissionId: number,
    data: { score?: number; comment?: string },
  ) {
    return request<void>({
      method: 'post',
      url: `/submissions/${submissionId}/grade`,
      data,
    })
  },

  returnSubmission(submissionId: number, reason: string) {
    return request<void>({
      method: 'post',
      url: `/submissions/${submissionId}/return`,
      data: { reason },
    })
  },
}

