<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { scoreApi } from '../../api/score'
import type { SubjectPayload, SubjectRow } from '../../types/score'

const rows = ref<SubjectRow[]>([])
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
const form = reactive<SubjectPayload>({
  subjectCode: '',
  subjectName: '',
  defaultFullScore: 150,
  sortOrder: 0,
  status: 'ENABLED',
})
const rules: FormRules<SubjectPayload> = {
  subjectCode: [{ required: true, message: '请输入学科编码', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入学科名称', trigger: 'blur' }],
  defaultFullScore: [{ required: true, message: '请输入默认满分', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await scoreApi.subjects({
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
    subjectCode: '',
    subjectName: '',
    defaultFullScore: 150,
    sortOrder: rows.value.length + 1,
    status: 'ENABLED',
  })
  dialogVisible.value = true
}

function openEdit(row: SubjectRow) {
  editingId.value = row.id
  Object.assign(form, {
    subjectCode: row.subjectCode,
    subjectName: row.subjectName,
    defaultFullScore: row.defaultFullScore,
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
      await scoreApi.updateSubject(editingId.value, { ...form })
      ElMessage.success('学科已更新')
    } else {
      await scoreApi.createSubject({ ...form })
      ElMessage.success('学科已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function remove(row: SubjectRow) {
  try {
    await ElMessageBox.confirm(
      `确认删除学科“${row.subjectName}”吗？已有成绩或教师关联的学科不能删除。`,
      '删除学科',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await scoreApi.deleteSubject(row.id)
  ElMessage.success('学科已删除')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>学科管理</h2>
        <p>默认满分用于成绩录入范围校验，已有成绩或任课关联的学科不能删除。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增学科</ElButton>
    </header>
    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="学科名称"
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
        <ElTable v-loading="loading" :data="rows" empty-text="暂无学科" stripe>
          <ElTableColumn prop="subjectCode" label="学科编码" width="150" />
          <ElTableColumn prop="subjectName" label="学科名称" min-width="150" />
          <ElTableColumn prop="defaultFullScore" label="默认满分" width="120" />
          <ElTableColumn prop="sortOrder" label="排序" width="90" />
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
      :title="editingId ? '编辑学科' : '新增学科'"
      width="520px"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <ElFormItem label="学科编码" prop="subjectCode">
          <ElInput v-model="form.subjectCode" maxlength="30" placeholder="如：MATH" />
        </ElFormItem>
        <ElFormItem label="学科名称" prop="subjectName">
          <ElInput v-model="form.subjectName" maxlength="50" />
        </ElFormItem>
        <ElFormItem label="默认满分" prop="defaultFullScore">
          <ElInputNumber v-model="form.defaultFullScore" :min="0.01" :max="999" />
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

