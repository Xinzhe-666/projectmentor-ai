<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <BrandLogo variant="full" />
        <p>{{ t('footer.summary') }}</p>
      </div>

      <div class="footer-facts">
        <span>{{ t('footer.preview') }}</span>
        <span>{{ t('footer.creditNotice') }}</span>
        <span>{{ t('footer.noExaggeration') }}</span>
        <span>{{ t('footer.audience') }}</span>
      </div>

      <details class="footer-disclaimer">
        <summary>{{ t('footer.privacyTitle') }}</summary>
        <div class="footer-disclaimer-body">
          <p>{{ t('footer.materialUsage') }}</p>
          <p>{{ t('footer.noSecrets') }}</p>
          <p>{{ t('footer.aiReference') }}</p>
          <p>{{ t('footer.notCertification') }}</p>
        </div>
      </details>

      <div class="footer-base">
        <div class="footer-links">
          <a href="https://github.com/Xinzhe-666/projectmentor-ai#readme" target="_blank" rel="noopener noreferrer">README</a>
          <span>{{ t('footer.evidenceReport') }}</span>
        </div>
        <p>{{ t('common.copyright') }}</p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import BrandLogo from '@/components/BrandLogo.vue'

const { t } = useI18n()
</script>

<style scoped>
.app-footer {
  padding: 0 max(24px, calc((100vw - 1320px) / 2));
  background: var(--pm-paper);
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.6;
}

.footer-inner {
  display: grid;
  padding: 54px 0 34px;
  grid-template-columns: minmax(240px, 0.9fr) minmax(300px, 1fr) minmax(260px, 0.8fr);
  gap: clamp(36px, 7vw, 96px);
  border-top: 1px solid var(--pm-stone-strong);
}

.footer-brand p {
  max-width: 40ch;
  margin: 18px 0 0;
}

.footer-facts {
  border-top: 1px solid var(--pm-stone-strong);
}

.footer-facts span {
  display: block;
  padding: 9px 0;
  border-bottom: 1px solid var(--pm-stone);
}

.footer-disclaimer {
  border-top: 1px solid var(--pm-stone-strong);
}

.footer-disclaimer summary {
  min-height: 44px;
  color: var(--pm-graphite);
  cursor: pointer;
  font-weight: 600;
  line-height: 44px;
}

.footer-disclaimer-body {
  padding-bottom: 8px;
}

.footer-disclaimer-body p {
  margin: 6px 0;
}

.footer-base {
  display: flex;
  padding-top: 20px;
  grid-column: 1 / -1;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-top: 1px solid var(--pm-stone);
}

.footer-base p {
  margin: 0;
}

.footer-links {
  display: flex;
  gap: 20px;
}

.footer-links a {
  color: var(--pm-primary-dark);
  font-weight: 600;
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 4px;
}

@media (max-width: 900px) {
  .footer-inner {
    grid-template-columns: 1fr 1fr;
  }

  .footer-brand {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .app-footer {
    padding: 0 16px;
  }

  .footer-inner {
    grid-template-columns: 1fr;
    gap: 30px;
  }

  .footer-brand {
    grid-column: auto;
  }

  .footer-base {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
