<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { dashboardApi } from '../../api/dashboard'
import DashboardCharts from '../../components/charts/DashboardCharts.vue'
import { useAuthStore } from '../../stores/auth'
import type { AdminDashboard } from '../../types/dashboard'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const dashboard = ref<AdminDashboard | null>(null)

async function loadData() {
  loading.value = true
  try {
    dashboard.value = await dashboardApi.admin()
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
        <p>SCHOOL OVERVIEW</p>
        <h2>{{ auth.user?.realName }}，欢迎查看全校运行概况</h2>
        <span>以下数据全部从学校业务数据库实时聚合。</span>
      </div>
      <div class="hero-badge">
        <strong>{{ dashboard?.classCount ?? 0 }}</strong>
        <span>个启用班级</span>
      </div>
    </section>

    <section class="metric-grid">
      <article>
        <span>在读学生</span>
        <strong>{{ dashboard?.studentCount ?? 0 }}</strong>
        <small>全校学生档案</small>
      </article>
      <article>
        <span>在职教师</span>
        <strong>{{ dashboard?.teacherCount ?? 0 }}</strong>
        <small>可参与班级教学</small>
      </article>
      <article>
        <span>家长用户</span>
        <strong>{{ dashboard?.parentCount ?? 0 }}</strong>
        <small>已建立家长档案</small>
      </article>
      <article>
        <span>已发布通知</span>
        <strong>{{ dashboard?.publishedNoticeCount ?? 0 }}</strong>
        <small>全校、年级和班级</small>
      </article>
      <article class="warning-card">
        <span>待审批请假</span>
        <strong>{{ dashboard?.pendingLeaveCount ?? 0 }}</strong>
        <small>等待班主任处理</small>
      </article>
    </section>

    <DashboardCharts
      mode="admin"
      :primary="dashboard?.studentsByGrade ?? []"
      :secondary="dashboard?.noticesByScope ?? []"
    />

    <section class="quick-panel">
      <div>
        <h3>常用管理入口</h3>
        <p>快速进入人员、通知与学业数据管理。</p>
      </div>
      <div class="quick-actions">
        <ElButton @click="router.push('/admin/users')">用户管理</ElButton>
        <ElButton @click="router.push('/admin/notices')">通知管理</ElButton>
        <ElButton @click="router.push('/admin/exams')">考试管理</ElButton>
        <ElButton @click="router.push('/admin/leaves')">请假记录</ElButton>
      </div>
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
    radial-gradient(circle at 90% 4%, rgba(115, 185, 255, 0.55), transparent 35%),
    linear-gradient(115deg, #0b3678, #1767c9 66%, #3284e7);
  box-shadow: 0 18px 40px rgba(22, 76, 156, 0.18);
}

.dashboard-hero p {
  margin: 0 0 13px;
  color: #c4e1ff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.dashboard-hero h2 {
  margin: 0;
  font-size: 30px;
}

.dashboard-hero span {
  display: block;
  margin-top: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.hero-badge {
  display: grid;
  min-width: 150px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.hero-badge strong {
  font-size: 38px;
}

.hero-badge span {
  margin: 3px 0 0;
  font-size: 12px;
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
.metric-grid strong,
.metric-grid small {
  display: block;
}

.metric-grid span {
  color: #7e8b9f;
  font-size: 12px;
}

.metric-grid strong {
  margin: 7px 0;
  color: #1d5eb9;
  font-size: 31px;
}

.metric-grid small {
  color: #a0a9b7;
  font-size: 11px;
}

.warning-card strong {
  color: #d28927;
}

.quick-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 26px;
  border: 1px solid #e7ecf4;
  border-radius: 15px;
  background: linear-gradient(145deg, #fff, #f5f9ff);
}

.quick-panel h3 {
  margin: 0;
  color: #20324e;
}

.quick-panel p {
  margin: 7px 0 0;
  color: #8a96a8;
  font-size: 12px;
}

.quick-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 1320px) {
  .metric-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>

