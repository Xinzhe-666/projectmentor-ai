<template>
  <el-dialog v-model="visible" title="反馈建议" width="720px" class="feedback-dialog" align-center>
    <p class="feedback-copy">
      当前反馈入口是轻量版：你可以复制模板后到 GitHub Issues 提交，也可以先在本地记录问题。
    </p>

    <el-input
      v-model="feedbackText"
      type="textarea"
      :autosize="{ minRows: 11, maxRows: 16 }"
      class="feedback-textarea"
    />

    <template #footer>
      <div class="feedback-actions">
        <el-button :icon="CopyDocument" @click="copyFeedbackTemplate">复制反馈模板</el-button>
        <el-button type="primary" :icon="Position" @click="openGithubIssues">前往 GitHub Issues</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { CopyDocument, Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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

const feedbackText = ref(`【问题类型】
功能 Bug / 体验建议 / 审计不准确 / 面试问题不合适 / 其他

【我遇到的问题】
请描述你在哪里遇到了什么问题。

【期望效果】
你希望它变成什么样？

【补充信息】
浏览器、项目类型、是否上传 ZIP、是否开启 AI。`)

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
  const text = feedbackText.value

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
</script>

<style scoped>
.feedback-copy {
  margin: 0 0 14px;
  color: #344054;
  line-height: 1.8;
}

.feedback-textarea {
  width: 100%;
}

.feedback-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
