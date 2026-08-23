<template>
  <span
    class="brand-logo"
    :class="[`brand-logo--${resolvedVariant}`, `brand-logo--${resolvedTone}`]"
  >
    <img class="brand-logo__mark" :src="markSource" alt="" width="40" height="40" />
    <span v-if="resolvedVariant !== 'icon'" class="brand-logo__copy">
      <strong>{{ resolvedVariant === 'compact' ? 'PMAI' : 'ProjectMentor AI' }}</strong>
      <small v-if="resolvedVariant === 'primary'">Authenticity. Evidence. Confidence.</small>
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import mark from '@/assets/brand/logo-mark.svg'
import inverseMark from '@/assets/brand/logo-mark-inverse.svg'
import monochromeMark from '@/assets/brand/logo-mark-mono.svg'

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'compact' | 'icon' | 'full' | 'mark'
    tone?: 'default' | 'inverted' | 'monochrome'
    inverse?: boolean
  }>(),
  {
    variant: 'primary',
    tone: 'default',
    inverse: false
  }
)

const resolvedVariant = computed(() => {
  if (props.variant === 'full') return 'primary'
  if (props.variant === 'mark') return 'icon'
  return props.variant
})

const resolvedTone = computed(() => (props.inverse ? 'inverted' : props.tone))
const markSource = computed(() => {
  if (resolvedTone.value === 'inverted') return inverseMark
  if (resolvedTone.value === 'monochrome') return monochromeMark
  return mark
})
</script>

<style scoped>
.brand-logo {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
  color: var(--pm-ink);
}

.brand-logo--inverted {
  color: var(--pm-surface);
}

.brand-logo__mark {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
}

.brand-logo__copy,
.brand-logo__copy strong,
.brand-logo__copy small {
  display: block;
}

.brand-logo__copy strong {
  color: currentColor;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.1;
  white-space: nowrap;
}

.brand-logo__copy small {
  margin-top: 5px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 8px;
  letter-spacing: 0.075em;
  line-height: 1;
  text-transform: uppercase;
  white-space: nowrap;
}

.brand-logo--inverted .brand-logo__copy small {
  color: var(--pm-inspection-muted);
}

.brand-logo--compact {
  gap: 9px;
}

.brand-logo--compact .brand-logo__mark {
  width: 32px;
  height: 32px;
}

.brand-logo--compact .brand-logo__copy strong {
  font-family: var(--pm-font-mono);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.brand-logo--icon {
  display: inline-flex;
}

.brand-logo--icon .brand-logo__mark {
  width: 34px;
  height: 34px;
}
</style>
