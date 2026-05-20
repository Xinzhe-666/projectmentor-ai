<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>面试深挖</h2>
          <p class="muted">选择项目和模式，模拟面试官连续追问。</p>
        </div>
      </div>
      <div class="panel-body">
        <el-form :model="form" label-width="100px">
          <el-form-item label="项目 ID" required>
            <el-input v-model="form.projectId" placeholder="请输入项目 ID" />
          </el-form-item>
          <el-form-item label="模式">
            <el-select v-model="form.mode">
              <el-option label="技术深挖" value="TECH_DEEP_DIVE" />
              <el-option label="HR 真实性" value="HR_REALITY" />
              <el-option label="压力面试" value="PRESSURE" />
              <el-option label="华为后端" value="HUAWEI_BACKEND" />
              <el-option label="AI 项目" value="AI_PROJECT" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="starting" @click="handleStart">开始面试</el-button>
            <el-button :disabled="!session" @click="handleFinish">结束面试</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section v-if="session" class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ session.projectName || `项目 ${session.projectId}` }}</h3>
          <p class="muted">模式：{{ session.mode }}，状态：{{ session.status }}</p>
        </div>
        <el-tag v-if="session.totalScore">得分 {{ session.totalScore }}</el-tag>
      </div>
      <div class="panel-body page-stack">
        <div class="message-list">
          <article
            v-for="message in session.messages"
            :key="message.id"
            class="chat-message"
            :class="{ user: message.role === 'USER' }"
          >
            <div class="chat-role">{{ message.role }}</div>
            <div>{{ message.content }}</div>
            <p v-if="message.feedback" class="muted">反馈：{{ message.feedback }}</p>
            <el-tag v-if="message.score" size="small">评分 {{ message.score }}</el-tag>
          </article>
        </div>

        <el-input v-model="answer" type="textarea" :rows="4" placeholder="输入你的回答" />
        <div class="toolbar">
          <el-button type="primary" :loading="submitting" @click="handleSubmitAnswer">提交回答</el-button>
          <span class="muted">提交后系统会继续追问或给出反馈。</span>
        </div>

        <el-alert v-if="session.summary" :title="session.summary" type="success" show-icon :closable="false" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { finishInterview, startInterview, submitAnswer } from '@/api/interview'
import type { InterviewSession } from '@/types/api'

const starting = ref(false)
const submitting = ref(false)
const session = ref<InterviewSession>()
const answer = ref('')
const form = reactive({
  projectId: '',
  mode: 'TECH_DEEP_DIVE'
})

async function handleStart() {
  if (!form.projectId) {
    ElMessage.warning('请输入项目 ID')
    return
  }

  starting.value = true
  try {
    session.value = await startInterview({
      projectId: Number(form.projectId),
      mode: form.mode
    })
  } finally {
    starting.value = false
  }
}

async function handleSubmitAnswer() {
  if (!session.value) {
    ElMessage.warning('请先开始面试')
    return
  }

  if (!answer.value.trim()) {
    ElMessage.warning('请输入回答')
    return
  }

  submitting.value = true
  try {
    session.value = await submitAnswer(session.value.id, answer.value)
    answer.value = ''
  } finally {
    submitting.value = false
  }
}

async function handleFinish() {
  if (!session.value) {
    return
  }

  session.value = await finishInterview(session.value.id)
  ElMessage.success('面试已结束')
}
</script>
