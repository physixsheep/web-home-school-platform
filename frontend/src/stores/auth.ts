import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { authApi } from '../api/auth'
import { TOKEN_KEY } from '../api/http'
import type { CurrentUser, LoginPayload, RoleCode } from '../types/auth'

const roleHomes: Record<RoleCode, string> = {
  ADMIN: '/admin/home',
  TEACHER: '/teacher/home',
  STUDENT: '/student/home',
  PARENT: '/parent/home',
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<CurrentUser | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value))
  const primaryRole = computed<RoleCode | null>(() => user.value?.roles[0] ?? null)
  const roleHome = computed(() =>
    primaryRole.value ? roleHomes[primaryRole.value] : '/login',
  )

  function setSession(nextToken: string, nextUser: CurrentUser) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(TOKEN_KEY, nextToken)
  }

  function clearSession() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const result = await authApi.login(payload)
      setSession(result.token, result.user)
      return result
    } finally {
      loading.value = false
    }
  }

  async function loadCurrentUser() {
    if (!token.value) {
      return null
    }
    user.value = await authApi.me()
    return user.value
  }

  async function logout() {
    loading.value = true
    try {
      if (token.value) {
        await authApi.logout()
      }
    } finally {
      clearSession()
      loading.value = false
    }
  }

  return {
    token,
    user,
    loading,
    isAuthenticated,
    primaryRole,
    roleHome,
    login,
    loadCurrentUser,
    logout,
    clearSession,
  }
})

