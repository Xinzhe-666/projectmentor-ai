<template>
  <div class="page-stack" v-loading="loading">
    <section class="panel">
      <div class="panel-body report-hero">
        <ScoreRing :score="report?.totalScore ?? 0" title="项目总分" />
        <div class="report-summary">
          <p class="eyebrow">Audit Report #{{ report?.id || reportId }}</p>
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
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

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

async function loadReport() {
  loading.value = true
  try {
    report.value = await getReportDetail(reportId)
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)
</script>

<style scoped>
.report-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 24px;
  align-items: center;
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

@media (max-width: 920px) {
  .report-content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .report-hero {
    grid-template-columns: 1fr;
  }
}
</style>
