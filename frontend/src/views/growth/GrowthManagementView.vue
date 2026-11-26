<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions,
} from 'element-plus'

import { fileApi } from '../../api/file'
import { growthApi } from '../../api/growth'
import type { EntityOption } from '../../types/organization'
import type { FileAttachmentInfo } from '../../types/notice'
import type { GrowthCategory, GrowthRow, GrowthSavePayload } from '../../types/growth'

const rows = ref<GrowthRow[]>([])
const students = ref<EntityOption[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<number | null>(null)
const selected = ref<GrowthRow | null>(null)
const formRef = ref<FormInstance>()
const attachments = ref<FileAttachmentInfo[]>([])
const newAttachmentIds = ref<number[]>([])
const committed = ref(false)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  keyword: '',
  category: '',
  studentId: undefined as number | undefined,
  page: 1,
  size: 10,
})
const form = reactive({
  studentId: undefined as number | undefined,
  category: 'STUDY' as GrowthCategory,
  title: '',
  content: '',
  occurredAt: new Date().toISOString().slice(0, 10),
  visibleToParent: true,
})
const rules: FormRules<typeof form> = {
  studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入记录内容', trigger: 'blur' }],
  occurredAt: [{ required: true, message: '请选择发生日期', trigger: 'change' }],
}

const categoryMap: Record<
  GrowthCategory,
  { label: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info'; color: string }
> = {
  STUDY: { label: '学习表现', type: 'primary', color: '#276dcc' },
  MORAL: { label: '德育表现', type: 'success', color: '#2c9770' },
  ACTIVITY: { label: '校园活动', type: 'warning', color: '#d88a26' },
  HONOR: { label: '荣誉奖励', type: 'danger', color: '#c95454' },
  COMPREHENSIVE: { label: '综合评价', type: 'info', color: '#6e61c6' },
}

async function loadData() {
  loading.value = true
  try {
    const result = await growthApi.manage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      studentId: query.studentId,
      dateFrom: dateRange.value?.[0],
      dateTo: dateRange.value?.[1],
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  students.value = await growthApi.options()
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.category = ''
  query.studentId = undefined
  dateRange.value = null
  search()
}

function resetForm() {
  Object.assign(form, {
    studentId: students.value[0]?.id,
    category: 'STUDY',
    title: '',
    content: '',
    occurredAt: new Date().toISOString().slice(0, 10),
    visibleToParent: true,
  })
  attachments.value = []
  newAttachmentIds.value = []
  committed.value = false
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: GrowthRow) {
  editingId.value = row.id
  Object.assign(form, {
    studentId: row.studentId,
    category: row.category,
    title: row.title,
    content: row.content,
    occurredAt: row.occurredAt,
    visibleToParent: row.visibleToParent === 1,
  })
  attachments.value = row.attachments ?? []
  newAttachmentIds.value = []
  committed.value = false
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.studentId) {
    return
  }
  const payload: GrowthSavePayload = {
    studentId: form.studentId,
    category: form.category,
    title: form.title,
    content: form.content,
    occurredAt: form.occurredAt,
    visibleToParent: form.visibleToParent,
    attachmentIds: attachments.value.map((item) => item.id),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await growthApi.update(editingId.value, payload)
      ElMessage.success('成长记录已更新')
    } else {
      await growthApi.create(payload)
      ElMessage.success('成长记录已创建')
    }
    committed.value = true
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function handleUpload(request: UploadRequestOptions) {
  uploading.value = true
  try {
    const uploaded = await fileApi.upload(request.file)
    attachments.value.push(uploaded)
    newAttachmentIds.value.push(uploaded.id)
    request.onSuccess(uploaded)
    ElMessage.success('附件上传成功')
  } catch {
    // HTTP interceptor displays the server error.
  } finally {
    uploading.value = false
  }
}

async function removeAttachment(attachment: FileAttachmentInfo) {
  if (newAttachmentIds.value.includes(attachment.id)) {
    await fileApi.deleteTemp(attachment.id)
    newAttachmentIds.value = newAttachmentIds.value.filter((id) => id !== attachment.id)
  }
  attachments.value = attachments.value.filter((item) => item.id !== attachment.id)
}

async function cleanupAttachments() {
  if (committed.value) {
    newAttachmentIds.value = []
    return
  }
  const ids = [...newAttachmentIds.value]
  newAttachmentIds.value = []
  for (const id of ids) {
    try {
      await fileApi.deleteTemp(id)
    } catch {
      // Temporary attachment may already be removed.
    }
  }
}

async function openDetail(row: GrowthRow) {
  selected.value = await growthApi.detail(row.id)
  detailVisible.value = true
}

async function remove(row: GrowthRow) {
  try {
    await ElMessageBox.confirm(`确认删除成长记录“${row.title}”吗？`, '删除记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await growthApi.delete(row.id)
  ElMessage.success('成长记录已删除')
  await loadData()
}

async function downloadAttachment(attachment: FileAttachmentInfo) {
  await fileApi.download(attachment)
  ElMessage.success('附件下载已开始')
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

function formatSize(size: number) {
  return size >= 1024 * 1024
    ? `${(size / 1024 / 1024).toFixed(1)} MB`
    : `${Math.max(1, Math.round(size / 1024))} KB`
}

onMounted(async () => {
  await Promise.all([loadData(), loadOptions()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>成长档案管理</h2>
        <p>记录本班学生的学习、德育、活动、荣誉和综合评价。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增成长记录</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="标题、内容、学生姓名或学号"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.category" clearable placeholder="全部分类">
          <ElOption
            v-for="(item, key) in categoryMap"
            :key="key"
            :label="item.label"
            :value="key"
          />
        </ElSelect>
        <ElSelect v-model="query.studentId" clearable filterable placeholder="全部学生">
          <ElOption
            v-for="item in students"
            :key="item.id"
            :label="`${item.label}（${item.secondary || ''}）`"
            :value="item.id"
          />
        </ElSelect>
        <ElDatePicker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
        />
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无成长记录" stripe>
          <ElTableColumn label="学生" min-width="130">
            <template #default="{ row }">
              <strong>{{ row.studentName }}</strong>
              <div class="muted-text">{{ row.studentNo }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="分类" width="110">
            <template #default="{ row }">
              <ElTag :type="categoryMap[row.category as GrowthCategory]?.type" effect="light">
                {{ categoryMap[row.category as GrowthCategory]?.label }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="记录" min-width="270">
            <template #default="{ row }">
              <strong>{{ row.title }}</strong>
              <p class="record-preview">{{ row.content }}</p>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="occurredAt" label="发生日期" width="120" />
          <ElTableColumn label="家长可见" width="100">
            <template #default="{ row }">
              <ElTag :type="row.visibleToParent === 1 ? 'success' : 'info'" effect="plain">
                {{ row.visibleToParent === 1 ? '可见' : '内部' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">查看</ElButton>
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
      :title="editingId ? '编辑成长记录' : '新增成长记录'"
      width="720px"
      @closed="cleanupAttachments"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="two-column">
          <ElFormItem label="学生" prop="studentId">
            <ElSelect
              v-model="form.studentId"
              :disabled="Boolean(editingId)"
              filterable
              style="width: 100%"
            >
              <ElOption
                v-for="item in students"
                :key="item.id"
                :label="`${item.label}（${item.secondary || ''}）`"
                :value="item.id"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="分类" prop="category">
            <ElSelect v-model="form.category" style="width: 100%">
              <ElOption
                v-for="(item, key) in categoryMap"
                :key="key"
                :label="item.label"
                :value="key"
              />
            </ElSelect>
          </ElFormItem>
        </div>
        <ElFormItem label="标题" prop="title">
          <ElInput v-model="form.title" maxlength="200" />
        </ElFormItem>
        <div class="two-column">
          <ElFormItem label="发生日期" prop="occurredAt">
            <ElDatePicker
              v-model="form.occurredAt"
              type="date"
              value-format="YYYY-MM-DD"
              :disabled-date="(date: Date) => date.getTime() > Date.now()"
              style="width: 100%"
            />
          </ElFormItem>
          <ElFormItem label="家长可见">
            <ElSwitch
              v-model="form.visibleToParent"
              active-text="是"
              inactive-text="否"
            />
          </ElFormItem>
        </div>
        <ElFormItem label="记录内容" prop="content">
          <ElInput
            v-model="form.content"
            maxlength="50000"
            :rows="9"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="附件 / 图片">
          <div class="attachment-editor">
            <ElUpload
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.txt"
              :http-request="handleUpload"
              :show-file-list="false"
            >
              <ElButton :loading="uploading" plain>选择并上传</ElButton>
            </ElUpload>
            <div v-if="attachments.length" class="attachment-list">
              <ElTag
                v-for="attachment in attachments"
                :key="attachment.id"
                closable
                type="info"
                @close="removeAttachment(attachment)"
              >
                {{ attachment.originalName }} · {{ formatSize(attachment.fileSize) }}
              </ElTag>
            </div>
          </div>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submit">保存</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDrawer v-model="detailVisible" size="620px" :with-header="false">
      <template v-if="selected">
        <div class="detail-heading">
          <div>
            <ElTag
              :type="categoryMap[selected.category]?.type"
              effect="plain"
            >
              {{ categoryMap[selected.category]?.label }}
            </ElTag>
            <h2>{{ selected.title }}</h2>
            <p>
              {{ selected.studentName }} · {{ selected.gradeName }}{{ selected.className }} ·
              {{ selected.occurredAt }}
            </p>
          </div>
        </div>
        <div class="detail-content">{{ selected.content }}</div>
        <div class="detail-meta">
          <span>记录教师：{{ selected.recorderName }}</span>
          <span>更新时间：{{ formatDate(selected.updatedAt) }}</span>
        </div>
        <div v-if="selected.attachments?.length" class="proof-list">
          <h3>附件与图片</h3>
          <button
            v-for="attachment in selected.attachments"
            :key="attachment.id"
            class="attachment-button"
            type="button"
            @click="downloadAttachment(attachment)"
          >
            <span>{{ attachment.originalName }}</span>
            <small>{{ formatSize(attachment.fileSize) }}</small>
          </button>
        </div>
      </template>
    </ElDrawer>
  </section>
</template>

<style scoped>
.record-preview {
  display: -webkit-box;
  overflow: hidden;
  margin: 5px 0 0;
  color: #8a96a8;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.attachment-editor {
  width: 100%;
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.detail-heading {
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f7;
}

.detail-heading h2 {
  margin: 12px 0 8px;
  color: #172844;
  font-size: 26px;
  line-height: 1.4;
}

.detail-heading p,
.detail-meta {
  color: #8794a7;
  font-size: 12px;
}

.detail-content {
  padding: 24px 0;
  color: #3b4c65;
  font-size: 15px;
  line-height: 1.95;
  white-space: pre-wrap;
}

.detail-meta {
  display: flex;
  gap: 18px;
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f7;
}

.proof-list {
  margin-top: 22px;
}

.proof-list h3 {
  margin: 0 0 12px;
  color: #263b5c;
  font-size: 15px;
}

.attachment-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 12px 14px;
  border: 1px solid #dce6f4;
  border-radius: 10px;
  color: #285da9;
  background: #f7faff;
  cursor: pointer;
}

.attachment-button small {
  color: #8794a7;
}
</style>

