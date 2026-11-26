<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import { scoreApi } from '../../api/score'
import type { ClassRow, GradeRow } from '../../types/organization'
import type { ExamPayload, ExamRow } from '../../types/score'

const rows = ref<ExamRow[]>([])
const grades = ref<GradeRow[]>([])
const classes = ref<ClassRow[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const query = reactive({
  keyword: '',
  status: '',
  academicYear: '',
  page: 1,
  size: 10,
})
const form = reactive<ExamPayload>({
  name: '',
  examType: 'MIDTERM',
  gradeId: 0,
  classId: undefined,
  academicYear: '2026-2027',
  term: '第一学期',
  examDate: '',
  status: 'DRAFT',
})
const rules: FormRules<ExamPayload> = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  examType: [{ required: true, message: '请选择考试类型', trigger: 'change' }],
  gradeId: [{ required: true, message: '请选择年级', trigger: 'change' }],
  academicYear: [{ required: true, message: '请输入学年', trigger: 'blur' }],
  term: [{ required: true, message: '请选择学期', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}
const filteredClasses = computed(() =>
  classes.value.filter((item) => item.gradeId === form.gradeId),
)

const examTypeNames: Record<string, string> = {
  QUIZ: '小测',
  MONTHLY: '月考',
  MIDTERM: '期中',
  FINAL: '期末',
}
const statusMap: Record<string, { label: string; type: 'info' | 'success' | 'warning' }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'warning' },
}

async function loadData() {
  loading.value = true
  try {
    const result = await scoreApi.exams({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      academicYear: query.academicYear || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [gradeResult, classResult] = await Promise.all([
    adminApi.grades({ page: 1, size: 100 }),
    adminApi.classes({ page: 1, size: 100 }),
  ])
  grades.value = gradeResult.records
  classes.value = classResult.records
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.status = ''
  query.academicYear = ''
  search()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    examType: 'MIDTERM',
    gradeId: grades.value[0]?.id ?? 0,
    classId: undefined,
    academicYear: '2026-2027',
    term: '第一学期',
    examDate: new Date().toISOString().slice(0, 10),
    status: 'DRAFT',
  })
  dialogVisible.value = true
}

function openEdit(row: ExamRow) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    examType: row.examType,
    gradeId: row.gradeId,
    classId: row.classId,
    academicYear: row.academicYear,
    term: row.term,
    examDate: row.examDate,
    status: row.status,
  })
  dialogVisible.value = true
}

function onGradeChange() {
  form.classId = undefined
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await scoreApi.updateExam(editingId.value, { ...form })
      ElMessage.success('考试已更新')
    } else {
      await scoreApi.createExam({ ...form })
      ElMessage.success('考试已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: ExamRow) {
  try {
    await ElMessageBox.confirm(
      `确认删除考试“${row.name}”吗？已有成绩的考试不能删除。`,
      '删除考试',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await scoreApi.deleteExam(row.id)
  ElMessage.success('考试已删除')
  await loadData()
}

onMounted(async () => {
  await Promise.all([loadData(), loadOptions()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>考试管理</h2>
        <p>考试可面向年级或指定班级，只有已发布考试会出现在教师录入和成绩分析中。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增考试</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="考试名称或类型"
          @keyup.enter="search"
        />
        <ElInput v-model="query.academicYear" clearable placeholder="学年" />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="草稿" value="DRAFT" />
          <ElOption label="已发布" value="PUBLISHED" />
          <ElOption label="已归档" value="ARCHIVED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>
      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无考试" stripe>
          <ElTableColumn label="考试" min-width="230">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong>
              <div class="muted-text">{{ examTypeNames[row.examType] || row.examType }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="范围" min-width="140">
            <template #default="{ row }">
              {{ row.className ? `${row.gradeName}${row.className}` : `${row.gradeName}全年级` }}
            </template>
          </ElTableColumn>
          <ElTableColumn prop="academicYear" label="学年" width="120" />
          <ElTableColumn prop="term" label="学期" width="110" />
          <ElTableColumn prop="examDate" label="考试日期" width="120" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusMap[row.status]?.type || 'info'" effect="light">
                {{ statusMap[row.status]?.label }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openEdit(row)">编辑</ElButton>
              <ElButton link type="danger" @click="remove(row)">删除</ElButton>
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

    <ElDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑考试' : '新增考试'"
      width="600px"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <ElFormItem label="考试名称" prop="name">
          <ElInput v-model="form.name" maxlength="150" />
        </ElFormItem>
        <div class="two-column">
          <ElFormItem label="考试类型" prop="examType">
            <ElSelect v-model="form.examType" style="width: 100%">
              <ElOption label="小测" value="QUIZ" />
              <ElOption label="月考" value="MONTHLY" />
              <ElOption label="期中" value="MIDTERM" />
              <ElOption label="期末" value="FINAL" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="考试日期" prop="examDate">
            <ElDatePicker
              v-model="form.examDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </ElFormItem>
        </div>
        <div class="two-column">
          <ElFormItem label="年级" prop="gradeId">
            <ElSelect v-model="form.gradeId" style="width: 100%" @change="onGradeChange">
              <ElOption
                v-for="grade in grades"
                :key="grade.id"
                :label="grade.gradeName"
                :value="grade.id"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="班级范围">
            <ElSelect v-model="form.classId" clearable placeholder="空表示全年级" style="width: 100%">
              <ElOption
                v-for="item in filteredClasses"
                :key="item.id"
                :label="item.className"
                :value="item.id"
              />
            </ElSelect>
          </ElFormItem>
        </div>
        <div class="two-column">
          <ElFormItem label="学年" prop="academicYear">
            <ElInput v-model="form.academicYear" maxlength="20" />
          </ElFormItem>
          <ElFormItem label="学期" prop="term">
            <ElSelect v-model="form.term" style="width: 100%">
              <ElOption label="第一学期" value="第一学期" />
              <ElOption label="第二学期" value="第二学期" />
            </ElSelect>
          </ElFormItem>
        </div>
        <ElFormItem label="状态" prop="status">
          <ElRadioGroup v-model="form.status">
            <ElRadio value="DRAFT">草稿</ElRadio>
            <ElRadio value="PUBLISHED">已发布</ElRadio>
            <ElRadio value="ARCHIVED">已归档</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submit">保存</ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
</style>

