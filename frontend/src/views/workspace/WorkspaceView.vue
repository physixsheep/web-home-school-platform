<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '../../stores/auth'
import type { RoleCode } from '../../types/auth'

const route = useRoute()
const auth = useAuthStore()
const refreshing = ref(false)

const roleDetails: Record<RoleCode, { label: string; focus: string }> = {
  ADMIN: {
    label: '学校管理员',
    focus: '核心教学、学生管理和家校互动数据已接入，后续将在此展示实时统计。',
  },
  TEACHER: {
    label: '教师',
    focus: '本班学生、通知、作业、成绩、请假、成长和互动已接入，后续将展示工作台统计。',
  },
  STUDENT: {
    label: '学生',
    focus: '后续将在此接入个人通知、作业、成绩与成长档案。',
  },
  PARENT: {
    label: '家长',
    focus: '后续将在此接入已绑定孩子的学习与成长信息。',
  },
}

const pageTitle = computed(() => route.meta.homeTitle ?? '工作台')
const currentRole = computed(() => auth.primaryRole)
const roleInfo = computed(() =>
  currentRole.value ? roleDetails[currentRole.value] : null,
)
const documentUrl = computed(() => {
  const origin = import.meta.env.VITE_API_ORIGIN ?? ''
  return `${origin}/swagger-ui.html`
})

async function refreshIdentity() {
  refreshing.value = true
  try {
    await auth.loadCurrentUser()
    ElMessage.success('当前身份信息已从后端刷新')
  } finally {
    refreshing.value = false
  }
}
</script>

<template>
  <div class="workspace">
    <section class="hero-card">
      <div>
        <p class="hero-kicker">{{ pageTitle }} · AUTHENTICATED</p>
        <h2>{{ auth.user?.realName }}，欢迎进入家校协同平台</h2>
        <p class="hero-description">
          当前账号已通过 JWT 身份校验。你的角色为
          <strong>{{ roleInfo?.label }}</strong>，页面访问范围由后端权限规则控制。
        </p>
        <p class="hero-focus">{{ roleInfo?.focus }}</p>
      </div>
      <div class="identity-badge">
        <span>ACCESS ROLE</span>
        <strong>{{ currentRole }}</strong>
      </div>
    </section>

    <section class="summary-grid">
      <article class="summary-card">
        <span class="card-number">01</span>
        <h3>身份认证</h3>
        <p>账号状态、角色和 Token 版本均从数据库实时读取。</p>
        <ElTag effect="light" type="success">已连通</ElTag>
      </article>
      <article class="summary-card">
        <span class="card-number">02</span>
        <h3>路由隔离</h3>
        <p>四类角色使用独立首页路径，前端守卫与后端接口同时约束。</p>
        <ElTag effect="light" type="success">已启用</ElTag>
      </article>
      <article class="summary-card">
        <span class="card-number">03</span>
        <h3>安全退出</h3>
        <p>退出时递增 Token 版本，使该账号此前签发的 Token 失效。</p>
        <ElTag effect="light" type="success">已实现</ElTag>
      </article>
    </section>

    <section class="detail-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <p>SESSION</p>
            <h3>当前登录信息</h3>
          </div>
          <ElButton :loading="refreshing" plain @click="refreshIdentity">
            刷新身份
          </ElButton>
        </div>
        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="用户 ID">
            {{ auth.user?.id }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="登录账号">
            {{ auth.user?.username }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="姓名">
            {{ auth.user?.realName }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="角色编码">
            {{ auth.user?.roles.join('、') }}
          </ElDescriptionsItem>
        </ElDescriptions>
      </article>

      <article class="panel next-panel">
        <p>NEXT MODULE</p>
        <h3>平台集成与验收</h3>
        <span>
          核心业务模块已完成。下一阶段将进行统计工作台联调、操作日志、
          权限回归、部署文档和最终验收。
        </span>
        <ElButton tag="a" :href="documentUrl" target="_blank" type="primary">
          查看后端接口文档
        </ElButton>
      </article>
    </section>
  </div>
</template>

<style scoped>
.workspace {
  display: grid;
  gap: 22px;
}

.hero-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 240px;
  overflow: hidden;
  padding: 38px 42px;
  border-radius: 18px;
  color: #fff;
  background:
    radial-gradient(circle at 92% 4%, rgba(106, 178, 255, 0.55), transparent 34%),
    linear-gradient(112deg, #0c3679, #1766c8 64%, #3284e7);
  box-shadow: 0 20px 44px rgba(18, 71, 148, 0.18);
}

.hero-card::after {
  position: absolute;
  right: 110px;
  bottom: -170px;
  width: 360px;
  height: 360px;
  border: 50px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: "";
}

.hero-card > div {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  margin: 0 0 18px;
  color: #c7e2ff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.hero-card h2 {
  margin: 0;
  font-size: 31px;
  line-height: 1.35;
}

.hero-description,
.hero-focus {
  max-width: 680px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.8;
}

.hero-description {
  margin: 18px 0 0;
}

.hero-focus {
  margin: 9px 0 0;
  font-size: 13px;
}

.identity-badge {
  align-self: center;
  min-width: 150px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.19);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
}

.identity-badge span,
.identity-badge strong {
  display: block;
}

.identity-badge span {
  color: rgba(255, 255, 255, 0.58);
  font-size: 10px;
  letter-spacing: 0.15em;
}

.identity-badge strong {
  margin-top: 9px;
  font-size: 22px;
  letter-spacing: 0.06em;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.summary-card,
.panel {
  border: 1px solid #e7ecf4;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(22, 53, 97, 0.05);
}

.summary-card {
  position: relative;
  padding: 26px 28px;
}

.card-number {
  position: absolute;
  top: 22px;
  right: 24px;
  color: #dbe5f3;
  font-size: 28px;
  font-weight: 800;
}

.summary-card h3,
.panel h3 {
  margin: 0;
  color: #1a2c4a;
}

.summary-card p {
  min-height: 46px;
  margin: 13px 0 20px;
  color: #7b8799;
  font-size: 13px;
  line-height: 1.75;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr);
  gap: 20px;
}

.panel {
  padding: 28px;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

.panel-heading p,
.next-panel > p {
  margin: 0 0 7px;
  color: #4177c5;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.next-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  background: linear-gradient(150deg, #f8fbff, #edf5ff);
}

.next-panel span {
  margin: 14px 0 24px;
  color: #6e7e95;
  font-size: 13px;
  line-height: 1.85;
}

.next-panel .el-button {
  margin-top: auto;
}
</style>

