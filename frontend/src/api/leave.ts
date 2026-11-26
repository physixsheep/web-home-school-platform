import { request } from './http'
import type { LeaveCreatePayload, LeaveRow } from '../types/leave'
import type { PageResult } from '../types/organization'

export const leaveApi = {
  page(params: {
    page: number
    size: number
    keyword?: string
    status?: string
    studentId?: number
  }) {
    return request<PageResult<LeaveRow>>({
      method: 'get',
      url: '/leaves',
      params,
    })
  },

  approvalPage(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<LeaveRow>>({
      method: 'get',
      url: '/leaves/approval',
      params,
    })
  },

  detail(leaveId: number) {
    return request<LeaveRow>({
      method: 'get',
      url: `/leaves/${leaveId}`,
    })
  },

  create(data: LeaveCreatePayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/leaves',
      data,
    })
  },

  cancel(leaveId: number) {
    return request<void>({
      method: 'post',
      url: `/leaves/${leaveId}/cancel`,
    })
  },

  decide(
    leaveId: number,
    data: { decision: 'APPROVED' | 'REJECTED'; opinion: string },
  ) {
    return request<void>({
      method: 'post',
      url: `/leaves/${leaveId}/decision`,
      data,
    })
  },
}

