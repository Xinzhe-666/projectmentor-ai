<template>
  <el-dialog v-model="visible" :title="t('donate.title')" width="680px" class="donate-dialog" align-center>
    <p class="donate-copy">
      {{ t('donate.copy') }}
    </p>

    <div class="donate-grid">
      <article v-for="method in donateMethods" :key="method.key" class="donate-card">
        <h3>{{ method.title }}</h3>
        <div class="qr-frame">
          <el-image :src="method.src" :alt="method.title" fit="contain" class="qr-image">
            <template #error>
              <div class="qr-placeholder">{{ t('donate.qrMissing') }}</div>
            </template>
          </el-image>
        </div>
      </article>
    </div>

    <p class="donate-note">{{ t('donate.note') }}</p>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

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

const donateMethods = computed(() => [
  {
    key: 'wechat',
    title: t('donate.wechat'),
    src: '/donate/wechat.png'
  },
  {
    key: 'alipay',
    title: t('donate.alipay'),
    src: '/donate/alipay.png'
  }
])
</script>

<style scoped>
.donate-copy {
  margin: 0;
  color: #344054;
  line-height: 1.8;
}

.donate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.donate-card {
  min-width: 0;
  padding: 18px;
  border: 1px solid rgba(223, 230, 240, 0.95);
  border-radius: 8px;
  background: #fbfdff;
}

.donate-card h3 {
  margin: 0 0 12px;
  font-size: 17px;
}

.qr-frame {
  display: grid;
  width: 100%;
  aspect-ratio: 1;
  place-items: center;
  overflow: hidden;
  border: 1px dashed #c8d4e5;
  border-radius: 8px;
  background: #ffffff;
}

.qr-image {
  width: 100%;
  height: 100%;
}

.qr-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #667085;
  font-size: 14px;
}

.donate-note {
  margin: 18px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 620px) {
  .donate-grid {
    grid-template-columns: 1fr;
  }
}
</style>
