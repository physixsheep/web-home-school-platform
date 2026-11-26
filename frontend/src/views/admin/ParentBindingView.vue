<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import type { EntityOption, ParentBindingRow } from '../../types/organization'

const rows = ref<ParentBindingRow[]>([])
const parents = ref<EntityOption[]>([])
const students = ref<EntityOption[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})
const form = reactive({
  parentId: undefined as number | undefined,
  studentId: undefined as number | undefined,
  relationship: 'MOTHER',
  isPrimary: 0,
})
const rules: FormRules<typeof form> = {
  parentId: [{ required: true, message: '请选择家长', trigger: 'change' }],
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  relationship: [{ required: true, message: '请选择家庭关系', trigger: 'change' }],
}

const relationshipNames: Record<string, string> = {
  FATHER: '父亲',
  MOTHER: '母亲',
  GUARDIAN: '监护人',
}

async function loadData() {
  loading.value = true
  try {
    const result = await adminApi.parentBindings({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [parentOptions, studentOptions] = await Promise.all([
    adminApi.parentOptions(),
    adminApi.studentOptions(),
  ])
  parents.value = parentOptions
  students.value = studentOptions
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.status = ''
  search()
}

function openCreate() {
  form.parentId = undefined
  form.studentId = undefined
  form.relationship = 'MOTHER'
  form.isPrimary = 0
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.parentId || !form.studentId) {
    return
  }
  saving.value = true
  try {
    await adminApi.createParentBinding({
      parentId: form.parentId,
      studentId: form.studentId,
      relationship: form.relationship,
      isPrimary: form.isPrimary,
    })
    ElMessage.success('家长学生绑定已创建并核验')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: ParentBindingRow) {
  const next = row.status === 'ACTIVE' ? 'REVOKED' : 'ACTIVE'
  const action = next === 'ACTIVE' ? '恢复' : '撤销'
  try {
    await ElMessageBox.confirm(
      `确认${action}“${row.parentName}—${row.studentName}”的绑定关系吗？`,
      `${action}绑定`,
      {
        type: 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await adminApi.updateParentBindingStatus(row.id, next)
  ElMessage.success(`绑定已${action}`)
  await loadData()
}

function optionLabel(options: EntityOption[], id: number) {
  const option = options.find((item) => item.id === id)
  return option ? `${option.label}（${option.secondary || id}）` : `ID ${id}`
}

onMounted(async () => {
  await Promise.all([loadData(), loadOptions()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>家长绑定</h2>
        <p>一个家长可绑定多个学生，一名学生也可关联多个家长；主要联系人按学生唯一处理。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增绑定</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="家长 / 学生 / 学号"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="生效" value="ACTIVE" />
          <ElOption label="待审核" value="PENDING" />
          <ElOption label="已撤销" value="REVOKED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无绑定关系" stripe>
          <ElTableColumn label="家长" min-width="160">
            <template #default="{ row }">
              <strong>{{ row.parentName }}</strong>
              <div class="muted-text">{{ row.parentAccount }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="学生" min-width="160">
            <template #default="{ row }">
              <strong>{{ row.studentName }}</strong>
              <div class="muted-text">{{ row.studentNo }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="关系" width="110">
            <template #default="{ row }">
              {{ relationshipNames[row.relationship] }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="主要联系人" width="120">
            <template #default="{ row }">
              <ElTag v-if="row.isPrimary === 1" type="primary" effect="light">是</ElTag>
              <span v-else class="muted-text">否</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag
                :type="row.status === 'ACTIVE' ? 'success' : row.status === 'PENDING' ? 'warning' : 'info'"
                effect="light"
              >
                {{ row.status === 'ACTIVE' ? '生效' : row.status === 'PENDING' ? '待审核' : '已撤销' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="核验时间" width="180">
            <template #default="{ row }">
              {{ row.verifiedAt ? new Date(row.verifiedAt).toLocaleString('zh-CN', { hour12: false }) : '—' }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <ElButton
                link
                :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                @click="changeStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '撤销' : '恢复' }}
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

    <ElDialog v-model="dialogVisible" title="新增家长学生绑定" width="560px">
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="104px">
        <ElFormItem label="家长" prop="parentId">
          <ElSelect
            v-model="form.parentId"
            filterable
            placeholder="选择家长"
            style="width: 100%"
          >
            <ElOption
              v-for="item in parents"
              :key="item.id"
              :label="optionLabel(parents, item.id)"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="学生" prop="studentId">
          <ElSelect
            v-model="form.studentId"
            filterable
            placeholder="选择学生"
            style="width: 100%"
          >
            <ElOption
              v-for="item in students"
              :key="item.id"
              :label="optionLabel(students, item.id)"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="家庭关系" prop="relationship">
          <ElSelect v-model="form.relationship" style="width: 100%">
            <ElOption label="父亲" value="FATHER" />
            <ElOption label="母亲" value="MOTHER" />
            <ElOption label="监护人" value="GUARDIAN" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="主要联系人">
          <ElSwitch
            v-model="form.isPrimary"
            :active-value="1"
            :inactive-value="0"
            active-text="是"
            inactive-text="否"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submit">创建并核验</ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

