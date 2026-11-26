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
import { leaveApi } from '../../api/leave'
import { useAuthStore } from '../../stores/auth'
import type { LeaveCreatePayload, LeaveRow } from '../../types/leave'
import type { FileAttachmentInfo } from '../../types/notice'

const auth = useAuthStore()
const isParent = computed(() => auth.primaryRole === 'PARENT')
const isStudent = computed(() => auth.primaryRole === 'STUDENT')
const rows = ref<LeaveRow[]>([])
const total = ref(0)
const loading = ref(false)
const children = ref<{ id: number; label: string; secondary?: string }[]>([])
const selectedStudentId = ref<number>()
const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})
const createVisible = ref(false)
const detailVisible = ref(false)
const selected = ref<LeaveRow | null>(null)
const formRef = ref<FormInstance>()
const saving = ref(false)
const uploading = ref(false)
const proofAttachments = ref<FileAttachmentInfo[]>([])
const newAttachmentIds = ref<number[]>([])
const committed = ref(false)
const form = reactive<LeaveCreatePayload>({
  leaveType: 'SICK',
  startAt: '',
  endAt: '',
  reason: '',
  attachmentIds: [],
})
const rules: FormRules<LeaveCreatePayload> = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endAt: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }],
}

const typeNames: Record<string, string> = {
  SICK: '病假',
  PERSONAL: '事假',
  OTHER: '其他',
}
const statusMap: Record<
  string,
  { label: string; type: 'info' | 'success' | 'danger' | 'warning' }
> = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已批准', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已撤销', type: 'info' },
}

async function loadOptions() {
  if (!isParent.value) {
    return
  }
  const options = await assignmentApi.options()
  children.value = options.children
  selectedStudentId.value = options.children[0]?.id
}

async function loadData() {
  if (isParent.value && !selectedStudentId.value) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const result = await leaveApi.page({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
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

function localDateTime(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 19)
}

function openCreate() {
  const now = new Date()
  const start = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  start.setHours(8, 0, 0, 0)
  const end = new Date(start.getTime() + 8 * 60 * 60 * 1000)
  Object.assign(form, {
    leaveType: 'SICK',
    startAt: localDateTime(start),
    endAt: localDateTime(end),
    reason: '',
    attachmentIds: [],
  })
  proofAttachments.value = []
  newAttachmentIds.value = []
  committed.value = false
  createVisible.value = true
}

async function handleUpload(request: UploadRequestOptions) {
  uploading.value = true
  try {
    const uploaded = await fileApi.upload(request.file)
    proofAttachments.value.push(uploaded)
    newAttachmentIds.value.push(uploaded.id)
    request.onSuccess(uploaded)
    ElMessage.success('证明附件上传成功')
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
  proofAttachments.value = proofAttachments.value.filter(
    (item) => item.id !== attachment.id,
  )
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

async function submitLeave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await leaveApi.create({
      ...form,
      attachmentIds: proofAttachments.value.map((item) => item.id),
    })
    committed.value = true
    createVisible.value = false
    ElMessage.success('请假申请已提交')
    await loadData()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: LeaveRow) {
  selected.value = await leaveApi.detail(row.id)
  detailVisible.value = true
}

async function cancelLeave(row: LeaveRow) {
  try {
    await ElMessageBox.confirm('确认撤销这条待审批请假申请吗？', '撤销请假', {
      type: 'warning',
      confirmButtonText: '撤销',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await leaveApi.cancel(row.id)
  ElMessage.success('请假申请已撤销')
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
  await loadOptions()
  await loadData()
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>{{ isParent ? '请假记录' : '我的请假' }}</h2>
        <p>
          {{ isParent ? '查看绑定孩子的请假申请和审批结果。' : '提交请假申请并查看班主任审批结果。' }}
        </p>
      </div>
      <div class="heading-actions">
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
        <ElButton v-if="isStudent" type="primary" @click="openCreate">请假申请</ElButton>
      </div>
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
          placeholder="请假原因"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="待审批" value="PENDING" />
          <ElOption label="已批准" value="APPROVED" />
          <ElOption label="已驳回" value="REJECTED" />
          <ElOption label="已撤销" value="CANCELLED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="query.keyword = ''; query.status = ''; search()">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无请假记录" stripe>
          <ElTableColumn label="学生" min-width="130">
            <template #default="{ row }">
              <strong>{{ row.studentName }}</strong>
              <div class="muted-text">{{ row.studentNo }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="类型" width="90">
            <template #default="{ row }">{{ typeNames[row.leaveType] }}</template>
          </ElTableColumn>
          <ElTableColumn label="请假时间" min-width="260">
            <template #default="{ row }">
              {{ formatDate(row.startAt) }} 至 {{ formatDate(row.endAt) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="原因" min-width="180">
            <template #default="{ row }">
              <span class="ellipsis">{{ row.reason }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusMap[row.status]?.type || 'info'" effect="light">
                {{ statusMap[row.status]?.label }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">详情</ElButton>
              <ElButton
                v-if="isStudent && row.status === 'PENDING'"
                link
                type="danger"
                @click="cancelLeave(row)"
              >
                撤销
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
      v-model="createVisible"
      title="提交请假申请"
      width="650px"
      @closed="cleanupAttachments"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <ElFormItem label="请假类型" prop="leaveType">
          <ElRadioGroup v-model="form.leaveType">
            <ElRadio value="SICK">病假</ElRadio>
            <ElRadio value="PERSONAL">事假</ElRadio>
            <ElRadio value="OTHER">其他</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
        <div class="time-grid">
          <ElFormItem label="开始时间" prop="startAt">
            <ElDatePicker
              v-model="form.startAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </ElFormItem>
          <ElFormItem label="结束时间" prop="endAt">
            <ElDatePicker
              v-model="form.endAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </ElFormItem>
        </div>
        <ElFormItem label="请假原因" prop="reason">
          <ElInput
            v-model="form.reason"
            maxlength="5000"
            :rows="5"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem label="证明材料">
          <div class="attachment-editor">
            <ElUpload
              accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.txt"
              :http-request="handleUpload"
              :show-file-list="false"
            >
              <ElButton :loading="uploading" plain>上传证明附件</ElButton>
            </ElUpload>
            <div v-if="proofAttachments.length" class="attachment-list">
              <ElTag
                v-for="attachment in proofAttachments"
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
          <ElButton @click="createVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submitLeave">提交申请</ElButton>
        </div>
      </template>
    </ElDialog>

    <ElDrawer v-model="detailVisible" size="620px" :with-header="false">
      <template v-if="selected">
        <div class="detail-heading">
          <div>
            <ElTag effect="plain">{{ typeNames[selected.leaveType] }}</ElTag>
            <h2>{{ selected.studentName }}的请假申请</h2>
            <p>{{ selected.gradeName }}{{ selected.className }} · {{ selected.studentNo }}</p>
          </div>
          <ElTag :type="statusMap[selected.status]?.type || 'info'" effect="light">
            {{ statusMap[selected.status]?.label }}
          </ElTag>
        </div>
        <ElDescriptions :column="1" border class="detail-descriptions">
          <ElDescriptionsItem label="开始时间">{{ formatDate(selected.startAt) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="结束时间">{{ formatDate(selected.endAt) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="请假原因">
            <span class="pre-wrap">{{ selected.reason }}</span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="审批教师">
            {{ selected.approverName || '—' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="审批时间">
            {{ formatDate(selected.approvedAt) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="审批意见">
            <span class="pre-wrap">{{ selected.approvalOpinion || '—' }}</span>
          </ElDescriptionsItem>
        </ElDescriptions>
        <div v-if="selected.attachments?.length" class="proof-list">
          <h3>证明材料</h3>
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
.heading-actions {
  display: flex;
  gap: 12px;
}

.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-grid {
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f7;
}

.detail-heading h2 {
  margin: 12px 0 6px;
  color: #172844;
  font-size: 25px;
}

.detail-heading p {
  margin: 0;
  color: #8a96a8;
  font-size: 12px;
}

.detail-descriptions {
  margin-top: 22px;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-word;
}

.proof-list {
  margin-top: 24px;
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

