<template>
  <span
    class="brand-logo"
    :class="[`brand-logo--${resolvedVariant}`, `brand-logo--${resolvedTone}`]"
  >
    <img class="brand-logo__image" :src="imageSource" alt="ProjectMentor AI" />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import compactLogo from '@/assets/brand/cropped/brand-board-compact.png'
import iconLogo from '@/assets/brand/cropped/brand-board-icon.png'
import inversePrimaryLogo from '@/assets/brand/cropped/brand-board-inverse-primary.png'
import primaryLogo from '@/assets/brand/cropped/brand-board-primary.png'

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
const imageSource = computed(() => {
  if (resolvedTone.value === 'inverted') return inversePrimaryLogo
  if (resolvedVariant.value === 'icon') return iconLogo
  if (resolvedVariant.value === 'compact') return compactLogo
  return primaryLogo
})
</script>

<style scoped>
.brand-logo {
  display: inline-block;
  min-width: 0;
  line-height: 0;
}

.brand-logo__image {
  display: block;
  width: 100%;
  height: auto;
}

.brand-logo--primary {
  width: min(280px, 100%);
}

.brand-logo--compact {
  width: 138px;
}

.brand-logo--icon {
  width: 42px;
}

.brand-logo--inverted.brand-logo--primary {
  width: min(300px, 100%);
}

@media (max-width: 520px) {
  .brand-logo--primary,
  .brand-logo--inverted.brand-logo--primary {
    width: min(240px, 100%);
  }

  .brand-logo--compact {
    width: 118px;
  }
}
</style>
