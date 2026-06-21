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
      <h1>{{ t('auth.registerTitle') }}</h1>
      <p>{{ t('auth.registerSubtitle') }}</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item :label="t('common.username')">
          <el-input v-model="form.username" size="large" :placeholder="t('auth.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.email')">
          <el-input v-model="form.email" size="large" :placeholder="t('auth.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('auth.verificationCode')">
          <div class="email-code-row">
            <el-input v-model="form.verificationCode" size="large" :placeholder="t('auth.verificationCodePlaceholder')" />
            <el-button
              size="large"
              :loading="sendingCode"
              :disabled="codeCountdown > 0"
              @click="handleSendEmailCode"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : t('auth.sendVerificationCode') }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item :label="t('common.password')">
          <el-input v-model="form.password" size="large" type="password" show-password :placeholder="t('auth.passwordPlaceholder')" />
        </el-form-item>
        <el-button class="full-button" type="primary" size="large" :loading="loading" @click="handleRegister">
          {{ t('auth.registerEnter') }}
        </el-button>
      </el-form>

      <p class="auth-tip">{{ t('auth.hasAccount') }}<RouterLink to="/login">{{ t('auth.goLogin') }}</RouterLink></p>
      <p class="auth-beta-note">{{ t('auth.betaNotice') }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { register, sendRegisterEmailCode } from '@/api/auth'
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
  if (!form.email) {
    ElMessage.warning(t('auth.fillEmailFirst'))
    return
  }

  sendingCode.value = true
  try {
    await sendRegisterEmailCode({ email: form.email })
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
  if (!form.username || !form.email || !form.password || !form.verificationCode) {
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

onBeforeUnmount(() => {
  if (codeTimer) {
    clearInterval(codeTimer)
  }
})
</script>

<style scoped>
.full-button {
  width: 100%;
}

.email-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 10px;
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

  .email-code-row {
    grid-template-columns: 1fr;
  }
}
</style>
