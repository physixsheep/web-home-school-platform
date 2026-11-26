import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import type { ApiResponse } from '../types/auth'

export const TOKEN_KEY = 'home-school-access-token'

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status as number | undefined
    const message =
      error.response?.data?.message ??
      (status ? `请求失败（${status}）` : '网络连接失败，请确认后端服务已启动')

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      }
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResponse<T>>(config)
  return response.data.data
}

export async function downloadAuthenticated(url: string, fallbackName: string) {
  const response = await client.get<Blob>(url, { responseType: 'blob' })
  const disposition = response.headers['content-disposition'] as string | undefined
  let filename = fallbackName
  if (disposition) {
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
    if (utf8Match?.[1]) {
      filename = decodeURIComponent(utf8Match[1])
    } else if (plainMatch?.[1]) {
      filename = plainMatch[1]
    }
  }
  const objectUrl = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(objectUrl)
}


