<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-heading">
        <span class="preview-badge">{{ t('footer.preview') }}</span>
        <strong>{{ t('common.appName') }}</strong>
      </div>

      <p class="footer-summary">{{ t('footer.summary') }}</p>

      <div class="footer-facts">
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

      <div class="footer-links">
        <a
          href="https://github.com/Xinzhe-666/projectmentor-ai#readme"
          target="_blank"
          rel="noopener noreferrer"
        >
          README
        </a>
        <span aria-hidden="true">·</span>
        <span>{{ t('footer.evidenceReport') }}</span>
      </div>

      <p class="footer-copyright">{{ t('common.copyright') }}</p>
    </div>
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
  padding: 22px 24px 28px;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.footer-inner {
  width: min(1120px, 100%);
  margin: 0 auto;
  padding: 18px 20px;
  border: 1px solid rgba(223, 230, 240, 0.86);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 34px rgba(28, 43, 68, 0.05);
  text-align: center;
}

.footer-heading,
.footer-facts,
.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px 14px;
  flex-wrap: wrap;
}

.footer-heading strong {
  color: #344054;
  font-size: 14px;
}

.preview-badge {
  padding: 3px 9px;
  border: 1px solid rgba(31, 111, 235, 0.22);
  border-radius: 999px;
  background: #eef6ff;
  color: #245089;
  font-weight: 800;
}

.footer-summary {
  max-width: 860px;
  margin: 10px auto 0;
  color: #475467;
}

.footer-facts {
  margin-top: 12px;
}

.footer-facts span {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.92);
  color: #475467;
}

.footer-disclaimer {
  max-width: 900px;
  margin: 14px auto 0;
  border-top: 1px solid rgba(223, 230, 240, 0.86);
  border-bottom: 1px solid rgba(223, 230, 240, 0.86);
}

.footer-disclaimer summary {
  padding: 10px 0;
  color: #344054;
  font-weight: 800;
  cursor: pointer;
}

.footer-disclaimer-body {
  padding: 0 12px 10px;
  text-align: left;
}

.footer-disclaimer-body p {
  margin: 5px 0;
}

.footer-links {
  margin-top: 12px;
}

.footer-links a {
  color: var(--pm-primary, #1f6feb);
  font-weight: 700;
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
}

.footer-copyright {
  margin: 10px 0 0;
}

@media (max-width: 620px) {
  .app-footer {
    padding: 18px 16px 22px;
  }

  .footer-inner {
    padding: 16px;
  }
}
</style>
