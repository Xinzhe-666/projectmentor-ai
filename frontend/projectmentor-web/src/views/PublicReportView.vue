<template>
  <div class="public-report-page">
    <header class="public-report-toolbar no-print">
      <BrandLogo variant="compact" />
      <div>
        <LanguageSwitch />
        <el-button type="primary" :icon="Printer" :disabled="!report" @click="handlePrint">
          {{ t('reportV5.actions.print') }}
        </el-button>
      </div>
    </header>

    <main class="public-report-main">
      <section v-if="loading" class="public-report-loading" aria-live="polite" :aria-label="t('reportV5.states.loading')">
        <el-skeleton :rows="14" animated />
      </section>

      <section v-else-if="error" class="public-report-error" role="alert">
        <span class="error-code">{{ t('reportV5.sharedDocumentType') }}</span>
        <h1>{{ t('reportV5.states.publicErrorTitle') }}</h1>
        <p>{{ t('reportV5.states.publicErrorDescription') }}</p>
        <el-button type="primary" @click="loadPublicReport">{{ t('reportV5.states.publicRetry') }}</el-button>
      </section>

      <template v-else-if="report">
        <p class="public-privacy-notice">
          <StatusLabel status="AVAILABLE" :label="t('reportV5.readOnly')" />
          <span>{{ t('reportV5.public.notice') }}</span>
        </p>
        <AuditReportDocument
          :report="report"
          :project-name="report.projectName || t('common.unnamedProject')"
          :tech-stack="report.techStack || ''"
          :project-type="report.projectType || ''"
          public-mode
        />
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'

import { getPublicReport } from '@/api/share'
import AuditReportDocument from '@/components/AuditReportDocument.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type { PublicReport } from '@/types/api'

const route = useRoute()
const { t } = useI18n()
const token = String(route.params.token || '')
const loading = ref(true)
const error = ref(false)
const report = ref<PublicReport>()

async function loadPublicReport() {
  loading.value = true
  error.value = false
  report.value = undefined

  try {
    report.value = await getPublicReport(token)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function handlePrint() {
  ElMessage.info(t('report.printTip'))
  window.setTimeout(() => window.print(), 100)
}

onMounted(loadPublicReport)
</script>

<style scoped>
.public-report-page {
  min-height: 100vh;
  background: var(--pm-paper);
}

.public-report-toolbar {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 12px max(20px, calc((100vw - 1000px) / 2));
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.public-report-toolbar > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-report-toolbar :deep(.el-button) {
  min-height: 44px;
}

.public-report-main {
  width: 100%;
  padding: 28px 20px 48px;
}

.public-privacy-notice,
.public-report-loading,
.public-report-error {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  background: var(--pm-surface);
}

.public-privacy-notice {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 16px;
  padding: 14px 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.65;
}

.public-report-loading,
.public-report-error {
  padding: 48px 56px;
  border: 1px solid var(--pm-stone-strong);
}

.public-report-loading {
  min-height: 620px;
}

.public-report-error {
  min-height: 360px;
  border-top: 4px solid var(--pm-ink);
}

.error-code {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.public-report-error h1 {
  max-width: 18ch;
  margin: 16px 0 0;
  color: var(--pm-ink);
  font-size: clamp(30px, 5vw, 44px);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 1.1;
}

.public-report-error p {
  max-width: 58ch;
  margin: 14px 0 24px;
  color: var(--pm-muted);
  line-height: 1.7;
}

@media (max-width: 620px) {
  .public-report-toolbar {
    align-items: flex-start;
    padding: 12px 16px;
  }

  .public-report-toolbar :deep(.brand-logo__copy) {
    display: none;
  }

  .public-report-toolbar :deep(.el-button) {
    padding-right: 10px;
    padding-left: 10px;
  }

  .public-report-main {
    padding: 18px 0 32px;
  }

  .public-privacy-notice {
    align-items: flex-start;
    flex-direction: column;
    margin-bottom: 12px;
    padding: 14px 20px;
  }

  .public-report-loading,
  .public-report-error {
    padding: 34px 20px;
    border-right: 0;
    border-left: 0;
  }
}

@media print {
  .public-report-page,
  .public-report-main {
    min-height: auto;
    padding: 0;
    background: #ffffff !important;
  }

  .public-privacy-notice {
    display: none;
  }
}
</style>
