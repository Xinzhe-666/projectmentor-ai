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

    <section class="auth-panel" aria-labelledby="auth-register-title">
      <div class="auth-topline">
        <RouterLink class="muted" to="/">{{ t('common.backHome') }}</RouterLink>
        <LanguageSwitch />
      </div>
      <div class="auth-form-content">
        <header class="auth-form-header">
          <h2 id="auth-register-title">{{ t('auth.registerTitle') }}</h2>
          <p>{{ t('auth.registerSubtitle') }}</p>
        </header>

        <el-form :model="form" label-position="top" @submit.prevent="handleRegister">
          <el-form-item :label="t('common.username')">
            <el-input v-model="form.username" size="large" autocomplete="username" :placeholder="t('auth.usernamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('common.email')">
            <el-input v-model="form.email" size="large" autocomplete="email" :placeholder="t('auth.emailPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('auth.verificationCode')">
            <div class="email-code-row">
              <el-input
                v-model="form.verificationCode"
                size="large"
                maxlength="6"
                inputmode="numeric"
                autocomplete="one-time-code"
                :placeholder="t('auth.verificationCodePlaceholder')"
              />
              <el-button
                class="email-code-button"
                type="primary"
                plain
                size="large"
                native-type="button"
                :loading="sendingCode"
                :disabled="sendingCode || codeCountdown > 0"
                @click="handleSendEmailCode"
              >
                {{ codeCountdown > 0 ? t('auth.codeCountdown', { seconds: codeCountdown }) : t('auth.sendCode') }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item :label="t('common.password')">
            <el-input v-model="form.password" size="large" type="password" autocomplete="new-password" show-password :placeholder="t('auth.passwordPlaceholder')" />
          </el-form-item>
          <el-button class="full-button" type="primary" size="large" native-type="submit" :loading="loading">
            {{ t('auth.registerEnter') }}
          </el-button>
        </el-form>

        <p class="auth-tip">{{ t('auth.hasAccount') }}<RouterLink to="/login">{{ t('auth.goLogin') }}</RouterLink></p>
        <p class="auth-beta-note">{{ t('auth.betaNotice') }}</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { register, sendRegisterEmailCode } from '@/api/auth'
import BrandLogo from '@/components/BrandLogo.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
let codeTimer: ReturnType<typeof setInterval> | undefined

const form = reactive({
  username: '',
  email: '',
  password: '',
  verificationCode: ''
})

const authHighlights = computed(() => [
  t('auth.highlights.evidence'),
  t('auth.highlights.qa'),
  t('auth.highlights.report')
])

async function handleSendEmailCode() {
  if (codeCountdown.value > 0) {
    return
  }

  if (!form.email.trim()) {
    ElMessage.warning(t('auth.emailRequiredBeforeCode'))
    return
  }

  sendingCode.value = true
  try {
    await sendRegisterEmailCode({ email: form.email.trim() })
    ElMessage.success(t('auth.codeSent'))
    startCodeCountdown()
  } finally {
    sendingCode.value = false
  }
}

function startCodeCountdown() {
  codeCountdown.value = 60
  if (codeTimer) {
    clearInterval(codeTimer)
  }
  codeTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0 && codeTimer) {
      clearInterval(codeTimer)
      codeTimer = undefined
    }
  }, 1000)
}

async function handleRegister() {
  if (!form.username.trim() || !form.email.trim() || !form.password) {
    ElMessage.warning(t('auth.fillRegister'))
    return
  }

  if (!form.verificationCode.trim()) {
    ElMessage.warning(t('auth.verificationCodeRequired'))
    return
  }

  loading.value = true
  try {
    const result = await register({
      ...form,
      email: form.email.trim(),
      verificationCode: form.verificationCode.trim()
    })
    userStore.setLoginState(result)
    ElMessage.success(t('auth.registerSuccess'))
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (codeTimer) {
    clearInterval(codeTimer)
  }
})
</script>
