export interface NameValue {
  name: string
  value: number
}

export interface AdminDashboard {
  studentCount: number
  teacherCount: number
  parentCount: number
  classCount: number
  publishedNoticeCount: number
  pendingLeaveCount: number
  studentsByGrade: NameValue[]
  noticesByScope: NameValue[]
}

export interface TeacherClassStat {
  classId: number
  className: string
  gradeName: string
  studentCount: number
  assignmentCount: number
  submittedCount: number
}

export interface TeacherDashboard {
  classCount: number
  studentCount: number
  publishedAssignmentCount: number
  submissionRate: number
  pendingGradingCount: number
  pendingLeaveCount: number
  classes: TeacherClassStat[]
}

