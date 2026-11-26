<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { noticeApi } from '../../api/notice'
import type { FileAttachmentInfo, NoticeRow } from '../../types/notice'

const rows = ref<NoticeRow[]>([])
const total = ref(0)
const loading = ref(false)
const drawerVisible = ref(false)
const selected = ref<NoticeRow | null>(null)
const query = reactive({
  keyword: '',
  page: 1,
  size: 10,
})

async function loadData() {
  loading.value = true
  try {
    const result = await noticeApi.readable({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
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

async function openNotice(row: NoticeRow) {
  const detail = await noticeApi.detail(row.id)
  selected.value = detail
  drawerVisible.value = true
  if (!detail.isRead) {
    await noticeApi.markRead(detail.id)
    selected.value = { ...detail, isRead: true, readCount: detail.readCount + 1 }
    const index = rows.value.findIndex((item) => item.id === detail.id)
    if (index >= 0) {
      rows.value[index] = {
        ...rows.value[index],
        isRead: true,
        readCount: rows.value[index].readCount + 1,
      }
    }
  }
}

async function download(attachment: FileAttachmentInfo) {
  await noticeApi.downloadAttachment(attachment)
  ElMessage.success('附件下载已开始')
}

function scopeLabel(row?: NoticeRow | null) {
  if (!row) {
    return ''
  }
  if (row.scopeType === 'ALL') {
    return '全校通知'
  }
  if (row.scopeType === 'GRADE') {
    return `${row.gradeName || ''}年级通知`
  }
  return `${row.gradeName || ''}${row.className || ''}班级通知`
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
        <h2>校园通知</h2>
        <p>展示全校、年级以及你当前关联班级的已发布通知。</p>
      </div>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="搜索通知标题或正文"
          @keyup.enter="search"
        />
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="query.keyword = ''; search()">重置</ElButton>
      </div>

      <div v-loading="loading" class="notice-list">
        <article
          v-for="notice in rows"
          :key="notice.id"
          class="notice-item"
          @click="openNotice(notice)"
        >
          <span class="unread-dot" :class="{ read: notice.isRead }" />
          <div class="notice-main">
            <div class="notice-meta">
              <ElTag size="small" effect="plain">{{ scopeLabel(notice) }}</ElTag>
              <span>{{ notice.publisherName }}</span>
              <span>{{ formatDate(notice.publishedAt) }}</span>
            </div>
            <h3>{{ notice.title }}</h3>
            <p>{{ notice.content }}</p>
          </div>
          <div class="notice-side">
            <ElTag v-if="!notice.isRead" type="warning" effect="light">未读</ElTag>
            <ElTag v-else type="info" effect="plain">已读</ElTag>
            <span v-if="notice.attachments?.length">
              {{ notice.attachments.length }} 个附件
            </span>
          </div>
        </article>
        <ElEmpty v-if="!loading && rows.length === 0" description="暂无可阅读通知" />
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

    <ElDrawer v-model="drawerVisible" size="620px" :with-header="false">
      <template v-if="selected">
        <div class="detail-header">
          <ElTag effect="plain">{{ scopeLabel(selected) }}</ElTag>
          <h2>{{ selected.title }}</h2>
          <div>
            <span>{{ selected.publisherName }}</span>
            <span>{{ formatDate(selected.publishedAt) }}</span>
          </div>
        </div>
        <div class="detail-content">{{ selected.content }}</div>
        <div v-if="selected.attachments?.length" class="detail-attachments">
          <h3>附件</h3>
          <button
            v-for="attachment in selected.attachments"
            :key="attachment.id"
            class="attachment-button"
            type="button"
            @click="download(attachment)"
          >
            <span>{{ attachment.originalName }}</span>
            <small>{{ formatSize(attachment.fileSize) }} · 点击下载</small>
          </button>
        </div>
      </template>
    </ElDrawer>
  </section>
</template>

<style scoped>
.notice-list {
  min-height: 260px;
  padding: 8px 22px;
}

.notice-item {
  display: flex;
  gap: 16px;
  padding: 22px 8px;
  border-bottom: 1px solid #eef2f7;
  cursor: pointer;
  transition: background 0.2s ease;
}

.notice-item:hover {
  background: #f8fbff;
}

.unread-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  margin-top: 8px;
  border-radius: 50%;
  background: #2d79df;
  box-shadow: 0 0 0 5px rgba(45, 121, 223, 0.1);
}

.unread-dot.read {
  background: #c8d1df;
  box-shadow: none;
}

.notice-main {
  min-width: 0;
  flex: 1;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #8a96a8;
  font-size: 12px;
}

.notice-main h3 {
  margin: 10px 0 7px;
  color: #1d304f;
  font-size: 17px;
}

.notice-main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #778499;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notice-side {
  display: flex;
  min-width: 84px;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  color: #95a0b1;
  font-size: 12px;
}

.detail-header {
  padding-bottom: 24px;
  border-bottom: 1px solid #edf1f7;
}

.detail-header h2 {
  margin: 16px 0 14px;
  color: #172844;
  font-size: 27px;
  line-height: 1.45;
}

.detail-header div {
  display: flex;
  gap: 18px;
  color: #8a96a8;
  font-size: 12px;
}

.detail-content {
  padding: 28px 0;
  color: #33445f;
  font-size: 15px;
  line-height: 2;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-attachments {
  padding-top: 20px;
  border-top: 1px solid #edf1f7;
}

.detail-attachments h3 {
  margin: 0 0 14px;
  color: #253a5b;
  font-size: 15px;
}

.attachment-button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 9px;
  padding: 13px 15px;
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

