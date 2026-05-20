<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>AI 幻觉检测</h2>
          <p class="muted">输入 AI 对项目的描述，检查夸大、缺证据和简历风险。</p>
        </div>
      </div>
      <div class="panel-body">
        <el-form :model="form" label-width="100px">
          <el-form-item label="项目 ID">
            <el-input v-model="form.projectId" placeholder="可选，传入后会结合项目文件证据" />
          </el-form-item>
          <el-form-item label="AI 回答" required>
            <el-input v-model="form.aiAnswer" type="textarea" :rows="8" placeholder="粘贴 AI 生成的项目描述" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleCheck">开始检测</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section v-if="result" class="panel">
      <div class="panel-title">
        <h3>检测结果</h3>
        <el-tag>{{ result.riskLevel }}</el-tag>
      </div>
      <div class="panel-body page-stack">
        <div class="score-grid">
          <div class="metric-card">
            <span>可信度</span>
            <strong>{{ result.credibilityScore }}</strong>
          </div>
          <div class="metric-card">
            <span>客观性</span>
            <strong>{{ result.objectivityScore }}</strong>
          </div>
          <div class="metric-card">
            <span>问题数</span>
            <strong>{{ result.issueCount }}</strong>
          </div>
          <div class="metric-card">
            <span>简历风险</span>
            <strong>{{ result.resumeRisk ? '有' : '无' }}</strong>
          </div>
        </div>

        <el-table :data="result.issues" stripe>
          <el-table-column prop="riskLevel" label="等级" width="100" />
          <el-table-column prop="issueType" label="类型" width="170" />
          <el-table-column prop="matchedText" label="命中文本" min-width="160" />
          <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
          <el-table-column prop="suggestion" label="建议" min-width="260" show-overflow-tooltip />
        </el-table>

        <div>
          <h3>不安全简历表述</h3>
          <el-tag v-for="statement in result.unsafeResumeStatements" :key="statement" class="tag-item" type="warning">
            {{ statement }}
          </el-tag>
        </div>

        <div>
          <h3>更稳妥改写</h3>
          <pre class="text-block">{{ result.saferRewrite || '-' }}</pre>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { checkHallucination } from '@/api/hallucination'
import type { HallucinationCheckResult } from '@/types/api'

const loading = ref(false)
const result = ref<HallucinationCheckResult>()
const form = reactive({
  projectId: '',
  aiAnswer: ''
})

async function handleCheck() {
  if (!form.aiAnswer.trim()) {
    ElMessage.warning('请输入 AI 回答内容')
    return
  }

  loading.value = true
  try {
    result.value = await checkHallucination({
      projectId: form.projectId ? Number(form.projectId) : undefined,
      aiAnswer: form.aiAnswer
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tag-item {
  margin: 0 8px 8px 0;
}
</style>
