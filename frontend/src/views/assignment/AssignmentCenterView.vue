<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type UploadRequestOptions,
} from 'element-plus'

import { assignmentApi } from '../../api/assignment'
import { fileApi } from '../../api/file'
import { useAuthStore } from '../../stores/auth'
import type { AssignmentRow } from '../../types/assignment'
import type { FileAttachmentInfo } from '../../types/notice'

const auth = useAuthStore()
const isParent = computed(() => auth.primaryRole === 'PARENT')
const isStudent = computed(() => auth.primaryRole === 'STUDENT')
const rows = ref<AssignmentRow[]>([])
const total = ref(0)
const loading = ref(false)
const children = ref<{ id: number; label: string; secondary?: string }[]>([])
const selectedStudentId = ref<number | undefined>()
const query = reactive({
  keyword: '',
  page: 1,
  size: 10,
})
const drawerVisible = ref(false)
const selected = ref<AssignmentRow | null>(null)
const submitVisible = ref(false)
const submitFormRef = ref<FormInstance>()
const submitting = ref(false)
const uploading = ref(false)
const submitForm = reactive({ content: '' })
const submissionAttachments = ref<FileAttachmentInfo[]>([])
const newAttachmentIds = ref<number[]>([])
const submitCommitted = ref(false)

const statusMap: Record<
  string,
  { label: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }
> = {
  UNSUBMITTED: { label: '未提交', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'success' },
  GRADED: { label: '已批改', type: 'primary' },
  RETURNED: { label: '已退回', type: 'danger' },
}

const canSubmit = computed(() => {
  const row = selected.value
  if (!isStudent.value || !row || row.status !== 'PUBLISHED') {
    return false
  }
  if (row.submissionStatus === 'GRADED') {
    return false
  }
  return row.submissionStatus === 'RETURNED' || new Date(row.deadline).getTime() > Date.now()
})

async function loadOptions() {
  if (!isParent.value) {
    return
  }
  const result = await assignmentApi.options()
  children.value = result.children
  selectedStudentId.value = result.children[0]?.id
}

async function loadData() {
  if (isParent.value && !selectedStudentId.value) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await assignmentApi.readable({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      studentId: isParent.value ? selectedStudentId.value : undefined,
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

function onChildChange() {
  search()
}

async function openDetail(row: AssignmentRow) {
  selected.value = await assignmentApi.detail(
    row.id,
    isParent.value ? selectedStudentId.value : undefined,
  )
  drawerVisible.value = true
}

function openSubmit() {
  if (!selected.value) {
    return
  }
  submitForm.content = selected.value.submissionContent || ''
  submissionAttachments.value = [...(selected.value.submissionAttachments || [])]
  newAttachmentIds.value = []
  submitCommitted.value = false
  submitVisible.value = true
}

async function handleUpload(request: UploadRequestOptions) {
  uploading.value = true
  try {
    const uploaded = await fileApi.upload(request.file)
    submissionAttachments.value.push(uploaded)
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
  submissionAttachments.value = submissionAttachments.value.filter(
    (item) => item.id !== attachment.id,
  )
}

async function cleanupNewAttachments() {
  if (submitCommitted.value) {
    newAttachmentIds.value = []
    return
  }
  const ids = [...newAttachmentIds.value]
  newAttachmentIds.value = []
  for (const id of ids) {
    try {
      await fileApi.deleteTemp(id)
    } catch {
      // Temporary attachment may already be gone.
    }
  }
}

async function submitAssignment() {
  if (
    !selected.value ||
    (!submitForm.content.trim() && submissionAttachments.value.length === 0)
  ) {
    ElMessage.warning('请填写提交内容或上传附件')
    return
  }
  submitting.value = true
  try {
    await assignmentApi.submit(selected.value.id, {
      content: submitForm.content.trim() || undefined,
      attachmentIds: submissionAttachments.value.map((item) => item.id),
    })
    submitCommitted.value = true
    submitVisible.value = false
    ElMessage.success(
      selected.value.submissionStatus === 'RETURNED' ? '作业已重新提交' : '作业已提交',
    )
    await Promise.all([loadData(), openDetail(selected.value)])
  } finally {
    submitting.value = false
  }
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

function isOverdue(value: string) {
  return new Date(value).getTime() <= Date.now()
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>{{ isParent ? '孩子作业' : '我的作业' }}</h2>
        <p>
          {{ isParent ? '查看绑定孩子的作业提交、截止和批改情况。' : '查看教师布置的作业并提交附件。' }}
        </p>
      </div>
      <ElSelect
        v-if="isParent"
        v-model="selectedStudentId"
        placeholder="选择孩子"
        style="width: 220px"
        @change="onChildChange"
      >
        <ElOption
          v-for="child in children"
          :key="child.id"
          :label="`${child.label}（${child.secondary || child.id}）`"
          :value="child.id"
        />
      </ElSelect>
    </header>

    <ElEmpty
      v-if="isParent && children.length === 0"
      description="当前账号尚未绑定学生"
    />

    <div v-else class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="搜索作业标题或内容"
          @keyup.enter="search"
        />
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="query.keyword = ''; search()">重置</ElButton>
      </div>

      <div v-loading="loading" class="assignment-list">
        <article
          v-for="assignment in rows"
          :key="assignment.id"
          class="assignment-item"
          @click="openDetail(assignment)"
        >
          <div class="subject-block">
            <span>{{ assignment.subjectName }}</span>
            <small>{{ assignment.gradeName }}{{ assignment.className }}</small>
          </div>
          <div class="assignment-main">
            <div class="assignment-meta">
              <span>{{ assignment.teacherName }}</span>
              <span :class="{ overdue: isOverdue(assignment.deadline) }">
                截止 {{ formatDate(assignment.deadline) }}
              </span>
            </div>
            <h3>{{ assignment.title }}</h3>
            <p>{{ assignment.content }}</p>
          </div>
          <div class="assignment-status">
            <ElTag
              :type="statusMap[assignment.submissionStatus || 'UNSUBMITTED']?.type || 'info'"
              effect="light"
            >
              {{ statusMap[assignment.submissionStatus || 'UNSUBMITTED']?.label }}
            </ElTag>
            <span v-if="assignment.score !== undefined">成绩 {{ assignment.score }}</span>
          </div>
        </article>
        <ElEmpty v-if="!loading && rows.length === 0" description="暂无作业" />
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

    <ElDrawer v-model="drawerVisible" size="680px" :with-header="false">
      <template v-if="selected">
        <div class="detail-heading">
          <div>
            <ElTag effect="plain">
              {{ selected.gradeName }}{{ selected.className }} · {{ selected.subjectName }}
            </ElTag>
            <h2>{{ selected.title }}</h2>
            <p>
              {{ selected.teacherName }} · 截止 {{ formatDate(selected.deadline) }}
            </p>
          </div>
          <ElTag
            :type="statusMap[selected.submissionStatus || 'UNSUBMITTED']?.type || 'info'"
            effect="light"
          >
            {{ statusMap[selected.submissionStatus || 'UNSUBMITTED']?.label }}
          </ElTag>
        </div>

        <section class="detail-section">
          <h3>作业要求</h3>
          <div class="pre-wrap">{{ selected.content }}</div>
          <div v-if="selected.attachments?.length" class="attachment-list">
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
        </section>

        <section
          v-if="selected.submissionStatus && selected.submissionStatus !== 'UNSUBMITTED'"
          class="detail-section submission-box"
        >
          <div class="section-heading">
            <h3>我的提交</h3>
            <span v-if="selected.attemptCount">第 {{ selected.attemptCount }} 次提交</span>
          </div>
          <p class="submission-time">{{ formatDate(selected.submittedAt) }}</p>
          <div class="pre-wrap">{{ selected.submissionContent || '仅提交了附件' }}</div>
          <div v-if="selected.submissionAttachments?.length" class="attachment-list">
            <button
              v-for="attachment in selected.submissionAttachments"
              :key="attachment.id"
              class="attachment-button"
              type="button"
              @click="downloadAttachment(attachment)"
            >
              <span>{{ attachment.originalName }}</span>
              <small>{{ formatSize(attachment.fileSize) }}</small>
            </button>
          </div>
          <ElAlert
            v-if="selected.submissionStatus === 'RETURNED'"
            :closable="false"
            :title="`教师退回原因：${selected.returnReason || '未填写'}`"
            show-icon
            type="warning"
          />
          <div v-if="selected.submissionStatus === 'GRADED'" class="grade-result">
            <div>
              <span>成绩</span>
              <strong>{{ selected.score ?? '—' }} / {{ selected.maxScore ?? '—' }}</strong>
            </div>
            <p>{{ selected.teacherComment || '教师未填写评语' }}</p>
          </div>
        </section>

        <div v-if="canSubmit" class="drawer-actions">
          <ElButton type="primary" @click="openSubmit">
            {{ selected.submissionStatus === 'RETURNED' ? '重新提交' : selected.submissionStatus === 'SUBMITTED' ? '修改提交' : '提交作业' }}
          </ElButton>
        </div>
      </template>
    </ElDrawer>

    <ElDialog
      v-model="submitVisible"
      :title="selected?.submissionStatus === 'RETURNED' ? '重新提交作业' : '提交作业'"
      width="640px"
      @closed="cleanupNewAttachments"
    >
      <ElForm ref="submitFormRef" :model="submitForm" label-width="90px">
        <ElFormItem label="提交内容">
          <ElInput
            v-model="submitForm.content"
            maxlength="50000"
            :rows="8"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="提交附件">
          <div class="attachment-editor">
            <ElUpload
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
              :http-request="handleUpload"
              :show-file-list="false"
            >
              <ElButton :loading="uploading" plain>选择并上传附件</ElButton>
            </ElUpload>
            <div v-if="submissionAttachments.length" class="attachment-list">
              <ElTag
                v-for="attachment in submissionAttachments"
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
          <ElButton @click="submitVisible = false">取消</ElButton>
          <ElButton :loading="submitting" type="primary" @click="submitAssignment">
            确认提交
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.assignment-list {
  min-height: 250px;
  padding: 8px 22px;
}

.assignment-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 8px;
  border-bottom: 1px solid #eef2f7;
  cursor: pointer;
  transition: background 0.2s ease;
}

.assignment-item:hover {
  background: #f8fbff;
}

.subject-block {
  display: grid;
  width: 72px;
  height: 72px;
  flex: 0 0 auto;
  place-content: center;
  justify-items: center;
  border-radius: 14px;
  color: #215eb7;
  background: linear-gradient(145deg, #edf5ff, #dfeeff);
}

.subject-block span {
  font-size: 16px;
  font-weight: 700;
}

.subject-block small {
  margin-top: 5px;
  color: #6f88aa;
  font-size: 10px;
}

.assignment-main {
  min-width: 0;
  flex: 1;
}

.assignment-main h3 {
  margin: 8px 0 6px;
  color: #1d304f;
  font-size: 17px;
}

.assignment-main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #7a8799;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.assignment-meta {
  display: flex;
  gap: 18px;
  color: #8a96a8;
  font-size: 12px;
}

.overdue {
  color: #d15353;
}

.assignment-status {
  display: flex;
  min-width: 90px;
  flex-direction: column;
  align-items: flex-end;
  gap: 9px;
  color: #6d7b8f;
  font-size: 12px;
}

.detail-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 22px;
  border-bottom: 1px solid #edf1f7;
}

.detail-heading h2 {
  margin: 13px 0 8px;
  color: #172844;
  font-size: 27px;
  line-height: 1.4;
}

.detail-heading p {
  margin: 0;
  color: #7d899b;
  font-size: 12px;
}

.detail-section {
  padding: 24px 0;
  border-bottom: 1px solid #edf1f7;
}

.detail-section h3 {
  margin: 0 0 14px;
  color: #253a5b;
  font-size: 15px;
}

.pre-wrap {
  color: #3c4d66;
  font-size: 14px;
  line-height: 1.95;
  white-space: pre-wrap;
  word-break: break-word;
}

.attachment-list {
  display: grid;
  gap: 9px;
  margin-top: 16px;
}

.attachment-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
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

.section-heading {
  display: flex;
  justify-content: space-between;
}

.section-heading span,
.submission-time {
  color: #8a96a8;
  font-size: 12px;
}

.submission-box .el-alert {
  margin-top: 18px;
}

.grade-result {
  margin-top: 18px;
  padding: 18px;
  border-radius: 12px;
  background: #f3f8ff;
}

.grade-result div {
  display: flex;
  justify-content: space-between;
}

.grade-result span {
  color: #708097;
}

.grade-result strong {
  color: #1d63c3;
  font-size: 22px;
}

.grade-result p {
  margin: 12px 0 0;
  color: #51627a;
  line-height: 1.75;
  white-space: pre-wrap;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 22px;
}

.attachment-editor {
  width: 100%;
}
</style>

