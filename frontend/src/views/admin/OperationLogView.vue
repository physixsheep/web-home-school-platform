<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { auditApi } from '../../api/audit'
import type { OperationLogRow } from '../../types/audit'

const rows = ref<OperationLogRow[]>([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({
  keyword: '',
  module: '',
  resultCode: undefined as number | undefined,
  page: 1,
  size: 10,
})

const modules = [
  '用户管理',
  '年级管理',
  '班级管理',
  '考试管理',
  '学科管理',
  '通知管理',
  '作业管理',
  '成绩管理',
  '请假审批',
  '成长档案',
  '家校互动',
  '附件管理',
  '身份认证',
  '系统操作',
]

async function loadData() {
  loading.value = true
  try {
    const result = await auditApi.logs({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      module: query.module || undefined,
      resultCode: query.resultCode,
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

function resetFilters() {
  query.keyword = ''
  query.module = ''
  query.resultCode = undefined
  dateRange.value = null
  search()
}

function resultType(code: number) {
  if (code < 300) return 'success'
  if (code < 400) return 'warning'
  return 'danger'
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadData)
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>操作日志</h2>
        <p>记录已登录用户新增、修改、删除和重要业务动作，不保存请求正文或密码。</p>
      </div>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="用户、模块、操作或路径"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.module" clearable placeholder="全部模块">
          <ElOption v-for="module in modules" :key="module" :label="module" :value="module" />
        </ElSelect>
        <ElSelect v-model="query.resultCode" clearable placeholder="全部结果">
          <ElOption label="成功 2xx" :value="200" />
          <ElOption label="参数错误 400" :value="400" />
          <ElOption label="未授权 401" :value="401" />
          <ElOption label="无权限 403" :value="403" />
          <ElOption label="冲突 409" :value="409" />
          <ElOption label="服务器错误 500" :value="500" />
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
        <ElTable v-loading="loading" :data="rows" empty-text="暂无操作日志" stripe>
          <ElTableColumn label="时间" width="180">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </ElTableColumn>
          <ElTableColumn label="操作者" width="140">
            <template #default="{ row }">
              <strong>{{ row.username || '未知用户' }}</strong>
              <div class="muted-text">{{ row.roles || '—' }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="module" label="模块" width="110" />
          <ElTableColumn prop="operation" label="操作" width="110" />
          <ElTableColumn label="请求" min-width="300">
            <template #default="{ row }">
              <code>{{ row.httpMethod }} {{ row.requestUri }}</code>
              <div v-if="row.requestParams" class="params">{{ row.requestParams }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="结果" width="90">
            <template #default="{ row }">
              <ElTag :type="resultType(row.resultCode)" effect="light">
                {{ row.resultCode }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="耗时" width="90">
            <template #default="{ row }">{{ row.durationMs }} ms</template>
          </ElTableColumn>
          <ElTableColumn prop="clientIp" label="IP" width="130" />
          <ElTableColumn prop="userAgent" label="客户端" min-width="220" show-overflow-tooltip />
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
  </section>
</template>

<style scoped>
code {
  color: #315783;
  font-size: 12px;
}

.params {
  overflow: hidden;
  margin-top: 5px;
  color: #8a96a8;
  font-family: Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

