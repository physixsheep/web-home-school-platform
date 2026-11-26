export type RoleCode = 'ADMIN' | 'TEACHER' | 'STUDENT' | 'PARENT'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface CurrentUser {
  id: number
  username: string
  realName: string
  roles: RoleCode[]
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  user: CurrentUser
}

