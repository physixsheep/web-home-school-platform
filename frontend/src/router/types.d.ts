import type { RoleCode } from '../types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: RoleCode[]
    homeTitle?: string
  }
}

export {}

