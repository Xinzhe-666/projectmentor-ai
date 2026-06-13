<template>
  <div
    ref="landingRoot"
    class="landing-page pm-aurora-bg"
    :class="{ 'landing-page--en': locale === 'en-US' }"
  >
    <div class="pm-grid-overlay" />
    <div class="pm-noise-overlay" />
    <nav class="landing-nav">
      <RouterLink class="landing-brand" to="/">
        <span>PM</span>
        <strong>ProjectMentor AI</strong>
      </RouterLink>
      <div class="nav-actions">
        <LanguageSwitch class="nav-language" />
        <el-button class="nav-feedback" text :icon="Message" @click="feedbackVisible = true">{{ t('common.feedback') }}</el-button>
        <el-button class="nav-donate" text :icon="Coffee" @click="donateVisible = true">{{ t('common.coffeeShort') }}</el-button>
        <el-button class="nav-login" text @click="router.push('/login')">{{ t('common.login') }}</el-button>
        <el-button class="nav-start" type="primary" :icon="ArrowRight" @click="goStart">{{ t('common.startNow') }}</el-button>
      </div>
    </nav>

    <section class="landing-hero pm-cinematic-hero">
      <div class="hero-copy pm-scroll-reveal" style="--reveal-index: 0">
        <p class="hero-kicker">{{ t('landing.heroEyebrow') }}</p>
        <h1>
          <span>ProjectMentor AI</span>
          <span class="pm-gradient-text">{{ t('landing.heroHeadline') }}</span>
        </h1>
        <p class="hero-subtitle">{{ t('landing.subtitle') }}</p>
        <p class="hero-subnote">{{ t('landing.heroSubnote') }}</p>

        <div class="hero-actions">
          <el-button size="large" type="primary" :icon="ArrowRight" @click="goStart">{{ t('landing.ctaAudit') }}</el-button>
          <el-button size="large" :icon="Search" @click="scrollToDemo">{{ t('landing.ctaDemo') }}</el-button>
        </div>

        <div class="pm-chip-row hero-proof-row">
          <span v-for="item in heroProofs" :key="item" class="pm-status-chip">
            {{ item }}
          </span>
        </div>
      </div>

      <div class="hero-visual pm-scroll-reveal" style="--reveal-index: 1">
        <div class="pm-product-window pm-gradient-border landing-mockup pm-floating-mockup">
          <div class="pm-mockup-topbar">
            <i class="pm-mockup-dot" />
            <i class="pm-mockup-dot" />
            <i class="pm-mockup-dot" />
            <span>{{ t('landing.mockup.windowTitle') }}</span>
          </div>
          <div class="mockup-body">
            <main class="mockup-stage">
              <div class="mockup-stage-head">
                <div>
                  <small>{{ t('landing.mockup.auditLabel') }}</small>
                  <strong>{{ t('landing.mockup.projectTitle') }}</strong>
                </div>
                <span class="pm-status-chip">{{ t('landing.mockup.evidenceStatus') }}</span>
              </div>

              <div class="mockup-summary">
                <div class="mockup-score-card">
                  <span>{{ t('landing.mockup.score') }}</span>
                  <strong>82</strong>
                  <small>{{ t('landing.mockup.scoreNote') }}</small>
                </div>
                <div class="mockup-evidence-card">
                  <span>{{ t('landing.mockup.evidenceTrust') }}</span>
                  <strong>{{ t('landing.mockup.strongEvidence') }}</strong>
                  <div class="trust-bars" aria-hidden="true">
                    <i />
                    <i />
                    <i />
                  </div>
                </div>
              </div>

              <div class="mockup-qa-bar">
                <span>{{ t('landing.mockup.askPlaceholder') }}</span>
                <strong>{{ t('landing.mockup.shareReady') }}</strong>
              </div>

              <div class="mockup-file-row">
                <span>{{ t('landing.mockup.reason') }}</span>
                <code>src/api/projectQa.ts</code>
                <code>src/views/ReportDetail.vue</code>
              </div>
            </main>
          </div>
        </div>
      </div>
    </section>

    <main class="landing-content">
      <section id="features" class="pm-section-shell pm-cinematic-section bento-section pm-scroll-reveal" style="--reveal-index: 0">
        <div class="pm-section-heading">
          <p class="eyebrow">{{ t('landing.bentoEyebrow') }}</p>
          <h2>{{ t('landing.bentoTitle') }}</h2>
          <p>{{ t('landing.bentoDesc') }}</p>
        </div>

        <div class="pm-bento-grid">
          <article
            v-for="(feature, index) in bentoFeatures"
            :key="feature.title"
            class="pm-bento-card bento-card pm-hover-lift pm-scroll-reveal"
            :class="feature.className"
            :style="{ '--reveal-index': index }"
          >
            <div class="bento-icon">
              <el-icon :size="24">
                <component :is="feature.icon" />
              </el-icon>
            </div>
            <div>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.description }}</p>
            </div>
            <div class="bento-mini-visual" aria-hidden="true">
              <i />
              <i />
              <i />
            </div>
            <span>{{ feature.meta }}</span>
          </article>
        </div>
      </section>

      <section class="pm-section-shell pm-cinematic-section workflow-section pm-scroll-reveal">
        <div class="pm-section-heading">
          <p class="eyebrow">{{ t('landing.workflowEyebrow') }}</p>
          <h2>{{ t('landing.workflowTitle') }}</h2>
        </div>
        <div class="flow-grid">
          <article v-for="(step, index) in workflow" :key="step.title" class="flow-card pm-premium-card pm-hover-lift pm-scroll-reveal" :style="{ '--reveal-index': index }">
            <span class="flow-index">{{ index + 1 }}</span>
            <el-icon :size="24">
              <component :is="step.icon" />
            </el-icon>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </section>

      <section class="pm-section-shell pm-cinematic-section comparison-section pm-scroll-reveal">
        <div class="pm-section-heading">
          <p class="eyebrow">{{ t('landing.comparison.eyebrow') }}</p>
          <h2>{{ t('landing.comparison.title') }}</h2>
          <p>{{ t('landing.comparison.description') }}</p>
        </div>
        <div class="comparison-grid">
          <article class="comparison-card comparison-card--rule pm-premium-card">
            <span>{{ t('landing.comparison.rule.badge') }}</span>
            <h3>{{ t('landing.comparison.rule.title') }}</h3>
            <p>{{ t('landing.comparison.rule.description') }}</p>
            <ul>
              <li v-for="item in ruleBenefits" :key="item">{{ item }}</li>
            </ul>
          </article>
          <article class="comparison-card comparison-card--ai pm-premium-card">
            <span>{{ t('landing.comparison.ai.badge') }}</span>
            <h3>{{ t('landing.comparison.ai.title') }}</h3>
            <p>{{ t('landing.comparison.ai.description') }}</p>
            <ul>
              <li v-for="item in aiBenefits" :key="item">{{ item }}</li>
            </ul>
          </article>
        </div>
      </section>

      <section class="pm-section-shell pm-cinematic-section audience-section pm-scroll-reveal">
        <div class="pm-section-heading center">
          <p class="eyebrow">{{ t('landing.audienceEyebrow') }}</p>
          <h2>{{ t('landing.audienceTitle') }}</h2>
          <p>{{ t('landing.audienceDesc') }}</p>
        </div>
        <div class="audience-grid">
          <article v-for="(audience, index) in audiences" :key="audience.title" class="audience-card pm-premium-card">
            <span>{{ index + 1 }}</span>
            <h3>{{ audience.title }}</h3>
            <p>{{ audience.description }}</p>
          </article>
        </div>
      </section>

      <section id="demo-flow" class="pm-section-shell pm-cinematic-section demo-section pm-scroll-reveal">
        <DemoWorkflow />
      </section>

      <section class="pm-section-shell pm-cinematic-section boundary-section pm-scroll-reveal">
        <div class="boundary-card trial-glass-notice pm-premium-card pm-gradient-border">
          <div class="pm-section-heading">
            <p class="eyebrow">{{ t('landing.boundaryEyebrow') }}</p>
            <h2>{{ t('landing.boundaryTitle') }}</h2>
            <p>{{ t('landing.boundaryDesc') }}</p>
            <div class="trial-glass-copy">{{ t('landing.trialDesc') }}</div>
          </div>
          <div class="boundary-list">
            <div v-for="boundary in boundaries" :key="boundary" class="boundary-item">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ boundary }}</span>
            </div>
          </div>
        </div>
      </section>
    </main>

    <AppFooter />
    <FeedbackDialog v-model="feedbackVisible" />
    <DonateDialog v-model="donateVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  Coffee,
  DocumentChecked,
  Files,
  FolderAdd,
  MagicStick,
  Message,
  Search,
  UploadFilled,
  WarningFilled
} from '@element-plus/icons-vue'

import AppFooter from '@/components/AppFooter.vue'
import DemoWorkflow from '@/components/DemoWorkflow.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import DonateDialog from '@/components/DonateDialog.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { locale, t } = useI18n()
const userStore = useUserStore()
const feedbackVisible = ref(false)
const donateVisible = ref(false)
const landingRoot = ref<HTMLElement | null>(null)
let revealObserver: IntersectionObserver | undefined

const heroProofs = computed(() => [
  t('landing.capabilities.audit'),
  t('landing.capabilities.qa'),
  t('landing.capabilities.interview'),
  t('landing.capabilities.share')
])

const bentoFeatures = computed(() => [
  {
    title: t('landing.bento.audit.title'),
    description: t('landing.bento.audit.description'),
    meta: t('landing.bento.audit.meta'),
    icon: DocumentChecked,
    className: 'pm-bento-card-accent'
  },
  {
    title: t('landing.bento.report.title'),
    description: t('landing.bento.report.description'),
    meta: t('landing.bento.report.meta'),
    icon: Files,
    className: ''
  },
  {
    title: t('landing.bento.interview.title'),
    description: t('landing.bento.interview.description'),
    meta: t('landing.bento.interview.meta'),
    icon: MagicStick,
    className: ''
  }
])

const workflow = computed(() => [
  {
    title: t('landing.workflow.create.title'),
    description: t('landing.workflow.create.description'),
    icon: FolderAdd
  },
  {
    title: t('landing.workflow.upload.title'),
    description: t('landing.workflow.upload.description'),
    icon: UploadFilled
  },
  {
    title: t('landing.workflow.report.title'),
    description: t('landing.workflow.report.description'),
    icon: DocumentChecked
  },
  {
    title: t('landing.workflow.review.title'),
    description: t('landing.workflow.review.description'),
    icon: Search
  }
])

const ruleBenefits = computed(() => [
  t('landing.comparison.rule.items.free'),
  t('landing.comparison.rule.items.stable'),
  t('landing.comparison.rule.items.explainable')
])

const aiBenefits = computed(() => [
  t('landing.comparison.ai.items.explanation'),
  t('landing.comparison.ai.items.resume'),
  t('landing.comparison.ai.items.interview')
])

const audiences = computed(() => [
  {
    title: t('landing.audiences.intern.title'),
    description: t('landing.audiences.intern.description')
  },
  {
    title: t('landing.audiences.aiBuilder.title'),
    description: t('landing.audiences.aiBuilder.description')
  },
  {
    title: t('landing.audiences.resume.title'),
    description: t('landing.audiences.resume.description')
  },
  {
    title: t('landing.audiences.interview.title'),
    description: t('landing.audiences.interview.description')
  }
])

const boundaries = computed(() => [
  t('landing.boundaries.notGuarantee'),
  t('landing.boundaries.reference'),
  t('landing.boundaries.noSecrets'),
  t('landing.boundaries.slowUpload'),
  t('landing.boundaries.beta'),
  t('landing.boundaries.verify')
])

function goStart() {
  router.push(userStore.isLoggedIn ? '/dashboard' : '/register')
}

function scrollToDemo() {
  const behavior: ScrollBehavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  landingRoot.value?.querySelector('#demo-flow')?.scrollIntoView({ behavior, block: 'start' })
}

function setupScrollReveal() {
  const root = landingRoot.value
  if (!root) {
    return
  }

  const revealItems = Array.from(root.querySelectorAll<HTMLElement>('.pm-fade-up, .pm-scroll-reveal'))
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    revealItems.forEach((item) => item.classList.add('is-visible'))
    return
  }

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) {
          return
        }

        entry.target.classList.add('is-visible')
        revealObserver?.unobserve(entry.target)
      })
    },
    { rootMargin: '0px 0px -12% 0px', threshold: 0.12 }
  )

  revealItems.forEach((item) => revealObserver?.observe(item))
}

onMounted(setupScrollReveal)

onBeforeUnmount(() => {
  revealObserver?.disconnect()
})
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  animation: pageFadeIn 420ms ease both;
}

.landing-nav {
  position: fixed;
  z-index: 20;
  top: 16px;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: min(1180px, calc(100% - 32px));
  padding: 10px 12px 10px 14px;
  border: 1px solid rgba(214, 224, 236, 0.68);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.62);
  box-shadow: 0 18px 48px rgba(28, 43, 68, 0.08);
  backdrop-filter: blur(24px);
  transform: translateX(-50%);
}

.landing-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.landing-brand span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #111827, #1f6feb);
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 12px 28px rgba(31, 111, 235, 0.2);
}

.nav-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-wrap: wrap;
}

.landing-hero {
  display: grid;
  min-height: 100svh;
  grid-template-columns: minmax(0, 0.86fr) minmax(560px, 1.14fr);
  align-items: center;
  gap: clamp(42px, 7vw, 92px);
  max-width: 1360px;
  margin: 0 auto;
  padding: 126px clamp(18px, 5vw, 72px) 72px;
}

.hero-copy h1 {
  display: grid;
  gap: 8px;
  max-width: 720px;
  margin: 0 0 22px;
  color: #111827;
  font-size: clamp(44px, 6.6vw, 86px);
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 640px;
  margin: 0;
  color: #475467;
  font-size: clamp(17px, 2vw, 21px);
  line-height: 1.72;
}

.hero-copy {
  min-width: 0;
}

.hero-subnote {
  max-width: 680px;
  margin: 12px 0 0;
  color: #667085;
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 30px;
  flex-wrap: wrap;
}

.hero-proof-row {
  margin-top: 22px;
}

.hero-visual {
  min-width: 0;
}

.landing-mockup {
  position: relative;
  min-height: 560px;
}

.landing-mockup::before {
  position: absolute;
  inset: -1px;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 0%, rgba(20, 184, 166, 0.16), transparent 34%),
    radial-gradient(circle at 100% 10%, rgba(31, 111, 235, 0.18), transparent 36%);
  content: "";
}

.pm-mockup-topbar span {
  margin-left: 8px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.mockup-body {
  display: grid;
  grid-template-columns: 144px minmax(0, 1fr) 230px;
  min-height: 520px;
}

.mockup-project-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 12px;
  border-right: 1px solid rgba(223, 230, 240, 0.78);
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.86), rgba(255, 255, 255, 0.58)),
    rgba(248, 251, 255, 0.78);
}

.mockup-brand-mini {
  display: grid;
  width: 52px;
  height: 34px;
  place-items: center;
  margin-bottom: 14px;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
}

.mockup-project-list button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 9px 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #475467;
  cursor: default;
  font-size: 12px;
  font-weight: 800;
  text-align: left;
}

.mockup-project-list button.active {
  background: rgba(31, 111, 235, 0.1);
  color: var(--pm-primary);
}

.mockup-project-list button small {
  display: grid;
  min-width: 28px;
  height: 22px;
  place-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #245089;
  font-weight: 900;
}

.mockup-stage {
  display: grid;
  align-content: stretch;
  gap: 18px;
  padding: 22px;
}

.mockup-stage-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.mockup-stage-head small {
  display: block;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.mockup-stage-head strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 22px;
}

.mockup-centerpiece {
  display: grid;
  grid-template-columns: minmax(180px, 0.72fr) minmax(0, 1fr);
  align-items: center;
  gap: 20px;
  min-height: 298px;
  padding: 22px;
  border: 1px solid rgba(214, 224, 236, 0.8);
  border-radius: 8px;
  background:
    radial-gradient(circle at 28% 18%, rgba(31, 111, 235, 0.18), transparent 38%),
    radial-gradient(circle at 82% 18%, rgba(20, 184, 166, 0.14), transparent 36%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(248, 251, 255, 0.66));
  box-shadow: 0 22px 62px rgba(31, 111, 235, 0.1);
}

.score-orb {
  display: grid;
  aspect-ratio: 1;
  place-items: center;
  min-width: 0;
  padding: 20px;
  border: 1px solid rgba(31, 111, 235, 0.18);
  border-radius: 999px;
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.92) 0 48%, transparent 49%),
    conic-gradient(from 220deg, var(--pm-primary), var(--pm-teal), rgba(214, 224, 236, 0.8), var(--pm-primary));
  box-shadow:
    0 26px 70px rgba(31, 111, 235, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  text-align: center;
}

.score-orb span,
.score-orb small,
.mockup-center-copy span,
.mockup-evidence-panel span {
  color: #667085;
  font-size: 12px;
  font-weight: 900;
}

.score-orb strong {
  color: #111827;
  font-size: clamp(54px, 6vw, 74px);
  line-height: 0.9;
}

.mockup-center-copy {
  min-width: 0;
}

.mockup-center-copy p {
  max-width: 340px;
  margin: 10px 0 0;
  color: #344054;
  font-size: 16px;
  line-height: 1.75;
}

.mockup-glow-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.mockup-glow-chips i {
  display: inline-flex;
  padding: 6px 10px;
  border: 1px solid rgba(31, 111, 235, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #245089;
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(31, 111, 235, 0.08);
}

.mockup-qa-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid rgba(31, 111, 235, 0.14);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(238, 246, 255, 0.72), rgba(255, 255, 255, 0.72)),
    rgba(255, 255, 255, 0.74);
}

.mockup-qa-bar span {
  color: #667085;
  font-size: 12px;
  font-weight: 900;
}

.mockup-qa-bar strong {
  min-width: 0;
  color: #111827;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.mockup-evidence-panel {
  display: grid;
  align-content: space-between;
  gap: 18px;
  padding: 22px 18px;
  border-left: 1px solid rgba(223, 230, 240, 0.78);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(248, 251, 255, 0.74)),
    rgba(255, 255, 255, 0.72);
}

.mockup-evidence-panel strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 20px;
}

.trust-bars {
  display: grid;
  gap: 8px;
}

.trust-bars i {
  display: block;
  height: 9px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--pm-primary), var(--pm-teal));
}

.trust-bars i:nth-child(2) {
  width: 78%;
  opacity: 0.7;
}

.trust-bars i:nth-child(3) {
  width: 52%;
  opacity: 0.42;
}

.mockup-evidence-list {
  display: grid;
  gap: 8px;
}

.mockup-evidence-list p {
  margin: 0 0 4px;
  color: #667085;
  font-size: 12px;
  font-weight: 900;
}

.mockup-evidence-list code {
  display: block;
  padding: 9px 10px;
  overflow-wrap: anywhere;
  border-radius: 8px;
  background: #111827;
  color: #e5edf7;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
}

.landing-content {
  position: relative;
  margin-top: clamp(0px, calc(100svh - 804px), 280px);
}

.landing-content::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.42) 18%, transparent 70%);
  content: "";
}

.showcase-mini-visual,
.bento-mini-visual {
  display: grid;
  gap: 7px;
  margin-top: 18px;
}

.showcase-mini-visual i,
.bento-mini-visual i {
  display: block;
  height: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(31, 111, 235, 0.2), rgba(20, 184, 166, 0.14));
}

.showcase-mini-visual i:nth-child(2),
.bento-mini-visual i:nth-child(2) {
  width: 78%;
}

.showcase-mini-visual i:nth-child(3),
.bento-mini-visual i:nth-child(3) {
  width: 52%;
}

.bento-card h3,
.flow-card h3,
.story-card h3 {
  margin: 14px 0 8px;
  color: #111827;
  font-size: 18px;
  line-height: 1.35;
}

.bento-card p,
.flow-card p,
.story-card p {
  margin: 0;
  color: #667085;
  line-height: 1.75;
}

.bento-card {
  grid-column: span 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
}

.bento-card.pm-bento-card-large {
  grid-column: span 3;
}

.bento-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.12), rgba(20, 184, 166, 0.12));
  color: var(--pm-primary);
}

.bento-card > span {
  color: #245089;
  font-size: 12px;
  font-weight: 900;
}

.story-section {
  padding-top: 72px;
}

.story-list {
  display: grid;
  gap: 14px;
  max-width: 980px;
  margin: 0 auto;
}

.story-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  align-items: flex-start;
  padding: 22px;
}

.story-card > span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 13px;
  font-weight: 900;
}

.story-card h3 {
  margin-top: 0;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.flow-card {
  min-width: 0;
  min-height: 210px;
  padding: 20px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 14px 32px rgba(28, 43, 68, 0.06);
}

.flow-card :deep(.el-icon) {
  color: var(--pm-primary);
}

.flow-index {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  margin-bottom: 12px;
  border-radius: 8px;
  background: #162033;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.comparison-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.comparison-card,
.audience-card {
  padding: 22px;
}

.comparison-card > span {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(20, 184, 166, 0.12);
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
}

.comparison-card--ai > span {
  background: rgba(31, 111, 235, 0.12);
  color: #245089;
}

.comparison-card h3,
.audience-card h3 {
  margin: 16px 0 8px;
  color: var(--pm-ink);
}

.comparison-card p,
.audience-card p {
  margin: 0;
  color: var(--pm-muted);
  line-height: 1.75;
}

.comparison-card ul {
  display: grid;
  gap: 10px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.comparison-card li {
  position: relative;
  padding-left: 20px;
  color: #344054;
  line-height: 1.6;
}

.comparison-card li::before {
  position: absolute;
  top: 0.55em;
  left: 0;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--pm-teal);
  content: "";
}

.comparison-card--ai li::before {
  background: var(--pm-primary);
}

.audience-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.audience-card > span {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

.boundary-section {
  padding-top: 44px;
}

.boundary-card {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(320px, 1.1fr);
  gap: 28px;
  padding: clamp(22px, 4vw, 34px);
}

.boundary-card .pm-section-heading {
  margin-bottom: 0;
}

.trial-glass-copy {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(214, 224, 236, 0.72);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.58);
  color: #475467;
  font-size: 14px;
  line-height: 1.75;
  box-shadow: 0 14px 34px rgba(28, 43, 68, 0.06);
  backdrop-filter: blur(14px);
}

.boundary-list {
  display: grid;
  gap: 12px;
}

.boundary-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid rgba(214, 224, 236, 0.72);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.62);
  color: #344054;
  line-height: 1.65;
}

.boundary-item :deep(.el-icon) {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #64748b;
}

@media (max-width: 1060px) {
  .landing-hero {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .hero-copy h1 {
    max-width: 860px;
  }

  .landing-mockup {
    min-height: auto;
  }

  .mockup-body {
    grid-template-columns: 136px minmax(0, 1fr);
  }

  .mockup-evidence-panel {
    display: none;
  }
}

@media (max-width: 980px) {
  .flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audience-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .boundary-card {
    grid-template-columns: 1fr;
  }

  .bento-card,
  .bento-card.pm-bento-card-large {
    grid-column: span 1;
  }
}

@media (max-width: 700px) {
  .landing-nav {
    position: sticky;
    top: 10px;
    left: auto;
    width: calc(100% - 24px);
    margin: 10px auto 0;
    align-items: center;
    flex-direction: row;
    gap: 8px;
    padding: 8px;
    transform: none;
  }

  .landing-brand {
    gap: 0;
  }

  .landing-brand strong {
    display: none;
  }

  .landing-brand span {
    width: 32px;
    height: 32px;
  }

  .nav-actions {
    justify-content: flex-end;
    flex: 1 1 auto;
    flex-wrap: nowrap;
    gap: 3px;
    min-width: 0;
    width: auto;
  }

  .nav-actions :deep(.el-button) {
    min-width: 0;
    padding: 7px 8px;
  }

  .nav-actions :deep(.nav-language) {
    flex: 0 0 auto;
    width: auto !important;
    padding: 3px;
  }

  .nav-actions :deep(.nav-language button) {
    flex: 0 0 auto;
    min-width: 0 !important;
    padding: 8px 8px !important;
  }

  :global(.landing-nav .language-switch) {
    width: auto !important;
    padding: 3px !important;
  }

  :global(.landing-nav .language-switch button) {
    flex: 0 0 auto !important;
    min-width: 0 !important;
    padding: 8px 8px !important;
  }

  .nav-feedback {
    width: 34px;
    padding-right: 7px !important;
    padding-left: 7px !important;
  }

  .nav-feedback :deep(span) {
    font-size: 0;
  }

  .nav-feedback :deep(.el-icon) {
    margin-right: 0;
    font-size: 16px;
  }

  .nav-start {
    width: 34px;
    padding-right: 7px !important;
    padding-left: 7px !important;
  }

  .nav-start :deep(span) {
    font-size: 0;
  }

  .nav-start :deep(.el-icon) {
    margin-right: 0;
    font-size: 16px;
  }

  .landing-hero {
    padding-top: 44px;
    padding-right: 16px;
    padding-left: 16px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .mockup-body {
    grid-template-columns: 1fr;
  }

  .mockup-project-list {
    flex-direction: row;
    align-items: center;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid rgba(223, 230, 240, 0.78);
  }

  .mockup-brand-mini {
    margin-bottom: 0;
  }

  .mockup-centerpiece {
    grid-template-columns: 1fr;
  }

  .mockup-qa-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .flow-grid {
    grid-template-columns: 1fr;
  }

  .comparison-grid,
  .audience-grid {
    grid-template-columns: 1fr;
  }
}

/* V4.4-0.3: keep the landing hero in a stable document-flow two-column layout. */
.landing-page {
  overflow-x: clip;
}

.landing-hero {
  width: min(1280px, calc(100% - 48px));
  min-height: clamp(660px, 84svh, 760px);
  grid-template-columns: minmax(0, 1fr) minmax(420px, 560px);
  gap: 56px;
  padding: 96px 0 56px;
  box-sizing: border-box;
}

.hero-copy {
  position: relative;
  z-index: 2;
  max-width: 680px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  margin: 0 0 18px;
  padding: 7px 12px;
  border: 1px solid rgba(31, 111, 235, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #245089;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.35;
  overflow-wrap: anywhere;
  box-shadow: 0 12px 28px rgba(28, 43, 68, 0.06);
  backdrop-filter: blur(14px);
}

.hero-copy h1 {
  max-width: 680px;
  margin-bottom: 22px;
  font-size: clamp(52px, 6.4vw, 88px);
  line-height: 1.02;
  overflow: visible;
}

.hero-copy h1 span {
  min-width: 0;
}

.hero-copy h1 > span:first-child {
  margin-bottom: 14px;
  font-size: clamp(30px, 3.2vw, 44px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.hero-copy h1 > span:last-child {
  font-size: clamp(46px, 5.2vw, 72px);
  line-height: 1.08;
  white-space: pre-line;
}

.landing-page--en .hero-copy h1 > span:last-child {
  font-size: clamp(34px, 2.7vw, 38px);
  line-height: 1.08;
}

.hero-subtitle {
  max-width: 610px;
  font-size: clamp(17px, 1.7vw, 20px);
  line-height: 1.68;
}

.hero-actions {
  margin-top: 28px;
}

.hero-proof-row {
  max-width: 620px;
  margin-top: 22px;
}

.hero-visual {
  position: relative;
  z-index: 1;
  justify-self: end;
  width: 100%;
  max-width: 560px;
  min-width: 0;
}

.landing-mockup,
.landing-mockup.pm-floating-mockup {
  width: 100%;
  max-width: 560px;
  min-height: 0;
  transform: none;
  animation: none;
}

.mockup-body {
  display: block;
  min-height: 0;
  padding: 24px;
}

.mockup-stage {
  display: grid;
  gap: 18px;
  padding: 0;
}

.mockup-stage-head {
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.mockup-stage-head > div {
  min-width: 0;
}

.mockup-stage-head strong {
  max-width: 100%;
  font-size: clamp(18px, 2vw, 22px);
  line-height: 1.25;
  overflow-wrap: normal;
}

.mockup-summary {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 14px;
}

.mockup-score-card,
.mockup-evidence-card {
  min-width: 0;
  min-height: 178px;
  padding: 20px;
  border: 1px solid rgba(214, 224, 236, 0.82);
  border-radius: 8px;
  background:
    radial-gradient(circle at 24% 10%, rgba(31, 111, 235, 0.12), transparent 34%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(248, 251, 255, 0.72));
  box-shadow: 0 18px 46px rgba(28, 43, 68, 0.08);
}

.mockup-score-card {
  display: grid;
  align-content: center;
  justify-items: start;
}

.mockup-score-card span,
.mockup-score-card small,
.mockup-evidence-card span,
.mockup-file-row span {
  color: #667085;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.35;
}

.mockup-score-card strong {
  margin: 10px 0 6px;
  color: #111827;
  font-size: clamp(58px, 5vw, 76px);
  line-height: 0.95;
}

.mockup-evidence-card {
  display: grid;
  align-content: center;
  gap: 14px;
}

.mockup-evidence-card strong {
  color: #111827;
  font-size: clamp(24px, 2.5vw, 32px);
  line-height: 1.1;
  overflow-wrap: normal;
}

.trust-bars {
  max-width: 220px;
}

.mockup-qa-bar {
  align-items: center;
  min-width: 0;
  padding: 15px 16px;
}

.mockup-qa-bar span,
.mockup-qa-bar strong {
  min-width: 0;
  line-height: 1.45;
  overflow-wrap: normal;
  word-break: normal;
}

.mockup-file-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.mockup-file-row code {
  display: inline-flex;
  max-width: 100%;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #111827;
  color: #e5edf7;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .landing-hero {
    width: min(860px, calc(100% - 40px));
    min-height: auto;
    grid-template-columns: 1fr;
    gap: 40px;
    padding: 112px 0 60px;
  }

  .hero-copy,
  .hero-copy h1,
  .hero-subtitle,
  .hero-proof-row {
    max-width: 100%;
  }

  .hero-visual,
  .landing-mockup {
    justify-self: stretch;
    max-width: 100%;
  }
}

@media (max-width: 640px) {
  .landing-nav {
    width: calc(100% - 20px);
  }

  .nav-feedback,
  .nav-donate {
    width: 34px;
    padding-right: 7px !important;
    padding-left: 7px !important;
  }

  .nav-feedback :deep(span),
  .nav-donate :deep(span) {
    font-size: 0;
  }

  .nav-feedback :deep(.el-icon),
  .nav-donate :deep(.el-icon) {
    margin-right: 0;
    font-size: 16px;
  }

  .nav-login {
    padding-right: 6px !important;
    padding-left: 6px !important;
  }

  .landing-hero {
    width: min(520px, calc(100% - 28px));
    gap: 30px;
    padding: 48px 0 44px;
  }

  .hero-copy h1 {
    font-size: clamp(30px, 8vw, 34px);
    line-height: 1.04;
  }

  .hero-copy h1 span {
    display: block;
    white-space: normal;
    overflow-wrap: anywhere;
    word-break: break-all;
    line-break: anywhere;
  }

  .hero-copy h1 > span:last-child,
  .landing-page--en .hero-copy h1 > span:last-child {
    font-size: clamp(30px, 8vw, 34px);
    white-space: pre-line;
  }

  .hero-copy h1 > span:first-child {
    margin-bottom: 10px;
    font-size: clamp(24px, 7vw, 30px);
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .mockup-body {
    padding: 16px;
  }

  .mockup-stage {
    gap: 14px;
  }

  .mockup-stage-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .mockup-summary {
    grid-template-columns: 1fr;
  }

  .mockup-score-card,
  .mockup-evidence-card {
    min-height: 132px;
    padding: 16px;
  }

  .mockup-score-card strong {
    font-size: 56px;
  }

  .mockup-evidence-card strong {
    font-size: 24px;
  }

  .mockup-qa-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
