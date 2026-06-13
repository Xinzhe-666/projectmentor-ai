<template>
  <div class="demo-workflow" :class="{ 'demo-workflow--compact': compact }">
    <div class="demo-workflow-head">
      <div>
        <p class="eyebrow">{{ t('demoFlow.eyebrow') }}</p>
        <h3>{{ t('demoFlow.title') }}</h3>
        <p>{{ t('demoFlow.description') }}</p>
      </div>
      <span class="pm-status-chip">{{ t('demoFlow.badge') }}</span>
    </div>

    <ol class="demo-workflow-grid">
      <li v-for="(step, index) in steps" :key="step">
        <span>{{ index + 1 }}</span>
        <strong>{{ step }}</strong>
      </li>
    </ol>

    <p class="demo-workflow-note">{{ t('demoFlow.note') }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false
})

const { t } = useI18n()

const steps = computed(() => [
  t('demoFlow.steps.create'),
  t('demoFlow.steps.upload'),
  t('demoFlow.steps.scan'),
  t('demoFlow.steps.report'),
  t('demoFlow.steps.matrix'),
  t('demoFlow.steps.enhance'),
  t('demoFlow.steps.interview'),
  t('demoFlow.steps.share')
])
</script>

<style scoped>
.demo-workflow {
  display: grid;
  gap: 22px;
  padding: 26px;
  border: 1px solid rgba(31, 111, 235, 0.16);
  border-radius: 8px;
  background:
    radial-gradient(circle at 0% 0%, rgba(20, 184, 166, 0.12), transparent 34%),
    radial-gradient(circle at 100% 0%, rgba(31, 111, 235, 0.14), transparent 36%),
    rgba(255, 255, 255, 0.82);
  box-shadow: 0 22px 58px rgba(31, 111, 235, 0.09);
}

.demo-workflow-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.demo-workflow-head h3 {
  margin: 8px 0;
  color: var(--pm-ink);
  font-size: clamp(24px, 3vw, 34px);
}

.demo-workflow-head p:last-child,
.demo-workflow-note {
  margin: 0;
  color: var(--pm-muted);
  line-height: 1.75;
}

.demo-workflow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.demo-workflow-grid li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 70px;
  padding: 13px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);
}

.demo-workflow-grid span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

.demo-workflow-grid strong {
  color: #344054;
  font-size: 13px;
  line-height: 1.5;
}

.demo-workflow--compact {
  padding: 20px;
  box-shadow: none;
}

.demo-workflow--compact .demo-workflow-head h3 {
  font-size: 22px;
}

.demo-workflow-note {
  padding-top: 14px;
  border-top: 1px solid rgba(223, 230, 240, 0.82);
  font-size: 13px;
}

@media (max-width: 840px) {
  .demo-workflow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .demo-workflow,
  .demo-workflow--compact {
    padding: 18px;
  }

  .demo-workflow-head {
    display: grid;
  }

  .demo-workflow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
