<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 50, message: '账号长度不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度必须为 8 到 72 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    await router.replace(auth.roleHome)
  } catch {
    form.password = ''
  }
}
</script>

<template>
  <main class="login-page">
    <section class="brand-panel">
      <div class="brand-grid" />
      <div class="brand-content">
        <p class="eyebrow">HOME · SCHOOL · GROWTH</p>
        <h1>让学校的每一次关注<br />都能抵达家庭</h1>
        <p class="brand-description">
          面向高中学校的管理者、教师、学生与家长，统一承载通知、学业、
          请假、成长档案与家校沟通。
        </p>
        <div class="feature-row">
          <span>统一身份</span>
          <span>数据隔离</span>
          <span>成长留痕</span>
        </div>
      </div>
      <div class="brand-orbit orbit-one" />
      <div class="brand-orbit orbit-two" />
    </section>

    <section class="form-panel">
      <div class="form-shell">
        <div class="mobile-brand">家校协同智慧互通平台</div>
        <p class="form-kicker">欢迎回来</p>
        <h2>登录校园协同平台</h2>
        <p class="form-hint">请使用学校分配的账号登录</p>

        <ElForm
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="submit"
        >
          <ElFormItem label="账号" prop="username">
            <ElInput
              v-model="form.username"
              autocomplete="username"
              clearable
              maxlength="50"
              placeholder="请输入账号"
            />
          </ElFormItem>
          <ElFormItem label="密码" prop="password">
            <ElInput
              v-model="form.password"
              autocomplete="current-password"
              maxlength="72"
              placeholder="请输入密码"
              show-password
              type="password"
            />
          </ElFormItem>
          <ElButton
            class="submit-button"
            :loading="auth.loading"
            type="primary"
            @click="submit"
          >
            登录
          </ElButton>
        </ElForm>

        <div class="security-note">
          <span class="security-dot" />
          登录凭据通过加密连接提交，账号状态与角色由后端实时校验。
        </div>
      </div>
      <footer>高中家校协同智慧互通平台 · 基础框架阶段</footer>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(520px, 1.18fr) minmax(480px, 0.82fr);
  min-height: 100vh;
  background: #f8faff;
}

.brand-panel {
  position: relative;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 76px 9vw;
  color: #fff;
  background:
    radial-gradient(circle at 18% 14%, rgba(111, 173, 255, 0.5), transparent 30%),
    linear-gradient(145deg, #0a2f70 0%, #1157bd 55%, #2c7be5 100%);
}

.brand-grid {
  position: absolute;
  inset: 0;
  opacity: 0.14;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.24) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.24) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(to bottom, #000, transparent 88%);
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 720px;
}

.eyebrow {
  margin: 0 0 28px;
  color: #b9d8ff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.2em;
}

h1 {
  margin: 0;
  font-size: clamp(42px, 4.3vw, 70px);
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.04em;
}

.brand-description {
  max-width: 620px;
  margin: 34px 0 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 17px;
  line-height: 1.9;
}

.feature-row {
  display: flex;
  gap: 14px;
  margin-top: 54px;
}

.feature-row span {
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  font-size: 14px;
  backdrop-filter: blur(8px);
}

.brand-orbit {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}

.orbit-one {
  right: -190px;
  bottom: -250px;
  width: 620px;
  height: 620px;
}

.orbit-two {
  right: -40px;
  bottom: -90px;
  width: 300px;
  height: 300px;
  border-width: 42px;
  opacity: 0.3;
}

.form-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 8vw;
}

.form-shell {
  width: 100%;
  max-width: 430px;
}

.mobile-brand {
  display: none;
}

.form-kicker {
  margin: 0 0 12px;
  color: #2467c4;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

h2 {
  margin: 0;
  color: #102344;
  font-size: 34px;
  line-height: 1.3;
}

.form-hint {
  margin: 12px 0 38px;
  color: #7a879c;
  font-size: 15px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-form-item__label) {
  color: #293b5c;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #dbe3ef inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2467c4 inset;
}

.submit-button {
  width: 100%;
  min-height: 50px;
  margin-top: 8px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(105deg, #1554b2, #2d7ee8);
  box-shadow: 0 12px 24px rgba(36, 103, 196, 0.24);
  font-size: 16px;
  font-weight: 700;
}

.security-note {
  display: flex;
  gap: 10px;
  margin-top: 36px;
  color: #7c899d;
  font-size: 13px;
  line-height: 1.7;
}

.security-dot {
  flex: 0 0 auto;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: #32a071;
  box-shadow: 0 0 0 5px rgba(50, 160, 113, 0.1);
}

footer {
  position: absolute;
  bottom: 28px;
  color: #9aa5b6;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .mobile-brand {
    display: block;
    margin-bottom: 54px;
    color: #1554b2;
    font-weight: 700;
  }
}
</style>

