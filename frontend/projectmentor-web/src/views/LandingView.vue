<template>
  <div ref="landingRoot" class="landing-page pm-premium-bg">
    <nav class="landing-nav">
      <RouterLink class="landing-brand" to="/">
        <span>PM</span>
        <strong>ProjectMentor AI</strong>
      </RouterLink>
      <div class="nav-actions">
        <LanguageSwitch />
        <el-button text :icon="Message" @click="feedbackVisible = true">{{ t('common.feedback') }}</el-button>
        <el-button text :icon="Coffee" @click="donateVisible = true">{{ t('common.donate') }}</el-button>
        <el-button text @click="router.push('/login')">{{ t('common.login') }}</el-button>
        <el-button type="primary" :icon="ArrowRight" @click="goStart">{{ t('common.startNow') }}</el-button>
      </div>
    </nav>

    <section class="landing-hero">
      <div class="hero-copy pm-fade-up" style="--reveal-index: 0">
        <p class="eyebrow">{{ t('landing.heroEyebrow') }}</p>
        <h1>
          <span>ProjectMentor AI</span>
          <span class="pm-gradient-text">{{ t('landing.heroHeadline') }}</span>
        </h1>
        <p class="hero-subtitle">{{ t('landing.subtitle') }}</p>

        <div class="hero-actions">
          <el-button size="large" type="primary" :icon="ArrowRight" @click="goStart">{{ t('common.startNow') }}</el-button>
          <el-button size="large" :icon="Search" @click="scrollToFeatures">{{ t('landing.ctaFeatures') }}</el-button>
          <el-button size="large" :icon="Message" @click="feedbackVisible = true">{{ t('common.feedback') }}</el-button>
        </div>

        <div class="pm-chip-row hero-proof-row">
          <span v-for="item in heroProofs" :key="item" class="pm-chip">
            <el-icon><Check /></el-icon>
            {{ item }}
          </span>
        </div>

        <el-alert
          class="trial-alert pm-premium-alert"
          :title="t('landing.trialTitle')"
          type="warning"
          show-icon
          :closable="false"
          :description="t('landing.trialDesc')"
        />
      </div>

      <div class="hero-visual pm-fade-up" style="--reveal-index: 1">
        <div class="pm-mockup-frame landing-mockup pm-float">
          <div class="pm-mockup-topbar">
            <i class="pm-mockup-dot" />
            <i class="pm-mockup-dot" />
            <i class="pm-mockup-dot" />
            <span>{{ t('landing.mockup.windowTitle') }}</span>
          </div>
          <div class="mockup-body">
            <aside class="mockup-sidebar">
              <div class="mockup-brand-mini">PMAI</div>
              <span v-for="item in mockupNav" :key="item">{{ item }}</span>
            </aside>
            <main class="mockup-main">
              <div class="mockup-dashboard-head">
                <div>
                  <small>{{ t('landing.mockup.auditLabel') }}</small>
                  <strong>{{ t('landing.mockup.projectTitle') }}</strong>
                </div>
                <el-tag type="success" effect="light">{{ t('landing.mockup.evidenceStatus') }}</el-tag>
              </div>

              <div class="mockup-score-row">
                <div>
                  <span>{{ t('landing.mockup.score') }}</span>
                  <strong>82</strong>
                </div>
                <div>
                  <span>{{ t('landing.mockup.evidence') }}</span>
                  <strong>14</strong>
                </div>
                <div>
                  <span>{{ t('landing.mockup.qa') }}</span>
                  <strong>8</strong>
                </div>
              </div>

              <div class="mockup-insight-grid">
                <article>
                  <el-icon><DocumentChecked /></el-icon>
                  <span>{{ t('landing.mockup.auditCard') }}</span>
                  <p>{{ t('landing.mockup.auditCardDesc') }}</p>
                </article>
                <article>
                  <el-icon><ChatDotRound /></el-icon>
                  <span>{{ t('landing.mockup.qaCard') }}</span>
                  <p>{{ t('landing.mockup.qaCardDesc') }}</p>
                </article>
              </div>

              <div class="mockup-evidence">
                <div>
                  <span>{{ t('landing.mockup.reason') }}</span>
                  <p>{{ t('landing.mockup.reasonText') }}</p>
                </div>
                <code>src/api/projectQa.ts</code>
              </div>
            </main>
          </div>
        </div>
      </div>
    </section>

    <main class="landing-content">
      <section class="pm-section-shell product-showcase pm-fade-up" style="--reveal-index: 0">
        <div class="pm-section-heading center">
          <p class="eyebrow">{{ t('landing.showcaseEyebrow') }}</p>
          <h2>{{ t('landing.showcaseTitle') }}</h2>
          <p>{{ t('landing.showcaseDesc') }}</p>
        </div>

        <div class="showcase-grid">
          <article v-for="(panel, index) in productPanels" :key="panel.title" class="showcase-card pm-glass-card pm-hover-lift" :style="{ '--reveal-index': index }">
            <el-icon :size="22">
              <component :is="panel.icon" />
            </el-icon>
            <h3>{{ panel.title }}</h3>
            <p>{{ panel.description }}</p>
          </article>
        </div>
      </section>

      <section id="features" class="pm-section-shell bento-section pm-fade-up" style="--reveal-index: 0">
        <div class="pm-section-heading">
          <p class="eyebrow">{{ t('landing.bentoEyebrow') }}</p>
          <h2>{{ t('landing.bentoTitle') }}</h2>
          <p>{{ t('landing.bentoDesc') }}</p>
        </div>

        <div class="pm-bento-grid">
          <article
            v-for="(feature, index) in bentoFeatures"
            :key="feature.title"
            class="pm-bento-card bento-card pm-hover-lift pm-fade-up"
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
            <span>{{ feature.meta }}</span>
          </article>
        </div>
      </section>

      <section class="pm-section-shell story-section">
        <div class="pm-section-heading center pm-fade-up">
          <p class="eyebrow">{{ t('landing.storyEyebrow') }}</p>
          <h2>{{ t('landing.storyTitle') }}</h2>
          <p>{{ t('landing.storyDesc') }}</p>
        </div>

        <div class="story-list">
          <article
            v-for="(story, index) in storySections"
            :key="story.title"
            class="story-card pm-fade-up"
            :style="{ '--reveal-index': index }"
          >
            <span>{{ index + 1 }}</span>
            <div>
              <h3>{{ story.title }}</h3>
              <p>{{ story.description }}</p>
            </div>
          </article>
        </div>
      </section>

      <section class="pm-section-shell workflow-section pm-fade-up">
        <div class="pm-section-heading">
          <p class="eyebrow">{{ t('landing.workflowEyebrow') }}</p>
          <h2>{{ t('landing.workflowTitle') }}</h2>
        </div>
        <div class="flow-grid">
          <article v-for="(step, index) in workflow" :key="step.title" class="flow-card pm-hover-lift pm-fade-up" :style="{ '--reveal-index': index }">
            <span class="flow-index">{{ index + 1 }}</span>
            <el-icon :size="24">
              <component :is="step.icon" />
            </el-icon>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </section>

      <section class="pm-section-shell boundary-section pm-fade-up">
        <div class="boundary-card pm-glass-card">
          <div class="pm-section-heading">
            <p class="eyebrow">{{ t('landing.boundaryEyebrow') }}</p>
            <h2>{{ t('landing.boundaryTitle') }}</h2>
            <p>{{ t('landing.boundaryDesc') }}</p>
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

    <DonateDialog v-model="donateVisible" />
    <FeedbackDialog v-model="feedbackVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  ChatDotRound,
  Check,
  Coffee,
  DocumentChecked,
  Files,
  FolderAdd,
  MagicStick,
  Message,
  Search,
  UploadFilled,
  UserFilled,
  WarningFilled
} from '@element-plus/icons-vue'

import DonateDialog from '@/components/DonateDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const donateVisible = ref(false)
const feedbackVisible = ref(false)
const landingRoot = ref<HTMLElement | null>(null)
let revealObserver: IntersectionObserver | undefined

const heroProofs = computed(() => [
  t('landing.capabilities.audit'),
  t('landing.capabilities.qa'),
  t('landing.capabilities.interview'),
  t('landing.capabilities.share')
])

const mockupNav = computed(() => [
  t('landing.mockup.navAudit'),
  t('landing.mockup.navQa'),
  t('landing.mockup.navAdmin')
])

const productPanels = computed(() => [
  {
    title: t('landing.productPanels.audit.title'),
    description: t('landing.productPanels.audit.description'),
    icon: DocumentChecked
  },
  {
    title: t('landing.productPanels.qa.title'),
    description: t('landing.productPanels.qa.description'),
    icon: ChatDotRound
  },
  {
    title: t('landing.productPanels.admin.title'),
    description: t('landing.productPanels.admin.description'),
    icon: UserFilled
  }
])

const bentoFeatures = computed(() => [
  {
    title: t('landing.bento.audit.title'),
    description: t('landing.bento.audit.description'),
    meta: t('landing.bento.audit.meta'),
    icon: DocumentChecked,
    className: 'is-wide'
  },
  {
    title: t('landing.bento.scan.title'),
    description: t('landing.bento.scan.description'),
    meta: t('landing.bento.scan.meta'),
    icon: Search,
    className: ''
  },
  {
    title: t('landing.bento.qa.title'),
    description: t('landing.bento.qa.description'),
    meta: t('landing.bento.qa.meta'),
    icon: ChatDotRound,
    className: ''
  },
  {
    title: t('landing.bento.interview.title'),
    description: t('landing.bento.interview.description'),
    meta: t('landing.bento.interview.meta'),
    icon: MagicStick,
    className: ''
  },
  {
    title: t('landing.bento.report.title'),
    description: t('landing.bento.report.description'),
    meta: t('landing.bento.report.meta'),
    icon: Files,
    className: 'is-wide'
  },
  {
    title: t('landing.bento.admin.title'),
    description: t('landing.bento.admin.description'),
    meta: t('landing.bento.admin.meta'),
    icon: Message,
    className: ''
  }
])

const storySections = computed(() => [
  {
    title: t('landing.story.why.title'),
    description: t('landing.story.why.description')
  },
  {
    title: t('landing.story.audit.title'),
    description: t('landing.story.audit.description')
  },
  {
    title: t('landing.story.interview.title'),
    description: t('landing.story.interview.description')
  },
  {
    title: t('landing.story.beta.title'),
    description: t('landing.story.beta.description')
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

function scrollToFeatures() {
  const behavior: ScrollBehavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  landingRoot.value?.querySelector('#features')?.scrollIntoView({ behavior, block: 'start' })
}

function setupScrollReveal() {
  const root = landingRoot.value
  if (!root) {
    return
  }

  const revealItems = Array.from(root.querySelectorAll<HTMLElement>('.pm-fade-up'))
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
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px clamp(18px, 5vw, 72px);
  border-bottom: 1px solid rgba(223, 230, 240, 0.62);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 34px rgba(28, 43, 68, 0.05);
  backdrop-filter: blur(22px);
}

.landing-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.landing-brand span {
  display: grid;
  width: 34px;
  height: 34px;
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
  gap: 8px;
  flex-wrap: wrap;
}

.landing-hero {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(0, 0.95fr) minmax(420px, 1.05fr);
  align-items: center;
  gap: clamp(32px, 6vw, 84px);
  max-width: 1320px;
  margin: 0 auto;
  padding: 128px clamp(18px, 5vw, 72px) 68px;
}

.hero-copy h1 {
  display: grid;
  gap: 8px;
  max-width: 760px;
  margin: 14px 0 20px;
  color: #111827;
  font-size: clamp(46px, 7vw, 88px);
  line-height: 0.98;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 720px;
  margin: 0;
  color: #475467;
  font-size: clamp(18px, 2.2vw, 22px);
  line-height: 1.72;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.hero-proof-row {
  margin-top: 24px;
}

.trial-alert {
  max-width: 720px;
  margin-top: 24px;
}

.hero-visual {
  min-width: 0;
}

.landing-mockup {
  position: relative;
  min-height: 560px;
}

.pm-mockup-topbar span {
  margin-left: 8px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.mockup-body {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  min-height: 522px;
}

.mockup-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 14px;
  border-right: 1px solid rgba(223, 230, 240, 0.78);
  background: rgba(248, 251, 255, 0.78);
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

.mockup-sidebar span {
  padding: 9px 10px;
  border-radius: 8px;
  color: #475467;
  font-size: 12px;
  font-weight: 800;
}

.mockup-sidebar span:first-of-type {
  background: rgba(31, 111, 235, 0.1);
  color: var(--pm-primary);
}

.mockup-main {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 20px;
}

.mockup-dashboard-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.mockup-dashboard-head small,
.mockup-score-row span,
.mockup-evidence span {
  display: block;
  color: #667085;
  font-size: 12px;
  font-weight: 800;
}

.mockup-dashboard-head strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 20px;
}

.mockup-score-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.mockup-score-row div,
.mockup-insight-grid article,
.mockup-evidence {
  border: 1px solid rgba(223, 230, 240, 0.84);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 12px 28px rgba(28, 43, 68, 0.06);
}

.mockup-score-row div {
  padding: 14px;
}

.mockup-score-row strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.mockup-insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mockup-insight-grid article {
  padding: 16px;
}

.mockup-insight-grid :deep(.el-icon) {
  color: var(--pm-primary);
}

.mockup-insight-grid span {
  display: block;
  margin-top: 12px;
  color: #111827;
  font-weight: 900;
}

.mockup-insight-grid p,
.mockup-evidence p {
  margin: 8px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
}

.mockup-evidence {
  display: grid;
  gap: 12px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgba(238, 246, 255, 0.74), rgba(240, 251, 249, 0.56)),
    rgba(255, 255, 255, 0.8);
}

.mockup-evidence code {
  display: block;
  padding: 10px 12px;
  overflow-wrap: anywhere;
  border-radius: 8px;
  background: #111827;
  color: #e5edf7;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
}

.landing-content {
  position: relative;
}

.product-showcase {
  padding-top: 44px;
}

.showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.showcase-card {
  min-height: 210px;
  padding: 22px;
}

.showcase-card :deep(.el-icon) {
  color: var(--pm-primary);
}

.showcase-card h3,
.bento-card h3,
.flow-card h3,
.story-card h3 {
  margin: 14px 0 8px;
  color: #111827;
  font-size: 18px;
  line-height: 1.35;
}

.showcase-card p,
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

.bento-card.is-wide {
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
  gap: 12px;
  max-width: 920px;
  margin: 0 auto;
}

.story-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  align-items: flex-start;
  padding: 22px;
  border: 1px solid rgba(214, 224, 236, 0.84);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 16px 38px rgba(28, 43, 68, 0.06);
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

.boundary-list {
  display: grid;
  gap: 12px;
}

.boundary-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid rgba(245, 158, 11, 0.24);
  border-radius: 8px;
  background: rgba(255, 251, 235, 0.76);
  color: #344054;
  line-height: 1.65;
}

.boundary-item :deep(.el-icon) {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #f59e0b;
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
}

@media (max-width: 980px) {
  .showcase-grid,
  .flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .boundary-card {
    grid-template-columns: 1fr;
  }

  .bento-card,
  .bento-card.is-wide {
    grid-column: span 1;
  }
}

@media (max-width: 700px) {
  .landing-nav {
    position: static;
    align-items: flex-start;
    flex-direction: column;
  }

  .nav-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .landing-hero {
    padding-top: 54px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .mockup-body {
    grid-template-columns: 1fr;
  }

  .mockup-sidebar {
    flex-direction: row;
    align-items: center;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid rgba(223, 230, 240, 0.78);
  }

  .mockup-brand-mini {
    margin-bottom: 0;
  }

  .mockup-score-row,
  .mockup-insight-grid,
  .showcase-grid,
  .flow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
