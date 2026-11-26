import type { RoleCode } from './auth'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface RoleOption {
  id: number
  roleCode: RoleCode
  roleName: string
  description: string
}

export interface UserRow {
  id: number
  username: string
  realName: string
  phone?: string
  email?: string
  status: 'ENABLED' | 'DISABLED'
  lastLoginAt?: string
  roleCode: RoleCode
  roleName: string
  identifier?: string
  gender?: string
  extraInfo?: string
}

export interface UserQuery {
  page: number
  size: number
  keyword?: string
  roleCode?: RoleCode | ''
  status?: string
}

export interface CreateUserPayload {
  username: string
  password: string
  realName: string
  phone?: string
  email?: string
  roleCode: RoleCode
  identifier?: string
  gender?: string
  title?: string
  specialty?: string
  hireDate?: string
  birthDate?: string
  enrollmentDate?: string
  occupation?: string
  emergencyContact?: string
}

export interface GradeRow {
  id: number
  gradeName: string
  gradeLevel: number
  sortOrder: number
  status: 'ENABLED' | 'DISABLED'
  createdAt: string
}

export interface GradePayload {
  gradeName: string
  gradeLevel: number
  sortOrder: number
  status: 'ENABLED' | 'DISABLED'
}

export interface ClassRow {
  id: number
  gradeId: number
  gradeName: string
  className: string
  academicYear: string
  graduationYear?: number
  status: 'ENABLED' | 'DISABLED'
  studentCount: number
  teacherCount: number
  createdAt: string
}

export interface ClassPayload {
  gradeId: number
  className: string
  academicYear: string
  graduationYear?: number
  status: 'ENABLED' | 'DISABLED'
}

export interface EntityOption {
  id: number
  label: string
  secondary?: string
}

export interface ParentBindingRow {
  id: number
  parentId: number
  parentName: string
  parentAccount: string
  studentId: number
  studentName: string
  studentNo: string
  relationship: 'FATHER' | 'MOTHER' | 'GUARDIAN'
  isPrimary: number
  status: 'PENDING' | 'ACTIVE' | 'REVOKED'
  verifiedAt?: string
  createdAt: string
}

export interface TeacherAssignmentRow {
  id: number
  teacherId: number
  teacherName: string
  employeeNo: string
  classId: number
  className: string
  gradeName: string
  subjectId?: number
  subjectName?: string
  relationType: 'HOMEROOM' | 'SUBJECT'
  startDate: string
  endDate?: string
  status: 'ACTIVE' | 'ENDED'
  createdAt: string
}

export interface StudentClassRow {
  id: number
  studentId: number
  studentName: string
  studentNo: string
  classId: number
  className: string
  gradeName: string
  academicYear: string
  startDate: string
  endDate?: string
  status: 'CURRENT' | 'ENDED'
  createdAt: string
}

