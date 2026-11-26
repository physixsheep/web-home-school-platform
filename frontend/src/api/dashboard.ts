import { request } from './http'
import type { AdminDashboard, TeacherDashboard } from '../types/dashboard'

export const dashboardApi = {
  admin() {
    return request<AdminDashboard>({
      method: 'get',
      url: '/dashboard/admin',
    })
  },

  teacher() {
    return request<TeacherDashboard>({
      method: 'get',
      url: '/dashboard/teacher',
    })
  },
}

