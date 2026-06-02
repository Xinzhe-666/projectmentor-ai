<template>
  <div class="public-report-page" v-loading="loading">
    <header class="public-report-header">
      <div class="public-heading">
        <span class="brand-mark">PM</span>
        <div>
          <p class="eyebrow">ProjectMentor AI</p>
          <h1>{{ t('publicReport.title') }}</h1>
          <p>{{ t('publicReport.subtitle') }}</p>
        </div>
      </div>
      <div class="public-header-actions">
        <LanguageSwitch />
        <el-tag effect="light" type="info">{{ t('common.readOnly') }}</el-tag>
      </div>
    </header>

    <main class="public-report-container">
      <section class="public-notice">
        {{ t('publicReport.notice') }}
      </section>

      <template v-if="report">
        <section class="panel public-hero-panel">
          <div class="panel-body public-hero">
            <div class="public-summary">
              <p class="eyebrow">{{ t('publicReport.eyebrow') }}</p>
              <h2>{{ report.projectName || t('common.unnamedProject') }}</h2>
              <div class="public-meta-grid">
                <span>
                  {{ t('common.projectName') }}
                  <strong>{{ report.projectName || t('common.unnamedProject') }}</strong>
                </span>
                <span>
                  {{ t('common.techStack') }}
                  <strong>{{ report.techStack || t('common.notFilled') }}</strong>
                </span>
                <span>
                  {{ t('common.projectType') }}
                  <strong>{{ report.projectType || t('common.notFilled') }}</strong>
                </span>
                <span>
                  {{ t('common.createTime') }}
                  <strong>{{ report.createTime || '-' }}</strong>
                </span>
              </div>
              <p class="public-summary-text">{{ report.summary || t('common.noData') }}</p>
            </div>
            <div class="public-score-card">
              <ScoreRing :score="report.totalScore ?? 0" :title="t('common.totalScore')" />
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <h3>{{ t('publicReport.scoreTitle') }}</h3>
          </div>
          <div class="panel-body public-score-layout">
            <RadarScoreChart :scores="radarScores" />
            <div class="score-grid">
              <article v-for="item in scoreRows" :key="item.label" class="metric-card">
                <span>{{ item.label }}</span>
                <strong>{{ formatScore(item.value) }}</strong>
              </article>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('report.contentTitle') }}</h3>
              <p class="muted">{{ t('publicReport.contentDesc') }}</p>
            </div>
          </div>
          <div class="panel-body public-content-grid">
            <article>
              <h4>{{ t('report.strengths') }}</h4>
              <MarkdownBlock :content="report.strengths" />
            </article>
            <article>
              <h4>{{ t('report.weaknesses') }}</h4>
              <MarkdownBlock :content="report.weaknesses" />
            </article>
            <article>
              <h4>{{ t('common.suggestions') }}</h4>
              <MarkdownBlock :content="report.suggestions" />
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('common.risks') }}</h3>
              <p class="muted">{{ t('publicReport.risksDesc') }}</p>
            </div>
          </div>
          <div class="panel-body">
            <RiskList :risks="report.riskPoints" />
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('common.evidence') }}</h3>
              <p class="muted">{{ t('publicReport.evidenceDesc') }}</p>
            </div>
          </div>
          <div class="panel-body">
            <EvidenceList :evidences="report.evidenceChain" />
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('publicReport.resumeTitle') }}</h3>
              <p class="muted">{{ t('publicReport.resumeDesc') }}</p>
            </div>
          </div>
          <div class="panel-body public-resume-grid">
            <article>
              <h4>{{ t('report.resumeBasic') }}</h4>
              <MarkdownBlock :content="report.resumeBasic" />
            </article>
            <article>
              <h4>{{ t('report.resumeStandard') }}</h4>
              <MarkdownBlock :content="report.resumeStandard" />
            </article>
            <article>
              <h4>{{ t('report.resumeAdvanced') }}</h4>
              <MarkdownBlock :content="report.resumeAdvanced" />
            </article>
          </div>
        </section>

        <footer class="public-footer">
          {{ t('publicReport.footer') }}
        </footer>
      </template>

      <section v-else-if="errorMessage" class="panel">
        <el-result
          icon="warning"
          :title="t('publicReport.unavailable')"
          :sub-title="errorMessage"
        />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

import { getPublicReport } from '@/api/share'
import EvidenceList from '@/components/EvidenceList.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import RadarScoreChart from '@/components/RadarScoreChart.vue'
import RiskList from '@/components/RiskList.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import type { PublicReport } from '@/types/api'

const route = useRoute()
const { t } = useI18n()
const token = String(route.params.token || '')
const loading = ref(false)
const report = ref<PublicReport>()
const errorMessage = ref('')

const radarScores = computed(() => ({
  runnabilityScore: report.value?.runnabilityScore,
  authenticityScore: report.value?.authenticityScore,
  structureScore: report.value?.structureScore,
  readmeScore: report.value?.readmeScore,
  securityScore: report.value?.securityScore,
  engineeringScore: report.value?.engineeringScore,
  interviewScore: report.value?.interviewScore
}))

const scoreRows = computed(() => [
  { label: t('report.scores.runnability'), value: report.value?.runnabilityScore },
  { label: t('report.scores.authenticity'), value: report.value?.authenticityScore },
  { label: t('report.scores.structure'), value: report.value?.structureScore },
  { label: t('report.scores.readme'), value: report.value?.readmeScore },
  { label: t('report.scores.security'), value: report.value?.securityScore },
  { label: t('report.scores.engineering'), value: report.value?.engineeringScore },
  { label: t('report.scores.interview'), value: report.value?.interviewScore }
])

function formatScore(value?: number) {
  if (!Number.isFinite(value)) {
    return '-'
  }

  return `${Math.round(Number(value))} ${t('common.points')}`
}

async function loadPublicReport() {
  loading.value = true
  errorMessage.value = ''

  try {
    report.value = await getPublicReport(token)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('publicReport.unavailableMessage')
  } finally {
    loading.value = false
  }
}

onMounted(loadPublicReport)
</script>

<style scoped>
.public-report-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f7fb 48%, #eef3f9 100%);
}

.public-report-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px max(20px, calc((100vw - 1180px) / 2 + 30px));
  border-bottom: 1px solid rgba(223, 230, 240, 0.86);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
}

.public-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.public-heading {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.public-heading h1 {
  margin: 4px 0 6px;
  color: var(--pm-ink);
  font-size: 24px;
  line-height: 1.25;
}

.public-heading p:not(.eyebrow) {
  margin: 0;
  color: var(--pm-muted);
  line-height: 1.6;
}

.public-report-container {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 30px 44px;
}

.public-notice {
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(31, 111, 235, 0.18);
  border-radius: 8px;
  background: #eef6ff;
  color: #245089;
  line-height: 1.7;
}

.public-hero-panel {
  overflow: hidden;
}

.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
}

.public-summary h2 {
  margin: 8px 0 10px;
  color: var(--pm-ink);
  font-size: 30px;
  line-height: 1.25;
}

.public-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.public-meta-grid span {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #fbfdff;
  color: var(--pm-muted);
  font-size: 12px;
}

.public-meta-grid strong {
  display: block;
  margin-top: 5px;
  color: #344054;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.public-summary-text {
  margin: 16px 0 0;
  padding: 12px 14px;
  border-left: 3px solid var(--pm-primary);
  background: #f8fbff;
  color: #344054;
  line-height: 1.85;
}

.public-score-card {
  padding: 12px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #ffffff;
}

.public-score-layout {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) minmax(320px, 1fr);
  gap: 18px;
  align-items: start;
}

.public-content-grid,
.public-resume-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.public-content-grid article,
.public-resume-grid article {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.public-content-grid h4,
.public-resume-grid h4 {
  margin: 0 0 12px;
}

.public-footer {
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  color: #475467;
  line-height: 1.7;
  text-align: center;
}

@media (max-width: 920px) {
  .public-score-layout,
  .public-meta-grid,
  .public-content-grid,
  .public-resume-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .public-report-header {
    align-items: flex-start;
    flex-direction: column;
    padding: 16px;
  }

  .public-header-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .public-report-container {
    padding: 18px 16px 32px;
  }

  .public-hero {
    grid-template-columns: 1fr;
  }

  .public-summary h2,
  .public-heading h1 {
    font-size: 24px;
  }
}
</style>
