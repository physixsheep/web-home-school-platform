<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { useAuthStore } from '../stores/auth'
import type { RoleCode } from '../types/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const roleNames: Record<RoleCode, string> = {
  ADMIN: '学校管理员',
  TEACHER: '教师',
  STUDENT: '学生',
  PARENT: '家长',
}

const homeRouteByRole: Record<RoleCode, string> = {
  ADMIN: '/admin/home',
  TEACHER: '/teacher/home',
  STUDENT: '/student/home',
  PARENT: '/parent/home',
}

const currentRole = computed(() => auth.primaryRole)
const currentRoleName = computed(() =>
  currentRole.value ? roleNames[currentRole.value] : '未分配角色',
)
const homePath = computed(() =>
  currentRole.value ? homeRouteByRole[currentRole.value] : '/login',
)
const pageTitle = computed(() => route.meta.homeTitle ?? '工作台')

const adminMenu = [
  { path: '/admin/home', label: '管理员首页' },
  { path: '/admin/users', label: '用户账号' },
  { path: '/admin/teachers', label: '教师管理' },
  { path: '/admin/students', label: '学生管理' },
  { path: '/admin/parents', label: '家长管理' },
  { path: '/admin/grades', label: '年级管理' },
  { path: '/admin/classes', label: '班级管理' },
  { path: '/admin/notices', label: '通知管理' },
  { path: '/admin/exams', label: '考试管理' },
  { path: '/admin/subjects', label: '学科管理' },
  { path: '/admin/leaves', label: '请假记录' },
  { path: '/admin/growth-records', label: '成长档案' },
  { path: '/admin/messages', label: '留言审计' },
  { path: '/admin/operation-logs', label: '操作日志' },
  { path: '/admin/teacher-assignments', label: '教师任课' },
  { path: '/admin/student-classes', label: '学生分班' },
  { path: '/admin/parent-bindings', label: '家长绑定' },
]

const teacherMenu = [
  { path: '/teacher/home', label: '教师工作台' },
  { path: '/teacher/assignments', label: '作业管理' },
  { path: '/teacher/scores', label: '成绩管理' },
  { path: '/teacher/leaves', label: '请假审批' },
  { path: '/teacher/growth-records', label: '成长档案' },
  { path: '/teacher/messages', label: '家校互动' },
  { path: '/teacher/notices', label: '通知管理' },
  { path: '/teacher/notices/read', label: '通知中心' },
]

const studentMenu = [
  { path: '/student/home', label: '学生首页' },
  { path: '/student/assignments', label: '我的作业' },
  { path: '/student/scores', label: '我的成绩' },
  { path: '/student/leaves', label: '我的请假' },
  { path: '/student/growth-records', label: '成长档案' },
  { path: '/student/messages', label: '家校留言' },
  { path: '/student/notices', label: '校园通知' },
]

const parentMenu = [
  { path: '/parent/home', label: '家长首页' },
  { path: '/parent/assignments', label: '孩子作业' },
  { path: '/parent/scores', label: '孩子成绩' },
  { path: '/parent/leaves', label: '请假记录' },
  { path: '/parent/growth-records', label: '成长档案' },
  { path: '/parent/messages', label: '家校互动' },
  { path: '/parent/notices', label: '校园通知' },
]

const menuItems = computed(() => {
  if (auth.primaryRole === 'ADMIN') {
    return adminMenu
  }
  if (auth.primaryRole === 'TEACHER') {
    return teacherMenu
  }
  if (auth.primaryRole === 'STUDENT') {
    return studentMenu
  }
  if (auth.primaryRole === 'PARENT') {
    return parentMenu
  }
  return [{ path: homePath.value, label: pageTitle.value }]
})

async function logout() {
  try {
    await ElMessageBox.confirm('退出后当前账号已签发的 Token 将失效，是否继续？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await auth.logout()
    ElMessage.success('已安全退出')
  } finally {
    await router.replace('/login')
  }
}
</script>

<template>
  <ElContainer class="app-shell">
    <ElAside class="sidebar" width="244px">
      <div class="logo">
        <div class="logo-mark">HS</div>
        <div>
          <strong>家校协同平台</strong>
          <span>智慧互通 · 成长共育</span>
        </div>
      </div>

      <div class="role-card">
        <span class="role-label">当前身份</span>
        <strong>{{ currentRoleName }}</strong>
      </div>

      <ElMenu
        :default-active="route.path"
        background-color="transparent"
        router
        text-color="rgba(255,255,255,.7)"
        active-text-color="#ffffff"
      >
        <ElMenuItem v-for="item in menuItems" :key="item.path" :index="item.path">
          <span class="menu-dot" />
          {{ item.label }}
        </ElMenuItem>
      </ElMenu>

      <div class="sidebar-footer">
        <span>平台版本</span>
        <strong>v1.0 · Integration</strong>
      </div>
    </ElAside>

    <ElContainer>
      <ElHeader class="topbar">
        <div>
          <p class="breadcrumb">家校协同 / {{ currentRoleName }}</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="user-area">
          <div class="user-avatar">
            {{ auth.user?.realName?.slice(0, 1) ?? '?' }}
          </div>
          <div class="user-meta">
            <strong>{{ auth.user?.realName }}</strong>
            <span>{{ auth.user?.username }}</span>
          </div>
          <ElButton plain @click="logout">退出登录</ElButton>
        </div>
      </ElHeader>

      <ElMain class="main-content">
        <RouterView />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow-x: hidden;
  overflow-y: auto;
  color: #fff;
  background:
    radial-gradient(circle at 0 0, rgba(52, 124, 225, 0.55), transparent 40%),
    linear-gradient(180deg, #0b326f 0%, #0e2857 100%);
}

.sidebar::after {
  position: absolute;
  right: -150px;
  bottom: -170px;
  width: 340px;
  height: 340px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: "";
}

.logo {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 30px 24px 26px;
}

.logo-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  color: #0b326f;
  background: #fff;
  font-size: 14px;
  font-weight: 800;
}

.logo strong,
.logo span {
  display: block;
}

.logo strong {
  font-size: 16px;
}

.logo span {
  margin-top: 5px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.role-card {
  display: flex;
  justify-content: space-between;
  margin: 4px 18px 24px;
  padding: 13px 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
}

.role-label {
  color: rgba(255, 255, 255, 0.52);
}

:deep(.el-menu-item) {
  height: 48px;
  margin: 6px 12px;
  border-radius: 9px;
  font-size: 14px;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2b6ed0, #2f7be5);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.16);
}

.menu-dot {
  width: 7px;
  height: 7px;
  margin-right: 12px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.sidebar-footer {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  padding: 20px 24px;
  color: rgba(255, 255, 255, 0.46);
  font-size: 11px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 92px;
  padding: 0 34px;
  border-bottom: 1px solid #e6ebf3;
  background: rgba(255, 255, 255, 0.92);
}

.breadcrumb {
  margin: 0 0 5px;
  color: #8794a8;
  font-size: 12px;
}

.topbar h1 {
  margin: 0;
  color: #14213d;
  font-size: 24px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 11px;
  color: #fff;
  background: linear-gradient(145deg, #215fc0, #3d8af0);
  font-weight: 700;
}

.user-meta {
  min-width: 88px;
}

.user-meta strong,
.user-meta span {
  display: block;
}

.user-meta strong {
  color: #213453;
  font-size: 13px;
}

.user-meta span {
  margin-top: 3px;
  color: #8d98aa;
  font-size: 11px;
}

.main-content {
  padding: 30px 34px 42px;
  background: #f4f7fc;
}
</style>

