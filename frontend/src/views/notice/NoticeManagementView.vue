<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions,
} from 'element-plus'

import { noticeApi } from '../../api/notice'
import { useAuthStore } from '../../stores/auth'
import type {
  FileAttachmentInfo,
  NoticeOptions,
  NoticeReader,
  NoticeRow,
  NoticeSavePayload,
} from '../../types/notice'

const auth = useAuthStore()
const rows = ref<NoticeRow[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const readersVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const attachments = ref<FileAttachmentInfo[]>([])
const newAttachmentIds = ref<number[]>([])
const committed = ref(false)
const readers = ref<NoticeReader[]>([])
const readerTotal = ref(0)
const readerPage = ref(1)
const currentNotice = ref<NoticeRow | null>(null)
const options = reactive<NoticeOptions>({ grades: [], classes: [] })
const query = reactive({
  keyword: '',
  status: '',
  scopeType: '',
  page: 1,
  size: 10,
})
const form = reactive<NoticeSavePayload>({
  title: '',
  content: '',
  scopeType: 'ALL',
  gradeId: undefined,
  classId: undefined,
  attachmentIds: [],
})
const isTeacher = computed(() => auth.primaryRole === 'TEACHER')

const scopeNames: Record<string, string> = {
  ALL: '全校',
  GRADE: '年级',
  CLASS: '班级',
}

const statusMap: Record<string, { label: string; type: 'success' | 'warning' | 'info' }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  WITHDRAWN: { label: '已撤回', type: 'warning' },
}

const rules: FormRules<NoticeSavePayload> = {
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入通知正文', trigger: 'blur' }],
  scopeType: [{ required: true, message: '请选择通知范围', trigger: 'change' }],
  gradeId: [
    {
      validator: (_rule, value, callback) => {
        if (form.scopeType === 'GRADE' && !value) {
          callback(new Error('请选择通知年级'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  classId: [
    {
      validator: (_rule, value, callback) => {
        if (form.scopeType === 'CLASS' && !value) {
          callback(new Error('请选择通知班级'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

async function loadData() {
  loading.value = true
  try {
    const result = await noticeApi.manage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      scopeType: query.scopeType || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const result = await noticeApi.options()
  options.grades = result.grades
  options.classes = result.classes
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.status = ''
  query.scopeType = ''
  search()
}

function resetForm() {
  Object.assign(form, {
    title: '',
    content: '',
    scopeType: isTeacher.value ? 'CLASS' : 'ALL',
    gradeId: undefined,
    classId: undefined,
    attachmentIds: [],
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

async function openEdit(row: NoticeRow) {
  const detail = await noticeApi.detail(row.id)
  editingId.value = row.id
  Object.assign(form, {
    title: detail.title,
    content: detail.content,
    scopeType: detail.scopeType,
    gradeId: detail.gradeId,
    classId: detail.classId,
    attachmentIds: detail.attachments?.map((item) => item.id) ?? [],
  })
  attachments.value = detail.attachments ?? []
  newAttachmentIds.value = []
  committed.value = false
  dialogVisible.value = true
}

async function saveNotice() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const payload: NoticeSavePayload = {
    title: form.title,
    content: form.content,
    scopeType: isTeacher.value ? 'CLASS' : form.scopeType,
    gradeId: form.scopeType === 'GRADE' ? form.gradeId : undefined,
    classId: form.scopeType === 'CLASS' ? form.classId : undefined,
    attachmentIds: attachments.value.map((item) => item.id),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await noticeApi.update(editingId.value, payload)
      ElMessage.success('通知已保存')
    } else {
      await noticeApi.create(payload)
      ElMessage.success('通知草稿已创建')
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
    const uploaded = await noticeApi.uploadAttachment(request.file)
    attachments.value.push(uploaded)
    newAttachmentIds.value.push(uploaded.id)
    request.onSuccess(uploaded)
    ElMessage.success('附件上传成功')
  } catch {
    // The HTTP interceptor already presents the server error.
  } finally {
    uploading.value = false
  }
}

async function removeAttachment(attachment: FileAttachmentInfo) {
  if (newAttachmentIds.value.includes(attachment.id)) {
    await noticeApi.deleteTempAttachment(attachment.id)
    newAttachmentIds.value = newAttachmentIds.value.filter((id) => id !== attachment.id)
  }
  attachments.value = attachments.value.filter((item) => item.id !== attachment.id)
}

async function cleanupUncommittedAttachments() {
  if (committed.value) {
    newAttachmentIds.value = []
    return
  }
  const ids = [...newAttachmentIds.value]
  newAttachmentIds.value = []
  for (const id of ids) {
    try {
      await noticeApi.deleteTempAttachment(id)
    } catch {
      // The backend may already have deleted the temporary file.
    }
  }
}

function closeEditor() {
  dialogVisible.value = false
}

async function publishNotice(row: NoticeRow) {
  try {
    await ElMessageBox.confirm(`确认发布通知“${row.title}”吗？`, '发布通知', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await noticeApi.publish(row.id)
  ElMessage.success('通知已发布')
  await loadData()
}

async function withdrawNotice(row: NoticeRow) {
  try {
    await ElMessageBox.confirm(`确认撤回通知“${row.title}”吗？`, '撤回通知', {
      type: 'warning',
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await noticeApi.withdraw(row.id)
  ElMessage.success('通知已撤回')
  await loadData()
}

async function deleteDraft(row: NoticeRow) {
  try {
    await ElMessageBox.confirm(`确认删除草稿“${row.title}”吗？`, '删除草稿', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await noticeApi.deleteDraft(row.id)
  ElMessage.success('通知草稿已删除')
  await loadData()
}

async function openReaders(row: NoticeRow) {
  currentNotice.value = row
  readerPage.value = 1
  readersVisible.value = true
  await loadReaders()
}

async function loadReaders() {
  if (!currentNotice.value) {
    return
  }
  const result = await noticeApi.readers(currentNotice.value.id, readerPage.value, 10)
  readers.value = result.records
  readerTotal.value = result.total
}

function scopeLabel(row: NoticeRow) {
  if (row.scopeType === 'ALL') {
    return '全校'
  }
  if (row.scopeType === 'GRADE') {
    return `${row.gradeName || ''}年级`
  }
  return `${row.gradeName || ''}${row.className || ''}`
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
        <h2>通知管理</h2>
        <p>
          {{ isTeacher ? '教师可向本人有效任教班级发布通知。' : '管理员可发布全校、年级或班级通知。' }}
        </p>
      </div>
      <ElButton type="primary" @click="openCreate">新建通知</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="通知标题或正文"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="草稿" value="DRAFT" />
          <ElOption label="已发布" value="PUBLISHED" />
          <ElOption label="已撤回" value="WITHDRAWN" />
        </ElSelect>
        <ElSelect v-model="query.scopeType" clearable placeholder="全部范围">
          <ElOption label="全校" value="ALL" />
          <ElOption label="年级" value="GRADE" />
          <ElOption label="班级" value="CLASS" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无通知" stripe>
          <ElTableColumn label="通知" min-width="300">
            <template #default="{ row }">
              <strong class="notice-title">{{ row.title }}</strong>
              <p class="notice-preview">{{ row.content }}</p>
            </template>
          </ElTableColumn>
          <ElTableColumn label="范围" width="130">
            <template #default="{ row }">{{ scopeLabel(row) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="publisherName" label="发布人" width="110" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusMap[row.status]?.type || 'info'" effect="light">
                {{ statusMap[row.status]?.label || row.status }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="已读人数" width="100">
            <template #default="{ row }">{{ row.readCount }}</template>
          </ElTableColumn>
          <ElTableColumn label="发布时间" width="180">
            <template #default="{ row }">{{ formatDate(row.publishedAt) }}</template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="270">
            <template #default="{ row }">
              <ElButton v-if="row.status !== 'WITHDRAWN'" link type="primary" @click="openEdit(row)">
                编辑
              </ElButton>
              <ElButton v-if="row.status === 'DRAFT'" link type="success" @click="publishNotice(row)">
                发布
              </ElButton>
              <ElButton v-if="row.status === 'DRAFT'" link type="danger" @click="deleteDraft(row)">
                删除
              </ElButton>
              <ElButton v-if="row.status === 'PUBLISHED'" link type="warning" @click="withdrawNotice(row)">
                撤回
              </ElButton>
              <ElButton
                v-if="row.status !== 'DRAFT'"
                link
                type="primary"
                @click="openReaders(row)"
              >
                阅读名单
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

    <ElDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑通知' : '新建通知'"
      width="780px"
      @closed="cleanupUncommittedAttachments"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="92px">
        <ElFormItem label="通知标题" prop="title">
          <ElInput v-model="form.title" maxlength="200" show-word-limit />
        </ElFormItem>
        <ElFormItem label="通知范围" prop="scopeType">
          <ElRadioGroup v-model="form.scopeType" :disabled="isTeacher">
            <ElRadioButton value="ALL">全校</ElRadioButton>
            <ElRadioButton value="GRADE">年级</ElRadioButton>
            <ElRadioButton value="CLASS">班级</ElRadioButton>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem v-if="form.scopeType === 'GRADE'" label="选择年级" prop="gradeId">
          <ElSelect v-model="form.gradeId" placeholder="选择年级" style="width: 100%">
            <ElOption
              v-for="grade in options.grades"
              :key="grade.id"
              :label="grade.label"
              :value="grade.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem v-if="form.scopeType === 'CLASS'" label="选择班级" prop="classId">
          <ElSelect
            v-model="form.classId"
            filterable
            placeholder="选择班级"
            style="width: 100%"
          >
            <ElOption
              v-for="item in options.classes"
              :key="item.id"
              :label="`${item.label}（${item.secondary || ''}）`"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="通知正文" prop="content">
          <ElInput
            v-model="form.content"
            maxlength="50000"
            :rows="10"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="附件">
          <div class="attachment-editor">
            <ElUpload
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
              :http-request="handleUpload"
              :show-file-list="false"
            >
              <ElButton :loading="uploading" plain>选择并上传附件</ElButton>
            </ElUpload>
            <span class="upload-tip">图片、PDF、Office 文档或 TXT，单文件不超过 20 MB</span>
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
          <ElButton @click="closeEditor">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="saveNotice">保存</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDrawer v-model="readersVisible" title="通知阅读名单" size="620px">
      <div class="reader-summary">
        <span>{{ currentNotice?.title }}</span>
        <strong>已读 {{ readerTotal }} 人</strong>
      </div>
      <ElTable :data="readers" empty-text="暂时没有人阅读">
        <ElTableColumn prop="realName" label="姓名" width="120" />
        <ElTableColumn prop="username" label="账号" min-width="140" />
        <ElTableColumn prop="roleName" label="角色" width="120" />
        <ElTableColumn label="阅读时间" width="180">
          <template #default="{ row }">{{ formatDate(row.readAt) }}</template>
        </ElTableColumn>
      </ElTable>
      <div class="pagination-row">
        <ElPagination
          v-model:current-page="readerPage"
          :page-size="10"
          :total="readerTotal"
          layout="total, prev, pager, next"
          @current-change="loadReaders"
        />
      </div>
    </ElDrawer>
  </section>
</template>

<style scoped>
.notice-title {
  display: block;
  overflow: hidden;
  color: #20324e;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-preview {
  display: -webkit-box;
  overflow: hidden;
  margin: 6px 0 0;
  color: #8a96a8;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.attachment-editor {
  width: 100%;
}

.upload-tip {
  display: block;
  margin-top: 8px;
  color: #8a96a8;
  font-size: 12px;
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.reader-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: 10px;
  color: #53647e;
  background: #f4f8ff;
}

.reader-summary strong {
  color: #2463b8;
}
</style>

