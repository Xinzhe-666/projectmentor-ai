<template>
  <span class="status-label" :class="`status-label--${tone}`" :aria-label="accessibleLabel">
    <i aria-hidden="true" />
    <span>{{ visibleLabel }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  status?: string | null
  label?: string
}>()

const { t } = useI18n()

const normalizedStatus = computed(() => (props.status || 'PENDING').trim().toUpperCase())

const tone = computed(() => {
  const toneMap: Record<string, string> = {
    SUCCESS: 'supported',
    FINISHED: 'supported',
    AVAILABLE: 'supported',
    SUPPORTED: 'supported',
    COMPLETED: 'supported',
    RUNNING: 'documented',
    ACTIVE: 'documented',
    ANALYZING: 'documented',
    DOC_ONLY: 'documented',
    DOCUMENTED_ONLY: 'documented',
    LOW: 'documented',
    PARTIAL: 'partial',
    WEAK: 'partial',
    MEDIUM: 'partial',
    RULES_ONLY: 'partial',
    WARNING: 'partial',
    FAILED: 'risky',
    HIGH: 'risky',
    UNAVAILABLE: 'risky',
    RISKY: 'risky',
    STRONG: 'supported',
    PENDING: 'neutral',
    CREATING: 'neutral',
    INSUFFICIENT: 'neutral',
    INSUFFICIENT_DATA: 'neutral',
    UNKNOWN: 'neutral',
    NO_EVIDENCE: 'neutral'
  }

  return toneMap[normalizedStatus.value] || 'neutral'
})

const statusTranslationKey = computed(() => {
  const translationMap: Record<string, string> = {
    PENDING: 'status.pending',
    RUNNING: 'status.running',
    ANALYZING: 'status.analyzing',
    SUCCESS: 'status.success',
    FINISHED: 'status.finished',
    FAILED: 'status.failed'
  }

  return translationMap[normalizedStatus.value]
})

const visibleLabel = computed(() => props.label || normalizedStatus.value.replace(/_/g, ' '))
const accessibleLabel = computed(() => {
  if (props.label) {
    return props.label
  }

  return statusTranslationKey.value ? t(statusTranslationKey.value) : visibleLabel.value
})
</script>

<style scoped>
.status-label {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  color: var(--pm-none);
  font-family: var(--pm-font-mono);
  font-size: 11px;
  letter-spacing: 0.035em;
  line-height: 1.25;
  text-transform: uppercase;
}

.status-label i {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  background: currentColor;
}

.status-label--supported {
  color: var(--pm-supported);
}

.status-label--documented {
  color: var(--pm-doc);
}

.status-label--partial {
  color: var(--pm-partial);
}

.status-label--risky {
  color: var(--pm-risk);
}

.status-label--neutral {
  color: var(--pm-none);
}
</style>
