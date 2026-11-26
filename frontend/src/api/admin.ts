import { request } from './http'
import type { RoleCode } from '../types/auth'
import type {
  ClassPayload,
  ClassRow,
  CreateUserPayload,
  EntityOption,
  GradePayload,
  GradeRow,
  PageResult,
  ParentBindingRow,
  RoleOption,
  StudentClassRow,
  TeacherAssignmentRow,
  UserQuery,
  UserRow,
} from '../types/organization'

export const adminApi = {
  users(params: UserQuery) {
    return request<PageResult<UserRow>>({
      method: 'get',
      url: '/admin/users',
      params,
    })
  },

  roles() {
    return request<RoleOption[]>({
      method: 'get',
      url: '/admin/roles',
    })
  },

  createUser(data: CreateUserPayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/users',
      data,
    })
  },

  updateUser(
    userId: number,
    data: { realName: string; phone?: string; email?: string },
  ) {
    return request<void>({
      method: 'put',
      url: `/admin/users/${userId}`,
      data,
    })
  },

  updateUserStatus(userId: number, status: 'ENABLED' | 'DISABLED') {
    return request<void>({
      method: 'patch',
      url: `/admin/users/${userId}/status`,
      data: { status },
    })
  },

  resetPassword(userId: number, newPassword: string) {
    return request<void>({
      method: 'post',
      url: `/admin/users/${userId}/reset-password`,
      data: { newPassword },
    })
  },

  grades(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<GradeRow>>({
      method: 'get',
      url: '/admin/grades',
      params,
    })
  },

  createGrade(data: GradePayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/grades',
      data,
    })
  },

  updateGrade(gradeId: number, data: GradePayload) {
    return request<void>({
      method: 'put',
      url: `/admin/grades/${gradeId}`,
      data,
    })
  },

  deleteGrade(gradeId: number) {
    return request<void>({
      method: 'delete',
      url: `/admin/grades/${gradeId}`,
    })
  },

  classes(params: {
    page: number
    size: number
    keyword?: string
    gradeId?: number
    academicYear?: string
    status?: string
  }) {
    return request<PageResult<ClassRow>>({
      method: 'get',
      url: '/admin/classes',
      params,
    })
  },

  createClass(data: ClassPayload) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/classes',
      data,
    })
  },

  updateClass(classId: number, data: ClassPayload) {
    return request<void>({
      method: 'put',
      url: `/admin/classes/${classId}`,
      data,
    })
  },

  deleteClass(classId: number) {
    return request<void>({
      method: 'delete',
      url: `/admin/classes/${classId}`,
    })
  },

  classOptions() {
    return request<EntityOption[]>({
      method: 'get',
      url: '/admin/options/classes',
    })
  },

  teacherOptions() {
    return request<EntityOption[]>({
      method: 'get',
      url: '/admin/options/teachers',
    })
  },

  studentOptions() {
    return request<EntityOption[]>({
      method: 'get',
      url: '/admin/options/students',
    })
  },

  parentOptions() {
    return request<EntityOption[]>({
      method: 'get',
      url: '/admin/options/parents',
    })
  },

  subjectOptions() {
    return request<EntityOption[]>({
      method: 'get',
      url: '/admin/options/subjects',
    })
  },

  parentBindings(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<ParentBindingRow>>({
      method: 'get',
      url: '/admin/parent-bindings',
      params,
    })
  },

  createParentBinding(data: {
    parentId: number
    studentId: number
    relationship: string
    isPrimary: number
  }) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/parent-bindings',
      data,
    })
  },

  updateParentBindingStatus(bindingId: number, status: 'ACTIVE' | 'REVOKED') {
    return request<void>({
      method: 'patch',
      url: `/admin/parent-bindings/${bindingId}/status`,
      data: { status },
    })
  },

  teacherAssignments(params: {
    page: number
    size: number
    keyword?: string
    relationType?: string
    status?: string
  }) {
    return request<PageResult<TeacherAssignmentRow>>({
      method: 'get',
      url: '/admin/teacher-assignments',
      params,
    })
  },

  createTeacherAssignment(data: {
    teacherId: number
    classId: number
    subjectId?: number
    relationType: 'HOMEROOM' | 'SUBJECT'
    startDate: string
    endDate?: string
  }) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/teacher-assignments',
      data,
    })
  },

  endTeacherAssignment(assignmentId: number) {
    return request<void>({
      method: 'patch',
      url: `/admin/teacher-assignments/${assignmentId}/end`,
    })
  },

  studentClasses(params: {
    page: number
    size: number
    keyword?: string
    status?: string
  }) {
    return request<PageResult<StudentClassRow>>({
      method: 'get',
      url: '/admin/student-classes',
      params,
    })
  },

  assignStudentClass(data: {
    studentId: number
    classId: number
    startDate: string
  }) {
    return request<{ id: number }>({
      method: 'post',
      url: '/admin/student-classes',
      data,
    })
  },
}

export type { RoleCode }

