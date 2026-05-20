<template>
  <div class="auth-page">
    <section class="auth-panel">
      <RouterLink class="muted" to="/">返回首页</RouterLink>
      <h1>登录 ProjectMentor</h1>
      <p>继续审计你的项目可信度和面试风险。</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button class="full-button" type="primary" size="large" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <p class="auth-tip">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const result = await login(form)
    userStore.setLoginState(result)
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/dashboard'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.full-button {
  width: 100%;
}

.auth-tip {
  margin-top: 18px;
  text-align: center;
}

.auth-tip a {
  color: var(--pm-primary);
  font-weight: 700;
}
</style>
