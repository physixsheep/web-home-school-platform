<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { fileApi } from '../../api/file'
import { leaveApi } from '../../api/leave'
import { useAuthStore } from '../../stores/auth'
import type { FileAttachmentInfo } from '../../types/notice'
import type { LeaveRow } from '../../types/leave'

const auth = useAuthStore()
const isTeacher = computed(() => auth.primaryRole === 'TEACHER')
const rows = ref<LeaveRow[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const decisionVisible = ref(false)
const selected = ref<LeaveRow | null>(null)
const detail = ref<LeaveRow | null>(null)
const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})
const decisionFormRef = ref<FormInstance>()
const decisionForm = reactive({
  decision: 'APPROVED' as 'APPROVED' | 'REJECTED',
  opinion: '',
})
const decisionRules: FormRules<typeof decisionForm> = {
  decision: [{ required: true, message: '请选择审批结果', trigger: 'change' }],
  opinion: [{ required: true, message: '请输入审批意见', trigger: 'blur' }],
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

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    }
    const result = isTeacher.value
      ? await leaveApi.approvalPage(params)
      : await leaveApi.page(params)
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

async function openDetail(row: LeaveRow) {
  selected.value = row
  detail.value = await leaveApi.detail(row.id)
  detailVisible.value = true
}

function openDecision(row: LeaveRow, decision: 'APPROVED' | 'REJECTED') {
  selected.value = row
  decisionForm.decision = decision
  decisionForm.opinion = ''
  decisionVisible.value = true
}

async function submitDecision() {
  const valid = await decisionFormRef.value?.validate().catch(() => false)
  if (!valid || !selected.value) {
    return
  }
  saving.value = true
  try {
    await leaveApi.decide(selected.value.id, {
      decision: decisionForm.decision,
      opinion: decisionForm.opinion,
    })
    ElMessage.success(decisionForm.decision === 'APPROVED' ? '请假已批准' : '请假已驳回')
    decisionVisible.value = false
    await loadData()
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

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>{{ isTeacher ? '请假审批' : '全校请假记录' }}</h2>
        <p>
          {{ isTeacher ? '当前班主任可审批本班学生请假，审批结果和意见会实时通知学生及家长。' : '查看全校请假申请与审批结果。' }}
        </p>
      </div>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="学生姓名、学号或原因"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="待审批" value="PENDING" />
          <ElOption label="已批准" value="APPROVED" />
          <ElOption label="已驳回" value="REJECTED" />
          <ElOption label="已撤销" value="CANCELLED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无请假记录" stripe>
          <ElTableColumn label="学生" min-width="130">
            <template #default="{ row }">
              <strong>{{ row.studentName }}</strong>
              <div class="muted-text">{{ row.studentNo }} · {{ row.gradeName }}{{ row.className }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="类型" width="90">
            <template #default="{ row }">{{ typeNames[row.leaveType] }}</template>
          </ElTableColumn>
          <ElTableColumn label="请假时间" min-width="260">
            <template #default="{ row }">
              {{ formatDate(row.startAt) }}<br />
              <span class="muted-text">至 {{ formatDate(row.endAt) }}</span>
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
          <ElTableColumn label="操作" width="205" fixed="right">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">详情</ElButton>
              <template v-if="isTeacher && row.status === 'PENDING'">
                <ElButton link type="success" @click="openDecision(row, 'APPROVED')">
                  批准
                </ElButton>
                <ElButton link type="danger" @click="openDecision(row, 'REJECTED')">
                  驳回
                </ElButton>
              </template>
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

    <ElDrawer v-model="detailVisible" size="620px" :with-header="false">
      <template v-if="detail">
        <div class="detail-heading">
          <div>
            <ElTag effect="plain">{{ typeNames[detail.leaveType] }}</ElTag>
            <h2>{{ detail.studentName }}的请假申请</h2>
            <p>{{ detail.gradeName }}{{ detail.className }} · {{ detail.studentNo }}</p>
          </div>
          <ElTag :type="statusMap[detail.status]?.type || 'info'" effect="light">
            {{ statusMap[detail.status]?.label }}
          </ElTag>
        </div>
        <ElDescriptions :column="1" border class="detail-descriptions">
          <ElDescriptionsItem label="开始时间">{{ formatDate(detail.startAt) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="结束时间">{{ formatDate(detail.endAt) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="原因">
            <span class="pre-wrap">{{ detail.reason }}</span>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="审批教师">{{ detail.approverName || '—' }}</ElDescriptionsItem>
          <ElDescriptionsItem label="审批时间">{{ formatDate(detail.approvedAt) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="审批意见">
            <span class="pre-wrap">{{ detail.approvalOpinion || '—' }}</span>
          </ElDescriptionsItem>
        </ElDescriptions>
        <div v-if="detail.attachments?.length" class="proof-list">
          <h3>证明材料</h3>
          <button
            v-for="attachment in detail.attachments"
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

    <ElDialog v-model="decisionVisible" title="请假审批" width="520px">
      <p class="decision-context">
        {{ selected?.studentName }} · {{ formatDate(selected?.startAt) }} 至
        {{ formatDate(selected?.endAt) }}
      </p>
      <ElForm
        ref="decisionFormRef"
        :model="decisionForm"
        :rules="decisionRules"
        label-width="90px"
      >
        <ElFormItem label="审批结果" prop="decision">
          <ElRadioGroup v-model="decisionForm.decision">
            <ElRadio value="APPROVED">批准</ElRadio>
            <ElRadio value="REJECTED">驳回</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="审批意见" prop="opinion">
          <ElInput
            v-model="decisionForm.opinion"
            maxlength="500"
            :rows="4"
            show-word-limit
            type="textarea"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="decisionVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submitDecision">
            确认审批
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.decision-context {
  margin: 0 0 18px;
  color: #728096;
  font-size: 13px;
}
</style>

