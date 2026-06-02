<template>
  <div class="landing-page">
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
      <img class="hero-image" src="@/assets/landing-hero.png" alt="ProjectMentor AI 项目审计工作台" />
      <div class="hero-overlay" />
      <div class="hero-content">
        <p class="eyebrow">{{ t('landing.heroEyebrow') }}</p>
        <h1>ProjectMentor AI</h1>
        <p class="hero-subtitle">
          {{ t('landing.subtitle') }}
        </p>
        <div class="hero-proof-row">
          <span>{{ t('landing.capabilities.audit') }}</span>
          <span>{{ t('landing.capabilities.qa') }}</span>
          <span>{{ t('landing.capabilities.interview') }}</span>
          <span>{{ t('landing.capabilities.share') }}</span>
        </div>
        <div class="hero-actions">
          <el-button size="large" type="primary" :icon="ArrowRight" @click="goStart">{{ t('common.startNow') }}</el-button>
          <el-button size="large" :icon="Message" @click="feedbackVisible = true">{{ t('common.feedback') }}</el-button>
          <el-button size="large" :icon="Coffee" @click="donateVisible = true">{{ t('common.donate') }}</el-button>
        </div>

        <el-alert
          class="trial-alert"
          :title="t('landing.trialTitle')"
          type="warning"
          show-icon
          :closable="false"
          :description="t('landing.trialDesc')"
        />
      </div>
    </section>

    <main class="landing-content">
      <section class="section-block product-position">
        <div class="section-heading">
          <p class="eyebrow">{{ t('landing.positionEyebrow') }}</p>
          <h2>{{ t('landing.positionTitle') }}</h2>
          <p>
            {{ t('landing.positionDesc') }}
          </p>
        </div>
        <div class="position-list">
          <div v-for="point in positionPoints" :key="point" class="position-item">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>{{ point }}</span>
          </div>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <p class="eyebrow">{{ t('landing.audienceEyebrow') }}</p>
          <h2>{{ t('landing.audienceTitle') }}</h2>
        </div>
        <div class="card-grid audience-grid">
          <article v-for="(audience, index) in audiences" :key="audience.title" class="info-card stagger-item" :style="{ '--stagger-index': index }">
            <el-icon :size="24">
              <component :is="audience.icon" />
            </el-icon>
            <h3>{{ audience.title }}</h3>
            <p>{{ audience.description }}</p>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <p class="eyebrow">{{ t('landing.workflowEyebrow') }}</p>
          <h2>{{ t('landing.workflowTitle') }}</h2>
        </div>
        <div class="flow-grid">
          <article v-for="(step, index) in workflow" :key="step.title" class="flow-card stagger-item" :style="{ '--stagger-index': index }">
            <span class="flow-index">{{ index + 1 }}</span>
            <el-icon :size="24">
              <component :is="step.icon" />
            </el-icon>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </article>
        </div>
      </section>

      <section class="section-block capability-section">
        <div class="section-heading">
          <p class="eyebrow">{{ t('landing.capabilityEyebrow') }}</p>
          <h2>{{ t('landing.capabilityTitle') }}</h2>
        </div>
        <div class="capability-grid">
          <div v-for="capability in capabilities" :key="capability" class="capability-pill">
            <el-icon><Check /></el-icon>
            <span>{{ capability }}</span>
          </div>
        </div>
      </section>

      <section class="section-block boundary-section">
        <div class="section-heading">
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
      </section>
    </main>

    <DonateDialog v-model="donateVisible" />
    <FeedbackDialog v-model="feedbackVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  ArrowRight,
  ChatDotRound,
  Check,
  CircleCheckFilled,
  Coffee,
  DocumentChecked,
  EditPen,
  Files,
  FolderAdd,
  MagicStick,
  Message,
  Search,
  UploadFilled,
  UserFilled,
  Warning,
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

const positionPoints = computed(() => [
  t('landing.positionPoints.resume'),
  t('landing.positionPoints.overclaim'),
  t('landing.positionPoints.evidence'),
  t('landing.positionPoints.hallucination'),
  t('landing.positionPoints.ownership')
])

const audiences = computed(() => [
  {
    title: t('landing.audiences.intern.title'),
    description: t('landing.audiences.intern.description'),
    icon: UserFilled
  },
  {
    title: t('landing.audiences.aiBuilder.title'),
    description: t('landing.audiences.aiBuilder.description'),
    icon: MagicStick
  },
  {
    title: t('landing.audiences.resume.title'),
    description: t('landing.audiences.resume.description'),
    icon: EditPen
  },
  {
    title: t('landing.audiences.interview.title'),
    description: t('landing.audiences.interview.description'),
    icon: ChatDotRound
  },
  {
    title: t('landing.audiences.evidence.title'),
    description: t('landing.audiences.evidence.description'),
    icon: Warning
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

const capabilities = computed(() => [
  t('landing.capabilities.audit'),
  t('landing.capabilities.scan'),
  t('landing.capabilities.evidenceReview'),
  t('landing.capabilities.qa'),
  t('landing.capabilities.interview'),
  t('landing.capabilities.share'),
  t('landing.capabilities.hallucination'),
  t('landing.capabilities.zip'),
  t('landing.capabilities.resume')
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
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.08), transparent 34%),
    linear-gradient(225deg, rgba(20, 184, 166, 0.1), transparent 38%),
    #f6f8fb;
  animation: pageFadeIn 420ms ease both;
}

.landing-nav {
  position: fixed;
  z-index: 10;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px clamp(18px, 5vw, 72px);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(223, 230, 240, 0.72);
  box-shadow: 0 10px 30px rgba(28, 43, 68, 0.05);
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
  background: #162033;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.nav-actions {
  align-items: center;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.nav-actions :deep(.language-switch) {
  margin-right: 4px;
}

.landing-hero {
  position: relative;
  min-height: 82vh;
  padding: 152px clamp(18px, 5vw, 72px) 104px;
  overflow: hidden;
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(118deg, rgba(248, 251, 255, 0.99) 0%, rgba(248, 251, 255, 0.93) 39%, rgba(248, 251, 255, 0.58) 64%, rgba(248, 251, 255, 0.32) 100%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.1), rgba(248, 251, 255, 0.98));
}

.hero-overlay::after {
  position: absolute;
  inset: -24%;
  background:
    linear-gradient(115deg, transparent 0%, rgba(31, 111, 235, 0.08) 26%, transparent 50%),
    linear-gradient(295deg, transparent 8%, rgba(20, 184, 166, 0.1) 42%, transparent 68%);
  animation: ambientDrift 16s ease-in-out infinite alternate;
  content: "";
}

.hero-content {
  position: relative;
  max-width: 760px;
  animation: contentRise 520ms ease both;
}

.hero-content h1 {
  margin: 12px 0 18px;
  max-width: 680px;
  color: #111827;
  font-size: clamp(42px, 6vw, 72px);
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 720px;
  margin: 0;
  color: #475467;
  font-size: 20px;
  line-height: 1.75;
}

.hero-proof-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.hero-proof-row span {
  padding: 8px 12px;
  border: 1px solid rgba(31, 111, 235, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  color: #245089;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 8px 22px rgba(31, 111, 235, 0.08);
  backdrop-filter: blur(12px);
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 30px;
  flex-wrap: wrap;
}

.trial-alert {
  max-width: 720px;
  margin-top: 24px;
  border-radius: 8px;
  box-shadow: 0 16px 40px rgba(245, 158, 11, 0.1);
}

.landing-content {
  max-width: 1180px;
  margin: -30px auto 0;
  padding: 0 clamp(18px, 5vw, 72px) 72px;
  position: relative;
}

.section-block {
  margin-top: 20px;
  padding: 26px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.88)),
    rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 40px rgba(28, 43, 68, 0.08);
  backdrop-filter: blur(18px);
}

.product-position {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(320px, 1.05fr);
  gap: 28px;
  align-items: center;
}

.section-heading h2 {
  margin: 8px 0 10px;
  color: #111827;
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: 0;
}

.section-heading p:last-child {
  margin: 0;
  color: #667085;
  line-height: 1.8;
}

.position-list {
  display: grid;
  gap: 12px;
}

.position-item,
.boundary-item,
.capability-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.position-item {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f4f8ff;
  color: #344054;
}

.position-item :deep(.el-icon),
.capability-pill :deep(.el-icon) {
  flex: 0 0 auto;
  color: #14b8a6;
}

.card-grid {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.audience-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.info-card,
.flow-card {
  min-width: 0;
  min-height: 178px;
  padding: 18px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 14px 30px rgba(28, 43, 68, 0.06);
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.info-card:hover,
.flow-card:hover {
  border-color: rgba(31, 111, 235, 0.24);
  box-shadow: 0 18px 38px rgba(31, 111, 235, 0.11);
  transform: translateY(-3px);
}

.info-card :deep(.el-icon),
.flow-card :deep(.el-icon) {
  color: #1f6feb;
}

.info-card h3,
.flow-card h3 {
  margin: 14px 0 8px;
  color: #111827;
  font-size: 17px;
  line-height: 1.35;
}

.info-card p,
.flow-card p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.flow-card {
  position: relative;
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

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.capability-pill {
  min-height: 46px;
  padding: 11px 14px;
  border: 1px solid rgba(20, 184, 166, 0.22);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(240, 251, 249, 0.98), rgba(255, 255, 255, 0.82));
  color: #344054;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.capability-pill:hover {
  border-color: rgba(20, 184, 166, 0.34);
  box-shadow: 0 14px 30px rgba(20, 184, 166, 0.1);
  transform: translateY(-2px);
}

.boundary-section {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(320px, 1.1fr);
  gap: 28px;
}

.boundary-list {
  display: grid;
  gap: 12px;
}

.boundary-item {
  align-items: flex-start;
  padding: 12px 14px;
  border: 1px solid rgba(245, 158, 11, 0.24);
  border-radius: 8px;
  background: #fffbeb;
  color: #344054;
  line-height: 1.65;
}

.boundary-item :deep(.el-icon) {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #f59e0b;
}

@media (max-width: 980px) {
  .product-position,
  .boundary-section {
    grid-template-columns: 1fr;
  }

  .audience-grid,
  .flow-grid,
  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
    min-height: auto;
    padding-top: 64px;
  }

  .hero-overlay {
    background: rgba(246, 248, 251, 0.88);
  }

  .landing-content {
    margin-top: 0;
  }

  .section-block {
    padding: 20px;
  }

  .audience-grid,
  .flow-grid,
  .capability-grid {
    grid-template-columns: 1fr;
  }
}
</style>
