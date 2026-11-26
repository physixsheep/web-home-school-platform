export interface OperationLogRow {
  id: number
  userId?: number
  username?: string
  roles?: string
  module: string
  operation: string
  httpMethod: string
  requestUri: string
  requestParams?: string
  resultCode: number
  clientIp?: string
  userAgent?: string
  durationMs: number
  createdAt: string
}

