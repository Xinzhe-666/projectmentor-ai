<template>
  <div ref="landingRoot" class="landing-page pm-aurora-bg">
    <div class="pm-grid-overlay" />
    <div class="pm-noise-overlay" />
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

    <section class="landing-hero pm-cinematic-hero">
      <div class="hero-copy pm-scroll-reveal" style="--reveal-index: 0">
        <p class="eyebrow">{{ t('landing.heroEyebrow') }}</p>
        <h1>
          <span>ProjectMentor AI</span>
          <span class="pm-gradient-text">{{ t('landing.heroHeadline') }}</span>
        </h1>
        <p class="hero-subtitle">{{ t('landing.subtitle') }}</p>
        <p class="hero-subnote">{{ t('landing.heroSubnote') }}</p>

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

      <div class="hero-visual pm-scroll-reveal" style="--reveal-index: 1">
        <div class="pm-product-window pm-gradient-border landing-mockup pm-floating-mockup">
          <div class="pm-mockup-topbar">
            <i class="pm-mockup-dot" />
            <i class="pm-mockup-dot" />
            <i class="pm-mockup-dot" />
            <span>{{ t('landing.mockup.windowTitle') }}</span>
          </div>
          <div class="mockup-body cinematic-mockup-body">
            <aside class="mockup-sidebar">
              <div class="mockup-brand-mini">PMAI</div>
              <button v-for="(item, index) in mockupNav" :key="item" :class="{ active: index === 0 }" type="button">
                <span>{{ item }}</span>
                <small v-if="index === 0">82</small>
              </button>
            </aside>
            <main class="mockup-main">
              <div class="mockup-dashboard-head">
                <div>
                  <small>{{ t('landing.mockup.auditLabel') }}</small>
                  <strong>{{ t('landing.mockup.projectTitle') }}</strong>
                </div>
                <span class="pm-status-chip">{{ t('landing.mockup.evidenceStatus') }}</span>
              </div>

              <div class="mockup-command-line">
                <span>{{ t('landing.mockup.command') }}</span>
                <strong>{{ t('landing.mockup.commandText') }}</strong>
              </div>

              <div class="mockup-score-layout">
                <div class="mockup-score-orb">
                  <span>{{ t('landing.mockup.score') }}</span>
                  <strong>82</strong>
                  <small>{{ t('landing.mockup.scoreNote') }}</small>
                </div>
                <div class="mockup-trust-panel">
                  <div>
                    <span>{{ t('landing.mockup.evidenceTrust') }}</span>
                    <strong>{{ t('landing.mockup.strongEvidence') }}</strong>
                  </div>
                  <div class="trust-bars" aria-hidden="true">
                    <i />
                    <i />
                    <i />
                  </div>
                </div>
              </div>

              <div class="mockup-score-row">
                <div class="pm-premium-card">
                  <span>{{ t('landing.mockup.score') }}</span>
                  <strong>82</strong>
                </div>
                <div class="pm-premium-card">
                  <span>{{ t('landing.mockup.evidence') }}</span>
                  <strong>14</strong>
                </div>
                <div class="pm-premium-card">
                  <span>{{ t('landing.mockup.qa') }}</span>
                  <strong>8</strong>
                </div>
              </div>

              <div class="mockup-insight-grid">
                <article class="pm-premium-card">
                  <el-icon><DocumentChecked /></el-icon>
                  <span>{{ t('landing.mockup.auditCard') }}</span>
                  <p>{{ t('landing.mockup.auditCardDesc') }}</p>
                </article>
                <article class="pm-premium-card">
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

              <div class="mockup-bottom-command">
                <span>{{ t('landing.mockup.askPlaceholder') }}</span>
                <strong>{{ t('landing.mockup.shareReady') }}</strong>
              </div>
            </main>
          </div>
        </div>
      </div>
    </section>

    <main class="landing-content">
      <section class="pm-section-shell pm-cinematic-section product-showcase pm-scroll-reveal" style="--reveal-index: 0">
        <div class="pm-section-heading center">
          <p class="eyebrow">{{ t('landing.showcaseEyebrow') }}</p>
          <h2>{{ t('landing.showcaseTitle') }}</h2>
          <p>{{ t('landing.showcaseDesc') }}</p>
        </div>

        <div class="showcase-grid">
          <article v-for="(panel, index) in productPanels" :key="panel.title" class="showcase-card pm-premium-card pm-hover-lift" :style="{ '--reveal-index': index }">
            <el-icon :size="22">
              <component :is="panel.icon" />
            </el-icon>
            <div class="showcase-mini-visual">
              <i />
              <i />
              <i />
            </div>
            <h3>{{ panel.title }}</h3>
            <p>{{ panel.description }}</p>
          </article>
        </div>
      </section>

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

      <section class="pm-section-shell pm-cinematic-section story-section">
        <div class="pm-section-heading center pm-scroll-reveal">
          <p class="eyebrow">{{ t('landing.storyEyebrow') }}</p>
          <h2>{{ t('landing.storyTitle') }}</h2>
          <p>{{ t('landing.storyDesc') }}</p>
        </div>

        <div class="story-list">
          <article
            v-for="(story, index) in storySections"
            :key="story.title"
            class="story-card pm-premium-card pm-scroll-reveal"
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

      <section class="pm-section-shell pm-cinematic-section boundary-section pm-scroll-reveal">
        <div class="boundary-card pm-premium-card">
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
    className: 'pm-bento-card-large pm-bento-card-accent'
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
    className: 'pm-bento-card-large'
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
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px clamp(18px, 5vw, 72px);
  border-bottom: 1px solid rgba(223, 230, 240, 0.62);
  background: rgba(255, 255, 255, 0.66);
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
  min-height: 104vh;
  grid-template-columns: minmax(0, 0.92fr) minmax(520px, 1.08fr);
  align-items: center;
  gap: clamp(38px, 7vw, 96px);
  max-width: 1420px;
  margin: 0 auto;
  padding: 132px clamp(18px, 5vw, 72px) 86px;
}

.hero-copy h1 {
  display: grid;
  gap: 8px;
  max-width: 820px;
  margin: 14px 0 20px;
  color: #111827;
  font-size: clamp(52px, 7.8vw, 104px);
  line-height: 0.94;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 760px;
  margin: 0;
  color: #475467;
  font-size: clamp(18px, 2.2vw, 22px);
  line-height: 1.72;
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
  min-height: 630px;
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
  grid-template-columns: 150px minmax(0, 1fr);
  min-height: 592px;
}

.mockup-sidebar {
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

.mockup-sidebar button {
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

.mockup-sidebar button.active {
  background: rgba(31, 111, 235, 0.1);
  color: var(--pm-primary);
}

.mockup-sidebar button small {
  display: grid;
  min-width: 28px;
  height: 22px;
  place-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #245089;
  font-weight: 900;
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

.mockup-command-line,
.mockup-bottom-command {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(31, 111, 235, 0.14);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(238, 246, 255, 0.72), rgba(255, 255, 255, 0.72)),
    rgba(255, 255, 255, 0.74);
}

.mockup-command-line span,
.mockup-bottom-command span {
  color: #667085;
  font-size: 12px;
  font-weight: 900;
}

.mockup-command-line strong,
.mockup-bottom-command strong {
  min-width: 0;
  color: #111827;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.mockup-score-layout {
  display: grid;
  grid-template-columns: minmax(160px, 0.8fr) minmax(0, 1.2fr);
  gap: 12px;
}

.mockup-score-orb,
.mockup-trust-panel {
  border: 1px solid rgba(214, 224, 236, 0.78);
  border-radius: 8px;
  background:
    radial-gradient(circle at 50% 0%, rgba(31, 111, 235, 0.12), transparent 36%),
    rgba(255, 255, 255, 0.78);
  box-shadow: 0 14px 34px rgba(28, 43, 68, 0.07);
}

.mockup-score-orb {
  display: grid;
  min-height: 150px;
  place-items: center;
  padding: 16px;
  text-align: center;
}

.mockup-score-orb span,
.mockup-score-orb small,
.mockup-trust-panel span {
  color: #667085;
  font-size: 12px;
  font-weight: 900;
}

.mockup-score-orb strong {
  color: #111827;
  font-size: 58px;
  line-height: 0.94;
}

.mockup-trust-panel {
  display: grid;
  align-content: space-between;
  gap: 18px;
  min-height: 150px;
  padding: 16px;
}

.mockup-trust-panel strong {
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

.landing-content::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.42) 18%, transparent 70%);
  content: "";
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
  .bento-card.pm-bento-card-large {
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

  .mockup-score-layout {
    grid-template-columns: 1fr;
  }

  .mockup-command-line,
  .mockup-bottom-command {
    align-items: flex-start;
    flex-direction: column;
  }

  .mockup-score-row,
  .mockup-insight-grid,
  .showcase-grid,
  .flow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
