<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import type { ClassPayload, ClassRow, GradeRow } from '../../types/organization'

const rows = ref<ClassRow[]>([])
const grades = ref<GradeRow[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const query = reactive({
  keyword: '',
  gradeId: undefined as number | undefined,
  academicYear: '',
  status: '',
  page: 1,
  size: 10,
})
const form = reactive<ClassPayload>({
  gradeId: 0,
  className: '',
  academicYear: '',
  graduationYear: undefined,
  status: 'ENABLED',
})

const rules: FormRules<ClassPayload> = {
  gradeId: [{ required: true, message: '请选择年级', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  academicYear: [{ required: true, message: '请输入学年', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await adminApi.classes({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      gradeId: query.gradeId,
      academicYear: query.academicYear || undefined,
      status: query.status || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadGrades() {
  const result = await adminApi.grades({ page: 1, size: 100 })
  grades.value = result.records
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.gradeId = undefined
  query.academicYear = ''
  query.status = ''
  search()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    gradeId: grades.value[0]?.id ?? 0,
    className: '',
    academicYear: '2026-2027',
    graduationYear: undefined,
    status: 'ENABLED',
  })
  dialogVisible.value = true
}

function openEdit(row: ClassRow) {
  editingId.value = row.id
  Object.assign(form, {
    gradeId: row.gradeId,
    className: row.className,
    academicYear: row.academicYear,
    graduationYear: row.graduationYear,
    status: row.status,
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await adminApi.updateClass(editingId.value, { ...form })
      ElMessage.success('班级已更新')
    } else {
      await adminApi.createClass({ ...form })
      ElMessage.success('班级已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: ClassRow) {
  try {
    await ElMessageBox.confirm(
      `确认删除“${row.gradeName}${row.className}”吗？有关联教师或学生履历的班级不能删除。`,
      '删除班级',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await adminApi.deleteClass(row.id)
  ElMessage.success('班级已删除')
  await loadData()
}

onMounted(async () => {
  await Promise.all([loadGrades(), loadData()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>班级管理</h2>
        <p>班级人数和教师人数由当前学生分班及有效教师关联实时统计。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增班级</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="班级 / 年级 / 学年"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.gradeId" clearable placeholder="全部年级">
          <ElOption
            v-for="grade in grades"
            :key="grade.id"
            :label="grade.gradeName"
            :value="grade.id"
          />
        </ElSelect>
        <ElInput v-model="query.academicYear" clearable placeholder="学年" />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="启用" value="ENABLED" />
          <ElOption label="停用" value="DISABLED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无班级数据" stripe>
          <ElTableColumn label="班级" min-width="150">
            <template #default="{ row }">
              <strong>{{ row.gradeName }}{{ row.className }}</strong>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="academicYear" label="学年" width="130" />
          <ElTableColumn prop="graduationYear" label="毕业年份" width="110">
            <template #default="{ row }">{{ row.graduationYear || '—' }}</template>
          </ElTableColumn>
          <ElTableColumn prop="studentCount" label="学生人数" width="110" />
          <ElTableColumn prop="teacherCount" label="教师关联数" width="120" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
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
      :title="editingId ? '编辑班级' : '新增班级'"
      width="520px"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <ElFormItem label="年级" prop="gradeId">
          <ElSelect v-model="form.gradeId" style="width: 100%">
            <ElOption
              v-for="grade in grades"
              :key="grade.id"
              :label="grade.gradeName"
              :value="grade.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="班级名称" prop="className">
          <ElInput v-model="form.className" maxlength="50" placeholder="如：1班" />
        </ElFormItem>
        <ElFormItem label="学年" prop="academicYear">
          <ElInput v-model="form.academicYear" maxlength="20" placeholder="如：2026-2027" />
        </ElFormItem>
        <ElFormItem label="毕业年份">
          <ElInputNumber
            v-model="form.graduationYear"
            :min="2000"
            :max="2100"
            placeholder="可空"
          />
        </ElFormItem>
        <ElFormItem label="状态" prop="status">
          <ElRadioGroup v-model="form.status">
            <ElRadio value="ENABLED">启用</ElRadio>
            <ElRadio value="DISABLED">停用</ElRadio>
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

