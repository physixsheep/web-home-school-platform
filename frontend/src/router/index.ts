import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/auth/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/login',
        },
        {
          path: 'admin/home',
          component: () => import('../views/dashboard/AdminDashboardView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '管理员首页',
          },
        },
        {
          path: 'admin/users',
          component: () => import('../views/admin/UserDirectoryView.vue'),
          props: { fixedRole: '' },
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '用户账号管理',
          },
        },
        {
          path: 'admin/teachers',
          component: () => import('../views/admin/UserDirectoryView.vue'),
          props: { fixedRole: 'TEACHER' },
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '教师管理',
          },
        },
        {
          path: 'admin/students',
          component: () => import('../views/admin/UserDirectoryView.vue'),
          props: { fixedRole: 'STUDENT' },
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '学生管理',
          },
        },
        {
          path: 'admin/parents',
          component: () => import('../views/admin/UserDirectoryView.vue'),
          props: { fixedRole: 'PARENT' },
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '家长管理',
          },
        },
        {
          path: 'admin/grades',
          component: () => import('../views/admin/GradeManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '年级管理',
          },
        },
        {
          path: 'admin/classes',
          component: () => import('../views/admin/ClassManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '班级管理',
          },
        },
        {
          path: 'admin/notices',
          component: () => import('../views/notice/NoticeManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '通知管理',
          },
        },
        {
          path: 'admin/exams',
          component: () => import('../views/admin/ExamManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '考试管理',
          },
        },
        {
          path: 'admin/subjects',
          component: () => import('../views/admin/SubjectManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '学科管理',
          },
        },
        {
          path: 'admin/leaves',
          component: () => import('../views/leave/LeaveManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '全校请假记录',
          },
        },
        {
          path: 'admin/growth-records',
          component: () => import('../views/growth/GrowthCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '成长档案总览',
          },
        },
        {
          path: 'admin/messages',
          component: () => import('../views/interaction/InteractionInboxView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '全校留言审计',
          },
        },
        {
          path: 'admin/operation-logs',
          component: () => import('../views/admin/OperationLogView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '操作日志',
          },
        },
        {
          path: 'admin/teacher-assignments',
          component: () => import('../views/admin/TeacherAssignmentView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '教师任课',
          },
        },
        {
          path: 'admin/student-classes',
          component: () => import('../views/admin/StudentClassView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '学生分班',
          },
        },
        {
          path: 'admin/parent-bindings',
          component: () => import('../views/admin/ParentBindingView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['ADMIN'],
            homeTitle: '家长绑定',
          },
        },
        {
          path: 'teacher/home',
          component: () => import('../views/dashboard/TeacherDashboardView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '教师工作台',
          },
        },
        {
          path: 'teacher/notices',
          component: () => import('../views/notice/NoticeManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '通知管理',
          },
        },
        {
          path: 'teacher/notices/read',
          component: () => import('../views/notice/NoticeCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '通知中心',
          },
        },
        {
          path: 'teacher/assignments',
          component: () => import('../views/assignment/AssignmentManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '作业管理',
          },
        },
        {
          path: 'teacher/scores',
          component: () => import('../views/score/TeacherScoreView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '成绩管理',
          },
        },
        {
          path: 'teacher/leaves',
          component: () => import('../views/leave/LeaveManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '请假审批',
          },
        },
        {
          path: 'teacher/growth-records',
          component: () => import('../views/growth/GrowthManagementView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '成长档案管理',
          },
        },
        {
          path: 'teacher/messages',
          component: () => import('../views/interaction/InteractionInboxView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['TEACHER'],
            homeTitle: '家校互动',
          },
        },
        {
          path: 'student/home',
          component: () => import('../views/workspace/WorkspaceView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '学生首页',
          },
        },
        {
          path: 'student/notices',
          component: () => import('../views/notice/NoticeCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '校园通知',
          },
        },
        {
          path: 'student/assignments',
          component: () => import('../views/assignment/AssignmentCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '我的作业',
          },
        },
        {
          path: 'student/scores',
          component: () => import('../views/score/ScoreCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '我的成绩',
          },
        },
        {
          path: 'student/leaves',
          component: () => import('../views/leave/LeaveCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '我的请假',
          },
        },
        {
          path: 'student/growth-records',
          component: () => import('../views/growth/GrowthCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '成长档案',
          },
        },
        {
          path: 'student/messages',
          component: () => import('../views/interaction/InteractionCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['STUDENT'],
            homeTitle: '家校留言',
          },
        },
        {
          path: 'parent/home',
          component: () => import('../views/workspace/WorkspaceView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '家长首页',
          },
        },
        {
          path: 'parent/notices',
          component: () => import('../views/notice/NoticeCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '校园通知',
          },
        },
        {
          path: 'parent/assignments',
          component: () => import('../views/assignment/AssignmentCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '孩子作业',
          },
        },
        {
          path: 'parent/scores',
          component: () => import('../views/score/ScoreCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '孩子成绩',
          },
        },
        {
          path: 'parent/leaves',
          component: () => import('../views/leave/LeaveCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '请假记录',
          },
        },
        {
          path: 'parent/growth-records',
          component: () => import('../views/growth/GrowthCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '成长档案',
          },
        },
        {
          path: 'parent/messages',
          component: () => import('../views/interaction/InteractionCenterView.vue'),
          meta: {
            requiresAuth: true,
            roles: ['PARENT'],
            homeTitle: '家校互动',
          },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/error/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.path === '/login') {
    if (!auth.token) {
      return true
    }
    try {
      if (!auth.user) {
        await auth.loadCurrentUser()
      }
      return auth.roleHome
    } catch {
      auth.clearSession()
      return true
    }
  }

  if (!to.meta.requiresAuth) {
    return true
  }

  if (!auth.token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (!auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      auth.clearSession()
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }
  }

  const requiredRoles = to.meta.roles
  if (requiredRoles?.length && !requiredRoles.some((role) => auth.user?.roles.includes(role))) {
    return auth.roleHome
  }

  return true
})

window.addEventListener('auth:unauthorized', () => {
  const auth = useAuthStore()
  auth.clearSession()
  if (router.currentRoute.value.path !== '/login') {
    void router.replace('/login')
  }
})

export default router

