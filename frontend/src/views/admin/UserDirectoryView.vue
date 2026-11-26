<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import type { RoleCode } from '../../types/auth'
import type {
  CreateUserPayload,
  RoleOption,
  UserRow,
} from '../../types/organization'

const props = withDefaults(
  defineProps<{
    fixedRole?: RoleCode | ''
  }>(),
  {
    fixedRole: '',
  },
)

const roleNames: Record<RoleCode, string> = {
  ADMIN: '学校管理员',
  TEACHER: '教师',
  STUDENT: '学生',
  PARENT: '家长',
}

const rows = ref<UserRow[]>([])
const roles = ref<RoleOption[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const query = reactive({
  keyword: '',
  roleCode: '',
  status: '',
  page: 1,
  size: 10,
})

const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const resetVisible = ref(false)
const resetFormRef = ref<FormInstance>()
const currentUser = ref<UserRow | null>(null)

function emptyCreateForm(): CreateUserPayload {
  return {
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    roleCode: (props.fixedRole || 'TEACHER') as RoleCode,
    identifier: '',
    gender: '',
    title: '',
    specialty: '',
    hireDate: '',
    birthDate: '',
    enrollmentDate: '',
    occupation: '',
    emergencyContact: '',
  }
}

const createForm = reactive<CreateUserPayload>(emptyCreateForm())
const editForm = reactive({
  realName: '',
  phone: '',
  email: '',
})
const resetForm = reactive({
  newPassword: '',
})

const profileRole = computed<RoleCode>(
  () => (props.fixedRole || createForm.roleCode) as RoleCode,
)
const pageTitle = computed(() =>
  props.fixedRole ? roleNames[props.fixedRole] + '管理' : '用户账号管理',
)
const identifierLabel = computed(() =>
  profileRole.value === 'TEACHER' ? '教师工号' : '学生学号',
)

const createRules: FormRules<CreateUserPayload> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 50, message: '账号不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度必须为 8 到 72 个字符', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  identifier: [
    {
      validator: (_rule, value, callback) => {
        if (['TEACHER', 'STUDENT'].includes(profileRole.value) && !value) {
          callback(new Error(identifierLabel.value + '不能为空'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const editRules: FormRules<typeof editForm> = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const resetRules: FormRules<typeof resetForm> = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度必须为 8 到 72 个字符', trigger: 'blur' },
  ],
}

async function loadData() {
  loading.value = true
  try {
    const result = await adminApi.users({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      roleCode: props.fixedRole || ((query.roleCode || undefined) as RoleCode | undefined),
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roles.value = await adminApi.roles()
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.roleCode = ''
  query.status = ''
  query.page = 1
  void loadData()
}

function openCreate() {
  Object.assign(createForm, emptyCreateForm())
  if (props.fixedRole) {
    createForm.roleCode = props.fixedRole
  }
  createVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const payload = { ...createForm }
    payload.phone ||= undefined
    payload.email ||= undefined
    payload.identifier ||= undefined
    payload.gender ||= undefined
    payload.title ||= undefined
    payload.specialty ||= undefined
    payload.hireDate ||= undefined
    payload.birthDate ||= undefined
    payload.enrollmentDate ||= undefined
    payload.occupation ||= undefined
    payload.emergencyContact ||= undefined
    await adminApi.createUser(payload)
    ElMessage.success('用户和人员档案已创建')
    createVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

function openEdit(row: UserRow) {
  currentUser.value = row
  editForm.realName = row.realName
  editForm.phone = row.phone || ''
  editForm.email = row.email || ''
  editVisible.value = true
}

async function submitEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid || !currentUser.value) {
    return
  }
  saving.value = true
  try {
    await adminApi.updateUser(currentUser.value.id, {
      realName: editForm.realName,
      phone: editForm.phone || undefined,
      email: editForm.email || undefined,
    })
    ElMessage.success('用户资料已更新')
    editVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: UserRow) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = nextStatus === 'ENABLED' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确认${action}账号“${row.username}”吗？${nextStatus === 'DISABLED' ? '该账号现有 Token 将立即失效。' : ''}`,
      `${action}账号`,
      {
        type: 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await adminApi.updateUserStatus(row.id, nextStatus)
  ElMessage.success(`账号已${action}`)
  await loadData()
}

function openReset(row: UserRow) {
  currentUser.value = row
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function submitReset() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid || !currentUser.value) {
    return
  }
  saving.value = true
  try {
    await adminApi.resetPassword(currentUser.value.id, resetForm.newPassword)
    ElMessage.success('密码已重置，原 Token 已失效')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '尚未登录'
}

function roleName(code?: string) {
  return code ? roleNames[code as RoleCode] ?? code : '未分配角色'
}

watch(
  () => props.fixedRole,
  () => {
    query.page = 1
    query.roleCode = ''
    void loadData()
  },
)

onMounted(async () => {
  await Promise.all([loadData(), loadRoles()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>{{ pageTitle }}</h2>
        <p>账号状态和人员档案来自数据库，创建时同步建立对应角色档案。</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        新增{{ props.fixedRole ? roleNames[props.fixedRole] : '用户' }}
      </ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="账号 / 姓名 / 手机号 / 工号学号"
          @keyup.enter="search"
        />
        <ElSelect v-if="!props.fixedRole" v-model="query.roleCode" clearable placeholder="全部角色">
          <ElOption
            v-for="role in roles"
            :key="role.roleCode"
            :label="role.roleName"
            :value="role.roleCode"
          />
        </ElSelect>
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="启用" value="ENABLED" />
          <ElOption label="禁用" value="DISABLED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="没有符合条件的用户" stripe>
          <ElTableColumn label="姓名 / 账号">
            <template #default="{ row }">
              <div class="identity-cell">
                <span class="avatar">{{ row.realName.slice(0, 1) }}</span>
                <div>
                  <strong>{{ row.realName }}</strong>
                  <span>{{ row.username }}</span>
                </div>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="角色" width="130">
            <template #default="{ row }">
              <span class="role-pill">{{ row.roleName || roleName(row.roleCode) }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="identifier" label="工号 / 学号" width="140">
            <template #default="{ row }">
              {{ row.identifier || '—' }}
            </template>
          </ElTableColumn>
          <ElTableColumn prop="phone" label="手机号" width="140">
            <template #default="{ row }">
              {{ row.phone || '—' }}
            </template>
          </ElTableColumn>
          <ElTableColumn prop="email" label="邮箱" min-width="190">
            <template #default="{ row }">
              {{ row.email || '—' }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="row.status === 'ENABLED' ? 'success' : 'danger'" effect="light">
                {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="最近登录" width="180">
            <template #default="{ row }">
              <span class="muted-text">{{ formatDate(row.lastLoginAt) }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="205">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openEdit(row)">编辑</ElButton>
              <ElButton link type="primary" @click="openReset(row)">重置密码</ElButton>
              <ElButton
                link
                :type="row.status === 'ENABLED' ? 'danger' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>

      <div class="pagination-row">
        <ElPagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="search"
        />
      </div>
    </div>

    <ElDialog v-model="createVisible" :title="`新增${roleNames[profileRole]}`" width="680px">
      <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-width="104px">
        <div class="form-grid">
          <ElFormItem label="登录账号" prop="username">
            <ElInput v-model="createForm.username" maxlength="50" />
          </ElFormItem>
          <ElFormItem label="初始密码" prop="password">
            <ElInput v-model="createForm.password" maxlength="72" show-password type="password" />
          </ElFormItem>
          <ElFormItem label="姓名" prop="realName">
            <ElInput v-model="createForm.realName" maxlength="50" />
          </ElFormItem>
          <ElFormItem v-if="!props.fixedRole" label="角色" prop="roleCode">
            <ElSelect v-model="createForm.roleCode" style="width: 100%">
              <ElOption
                v-for="role in roles"
                :key="role.roleCode"
                :label="role.roleName"
                :value="role.roleCode"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="手机号" prop="phone">
            <ElInput v-model="createForm.phone" maxlength="20" />
          </ElFormItem>
          <ElFormItem label="邮箱" prop="email">
            <ElInput v-model="createForm.email" maxlength="100" />
          </ElFormItem>
          <ElFormItem v-if="profileRole !== 'ADMIN'" label="性别">
            <ElSelect v-model="createForm.gender" clearable style="width: 100%">
              <ElOption label="男" value="MALE" />
              <ElOption label="女" value="FEMALE" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem
            v-if="['TEACHER', 'STUDENT'].includes(profileRole)"
            :label="identifierLabel"
            prop="identifier"
          >
            <ElInput v-model="createForm.identifier" maxlength="30" />
          </ElFormItem>
          <template v-if="profileRole === 'TEACHER'">
            <ElFormItem label="职称">
              <ElInput v-model="createForm.title" maxlength="50" />
            </ElFormItem>
            <ElFormItem label="专业方向">
              <ElInput v-model="createForm.specialty" maxlength="100" />
            </ElFormItem>
            <ElFormItem label="入职日期">
              <ElDatePicker
                v-model="createForm.hireDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </ElFormItem>
          </template>
          <template v-if="profileRole === 'STUDENT'">
            <ElFormItem label="出生日期">
              <ElDatePicker
                v-model="createForm.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </ElFormItem>
            <ElFormItem label="入学日期">
              <ElDatePicker
                v-model="createForm.enrollmentDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </ElFormItem>
          </template>
          <template v-if="profileRole === 'PARENT'">
            <ElFormItem label="职业">
              <ElInput v-model="createForm.occupation" maxlength="100" />
            </ElFormItem>
            <ElFormItem label="紧急联系电话">
              <ElInput v-model="createForm.emergencyContact" maxlength="20" />
            </ElFormItem>
          </template>
        </div>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="createVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submitCreate">创建</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDialog v-model="editVisible" title="编辑用户资料" width="520px">
      <ElForm ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <ElFormItem label="姓名" prop="realName">
          <ElInput v-model="editForm.realName" maxlength="50" />
        </ElFormItem>
        <ElFormItem label="手机号" prop="phone">
          <ElInput v-model="editForm.phone" maxlength="20" />
        </ElFormItem>
        <ElFormItem label="邮箱" prop="email">
          <ElInput v-model="editForm.email" maxlength="100" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="editVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submitEdit">保存</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDialog v-model="resetVisible" title="重置密码" width="460px">
      <p class="reset-hint">
        正在重置账号 <strong>{{ currentUser?.username }}</strong> 的密码，保存后原 Token 将失效。
      </p>
      <ElForm ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <ElFormItem label="新密码" prop="newPassword">
          <ElInput v-model="resetForm.newPassword" maxlength="72" show-password type="password" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="resetVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submitReset">确认重置</ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.identity-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(145deg, #1e5fbd, #4290ee);
  font-size: 13px;
  font-weight: 700;
}

.identity-cell strong,
.identity-cell span {
  display: block;
}

.identity-cell strong {
  color: #20324e;
  font-size: 13px;
}

.identity-cell span {
  margin-top: 3px;
  color: #8a96a8;
  font-size: 11px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.reset-hint {
  margin: 0 0 20px;
  color: #68768b;
  font-size: 13px;
  line-height: 1.7;
}
</style>

