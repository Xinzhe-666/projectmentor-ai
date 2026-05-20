<template>
  <div class="page-stack" v-loading="loading">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>审计报告 #{{ report?.id }}</h2>
          <p class="muted">项目 ID：{{ report?.projectId }}</p>
        </div>
        <el-tag type="success">总分 {{ report?.totalScore ?? '-' }}</el-tag>
      </div>
      <div class="panel-body">
        <div class="score-grid">
          <div class="metric-card" v-for="score in scoreItems" :key="score.label">
            <span>{{ score.label }}</span>
            <strong>{{ score.value ?? '-' }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title"><h3>核心结论</h3></div>
      <div class="panel-body report-grid">
        <article>
          <h4>Summary</h4>
          <p>{{ report?.summary || '-' }}</p>
        </article>
        <article>
          <h4>Strengths</h4>
          <p>{{ report?.strengths || '-' }}</p>
        </article>
        <article>
          <h4>Weaknesses</h4>
          <p>{{ report?.weaknesses || '-' }}</p>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title"><h3>风险与证据</h3></div>
      <div class="panel-body report-grid">
        <article>
          <h4>riskPoints</h4>
          <pre class="json-block">{{ formatJsonString(report?.riskPoints) }}</pre>
        </article>
        <article>
          <h4>evidenceChain</h4>
          <pre class="json-block">{{ formatJsonString(report?.evidenceChain) }}</pre>
        </article>
        <article>
          <h4>suggestions</h4>
          <pre class="json-block">{{ formatJsonString(report?.suggestions) }}</pre>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title"><h3>简历描述建议</h3></div>
      <div class="panel-body report-grid">
        <article>
          <h4>保守版</h4>
          <pre class="text-block">{{ report?.resumeBasic || '-' }}</pre>
        </article>
        <article>
          <h4>标准版</h4>
          <pre class="text-block">{{ report?.resumeStandard || '-' }}</pre>
        </article>
        <article>
          <h4>进阶版</h4>
          <pre class="text-block">{{ report?.resumeAdvanced || '-' }}</pre>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { getReportDetail } from '@/api/analysis'
import type { AnalysisReport } from '@/types/api'

const route = useRoute()
const reportId = Number(route.params.id)
const loading = ref(false)
const report = ref<AnalysisReport>()

const scoreItems = computed(() => [
  { label: '可运行性', value: report.value?.runnabilityScore },
  { label: '真实性', value: report.value?.authenticityScore },
  { label: '代码结构', value: report.value?.structureScore },
  { label: 'README', value: report.value?.readmeScore },
  { label: '安全性', value: report.value?.securityScore },
  { label: '工程化', value: report.value?.engineeringScore },
  { label: '面试价值', value: report.value?.interviewScore },
  { label: '总分', value: report.value?.totalScore }
])

function formatJsonString(value?: string) {
  if (!value) {
    return '-'
  }

  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

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
.report-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.report-grid article {
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.report-grid h4 {
  margin: 0 0 10px;
}

.report-grid p {
  margin: 0;
  line-height: 1.8;
}
</style>
