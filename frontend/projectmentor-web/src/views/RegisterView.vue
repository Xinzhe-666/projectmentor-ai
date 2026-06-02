<template>
  <div class="auth-page">
    <section class="auth-panel">
      <div class="auth-topline">
        <RouterLink class="muted" to="/">{{ t('common.backHome') }}</RouterLink>
        <LanguageSwitch />
      </div>
      <h1>{{ t('auth.registerTitle') }}</h1>
      <p>{{ t('auth.registerSubtitle') }}</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item :label="t('common.username')">
          <el-input v-model="form.username" size="large" :placeholder="t('auth.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.email')">
          <el-input v-model="form.email" size="large" :placeholder="t('auth.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.password')">
          <el-input v-model="form.password" size="large" type="password" show-password :placeholder="t('auth.passwordPlaceholder')" />
        </el-form-item>
        <el-button class="full-button" type="primary" size="large" :loading="loading" @click="handleRegister">
          {{ t('auth.registerEnter') }}
        </el-button>
      </el-form>

      <p class="auth-tip">{{ t('auth.hasAccount') }}<RouterLink to="/login">{{ t('auth.goLogin') }}</RouterLink></p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { register } from '@/api/auth'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: ''
})

async function handleRegister() {
  if (!form.username || !form.email || !form.password) {
    ElMessage.warning(t('auth.fillRegister'))
    return
  }

  loading.value = true
  try {
    const result = await register(form)
    userStore.setLoginState(result)
    ElMessage.success(t('auth.registerSuccess'))
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

.auth-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.auth-tip {
  margin-top: 18px;
  text-align: center;
}

.auth-tip a {
  margin-left: 4px;
  color: var(--pm-primary);
  font-weight: 700;
}

@media (max-width: 520px) {
  .auth-topline {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
