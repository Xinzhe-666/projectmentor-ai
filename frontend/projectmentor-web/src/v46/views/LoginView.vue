<template>
  <div class="auth-page pm-aurora-bg">
    <div class="pm-grid-overlay" />
    <div class="pm-noise-overlay" />
    <section class="auth-product-panel pm-fade-up is-visible">
      <div class="auth-product-badge">ProjectMentor AI · {{ t('common.beta') }}</div>
      <h1>{{ t('auth.valueTitle') }}</h1>
      <p>{{ t('auth.valueDesc') }}</p>
      <div class="auth-highlight-list">
        <span v-for="item in authHighlights" :key="item">{{ item }}</span>
      </div>
    </section>

    <section class="auth-panel pm-premium-card pm-gradient-border pm-fade-up is-visible">
      <div class="auth-topline">
        <RouterLink class="muted" to="/">{{ t('common.backHome') }}</RouterLink>
        <LanguageSwitch />
      </div>
      <h1>{{ t('auth.loginTitle') }}</h1>
      <p>{{ t('auth.loginSubtitle') }}</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item :label="t('common.username')">
          <el-input v-model="form.username" size="large" :placeholder="t('auth.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.password')">
          <el-input v-model="form.password" size="large" type="password" show-password :placeholder="t('auth.passwordPlaceholder')" />
        </el-form-item>
        <el-button class="full-button" type="primary" size="large" :loading="loading" @click="handleLogin">
          {{ t('common.login') }}
        </el-button>
      </el-form>

      <p class="auth-tip">{{ t('auth.noAccount') }}<RouterLink to="/register">{{ t('auth.registerNow') }}</RouterLink></p>
      <p class="auth-beta-note">{{ t('auth.betaNotice') }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { login } from '@/v46/api/auth'
import LanguageSwitch from '@/v46/components/LanguageSwitch.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const authHighlights = computed(() => [
  t('auth.highlights.evidence'),
  t('auth.highlights.qa'),
  t('auth.highlights.report')
])

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning(t('auth.fillLogin'))
    return
  }

  loading.value = true
  try {
    const result = await login(form)
    userStore.setLoginState(result)
    ElMessage.success(t('auth.loginSuccess'))
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
