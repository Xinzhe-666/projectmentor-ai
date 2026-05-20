<template>
  <div class="auth-page">
    <section class="auth-panel">
      <RouterLink class="muted" to="/">返回首页</RouterLink>
      <h1>创建账号</h1>
      <p>用一次项目上传，换一轮更真实的面试预演。</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" size="large" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button class="full-button" type="primary" size="large" :loading="loading" @click="handleRegister">
          注册并进入
        </el-button>
      </el-form>

      <p class="auth-tip">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: ''
})

async function handleRegister() {
  if (!form.username || !form.email || !form.password) {
    ElMessage.warning('请完整填写注册信息')
    return
  }

  loading.value = true
  try {
    const result = await register(form)
    userStore.setLoginState(result)
    ElMessage.success('注册成功')
    router.push('/dashboard')
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
