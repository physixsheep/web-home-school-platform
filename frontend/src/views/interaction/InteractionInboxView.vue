<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { interactionApi } from '../../api/interaction'
import { useAuthStore } from '../../stores/auth'
import type { MessageDetail, MessageRow } from '../../types/interaction'

const auth = useAuthStore()
const isTeacher = computed(() => auth.primaryRole === 'TEACHER')
const rows = ref<MessageRow[]>([])
const total = ref(0)
const loading = ref(false)
const sending = ref(false)
const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})
const detailVisible = ref(false)
const detail = ref<MessageDetail | null>(null)
const replyContent = ref('')
const threadElement = ref<HTMLDivElement>()

const statusMap: Record<
  string,
  { label: string; type: 'info' | 'success' | 'warning' | 'danger' }
> = {
  UNREAD: { label: '未读', type: 'warning' },
  READ: { label: '已读', type: 'info' },
  REPLIED: { label: '已回复', type: 'success' },
  CLOSED: { label: '已关闭', type: 'danger' },
}

async function loadData() {
  loading.value = true
  try {
    const result = await interactionApi.received({
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

async function openDetail(row: MessageRow) {
  if (isTeacher.value && row.status === 'UNREAD') {
    await interactionApi.markRead(row.id)
  }
  detail.value = await interactionApi.detail(row.id)
  replyContent.value = ''
  detailVisible.value = true
  await loadData()
  await nextTick()
  if (threadElement.value) {
    threadElement.value.scrollTop = threadElement.value.scrollHeight
  }
}

async function sendReply() {
  if (!detail.value || !replyContent.value.trim()) {
    return
  }
  sending.value = true
  try {
    await interactionApi.reply(detail.value.message.id, replyContent.value)
    replyContent.value = ''
    detail.value = await interactionApi.detail(detail.value.message.id)
    await loadData()
    await nextTick()
    if (threadElement.value) {
      threadElement.value.scrollTop = threadElement.value.scrollHeight
    }
  } finally {
    sending.value = false
  }
}

async function closeThread() {
  if (!detail.value) {
    return
  }
  try {
    await ElMessageBox.confirm('关闭后双方不能继续回复，确认关闭该留言吗？', '关闭留言', {
      type: 'warning',
      confirmButtonText: '关闭',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  await interactionApi.close(detail.value.message.id)
  detail.value = await interactionApi.detail(detail.value.message.id)
  ElMessage.success('留言会话已关闭')
  await loadData()
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>{{ isTeacher ? '家校互动' : '全校留言审计' }}</h2>
        <p>
          {{ isTeacher ? '查看家长和学生留言，回复问题并管理会话状态。' : '查看全校家校沟通记录。' }}
        </p>
      </div>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="主题、内容或家长学生姓名"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="未读" value="UNREAD" />
          <ElOption label="已读" value="READ" />
          <ElOption label="已回复" value="REPLIED" />
          <ElOption label="已关闭" value="CLOSED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无留言" stripe>
          <ElTableColumn label="发送人" width="130">
            <template #default="{ row }">
              <strong>{{ row.senderName }}</strong>
              <div class="muted-text">{{ row.senderRole === 'PARENT' ? '家长' : '学生' }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="学生" width="130">
            <template #default="{ row }">
              {{ row.studentName }}
              <div class="muted-text">{{ row.gradeName }}{{ row.className }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="留言" min-width="260">
            <template #default="{ row }">
              <strong>{{ row.subject }}</strong>
              <p class="preview">{{ row.lastReplyPreview || row.content }}</p>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusMap[row.status]?.type || 'info'" effect="light">
                {{ statusMap[row.status]?.label }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="最近沟通" width="180">
            <template #default="{ row }">{{ formatDate(row.lastReplyAt || row.createdAt) }}</template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <ElButton link type="primary" @click="openDetail(row)">查看会话</ElButton>
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

    <ElDrawer v-model="detailVisible" size="700px" :with-header="false">
      <template v-if="detail">
        <div class="thread-heading">
          <div>
            <h2>{{ detail.message.subject }}</h2>
            <p>
              {{ detail.message.senderName }} · {{ detail.message.studentName }} ·
              {{ formatDate(detail.message.createdAt) }}
            </p>
          </div>
          <div class="thread-actions">
            <ElTag :type="statusMap[detail.message.status]?.type || 'info'" effect="light">
              {{ statusMap[detail.message.status]?.label }}
            </ElTag>
            <ElButton
              v-if="isTeacher && detail.message.status !== 'CLOSED'"
              plain
              type="danger"
              @click="closeThread"
            >
              关闭会话
            </ElButton>
          </div>
        </div>
        <div ref="threadElement" class="thread">
          <div class="bubble initiator">
            <span>{{ detail.message.senderName }}</span>
            <p>{{ detail.message.content }}</p>
            <small>{{ formatDate(detail.message.createdAt) }}</small>
          </div>
          <div
            v-for="reply in detail.replies"
            :key="reply.id"
            class="bubble"
            :class="{ teacher: reply.replierRole === 'TEACHER' }"
          >
            <span>{{ reply.replierName }}</span>
            <p>{{ reply.content }}</p>
            <small>{{ formatDate(reply.createdAt) }}</small>
          </div>
        </div>
        <div v-if="isTeacher && detail.message.status !== 'CLOSED'" class="reply-box">
          <ElInput
            v-model="replyContent"
            maxlength="10000"
            :rows="3"
            placeholder="输入回复内容..."
            type="textarea"
          />
          <ElButton :loading="sending" type="primary" @click="sendReply">发送回复</ElButton>
        </div>
        <ElAlert
          v-if="!isTeacher"
          :closable="false"
          title="管理员审计视图为只读"
          type="info"
        />
        <ElAlert
          v-else-if="detail.message.status === 'CLOSED'"
          :closable="false"
          title="该留言会话已关闭"
          type="info"
        />
      </template>
    </ElDrawer>
  </section>
</template>

<style scoped>
.preview {
  overflow: hidden;
  margin: 5px 0 0;
  color: #8a96a8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.thread-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f7;
}

.thread-heading h2 {
  margin: 0 0 8px;
  color: #172844;
  font-size: 24px;
}

.thread-heading p {
  margin: 0;
  color: #8a96a8;
  font-size: 12px;
}

.thread-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thread {
  display: flex;
  max-height: 450px;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding: 22px 4px;
}

.bubble {
  align-self: flex-start;
  max-width: 78%;
  padding: 12px 15px;
  border-radius: 4px 14px 14px;
  color: #33445e;
  background: #f1f4f9;
}

.bubble.initiator,
.bubble.teacher {
  align-self: flex-end;
  border-radius: 14px 4px 14px 14px;
  color: #fff;
  background: linear-gradient(135deg, #2467c4, #3b88e4);
}

.bubble span,
.bubble small {
  font-size: 11px;
  opacity: 0.66;
}

.bubble p {
  margin: 7px 0;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.reply-box {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: end;
  padding-top: 18px;
  border-top: 1px solid #edf1f7;
}
</style>

