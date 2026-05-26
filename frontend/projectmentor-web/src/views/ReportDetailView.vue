<template>
  <div class="page-stack report-print-root" v-loading="loading">
    <section class="panel">
      <div class="panel-body report-hero">
        <ScoreRing :score="report?.totalScore ?? 0" title="项目总分" />
        <div class="report-summary">
          <div class="report-summary-top">
            <p class="eyebrow">Audit Report #{{ report?.id || reportId }}</p>
            <el-button class="report-print-button no-print" type="primary" :icon="Printer" @click="handlePrint">
              打印 / 保存为 PDF
            </el-button>
          </div>
          <h2>{{ report?.summary || '报告摘要生成中' }}</h2>
          <p class="muted">项目 ID：{{ report?.projectId || '-' }} · 生成时间：{{ report?.createTime || '-' }}</p>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>评分雷达图</h3>
          <p class="muted">从可运行性、真实性、结构、README、安全、工程化和面试价值七个维度观察项目。</p>
        </div>
      </div>
      <div class="panel-body">
        <RadarScoreChart :scores="radarScores" />
        <div class="print-score-list print-only">
          <article v-for="item in scoreRows" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ formatScore(item.value) }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>报告内容</h3>
      </div>
      <div class="panel-body report-content-grid">
        <article>
          <h4>优势</h4>
          <MarkdownBlock :content="report?.strengths" />
        </article>
        <article>
          <h4>短板</h4>
          <MarkdownBlock :content="report?.weaknesses" />
        </article>
        <article>
          <h4>建议</h4>
          <MarkdownBlock :content="report?.suggestions" />
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>风险点</h3>
      </div>
      <div class="panel-body">
        <RiskList :risks="report?.riskPoints" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>证据链</h3>
      </div>
      <div class="panel-body">
        <EvidenceList :evidences="report?.evidenceChain" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>简历描述</h3>
      </div>
      <div class="panel-body">
        <el-tabs model-value="basic">
          <el-tab-pane label="基础版" name="basic">
            <MarkdownBlock :content="report?.resumeBasic" />
          </el-tab-pane>
          <el-tab-pane label="标准版" name="standard">
            <MarkdownBlock :content="report?.resumeStandard" />
          </el-tab-pane>
          <el-tab-pane label="进阶版" name="advanced">
            <MarkdownBlock :content="report?.resumeAdvanced" />
          </el-tab-pane>
        </el-tabs>
        <div class="resume-print-sections print-only">
          <article>
            <h4>基础版</h4>
            <MarkdownBlock :content="report?.resumeBasic" />
          </article>
          <article>
            <h4>标准版</h4>
            <MarkdownBlock :content="report?.resumeStandard" />
          </article>
          <article>
            <h4>进阶版</h4>
            <MarkdownBlock :content="report?.resumeAdvanced" />
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Printer } from '@element-plus/icons-vue'

import { getReportDetail } from '@/api/analysis'
import EvidenceList from '@/components/EvidenceList.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import RadarScoreChart from '@/components/RadarScoreChart.vue'
import RiskList from '@/components/RiskList.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import type { AnalysisReport } from '@/types/api'

const route = useRoute()
const reportId = Number(route.params.id)
const loading = ref(false)
const report = ref<AnalysisReport>()

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
  { label: '可运行性', value: report.value?.runnabilityScore },
  { label: '真实性', value: report.value?.authenticityScore },
  { label: '结构', value: report.value?.structureScore },
  { label: 'README', value: report.value?.readmeScore },
  { label: '安全', value: report.value?.securityScore },
  { label: '工程化', value: report.value?.engineeringScore },
  { label: '面试价值', value: report.value?.interviewScore }
])

function formatScore(value?: number) {
  if (!Number.isFinite(value)) {
    return '-'
  }

  return `${Math.round(Number(value))} 分`
}

async function loadReport() {
  loading.value = true
  try {
    report.value = await getReportDetail(reportId)
  } finally {
    loading.value = false
  }
}

function handlePrint() {
  window.print()
}

onMounted(loadReport)
</script>

<style scoped>
.print-only {
  display: none;
}

.report-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 24px;
  align-items: center;
}

.report-summary-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.report-print-button {
  flex: 0 0 auto;
}

.report-summary h2 {
  margin: 8px 0 10px;
  font-size: 28px;
  line-height: 1.35;
}

.report-content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.report-content-grid article {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.report-content-grid h4 {
  margin: 0 0 12px;
}

.print-score-list,
.resume-print-sections {
  gap: 12px;
}

.print-score-list article,
.resume-print-sections article {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.print-score-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.print-score-list span {
  color: var(--pm-muted);
}

.print-score-list strong {
  color: var(--pm-ink);
  font-size: 18px;
}

.resume-print-sections h4 {
  margin: 0 0 10px;
}

@media (max-width: 920px) {
  .report-content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .report-hero {
    grid-template-columns: 1fr;
  }

  .report-summary-top {
    flex-direction: column;
  }
}

@media print {
  @page {
    margin: 14mm;
  }

  :global(html),
  :global(body),
  :global(#app) {
    background: #ffffff !important;
    color: #111827 !important;
  }

  :global(body) {
    min-width: 0;
  }

  :global(.app-sidebar),
  :global(.app-header),
  :global(.el-overlay),
  :global(.el-loading-mask),
  .no-print {
    display: none !important;
  }

  :global(.shell),
  :global(.shell-main),
  :global(.page-container) {
    display: block !important;
    width: 100% !important;
    max-width: none !important;
    min-height: auto !important;
    margin: 0 !important;
    padding: 0 !important;
    background: #ffffff !important;
  }

  .report-print-root {
    display: block;
    color: #111827;
    background: #ffffff;
  }

  .panel {
    margin: 0 0 12px;
    break-inside: avoid;
    page-break-inside: avoid;
    border: 1px solid #d0d5dd;
    background: #ffffff !important;
    box-shadow: none !important;
  }

  .panel-title,
  .panel-body {
    padding: 14px 16px;
  }

  .panel-title {
    border-bottom-color: #d0d5dd;
  }

  .report-hero,
  .report-content-grid,
  .print-score-list,
  .resume-print-sections {
    display: grid !important;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .report-summary h2 {
    color: #111827;
    font-size: 22px;
  }

  .muted,
  .eyebrow,
  :deep(.el-tag),
  :deep(.source-file) {
    color: #344054 !important;
  }

  .report-content-grid article,
  .print-score-list article,
  .resume-print-sections article,
  :deep(.risk-card),
  :deep(.evidence-card),
  :deep(.empty-state),
  :deep(.text-block) {
    break-inside: avoid;
    page-break-inside: avoid;
    border-color: #d0d5dd !important;
    background: #ffffff !important;
    box-shadow: none !important;
  }

  :deep(.radar-chart) {
    min-height: 280px;
    break-inside: avoid;
    page-break-inside: avoid;
  }

  :deep(.el-tabs) {
    display: none !important;
  }

  :deep(.markdown-body),
  :deep(.risk-card),
  :deep(.evidence-card),
  :deep(.text-block) {
    color: #111827 !important;
  }

  :deep(.text-block) {
    white-space: pre-wrap;
  }

  * {
    print-color-adjust: exact;
    -webkit-print-color-adjust: exact;
  }
}
</style>
