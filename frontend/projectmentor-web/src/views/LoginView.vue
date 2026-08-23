<template>
  <main class="auth-page">
    <section class="auth-product-panel" aria-labelledby="auth-value-title">
      <BrandLogo variant="primary" tone="inverted" />
      <div class="auth-product-copy">
        <h1 id="auth-value-title">{{ t('auth.valueTitle') }}</h1>
        <p>{{ t('auth.valueDesc') }}</p>
      </div>
      <ol class="auth-highlight-list">
        <li v-for="(item, index) in authHighlights" :key="item">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
          <p>{{ item }}</p>
        </li>
      </ol>
      <span class="auth-release">{{ t('common.beta') }}</span>
    </section>

    <section class="auth-panel" aria-labelledby="auth-login-title">
      <div class="auth-topline">
        <RouterLink class="muted" to="/">{{ t('common.backHome') }}</RouterLink>
        <LanguageSwitch />
      </div>
      <div class="auth-form-content">
        <header class="auth-form-header">
          <h2 id="auth-login-title">{{ t('auth.loginTitle') }}</h2>
          <p>{{ t('auth.loginSubtitle') }}</p>
        </header>

        <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
          <el-form-item :label="t('common.username')">
            <el-input v-model="form.username" size="large" autocomplete="username" :placeholder="t('auth.usernamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('common.password')">
            <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" show-password :placeholder="t('auth.passwordPlaceholder')" />
          </el-form-item>
          <el-button class="full-button" type="primary" size="large" native-type="submit" :loading="loading">
            {{ t('common.login') }}
          </el-button>
        </el-form>

        <p class="auth-tip">{{ t('auth.noAccount') }}<RouterLink to="/register">{{ t('auth.registerNow') }}</RouterLink></p>
        <p class="auth-beta-note">{{ t('auth.betaNotice') }}</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { login } from '@/api/auth'
import BrandLogo from '@/components/BrandLogo.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
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
