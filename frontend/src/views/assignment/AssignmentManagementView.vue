<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions,
} from 'element-plus'

import { assignmentApi } from '../../api/assignment'
import { fileApi } from '../../api/file'
import type {
  AssignmentRow,
  AssignmentSavePayload,
  SubmissionRow,
  TeachingOption,
} from '../../types/assignment'
import type { FileAttachmentInfo } from '../../types/notice'

const rows = ref<AssignmentRow[]>([])
const teachings = ref<TeachingOption[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const attachments = ref<FileAttachmentInfo[]>([])
const newAttachmentIds = ref<number[]>([])
const committed = ref(false)
const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})
const form = reactive({
  title: '',
  content: '',
  teachingKey: '',
  classId: 0,
  subjectId: 0,
  deadline: '',
  maxScore: 100 as number | undefined,
})
const rules: FormRules<typeof form> = {
  title: [{ required: true, message: '请输入作业标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入作业内容', trigger: 'blur' }],
  teachingKey: [{ required: true, message: '请选择班级和学科', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
}

const submissionsVisible = ref(false)
const currentAssignment = ref<AssignmentRow | null>(null)
const submissions = ref<SubmissionRow[]>([])
const submissionTotal = ref(0)
const submissionPage = ref(1)
const submissionLoading = ref(false)
const submissionQuery = reactive({
  keyword: '',
  status: '',
})
const gradeVisible = ref(false)
const gradeFormRef = ref<FormInstance>()
const currentSubmission = ref<SubmissionRow | null>(null)
const gradeForm = reactive({
  score: undefined as number | undefined,
  comment: '',
})
const returnVisible = ref(false)
const returnFormRef = ref<FormInstance>()
const returnForm = reactive({ reason: '' })
const gradeRules: FormRules<typeof gradeForm> = {
  score: [
    { required: true, message: '请输入成绩', trigger: 'change' },
    { type: 'number', min: 0, message: '成绩不能小于 0', trigger: 'change' },
  ],
}
const returnRules: FormRules<typeof returnForm> = {
  reason: [{ required: true, message: '请输入退回原因', trigger: 'blur' }],
}

const statusMap: Record<string, { label: string; type: 'info' | 'success' | 'warning' }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  CLOSED: { label: '已关闭', type: 'warning' },
}
const submissionStatusMap: Record<
  string,
  { label: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }
> = {
  UNSUBMITTED: { label: '未提交', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'success' },
  GRADED: { label: '已批改', type: 'primary' },
  RETURNED: { label: '已退回', type: 'danger' },
}

const teachingOptions = computed(() =>
  teachings.value.map((item) => ({
    value: `${item.classId}:${item.subjectId}`,
    label: `${item.gradeName}${item.className} · ${item.subjectName}`,
  })),
)

async function loadData() {
  loading.value = true
  try {
    const result = await assignmentApi.manage({
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
  const result = await assignmentApi.options()
  teachings.value = result.teachings
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

function localDateTime(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 19)
}

function resetForm() {
  Object.assign(form, {
    title: '',
    content: '',
    teachingKey: '',
    classId: 0,
    subjectId: 0,
    deadline: localDateTime(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
    maxScore: 100,
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

async function openEdit(row: AssignmentRow) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    content: row.content,
    teachingKey: `${row.classId}:${row.subjectId}`,
    classId: row.classId,
    subjectId: row.subjectId,
    deadline: row.deadline.slice(0, 19),
    maxScore: row.maxScore,
  })
  attachments.value = row.attachments ?? []
  newAttachmentIds.value = []
  committed.value = false
  dialogVisible.value = true
}

function onTeachingChange(value: string) {
  const [classId, subjectId] = value.split(':').map(Number)
  form.classId = classId
  form.subjectId = subjectId
}

async function saveAssignment() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const payload: AssignmentSavePayload = {
    title: form.title,
    content: form.content,
    classId: form.classId,
    subjectId: form.subjectId,
    deadline: form.deadline.length === 16 ? `${form.deadline}:00` : form.deadline,
    maxScore: form.maxScore,
    attachmentIds: attachments.value.map((item) => item.id),
  }
  saving.value = true
  try {
    if (editingId.value) {
      await assignmentApi.update(editingId.value, payload)
      ElMessage.success('作业已保存')
    } else {
      await assignmentApi.create(payload)
      ElMessage.success('作业草稿已创建')
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

async function cleanupUncommittedAttachments() {
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
      // Temporary attachment may already have been removed.
    }
  }
}

async function publish(row: AssignmentRow) {
  try {
    await ElMessageBox.confirm(`确认发布作业“${row.title}”吗？`, '发布作业', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await assignmentApi.publish(row.id)
  ElMessage.success('作业已发布')
  await loadData()
}

async function closeAssignment(row: AssignmentRow) {
  try {
    await ElMessageBox.confirm(`确认关闭作业“${row.title}”吗？`, '关闭作业', {
      type: 'warning',
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await assignmentApi.close(row.id)
  ElMessage.success('作业已关闭')
  await loadData()
}

async function deleteDraft(row: AssignmentRow) {
  try {
    await ElMessageBox.confirm(`确认删除草稿“${row.title}”吗？`, '删除草稿', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await assignmentApi.deleteDraft(row.id)
  ElMessage.success('作业草稿已删除')
  await loadData()
}

async function openSubmissions(row: AssignmentRow) {
  currentAssignment.value = row
  submissionQuery.keyword = ''
  submissionQuery.status = ''
  submissionPage.value = 1
  submissionsVisible.value = true
  await loadSubmissions()
}

async function loadSubmissions() {
  if (!currentAssignment.value) {
    return
  }
  submissionLoading.value = true
  try {
    const result = await assignmentApi.submissions(currentAssignment.value.id, {
      page: submissionPage.value,
      size: 10,
      keyword: submissionQuery.keyword || undefined,
      status: submissionQuery.status || undefined,
    })
    submissions.value = result.records
    submissionTotal.value = result.total
  } finally {
    submissionLoading.value = false
  }
}

function openGrade(row: SubmissionRow) {
  currentSubmission.value = row
  gradeForm.score = row.score
  gradeForm.comment = row.comment || ''
  gradeVisible.value = true
}

async function submitGrade() {
  const valid = await gradeFormRef.value?.validate().catch(() => false)
  if (!valid || !currentSubmission.value?.submissionId) {
    return
  }
  saving.value = true
  try {
    await assignmentApi.grade(currentSubmission.value.submissionId, {
      score: gradeForm.score,
      comment: gradeForm.comment || undefined,
    })
    ElMessage.success('作业批改已保存')
    gradeVisible.value = false
    await Promise.all([loadSubmissions(), loadData()])
  } finally {
    saving.value = false
  }
}

function openReturn(row: SubmissionRow) {
  currentSubmission.value = row
  returnForm.reason = ''
  returnVisible.value = true
}

async function submitReturn() {
  const valid = await returnFormRef.value?.validate().catch(() => false)
  if (!valid || !currentSubmission.value?.submissionId) {
    return
  }
  saving.value = true
  try {
    await assignmentApi.returnSubmission(
      currentSubmission.value.submissionId,
      returnForm.reason,
    )
    ElMessage.success('作业已退回')
    returnVisible.value = false
    await Promise.all([loadSubmissions(), loadData()])
  } finally {
    saving.value = false
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

onMounted(async () => {
  await Promise.all([loadData(), loadOptions()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>作业管理</h2>
        <p>作业只能发布到本人任教的班级和学科，提交清单包含当前班级全部学生。</p>
      </div>
      <ElButton type="primary" @click="openCreate">布置作业</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="作业标题或内容"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="草稿" value="DRAFT" />
          <ElOption label="已发布" value="PUBLISHED" />
          <ElOption label="已关闭" value="CLOSED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无作业" stripe>
          <ElTableColumn label="作业" min-width="260">
            <template #default="{ row }">
              <strong>{{ row.title }}</strong>
              <p class="assignment-sub">
                {{ row.gradeName }}{{ row.className }} · {{ row.subjectName }}
              </p>
            </template>
          </ElTableColumn>
          <ElTableColumn label="截止时间" width="180">
            <template #default="{ row }">{{ formatDate(row.deadline) }}</template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusMap[row.status]?.type || 'info'" effect="light">
                {{ statusMap[row.status]?.label || row.status }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="提交情况" width="160">
            <template #default="{ row }">
              已交 {{ row.submittedCount || 0 }} / {{ row.studentCount || 0 }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="已批改" width="100">
            <template #default="{ row }">{{ row.gradedCount || 0 }}</template>
          </ElTableColumn>
          <ElTableColumn label="未交" width="90">
            <template #default="{ row }">
              <span :class="{ danger: (row.unsubmittedCount || 0) > 0 }">
                {{ row.unsubmittedCount || 0 }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="285">
            <template #default="{ row }">
              <ElButton v-if="row.status !== 'CLOSED'" link type="primary" @click="openEdit(row)">
                编辑
              </ElButton>
              <ElButton v-if="row.status === 'DRAFT'" link type="success" @click="publish(row)">
                发布
              </ElButton>
              <ElButton v-if="row.status === 'DRAFT'" link type="danger" @click="deleteDraft(row)">
                删除
              </ElButton>
              <ElButton v-if="row.status === 'PUBLISHED'" link type="warning" @click="closeAssignment(row)">
                关闭
              </ElButton>
              <ElButton
                v-if="row.status !== 'DRAFT'"
                link
                type="primary"
                @click="openSubmissions(row)"
              >
                提交清单
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
      :title="editingId ? '编辑作业' : '布置作业'"
      width="780px"
      @closed="cleanupUncommittedAttachments"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="100px">
        <ElFormItem label="作业标题" prop="title">
          <ElInput v-model="form.title" maxlength="200" show-word-limit />
        </ElFormItem>
        <ElFormItem label="班级学科" prop="teachingKey">
          <ElSelect
            v-model="form.teachingKey"
            placeholder="选择本人任教班级和学科"
            style="width: 100%"
            @change="onTeachingChange"
          >
            <ElOption
              v-for="item in teachingOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>
        <div class="two-column">
          <ElFormItem label="截止时间" prop="deadline">
            <ElDatePicker
              v-model="form.deadline"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </ElFormItem>
          <ElFormItem label="满分">
            <ElInputNumber v-model="form.maxScore" :min="0.01" :max="999" />
          </ElFormItem>
        </div>
        <ElFormItem label="作业内容" prop="content">
          <ElInput
            v-model="form.content"
            maxlength="50000"
            :rows="9"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="作业附件">
          <div class="attachment-editor">
            <ElUpload
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
              :http-request="handleUpload"
              :show-file-list="false"
            >
              <ElButton :loading="uploading" plain>选择并上传附件</ElButton>
            </ElUpload>
            <span class="upload-tip">单文件不超过 20 MB</span>
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
          <ElButton :loading="saving" type="primary" @click="saveAssignment">保存</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDrawer v-model="submissionsVisible" size="860px" :with-header="false">
      <div class="drawer-heading">
        <div>
          <ElTag effect="plain">提交清单</ElTag>
          <h2>{{ currentAssignment?.title }}</h2>
          <p>截止：{{ formatDate(currentAssignment?.deadline) }}</p>
        </div>
      </div>
      <div class="filter-bar drawer-filter">
        <ElInput
          v-model="submissionQuery.keyword"
          clearable
          placeholder="学生姓名或学号"
          @keyup.enter="loadSubmissions"
        />
        <ElSelect v-model="submissionQuery.status" clearable placeholder="全部状态">
          <ElOption label="未提交" value="UNSUBMITTED" />
          <ElOption label="已提交" value="SUBMITTED" />
          <ElOption label="已批改" value="GRADED" />
          <ElOption label="已退回" value="RETURNED" />
        </ElSelect>
        <ElButton type="primary" @click="loadSubmissions">查询</ElButton>
      </div>
      <ElTable v-loading="submissionLoading" :data="submissions" empty-text="暂无学生">
        <ElTableColumn label="学生" width="140">
          <template #default="{ row }">
            <strong>{{ row.studentName }}</strong>
            <div class="muted-text">{{ row.studentNo }}</div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="submissionStatusMap[row.status]?.type || 'info'" effect="light">
              {{ submissionStatusMap[row.status]?.label || row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="attemptCount" label="次数" width="75" />
        <ElTableColumn label="提交内容" min-width="160">
          <template #default="{ row }">
            <span v-if="row.content" class="truncate-text">{{ row.content }}</span>
            <span v-else class="muted-text">—</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="附件" width="100">
          <template #default="{ row }">
            <ElButton
              v-if="row.attachments?.length"
              link
              type="primary"
              @click="downloadAttachment(row.attachments[0])"
            >
              {{ row.attachments.length }} 个
            </ElButton>
            <span v-else class="muted-text">—</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="成绩" width="80">
          <template #default="{ row }">{{ row.score ?? '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.submissionId && row.status !== 'RETURNED'">
              <ElButton link type="primary" @click="openGrade(row)">批改</ElButton>
              <ElButton link type="warning" @click="openReturn(row)">退回</ElButton>
            </template>
            <span v-else class="muted-text">{{ row.status === 'UNSUBMITTED' ? '等待提交' : '等待重交' }}</span>
          </template>
        </ElTableColumn>
      </ElTable>
      <div class="pagination-row">
        <ElPagination
          v-model:current-page="submissionPage"
          :page-size="10"
          :total="submissionTotal"
          layout="total, prev, pager, next"
          @current-change="loadSubmissions"
        />
      </div>
    </ElDrawer>

    <ElDialog v-model="gradeVisible" title="批改作业" width="500px">
      <p class="dialog-context">
        {{ currentSubmission?.studentName }} · 第 {{ currentSubmission?.attemptCount }} 次提交
      </p>
      <ElForm ref="gradeFormRef" :model="gradeForm" :rules="gradeRules" label-width="90px">
        <ElFormItem label="成绩" prop="score">
          <ElInputNumber
            v-model="gradeForm.score"
            :min="0"
            :max="currentAssignment?.maxScore || 999"
          />
          <span class="score-hint">/ {{ currentAssignment?.maxScore || '未设置满分' }}</span>
        </ElFormItem>
        <ElFormItem label="评语">
          <ElInput
            v-model="gradeForm.comment"
            maxlength="5000"
            :rows="5"
            type="textarea"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="gradeVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submitGrade">保存批改</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDialog v-model="returnVisible" title="退回作业" width="500px">
      <p class="dialog-context">退回后学生可重新提交，原批改结果将暂时隐藏。</p>
      <ElForm ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="90px">
        <ElFormItem label="退回原因" prop="reason">
          <ElInput
            v-model="returnForm.reason"
            maxlength="500"
            :rows="4"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="returnVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="warning" @click="submitReturn">确认退回</ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.assignment-sub {
  margin: 6px 0 0;
  color: #8a96a8;
  font-size: 12px;
}

.danger {
  color: #d75454;
  font-weight: 700;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.attachment-editor {
  width: 100%;
}

.upload-tip,
.score-hint {
  color: #8a96a8;
  font-size: 12px;
}

.upload-tip {
  display: block;
  margin-top: 8px;
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.drawer-heading {
  padding: 4px 0 22px;
  border-bottom: 1px solid #edf1f7;
}

.drawer-heading h2 {
  margin: 12px 0 5px;
  color: #172844;
  font-size: 24px;
}

.drawer-heading p,
.dialog-context {
  color: #7d899b;
  font-size: 13px;
}

.drawer-filter {
  margin-top: 18px;
}

.truncate-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.score-hint {
  margin-left: 8px;
}
</style>

