<template>
  <footer class="app-footer">
    {{ t('common.copyright') }}
  </footer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.app-footer {
  position: relative;
  z-index: 1;
  padding: 18px 24px 24px;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}
</style>
