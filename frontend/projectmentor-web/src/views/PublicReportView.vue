<template>
  <div class="public-report-page" v-loading="loading">
    <header class="public-report-header">
      <div class="public-heading">
        <span class="brand-mark">PM</span>
        <div>
          <p class="eyebrow">ProjectMentor AI</p>
          <h1>ProjectMentor AI 只读分享报告</h1>
          <p>该报告由用户主动分享，仅供项目复盘和面试准备参考。</p>
        </div>
      </div>
      <el-tag effect="light" type="info">Read only</el-tag>
    </header>

    <main class="public-report-container">
      <section class="public-notice">
        公开分享页仅展示脱敏后的只读报告内容，不包含用户身份、额度流水、AI 调用日志或项目源码内容。
      </section>

      <template v-if="report">
        <section class="panel public-hero-panel">
          <div class="panel-body public-hero">
            <div class="public-summary">
              <p class="eyebrow">Shared Audit Report</p>
              <h2>{{ report.projectName || '未命名项目' }}</h2>
              <div class="public-meta-grid">
                <span>
                  项目名称
                  <strong>{{ report.projectName || '未命名项目' }}</strong>
                </span>
                <span>
                  技术栈
                  <strong>{{ report.techStack || '未填写' }}</strong>
                </span>
                <span>
                  项目类型
                  <strong>{{ report.projectType || '未填写' }}</strong>
                </span>
                <span>
                  生成时间
                  <strong>{{ report.createTime || '-' }}</strong>
                </span>
              </div>
              <p class="public-summary-text">{{ report.summary || '暂无报告摘要' }}</p>
            </div>
            <div class="public-score-card">
              <ScoreRing :score="report.totalScore ?? 0" title="总分" />
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <h3>维度评分</h3>
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
              <h3>报告内容</h3>
              <p class="muted">从优势、短板和后续建议快速判断项目当前状态。</p>
            </div>
          </div>
          <div class="panel-body public-content-grid">
            <article>
              <h4>优势</h4>
              <MarkdownBlock :content="report.strengths" />
            </article>
            <article>
              <h4>短板</h4>
              <MarkdownBlock :content="report.weaknesses" />
            </article>
            <article>
              <h4>建议</h4>
              <MarkdownBlock :content="report.suggestions" />
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>风险点</h3>
              <p class="muted">风险等级越高，越需要优先补充证据或降低简历表述强度。</p>
            </div>
          </div>
          <div class="panel-body">
            <RiskList :risks="report.riskPoints" />
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>证据链</h3>
              <p class="muted">通过 sourceFile、evidence 和 suggestion 追溯每条判断的来源。</p>
            </div>
          </div>
          <div class="panel-body">
            <EvidenceList :evidences="report.evidenceChain" />
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <div>
              <h3>简历描述参考</h3>
              <p class="muted">三版描述保留不同表达强度，复制前建议结合个人真实贡献再调整。</p>
            </div>
          </div>
          <div class="panel-body public-resume-grid">
            <article>
              <h4>基础版</h4>
              <MarkdownBlock :content="report.resumeBasic" />
            </article>
            <article>
              <h4>标准版</h4>
              <MarkdownBlock :content="report.resumeStandard" />
            </article>
            <article>
              <h4>进阶版</h4>
              <MarkdownBlock :content="report.resumeAdvanced" />
            </article>
          </div>
        </section>

        <footer class="public-footer">
          ProjectMentor AI 关注项目真实性、证据链和面试可解释性，不承诺自动审计完全准确。
        </footer>
      </template>

      <section v-else-if="errorMessage" class="panel">
        <el-result
          icon="warning"
          title="分享链接不可用"
          :sub-title="errorMessage"
        />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getPublicReport } from '@/api/share'
import EvidenceList from '@/components/EvidenceList.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import RadarScoreChart from '@/components/RadarScoreChart.vue'
import RiskList from '@/components/RiskList.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import type { PublicReport } from '@/types/api'

const route = useRoute()
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

async function loadPublicReport() {
  loading.value = true
  errorMessage.value = ''

  try {
    report.value = await getPublicReport(token)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '分享链接不存在、已关闭或已过期'
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
    padding: 16px;
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
