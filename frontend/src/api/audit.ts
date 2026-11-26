import { request } from './http'
import type { OperationLogRow } from '../types/audit'
import type { PageResult } from '../types/organization'

export const auditApi = {
  logs(params: {
    page: number
    size: number
    keyword?: string
    module?: string
    resultCode?: number
    dateFrom?: string
    dateTo?: string
  }) {
    return request<PageResult<OperationLogRow>>({
      method: 'get',
      url: '/admin/operation-logs',
      params,
    })
  },
}

