<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function goBack() {
  void router.replace(auth.token ? auth.roleHome : '/login')
}
</script>

<template>
  <main class="not-found">
    <div class="error-code">404</div>
    <h1>页面没有找到</h1>
    <p>你访问的地址不存在，或当前链接已经失效。</p>
    <ElButton type="primary" @click="goBack">返回首页</ElButton>
  </main>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  place-content: center;
  justify-items: center;
  background:
    radial-gradient(circle at 50% 22%, #e3efff, transparent 34%),
    #f7faff;
}

.error-code {
  color: #1d62c2;
  font-size: 112px;
  font-weight: 800;
  letter-spacing: -0.08em;
}

h1 {
  margin: -8px 0 0;
  color: #172844;
}

p {
  margin: 14px 0 28px;
  color: #7d899a;
}
</style>

