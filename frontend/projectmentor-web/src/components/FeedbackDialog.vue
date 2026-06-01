<template>
  <el-dialog v-model="visible" title="反馈建议" width="720px" class="feedback-dialog" align-center>
    <el-form class="feedback-form" label-position="top" @submit.prevent>
      <el-form-item label="反馈类型" required>
        <el-select v-model="form.type" class="feedback-select">
          <el-option v-for="option in feedbackTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="反馈内容" required>
        <el-input
          v-model="form.content"
          type="textarea"
          maxlength="2000"
          show-word-limit
          :autosize="{ minRows: 7, maxRows: 13 }"
          placeholder="请描述你在哪里遇到了什么问题，以及你期望它变成什么样。"
        />
      </el-form-item>

      <el-form-item label="联系方式">
        <el-input v-model="form.contact" maxlength="255" show-word-limit placeholder="可选，邮箱 / 微信 / 其他方便联系的方式" />
      </el-form-item>

      <el-form-item label="来源页面">
        <el-input v-model="pageUrl" readonly />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="feedback-actions">
        <el-button :icon="CopyDocument" @click="copyFeedbackTemplate">复制反馈模板</el-button>
        <el-button :icon="Position" @click="openGithubIssues">前往 GitHub Issues</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交反馈</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { CopyDocument, Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { submitFeedback } from '@/api/feedback'
import { useUserStore } from '@/stores/user'
import type { FeedbackType } from '@/types/api'

const GITHUB_ISSUES_URL = 'https://github.com/Xinzhe-666/projectmentor-ai/issues'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const userStore = useUserStore()
const submitting = ref(false)
const pageUrl = ref('')

const feedbackTypeOptions: Array<{ label: string; value: FeedbackType }> = [
  { label: '功能 Bug', value: 'BUG' },
  { label: '体验建议', value: 'UX' },
  { label: '审计不准确', value: 'AUDIT_INACCURATE' },
  { label: '问答不准确', value: 'QA_INACCURATE' },
  { label: '面试问题', value: 'INTERVIEW_QUESTION' },
  { label: '上传问题', value: 'UPLOAD' },
  { label: '其他', value: 'OTHER' }
]

const form = reactive({
  type: 'BUG' as FeedbackType,
  content: '',
  contact: ''
})

const feedbackTypeLabel = computed(() => {
  return feedbackTypeOptions.find((option) => option.value === form.type)?.label || form.type
})

const feedbackTemplate = computed(() => `【问题类型】
${feedbackTypeLabel.value}

【我遇到的问题】
${form.content.trim() || '请描述你在哪里遇到了什么问题。'}

【期望效果】
你希望它变成什么样？

【联系方式】
${form.contact.trim() || '可选'}

【来源页面】
${pageUrl.value || '未记录'}

【补充信息】
浏览器、项目类型、是否上传 ZIP、是否开启 AI。`)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      pageUrl.value = window.location.href
    }
  },
  { immediate: true }
)

function fallbackCopy(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  try {
    return document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
  }
}

async function copyFeedbackTemplate() {
  const text = feedbackTemplate.value

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      ElMessage.success('反馈模板已复制')
      return
    }
  } catch {
    // HTTP 环境或浏览器权限限制时，继续使用传统复制方式。
  }

  if (fallbackCopy(text)) {
    ElMessage.success('反馈模板已复制')
    return
  }

  ElMessage.warning('复制失败，请手动复制文本框内容')
}

function openGithubIssues() {
  window.open(GITHUB_ISSUES_URL, '_blank', 'noopener,noreferrer')
}

async function handleSubmit() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后提交反馈')
    return
  }

  const content = form.content.trim()
  if (content.length < 5) {
    ElMessage.warning('反馈内容至少需要 5 个字符')
    return
  }

  submitting.value = true
  try {
    await submitFeedback({
      type: form.type,
      content,
      contact: form.contact.trim() || undefined,
      pageUrl: pageUrl.value || window.location.href
    })
    ElMessage.success('反馈已提交，感谢你的帮助！')
    form.type = 'BUG'
    form.content = ''
    form.contact = ''
    visible.value = false
  } catch {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录后提交反馈')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.feedback-form {
  display: grid;
  gap: 2px;
}

.feedback-select {
  width: 100%;
}

.feedback-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
