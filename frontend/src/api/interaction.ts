import { request } from './http'
import type { MessageDetail, MessageRow } from '../types/interaction'
import type { EntityOption, PageResult } from '../types/organization'

export const interactionApi = {
  mine(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<MessageRow>>({
      method: 'get',
      url: '/messages/mine',
      params,
    })
  },

  received(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<MessageRow>>({
      method: 'get',
      url: '/messages/received',
      params,
    })
  },

  teacherOptions(studentId?: number) {
    return request<EntityOption[]>({
      method: 'get',
      url: '/messages/options/teachers',
      params: { studentId },
    })
  },

  detail(messageId: number) {
    return request<MessageDetail>({
      method: 'get',
      url: `/messages/${messageId}`,
    })
  },

  create(data: {
    studentId: number
    teacherId: number
    subject: string
    content: string
  }) {
    return request<{ id: number }>({
      method: 'post',
      url: '/messages',
      data,
    })
  },

  markRead(messageId: number) {
    return request<void>({
      method: 'post',
      url: `/messages/${messageId}/read`,
    })
  },

  reply(messageId: number, content: string) {
    return request<void>({
      method: 'post',
      url: `/messages/${messageId}/replies`,
      data: { content },
    })
  },

  close(messageId: number) {
    return request<void>({
      method: 'post',
      url: `/messages/${messageId}/close`,
    })
  },
}

