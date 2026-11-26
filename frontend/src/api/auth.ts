import { request } from './http'
import type { CurrentUser, LoginPayload, LoginResult } from '../types/auth'

export const authApi = {
  login(payload: LoginPayload) {
    return request<LoginResult>({
      method: 'post',
      url: '/auth/login',
      data: payload,
    })
  },

  logout() {
    return request<void>({
      method: 'post',
      url: '/auth/logout',
    })
  },

  me() {
    return request<CurrentUser>({
      method: 'get',
      url: '/auth/me',
    })
  },
}

