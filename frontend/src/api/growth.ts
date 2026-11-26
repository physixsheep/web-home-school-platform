import { request } from './http'
import type { GrowthSavePayload, GrowthRow } from '../types/growth'
import type { PageResult, EntityOption } from '../types/organization'

export const growthApi = {
  page(params: {
    page: number
    size: number
    keyword?: string
    category?: string
    studentId?: number
    dateFrom?: string
    dateTo?: string
  }) {
    return request<PageResult<GrowthRow>>({
      method: 'get',
      url: '/growth-records',
      params,
    })
  },

  manage(params: {
    page: number
    size: number
    keyword?: string
    category?: string
    studentId?: number
    dateFrom?: string
    dateTo?: string
  }) {
    return request<PageResult<GrowthRow>>({
      method: 'get',
      url: '/growth-records/manage',
      params,
    })
  },

  options() {
    return request<EntityOption[]>({
      method: 'get',
      url: '/growth-records/options/students',
    })
  },

  detail(recordId: number) {
    return request<GrowthRow>({
      method: 'get',
      url: `/growth-records/${recordId}`,
    })
  },

  create(data: GrowthSavePayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/growth-records',
      data,
    })
  },

  update(recordId: number, data: GrowthSavePayload) {
    return request<void>({
      method: 'put',
      url: `/growth-records/${recordId}`,
      data,
    })
  },

  delete(recordId: number) {
    return request<void>({
      method: 'delete',
      url: `/growth-records/${recordId}`,
    })
  },
}

