<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import type { GradePayload, GradeRow } from '../../types/organization'

const rows = ref<GradeRow[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})
const form = reactive<GradePayload>({
  gradeName: '',
  gradeLevel: 1,
  sortOrder: 0,
  status: 'ENABLED',
})

const rules: FormRules<GradePayload> = {
  gradeName: [{ required: true, message: '请输入年级名称', trigger: 'blur' }],
  gradeLevel: [{ required: true, message: '请选择年级层级', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await adminApi.grades({
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
  editingId.value = null
  Object.assign(form, {
    gradeName: '',
    gradeLevel: 1,
    sortOrder: 0,
    status: 'ENABLED',
  })
  dialogVisible.value = true
}

function openEdit(row: GradeRow) {
  editingId.value = row.id
  Object.assign(form, {
    gradeName: row.gradeName,
    gradeLevel: row.gradeLevel,
    sortOrder: row.sortOrder,
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
      await adminApi.updateGrade(editingId.value, { ...form })
      ElMessage.success('年级已更新')
    } else {
      await adminApi.createGrade({ ...form })
      ElMessage.success('年级已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: GradeRow) {
  try {
    await ElMessageBox.confirm(`确认删除年级“${row.gradeName}”吗？`, '删除年级', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await adminApi.deleteGrade(row.id)
  ElMessage.success('年级已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>年级管理</h2>
        <p>维护高中三个年级的基础信息；已有班级的年级不能删除。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增年级</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="搜索年级名称"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="启用" value="ENABLED" />
          <ElOption label="停用" value="DISABLED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无年级数据" stripe>
          <ElTableColumn prop="gradeName" label="年级名称" min-width="150" />
          <ElTableColumn prop="gradeLevel" label="年级层级" width="120" />
          <ElTableColumn prop="sortOrder" label="排序" width="100" />
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <ElTag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="创建时间" width="190">
            <template #default="{ row }">
              {{ new Date(row.createdAt).toLocaleString('zh-CN', { hour12: false }) }}
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
      :title="editingId ? '编辑年级' : '新增年级'"
      width="480px"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <ElFormItem label="年级名称" prop="gradeName">
          <ElInput v-model="form.gradeName" maxlength="30" placeholder="如：高一" />
        </ElFormItem>
        <ElFormItem label="年级层级" prop="gradeLevel">
          <ElSelect v-model="form.gradeLevel" style="width: 100%">
            <ElOption label="高一（1）" :value="1" />
            <ElOption label="高二（2）" :value="2" />
            <ElOption label="高三（3）" :value="3" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="排序" prop="sortOrder">
          <ElInputNumber v-model="form.sortOrder" :min="0" :max="999" />
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

