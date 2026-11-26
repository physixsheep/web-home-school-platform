<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { dashboardApi } from '../../api/dashboard'
import DashboardCharts from '../../components/charts/DashboardCharts.vue'
import { useAuthStore } from '../../stores/auth'
import type { NameValue, TeacherDashboard } from '../../types/dashboard'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const dashboard = ref<TeacherDashboard | null>(null)

const pendingItems = computed<NameValue[]>(() => [
  { name: '待批改作业', value: dashboard.value?.pendingGradingCount ?? 0 },
  { name: '待审批请假', value: dashboard.value?.pendingLeaveCount ?? 0 },
])

async function loadData() {
  loading.value = true
  try {
    dashboard.value = await dashboardApi.teacher()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="dashboard-hero">
      <div>
        <p>TEACHING WORKSPACE</p>
        <h2>{{ auth.user?.realName }}，今天有 {{ (dashboard?.pendingGradingCount || 0) + (dashboard?.pendingLeaveCount || 0) }} 项待处理事务</h2>
        <span>作业提交率、批改和请假审批数据实时更新。</span>
      </div>
      <div class="hero-progress">
        <span>作业提交率</span>
        <strong>{{ Number(dashboard?.submissionRate || 0).toFixed(2) }}%</strong>
        <ElProgress
          :percentage="Math.min(100, Number(dashboard?.submissionRate || 0))"
          :show-text="false"
          :stroke-width="9"
        />
      </div>
    </section>

    <section class="metric-grid">
      <article>
        <span>管理班级</span>
        <strong>{{ dashboard?.classCount ?? 0 }}</strong>
      </article>
      <article>
        <span>本班学生</span>
        <strong>{{ dashboard?.studentCount ?? 0 }}</strong>
      </article>
      <article>
        <span>已发布作业</span>
        <strong>{{ dashboard?.publishedAssignmentCount ?? 0 }}</strong>
      </article>
      <article class="warning">
        <span>待批改作业</span>
        <strong>{{ dashboard?.pendingGradingCount ?? 0 }}</strong>
      </article>
      <article class="warning">
        <span>待审批请假</span>
        <strong>{{ dashboard?.pendingLeaveCount ?? 0 }}</strong>
      </article>
    </section>

    <DashboardCharts
      mode="teacher"
      :primary="dashboard?.classes ?? []"
      :secondary="pendingItems"
    />

    <section class="class-panel">
      <div class="panel-heading">
        <div>
          <h3>我的班级概况</h3>
          <p>按有效教师班级关联实时统计。</p>
        </div>
        <div class="quick-actions">
          <ElButton @click="router.push('/teacher/assignments')">作业管理</ElButton>
          <ElButton @click="router.push('/teacher/leaves')">请假审批</ElButton>
          <ElButton @click="router.push('/teacher/scores')">成绩管理</ElButton>
        </div>
      </div>
      <ElTable :data="dashboard?.classes ?? []" empty-text="暂无有效班级关联">
        <ElTableColumn label="班级" min-width="160">
          <template #default="{ row }">{{ row.gradeName }}{{ row.className }}</template>
        </ElTableColumn>
        <ElTableColumn prop="studentCount" label="学生人数" width="120" />
        <ElTableColumn prop="assignmentCount" label="已发布作业" width="130" />
        <ElTableColumn prop="submittedCount" label="累计提交" width="120" />
      </ElTable>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 20px;
}

.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 200px;
  padding: 34px 40px;
  border-radius: 18px;
  color: #fff;
  background:
    radial-gradient(circle at 90% 5%, rgba(116, 188, 255, 0.5), transparent 35%),
    linear-gradient(115deg, #0d397f, #1e6bce 68%, #3988e6);
  box-shadow: 0 18px 38px rgba(23, 80, 157, 0.18);
}

.dashboard-hero p {
  margin: 0 0 13px;
  color: #c5e2ff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.dashboard-hero h2 {
  max-width: 720px;
  margin: 0;
  font-size: 28px;
}

.dashboard-hero > div > span {
  display: block;
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.68);
}

.hero-progress {
  width: 230px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-progress span,
.hero-progress strong {
  display: block;
}

.hero-progress span {
  margin: 0;
  color: rgba(255, 255, 255, 0.65);
  font-size: 11px;
}

.hero-progress strong {
  margin: 7px 0 10px;
  font-size: 30px;
}

:deep(.hero-progress .el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.16);
}

:deep(.hero-progress .el-progress-bar__inner) {
  background: #fff;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}

.metric-grid article {
  padding: 22px;
  border: 1px solid #e7ecf4;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(22, 53, 97, 0.05);
}

.metric-grid span,
.metric-grid strong {
  display: block;
}

.metric-grid span {
  color: #7e8b9f;
  font-size: 12px;
}

.metric-grid strong {
  margin-top: 8px;
  color: #1d5eb9;
  font-size: 31px;
}

.metric-grid .warning strong {
  color: #d28927;
}

.class-panel {
  overflow: hidden;
  border: 1px solid #e7ecf4;
  border-radius: 15px;
  background: #fff;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px;
  border-bottom: 1px solid #edf1f7;
}

.panel-heading h3 {
  margin: 0;
  color: #20324e;
}

.panel-heading p {
  margin: 6px 0 0;
  color: #8a96a8;
  font-size: 12px;
}

.quick-actions {
  display: flex;
  gap: 10px;
}
</style>

