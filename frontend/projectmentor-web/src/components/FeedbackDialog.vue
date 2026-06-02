<template>
  <el-dialog v-model="visible" :title="t('feedback.title')" width="720px" class="feedback-dialog" align-center>
    <el-form class="feedback-form" label-position="top" @submit.prevent>
      <el-form-item :label="t('feedback.type')" required>
        <el-select v-model="form.type" class="feedback-select">
          <el-option v-for="option in feedbackTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('feedback.content')" required>
        <el-input
          v-model="form.content"
          type="textarea"
          maxlength="2000"
          show-word-limit
          :autosize="{ minRows: 7, maxRows: 13 }"
          :placeholder="t('feedback.placeholder')"
        />
      </el-form-item>

      <el-form-item :label="t('feedback.contact')">
        <el-input v-model="form.contact" maxlength="255" show-word-limit :placeholder="t('feedback.contactPlaceholder')" />
      </el-form-item>

      <el-form-item :label="t('feedback.pageUrl')">
        <el-input v-model="pageUrl" readonly />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="feedback-actions">
        <el-button :icon="CopyDocument" @click="copyFeedbackTemplate">{{ t('feedback.copyTemplate') }}</el-button>
        <el-button :icon="Position" @click="openGithubIssues">{{ t('feedback.githubIssues') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ t('feedback.submit') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
const userStore = useUserStore()
const submitting = ref(false)
const pageUrl = ref('')

const feedbackTypeOptions = computed<Array<{ label: string; value: FeedbackType }>>(() => [
  { label: t('feedback.types.BUG'), value: 'BUG' },
  { label: t('feedback.types.UX'), value: 'UX' },
  { label: t('feedback.types.AUDIT_INACCURATE'), value: 'AUDIT_INACCURATE' },
  { label: t('feedback.types.QA_INACCURATE'), value: 'QA_INACCURATE' },
  { label: t('feedback.types.INTERVIEW_QUESTION'), value: 'INTERVIEW_QUESTION' },
  { label: t('feedback.types.UPLOAD'), value: 'UPLOAD' },
  { label: t('feedback.types.OTHER'), value: 'OTHER' }
])

const form = reactive({
  type: 'BUG' as FeedbackType,
  content: '',
  contact: ''
})

const feedbackTypeLabel = computed(() => {
  return feedbackTypeOptions.value.find((option) => option.value === form.type)?.label || form.type
})

const feedbackTemplate = computed(() => `${t('feedback.template.type')}
${feedbackTypeLabel.value}

${t('feedback.template.issue')}
${form.content.trim() || t('feedback.template.issuePlaceholder')}

${t('feedback.template.expectation')}
${t('feedback.template.expectationPlaceholder')}

${t('feedback.template.contact')}
${form.contact.trim() || t('feedback.template.contactPlaceholder')}

${t('feedback.template.page')}
${pageUrl.value || t('feedback.template.pagePlaceholder')}

${t('feedback.template.extra')}
${t('feedback.template.extraPlaceholder')}`)

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
      ElMessage.success(t('feedback.templateCopied'))
      return
    }
  } catch {
    // HTTP 环境或浏览器权限限制时，继续使用传统复制方式。
  }

  if (fallbackCopy(text)) {
    ElMessage.success(t('feedback.templateCopied'))
    return
  }

  ElMessage.warning(t('feedback.copyFailed'))
}

function openGithubIssues() {
  window.open(GITHUB_ISSUES_URL, '_blank', 'noopener,noreferrer')
}

async function handleSubmit() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning(t('feedback.needLogin'))
    return
  }

  const content = form.content.trim()
  if (content.length < 5) {
    ElMessage.warning(t('feedback.minContent'))
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
    ElMessage.success(t('feedback.submitted'))
    form.type = 'BUG'
    form.content = ''
    form.contact = ''
    visible.value = false
  } catch {
    if (!userStore.isLoggedIn) {
      ElMessage.warning(t('feedback.needLogin'))
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
