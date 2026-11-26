<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import type { EntityOption, StudentClassRow } from '../../types/organization'

const rows = ref<StudentClassRow[]>([])
const students = ref<EntityOption[]>([])
const classes = ref<EntityOption[]>([])
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
  studentId: undefined as number | undefined,
  classId: undefined as number | undefined,
  startDate: new Date().toISOString().slice(0, 10),
})
const rules: FormRules<typeof form> = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择入班日期', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await adminApi.studentClasses({
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
  const [studentOptions, classOptions] = await Promise.all([
    adminApi.studentOptions(),
    adminApi.classOptions(),
  ])
  students.value = studentOptions
  classes.value = classOptions
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

function openAssign() {
  Object.assign(form, {
    studentId: undefined,
    classId: undefined,
    startDate: new Date().toISOString().slice(0, 10),
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.studentId || !form.classId) {
    return
  }
  saving.value = true
  try {
    await adminApi.assignStudentClass({
      studentId: form.studentId,
      classId: form.classId,
      startDate: form.startDate,
    })
    ElMessage.success('学生班级已更新，历史履历已保留')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

function optionLabel(options: EntityOption[], id?: number) {
  if (!id) {
    return '—'
  }
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
        <h2>学生分班</h2>
        <p>首次分班或调班都会写入班级履历；旧班级自动结束，历史成绩与统计可保留当时班级。</p>
      </div>
      <ElButton type="primary" @click="openAssign">分班 / 调班</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="学生 / 学号 / 班级"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="当前班级" value="CURRENT" />
          <ElOption label="历史班级" value="ENDED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无学生班级履历" stripe>
          <ElTableColumn label="学生" min-width="150">
            <template #default="{ row }">
              <strong>{{ row.studentName }}</strong>
              <div class="muted-text">{{ row.studentNo }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="班级" min-width="145">
            <template #default="{ row }">
              {{ row.gradeName }}{{ row.className }}
            </template>
          </ElTableColumn>
          <ElTableColumn prop="academicYear" label="学年" width="125" />
          <ElTableColumn label="在班时间" width="220">
            <template #default="{ row }">
              {{ row.startDate }} 至 {{ row.endDate || '现在' }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <ElTag :type="row.status === 'CURRENT' ? 'success' : 'info'" effect="light">
                {{ row.status === 'CURRENT' ? '当前班级' : '历史班级' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="记录时间" width="180">
            <template #default="{ row }">
              {{ new Date(row.createdAt).toLocaleString('zh-CN', { hour12: false }) }}
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

    <ElDialog v-model="dialogVisible" title="学生分班 / 调班" width="560px">
      <ElAlert
        :closable="false"
        show-icon
        title="若学生已有当前班级，本次操作会自动结束原班级履历。"
        type="info"
      />
      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        class="assign-form"
        label-width="104px"
      >
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
        <ElFormItem label="目标班级" prop="classId">
          <ElSelect
            v-model="form.classId"
            filterable
            placeholder="选择班级"
            style="width: 100%"
          >
            <ElOption
              v-for="item in classes"
              :key="item.id"
              :label="optionLabel(classes, item.id)"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="开始日期" prop="startDate">
          <ElDatePicker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submit">确认分班</ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.assign-form {
  margin-top: 20px;
}
</style>

