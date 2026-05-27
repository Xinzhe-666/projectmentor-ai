<template>
  <div class="page-stack report-print-root" v-loading="loading">
    <section class="panel report-cover">
      <div class="panel-body report-hero">
        <div class="report-identity">
          <p class="eyebrow">ProjectMentor AI Audit Report</p>
          <h1>{{ reportProjectName }}</h1>
          <div class="report-meta-grid">
            <span>
              报告编号
              <strong>#{{ report?.id || reportId }}</strong>
            </span>
            <span>
              项目编号
              <strong>#{{ report?.projectId || '-' }}</strong>
            </span>
            <span>
              技术栈
              <strong>{{ reportTechStack }}</strong>
            </span>
            <span>
              生成时间
              <strong>{{ report?.createTime || '-' }}</strong>
            </span>
          </div>
          <p class="report-notice">
            基于规则扫描与 AI 增强生成，仅供项目复盘和面试准备参考。
          </p>
        </div>
        <div class="report-score-card">
          <ScoreRing :score="report?.totalScore ?? 0" title="总分" />
        </div>
      </div>
      <div class="report-actions no-print">
        <el-button class="report-print-button" type="primary" :icon="Printer" @click="handlePrint">
          打印 / 保存为 PDF
        </el-button>
        <el-button
          :type="shareInfo?.enabled ? 'default' : 'primary'"
          :icon="shareInfo?.enabled ? Refresh : Link"
          :loading="shareLoading"
          @click="handleCreateShare"
        >
          {{ shareInfo?.enabled ? '刷新分享链接' : '生成分享链接' }}
        </el-button>
        <el-button
          v-if="shareInfo?.enabled && fullShareUrl"
          :icon="CopyDocument"
          @click="handleCopyShare"
        >
          复制分享链接
        </el-button>
        <el-button
          v-if="shareInfo?.enabled"
          type="danger"
          plain
          :icon="Close"
          :loading="shareLoading"
          @click="handleDisableShare"
        >
          关闭分享
        </el-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>报告摘要</h3>
          <p class="muted">先看整体判断，再进入风险点、证据链和简历表达建议。</p>
        </div>
      </div>
      <div class="panel-body">
        <p class="report-summary-text">{{ report?.summary || '报告摘要生成中' }}</p>
      </div>
    </section>

    <section v-if="shareInfo?.enabled && fullShareUrl" class="panel no-print">
      <div class="panel-title">
        <div>
          <h3>只读分享链接</h3>
          <p class="muted">公开访问仅展示脱敏后的报告内容，不包含用户信息、额度流水、AI 调用日志或项目源码。</p>
        </div>
      </div>
      <div class="panel-body share-link-row">
        <el-input :model-value="fullShareUrl" readonly>
          <template #append>
            <el-button :icon="CopyDocument" @click="handleCopyShare">复制</el-button>
          </template>
        </el-input>
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
        <div>
          <h3>风险点</h3>
          <p class="muted">按 HIGH / MEDIUM / LOW 区分优先级，逐条查看风险类型、证据和建议。</p>
        </div>
      </div>
      <div class="panel-body">
        <RiskList :risks="report?.riskPoints" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>证据链</h3>
          <p class="muted">重点关注 sourceFile、evidence 和 suggestion，判断结论来自哪里。</p>
        </div>
      </div>
      <div class="panel-body">
        <EvidenceList :evidences="report?.evidenceChain" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>简历描述</h3>
          <p class="muted">三版文案同时展开，便于按岗位和面试准备程度复制调整。</p>
        </div>
      </div>
      <div class="panel-body">
        <div class="resume-copy-grid">
          <article v-for="section in resumeSections" :key="section.title" class="resume-copy-card">
            <div class="resume-copy-head">
              <h4>{{ section.title }}</h4>
              <span>{{ section.description }}</span>
            </div>
            <MarkdownBlock :content="section.content" />
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, CopyDocument, Link, Printer, Refresh } from '@element-plus/icons-vue'

import { getReportDetail } from '@/api/analysis'
import { getProjectDetail } from '@/api/project'
import { createReportShare, disableReportShare, getReportShare } from '@/api/share'
import EvidenceList from '@/components/EvidenceList.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import RadarScoreChart from '@/components/RadarScoreChart.vue'
import RiskList from '@/components/RiskList.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import type { AnalysisReport, Project, ReportShare } from '@/types/api'

const route = useRoute()
const reportId = Number(route.params.id)
const loading = ref(false)
const shareLoading = ref(false)
const report = ref<AnalysisReport>()
const project = ref<Project>()
const shareInfo = ref<ReportShare>()

const reportProjectName = computed(() => {
  if (project.value?.name) {
    return project.value.name
  }

  if (report.value?.projectId) {
    return `项目 #${report.value.projectId}`
  }

  return '项目报告'
})

const reportTechStack = computed(() => project.value?.techStack || '未填写')

const fullShareUrl = computed(() => {
  const token = shareInfo.value?.shareToken

  if (!shareInfo.value?.enabled || !token) {
    return ''
  }

  return `${window.location.origin}/share/reports/${token}`
})

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

const resumeSections = computed(() => [
  {
    title: '基础版',
    description: '适合保守描述，先保证可解释。',
    content: report.value?.resumeBasic
  },
  {
    title: '标准版',
    description: '适合简历主体，兼顾亮点和边界。',
    content: report.value?.resumeStandard
  },
  {
    title: '进阶版',
    description: '适合面试延展，强调证据和实现理解。',
    content: report.value?.resumeAdvanced
  }
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
    project.value = undefined

    if (report.value.projectId) {
      try {
        project.value = await getProjectDetail(report.value.projectId)
      } catch {
        project.value = undefined
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadShareInfo() {
  shareLoading.value = true
  try {
    shareInfo.value = await getReportShare(reportId)
  } finally {
    shareLoading.value = false
  }
}

async function handleCreateShare() {
  const wasEnabled = Boolean(shareInfo.value?.enabled)
  shareLoading.value = true

  try {
    shareInfo.value = await createReportShare(reportId)
    ElMessage.success(wasEnabled ? '分享链接已刷新' : '分享链接已生成')
  } finally {
    shareLoading.value = false
  }
}

async function handleCopyShare() {
  if (!fullShareUrl.value) {
    return
  }

  try {
    await navigator.clipboard.writeText(fullShareUrl.value)
    ElMessage.success('分享链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制链接')
  }
}

async function handleDisableShare() {
  shareLoading.value = true

  try {
    await disableReportShare(reportId)
    shareInfo.value = {
      reportId,
      enabled: false
    }
    ElMessage.success('分享链接已关闭')
  } finally {
    shareLoading.value = false
  }
}

function handlePrint() {
  window.print()
}

onMounted(() => {
  loadReport()
  loadShareInfo()
})
</script>

<style scoped>
.print-only {
  display: none;
}

.report-cover {
  overflow: hidden;
}

.report-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
}

.report-identity h1 {
  margin: 8px 0 16px;
  color: var(--pm-ink);
  font-size: 30px;
  line-height: 1.2;
}

.report-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.report-meta-grid span {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #fbfdff;
  color: var(--pm-muted);
  font-size: 12px;
}

.report-meta-grid strong {
  display: block;
  margin-top: 5px;
  color: #344054;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.report-notice {
  margin: 16px 0 0;
  padding: 12px 14px;
  border: 1px solid rgba(31, 111, 235, 0.18);
  border-radius: 8px;
  background: #eef6ff;
  color: #245089;
  line-height: 1.7;
}

.report-score-card {
  padding: 12px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #ffffff;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--pm-border);
  background: #fbfdff;
}

.report-print-button {
  flex: 0 0 auto;
}

.share-link-row {
  display: block;
}

.report-summary-text {
  margin: 0;
  color: #344054;
  font-size: 17px;
  line-height: 1.85;
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
.resume-copy-grid {
  gap: 12px;
}

.print-score-list article,
.resume-copy-card {
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

.resume-copy-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.resume-copy-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-copy-head h4 {
  margin: 0;
  color: var(--pm-ink);
}

.resume-copy-head span {
  display: block;
  margin-top: 6px;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 920px) {
  .report-meta-grid,
  .report-content-grid,
  .resume-copy-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .report-hero {
    grid-template-columns: 1fr;
  }

  .report-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .report-identity h1 {
    font-size: 24px;
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
  .resume-copy-grid {
    display: grid !important;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .report-identity h1 {
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
  .resume-copy-card,
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
