<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { assignmentApi } from '../../api/assignment'
import { fileApi } from '../../api/file'
import { growthApi } from '../../api/growth'
import { useAuthStore } from '../../stores/auth'
import type { GrowthCategory, GrowthRow } from '../../types/growth'
import type { FileAttachmentInfo } from '../../types/notice'

const auth = useAuthStore()
const isParent = computed(() => auth.primaryRole === 'PARENT')
const isAdmin = computed(() => auth.primaryRole === 'ADMIN')
const rows = ref<GrowthRow[]>([])
const total = ref(0)
const loading = ref(false)
const children = ref<{ id: number; label: string; secondary?: string }[]>([])
const selectedStudentId = ref<number>()
const detailVisible = ref(false)
const selected = ref<GrowthRow | null>(null)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  keyword: '',
  category: '',
  page: 1,
  size: 10,
})

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
    const result = await growthApi.page({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      studentId: isParent.value ? selectedStudentId.value : undefined,
      dateFrom: dateRange.value?.[0],
      dateTo: dateRange.value?.[1],
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

async function openDetail(row: GrowthRow) {
  selected.value = await growthApi.detail(row.id)
  detailVisible.value = true
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
        <h2>{{ isAdmin ? '成长档案总览' : '成长档案' }}</h2>
        <p>
          {{ isAdmin ? '查看全校学生成长记录。' : isParent ? '查看绑定孩子的学习、德育、活动与荣誉记录。' : '查看属于你的学习与成长足迹。' }}
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
          placeholder="标题或内容"
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
        <ElDatePicker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
        />
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="query.keyword = ''; query.category = ''; dateRange = null; search()">
          重置
        </ElButton>
      </div>

      <div v-loading="loading" class="growth-timeline">
        <article
          v-for="row in rows"
          :key="row.id"
          class="growth-item"
          @click="openDetail(row)"
        >
          <div
            class="timeline-dot"
            :style="{ background: categoryMap[row.category]?.color }"
          />
          <div class="growth-date">
            <strong>{{ row.occurredAt }}</strong>
            <span>{{ row.recorderName }}</span>
          </div>
          <div class="growth-card">
            <header>
              <ElTag
                :type="categoryMap[row.category]?.type"
                effect="light"
              >
                {{ categoryMap[row.category]?.label }}
              </ElTag>
              <span v-if="!isParent && row.visibleToParent === 0" class="internal-tag">
                仅校内可见
              </span>
            </header>
            <h3>{{ row.title }}</h3>
            <p>{{ row.content }}</p>
            <footer>
              <span>{{ row.gradeName }}{{ row.className }}</span>
              <span v-if="row.attachments?.length">
                {{ row.attachments.length }} 个附件
              </span>
            </footer>
          </div>
        </article>
        <ElEmpty v-if="!loading && rows.length === 0" description="暂无成长记录" />
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

    <ElDrawer v-model="detailVisible" size="650px" :with-header="false">
      <template v-if="selected">
        <div class="detail-heading">
          <div>
            <ElTag :type="categoryMap[selected.category]?.type" effect="plain">
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
          <span>记录时间：{{ formatDate(selected.updatedAt) }}</span>
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
.growth-timeline {
  min-height: 280px;
  padding: 18px 26px;
}

.growth-item {
  position: relative;
  display: grid;
  grid-template-columns: 18px 130px 1fr;
  gap: 14px;
  padding-bottom: 22px;
  cursor: pointer;
}

.growth-item::before {
  position: absolute;
  top: 18px;
  bottom: 0;
  left: 8px;
  width: 1px;
  background: #e2e9f3;
  content: "";
}

.growth-item:last-child::before {
  display: none;
}

.timeline-dot {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  margin: 9px 0 0 3px;
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 2px #dbe6f5;
}

.growth-date {
  padding-top: 5px;
}

.growth-date strong,
.growth-date span {
  display: block;
}

.growth-date strong {
  color: #4e607a;
  font-size: 13px;
}

.growth-date span {
  margin-top: 5px;
  color: #94a0b0;
  font-size: 11px;
}

.growth-card {
  padding: 18px 20px;
  border: 1px solid #e7ecf4;
  border-radius: 14px;
  background: #fff;
  transition: box-shadow 0.2s ease;
}

.growth-card:hover {
  box-shadow: 0 10px 28px rgba(24, 65, 118, 0.09);
}

.growth-card header,
.growth-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.internal-tag {
  color: #9a6a2f;
  font-size: 11px;
}

.growth-card h3 {
  margin: 11px 0 7px;
  color: #20324e;
  font-size: 17px;
}

.growth-card p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #738197;
  font-size: 13px;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.growth-card footer {
  margin-top: 12px;
  color: #96a1b1;
  font-size: 11px;
}

.detail-heading {
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f7;
}

.detail-heading h2 {
  margin: 12px 0 8px;
  color: #172844;
  font-size: 27px;
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
  line-height: 2;
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

