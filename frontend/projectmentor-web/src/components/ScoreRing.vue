<template>
  <div class="score-ring-card">
    <div class="score-ring" :style="ringStyle" role="img" :aria-label="t('scoreRing.aria', { title, score: normalizedScore })">
      <div class="score-ring-inner">
        <strong>{{ normalizedScore }}</strong>
        <span>{{ t('common.points') }}</span>
      </div>
    </div>
    <div class="score-ring-meta">
      <h3>{{ title }}</h3>
      <el-tag :type="tagType" effect="light">{{ gradeText }}</el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  score: number
  title: string
}>()

const { t } = useI18n()

const normalizedScore = computed(() => {
  if (!Number.isFinite(props.score)) {
    return 0
  }

  return Math.max(0, Math.min(100, Math.round(props.score)))
})

const color = computed(() => {
  if (normalizedScore.value >= 80) {
    return '#10b981'
  }

  if (normalizedScore.value >= 60) {
    return '#f59e0b'
  }

  return '#ef4444'
})

const gradeText = computed(() => {
  if (normalizedScore.value >= 80) {
    return t('scoreRing.excellent')
  }

  if (normalizedScore.value >= 60) {
    return t('scoreRing.medium')
  }

  return t('scoreRing.highRisk')
})

const tagType = computed(() => {
  if (normalizedScore.value >= 80) {
    return 'success'
  }

  if (normalizedScore.value >= 60) {
    return 'warning'
  }

  return 'danger'
})

const ringStyle = computed(() => ({
  background: `conic-gradient(${color.value} ${normalizedScore.value * 3.6}deg, #e8edf5 0deg)`
}))
</script>

<style scoped>
.score-ring-card {
  display: flex;
  align-items: center;
  gap: 18px;
}

.score-ring {
  display: grid;
  width: 132px;
  height: 132px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
}

.score-ring-inner {
  display: grid;
  width: 98px;
  height: 98px;
  place-items: center;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(223, 230, 240, 0.9);
}

.score-ring-inner strong {
  align-self: end;
  color: var(--pm-ink);
  font-size: 34px;
  line-height: 1;
}

.score-ring-inner span {
  align-self: start;
  color: var(--pm-muted);
  font-size: 13px;
}

.score-ring-meta h3 {
  margin: 0 0 10px;
  font-size: 18px;
}

@media (max-width: 620px) {
  .score-ring-card {
    justify-content: space-between;
  }

  .score-ring {
    width: 112px;
    height: 112px;
  }

  .score-ring-inner {
    width: 82px;
    height: 82px;
  }
}
</style>
