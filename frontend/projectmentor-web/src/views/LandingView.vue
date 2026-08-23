<template>
  <div ref="landingRoot" class="landing-page" :class="{ 'landing-page--en': locale === 'en-US' }">
    <header class="site-header">
      <div class="site-header__inner">
        <RouterLink class="site-brand" to="/" aria-label="ProjectMentor AI">
          <BrandLogo variant="full" />
        </RouterLink>
        <span class="site-system">{{ t('landing.v5.nav.system') }}</span>
        <nav class="site-actions" :aria-label="t('landing.v5.nav.system')">
          <LanguageSwitch class="site-language" />
          <button v-if="!userStore.isLoggedIn" class="text-action text-action--login" type="button" @click="router.push('/login')">
            {{ t('landing.v5.nav.login') }}
          </button>
          <details class="utility-menu">
            <summary>{{ t('landing.v5.nav.more') }}</summary>
            <div class="utility-menu__panel">
              <button v-if="!userStore.isLoggedIn" class="utility-menu__login" type="button" @click="router.push('/login')">{{ t('landing.v5.nav.login') }}</button>
              <button type="button" @click="feedbackVisible = true">{{ t('landing.v5.nav.feedback') }}</button>
              <button type="button" @click="donateVisible = true">{{ t('landing.v5.nav.support') }}</button>
            </div>
          </details>
          <button class="primary-action primary-action--nav" type="button" @click="goStart">
            {{ userStore.isLoggedIn ? t('landing.v5.nav.workspace') : t('landing.v5.nav.start') }}
          </button>
        </nav>
      </div>
    </header>

    <main>
      <section class="hero-shell">
        <div class="system-rail" aria-label="System metadata">
          <span>{{ t('landing.v5.meta.release') }}</span>
          <span>{{ t('landing.v5.meta.environment') }}</span>
          <span class="system-rail__healthy"><i aria-hidden="true" />{{ t('landing.v5.meta.rules') }}</span>
          <span class="system-rail__privacy">{{ t('landing.v5.meta.privacy') }}</span>
        </div>

        <div class="hero-intro">
          <h1>{{ t('landing.v5.hero.title') }}</h1>
          <div class="hero-intro__support">
            <p>{{ t('landing.v5.hero.description') }}</p>
            <div class="hero-actions">
              <button class="primary-action" type="button" @click="goStart">{{ t('landing.v5.hero.primary') }}</button>
              <button class="secondary-action" type="button" @click="scrollToMethod">{{ t('landing.v5.hero.secondary') }}</button>
            </div>
            <small>{{ t('landing.v5.hero.note') }}</small>
          </div>
        </div>

        <section class="audit-workbench" aria-labelledby="audit-title">
          <div class="audit-workbench__topbar">
            <div>
              <strong id="audit-title">{{ t('landing.v5.audit.title') }}</strong>
              <span>{{ t('landing.v5.audit.illustrative') }}</span>
            </div>
            <div class="audit-phase" :data-phase="auditPhase" aria-live="polite">
              <i aria-hidden="true" />
              {{ phaseLabel }}
            </div>
          </div>

          <p class="audit-note">{{ t('landing.v5.audit.illustrativeNote') }}</p>

          <div class="audit-ledger" role="table" :aria-label="t('landing.v5.audit.title')">
            <div class="audit-ledger__head" role="row">
              <span role="columnheader">{{ t('landing.v5.audit.claim') }}</span>
              <span role="columnheader">{{ t('landing.v5.audit.evidence') }}</span>
              <span role="columnheader">{{ t('landing.v5.audit.verdict') }}</span>
            </div>
            <div
              v-for="(claim, index) in claims"
              :key="claim.id"
              class="claim-row"
              :class="{ 'claim-row--selected': index === 0 }"
              role="row"
            >
              <span class="claim-row__claim" role="cell">{{ claim.claim }}</span>
              <code role="cell">{{ claim.path }}</code>
              <span class="verdict" :data-status="claim.status" role="cell">
                <i aria-hidden="true" />{{ statusLabel(claim.status) }}
              </span>
            </div>
          </div>

          <div class="evidence-inspector">
            <div class="evidence-inspector__meta">
              <span>{{ t('landing.v5.audit.selected') }}</span>
              <strong>{{ t('landing.v5.audit.claims.jwt') }}</strong>
              <dl>
                <div>
                  <dt>{{ t('landing.v5.audit.sourceMatch') }}</dt>
                  <dd><code>backend/projectmentor-server/src/main/java/com/xinzhe/projectmentor/util/JwtUtil.java</code></dd>
                </div>
                <div>
                  <dt>{{ t('landing.v5.audit.verdict') }}</dt>
                  <dd><span class="verdict" data-status="supported"><i aria-hidden="true" />{{ t('landing.v5.status.supported') }}</span></dd>
                </div>
              </dl>
            </div>
            <div class="source-window" :class="{ 'source-window--resolved': auditPhase === 2 }">
              <div class="source-window__meta">
                <span>{{ t('landing.v5.audit.line') }}</span>
                <span>{{ t('landing.v5.audit.confidence') }}</span>
              </div>
              <pre><code><span>27</span> if (secret == null || secret.length() &lt; 32) {
<span class="source-line"><b>28</b>   throw new IllegalArgumentException(...);</span>
<span>29</span> }
<span class="source-line"><b>30</b> this.secretKey = Keys.hmacShaKeyFor(
     secret.getBytes(StandardCharsets.UTF_8));</span></code></pre>
              <p>{{ t('landing.v5.audit.sourceReason') }}</p>
            </div>
          </div>
        </section>
      </section>

      <section id="method" class="content-section method-section">
        <div class="section-heading">
          <h2>{{ t('landing.v5.method.title') }}</h2>
          <p>{{ t('landing.v5.method.description') }}</p>
        </div>
        <div class="method-flow">
          <article>
            <span class="method-flow__node">C</span>
            <div>
              <h3>{{ t('landing.v5.method.claimTitle') }}</h3>
              <p>{{ t('landing.v5.method.claimDesc') }}</p>
            </div>
          </article>
          <article>
            <span class="method-flow__node">E</span>
            <div>
              <h3>{{ t('landing.v5.method.evidenceTitle') }}</h3>
              <p>{{ t('landing.v5.method.evidenceDesc') }}</p>
            </div>
          </article>
          <article>
            <span class="method-flow__node">V</span>
            <div>
              <h3>{{ t('landing.v5.method.verdictTitle') }}</h3>
              <p>{{ t('landing.v5.method.verdictDesc') }}</p>
            </div>
          </article>
        </div>
      </section>

      <section class="content-section taxonomy-section">
        <div class="section-heading section-heading--compact">
          <h2>{{ t('landing.v5.taxonomy.title') }}</h2>
          <p>{{ t('landing.v5.taxonomy.description') }}</p>
        </div>
        <div class="status-table">
          <div v-for="status in statuses" :key="status.key" class="status-table__row">
            <span class="verdict" :data-status="status.key"><i aria-hidden="true" />{{ t(`landing.v5.status.${status.labelKey}`) }}</span>
            <p>{{ t(`landing.v5.taxonomy.${status.labelKey}`) }}</p>
          </div>
        </div>
      </section>

      <section class="layers-section">
        <div class="layers-section__intro">
          <h2>{{ t('landing.v5.layers.title') }}</h2>
          <p>{{ t('landing.v5.layers.description') }}</p>
        </div>
        <div class="layers-ledger">
          <article>
            <span>{{ t('landing.v5.layers.ruleLabel') }}</span>
            <h3>{{ t('landing.v5.layers.ruleTitle') }}</h3>
            <p>{{ t('landing.v5.layers.ruleDesc') }}</p>
          </article>
          <article>
            <span>{{ t('landing.v5.layers.aiLabel') }}</span>
            <h3>{{ t('landing.v5.layers.aiTitle') }}</h3>
            <p>{{ t('landing.v5.layers.aiDesc') }}</p>
          </article>
        </div>
      </section>

      <section id="privacy-disclaimer" class="boundary-section">
        <div class="boundary-section__copy">
          <h2>{{ t('landing.v5.boundary.title') }}</h2>
          <p>{{ t('landing.v5.boundary.description') }}</p>
        </div>
        <div class="boundary-panel">
          <strong>{{ t('landing.v5.boundary.warning') }}</strong>
          <details>
            <summary>{{ t('landing.v5.boundary.details') }}</summary>
            <ul>
              <li>{{ t('landing.v5.boundary.item1') }}</li>
              <li>{{ t('landing.v5.boundary.item2') }}</li>
              <li>{{ t('landing.v5.boundary.item3') }}</li>
            </ul>
          </details>
          <div class="boundary-actions">
            <button class="primary-action" type="button" @click="goStart">{{ t('landing.v5.boundary.primary') }}</button>
            <button v-if="!userStore.isLoggedIn" class="secondary-action secondary-action--dark" type="button" @click="router.push('/login')">
              {{ t('landing.v5.boundary.secondary') }}
            </button>
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
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import AppFooter from '@/components/AppFooter.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import DonateDialog from '@/components/DonateDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useUserStore } from '@/stores/user'

type EvidenceStatus = 'supported' | 'partial' | 'docOnly' | 'noEvidence' | 'risky'

const router = useRouter()
const { locale, t } = useI18n()
const userStore = useUserStore()
const landingRoot = ref<HTMLElement | null>(null)
const feedbackVisible = ref(false)
const donateVisible = ref(false)
const auditPhase = ref(0)
const timers: Array<ReturnType<typeof setTimeout>> = []

const phaseLabel = computed(() => {
  const keys = ['searching', 'found', 'verified'] as const
  return t(`landing.v5.audit.phase.${keys[auditPhase.value]}`)
})

const claims = computed<Array<{ id: string; claim: string; path: string; status: EvidenceStatus }>>(() => [
  { id: 'jwt', claim: t('landing.v5.audit.claims.jwt'), path: t('landing.v5.audit.paths.jwt'), status: 'supported' },
  { id: 'async', claim: t('landing.v5.audit.claims.async'), path: t('landing.v5.audit.paths.async'), status: 'partial' },
  { id: 'preview', claim: t('landing.v5.audit.claims.preview'), path: t('landing.v5.audit.paths.preview'), status: 'docOnly' },
  { id: 'redis', claim: t('landing.v5.audit.claims.redis'), path: t('landing.v5.audit.paths.redis'), status: 'noEvidence' },
  { id: 'scale', claim: t('landing.v5.audit.claims.scale'), path: t('landing.v5.audit.paths.scale'), status: 'risky' }
])

const statuses: Array<{ key: EvidenceStatus; labelKey: EvidenceStatus }> = [
  { key: 'supported', labelKey: 'supported' },
  { key: 'partial', labelKey: 'partial' },
  { key: 'docOnly', labelKey: 'docOnly' },
  { key: 'noEvidence', labelKey: 'noEvidence' },
  { key: 'risky', labelKey: 'risky' }
]

function statusLabel(status: EvidenceStatus) {
  return t(`landing.v5.status.${status}`)
}

function goStart() {
  router.push(userStore.isLoggedIn ? '/dashboard' : '/register')
}

function scrollToMethod() {
  const behavior: ScrollBehavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  landingRoot.value?.querySelector('#method')?.scrollIntoView({ behavior, block: 'start' })
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    auditPhase.value = 2
    return
  }
  timers.push(setTimeout(() => (auditPhase.value = 1), 520))
  timers.push(setTimeout(() => (auditPhase.value = 2), 1160))
})

onBeforeUnmount(() => timers.forEach((timer) => clearTimeout(timer)))
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  overflow: clip;
  background: var(--pm-paper);
  color: var(--pm-ink);
}

button,
summary,
a {
  -webkit-tap-highlight-color: transparent;
}

button {
  font: inherit;
}

.site-header {
  position: relative;
  z-index: 20;
  border-bottom: 1px solid var(--pm-stone);
  background: var(--pm-paper);
}

.site-header__inner {
  display: flex;
  width: min(1320px, calc(100% - 48px));
  min-height: 72px;
  margin: 0 auto;
  align-items: center;
  gap: 22px;
}

.site-brand {
  display: flex;
  flex: 0 0 auto;
  min-height: 44px;
  align-items: center;
}

.site-system {
  padding-left: 22px;
  border-left: 1px solid var(--pm-stone-strong);
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.site-actions {
  display: flex;
  margin-left: auto;
  align-items: center;
  gap: 8px;
}

.text-action,
.utility-menu summary {
  min-height: 44px;
  padding: 0 12px;
  border: 0;
  background: transparent;
  color: var(--pm-graphite);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  line-height: 44px;
}

.text-action:hover,
.utility-menu summary:hover {
  color: var(--pm-primary);
}

.primary-action,
.secondary-action {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-radius: var(--pm-radius-sm);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background var(--pm-motion-fast) ease-out, border-color var(--pm-motion-fast) ease-out, color var(--pm-motion-fast) ease-out;
}

.primary-action {
  border: 1px solid var(--pm-primary);
  background: var(--pm-primary);
  color: #fff;
}

.primary-action:hover {
  border-color: var(--pm-primary-dark);
  background: var(--pm-primary-dark);
}

.primary-action--nav {
  min-height: 44px;
  padding: 0 16px;
  font-size: 13px;
}

.secondary-action {
  border: 1px solid var(--pm-stone-strong);
  background: transparent;
  color: var(--pm-ink);
}

.secondary-action:hover {
  border-color: var(--pm-ink);
  background: #fff;
}

.utility-menu {
  position: relative;
}

.utility-menu summary {
  list-style: none;
}

.utility-menu summary::-webkit-details-marker {
  display: none;
}

.utility-menu summary::after {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin: 0 0 3px 8px;
  border-right: 1px solid currentColor;
  border-bottom: 1px solid currentColor;
  content: "";
  transform: rotate(45deg);
}

.utility-menu[open] summary::after {
  transform: rotate(225deg) translate(-2px, -2px);
}

.utility-menu__panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 188px;
  padding: 6px;
  border: 1px solid var(--pm-stone-strong);
  border-radius: var(--pm-radius-md);
  background: var(--pm-surface);
  box-shadow: 0 16px 32px rgba(11, 18, 32, 0.12);
}

.utility-menu__panel button {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 0;
  border-radius: var(--pm-radius-sm);
  background: transparent;
  color: var(--pm-graphite);
  cursor: pointer;
  text-align: left;
}

.utility-menu__panel button:hover {
  background: var(--pm-primary-soft);
  color: var(--pm-primary-dark);
}

.utility-menu__login {
  display: none;
}

.hero-shell,
.content-section {
  width: min(1320px, calc(100% - 48px));
  margin: 0 auto;
}

.system-rail {
  display: grid;
  min-height: 42px;
  grid-template-columns: auto auto auto 1fr;
  align-items: center;
  border-bottom: 1px solid var(--pm-stone);
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.system-rail > span {
  padding: 0 14px;
  border-right: 1px solid var(--pm-stone);
}

.system-rail > span:first-child {
  padding-left: 0;
}

.system-rail__healthy {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--pm-supported);
}

.system-rail__healthy i,
.audit-phase i {
  width: 6px;
  height: 6px;
  background: currentColor;
}

.system-rail .system-rail__privacy {
  justify-self: end;
  padding-right: 0;
  border-right: 0;
  color: var(--pm-risk);
}

.hero-intro {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.72fr);
  gap: clamp(42px, 8vw, 128px);
  padding: clamp(64px, 8.5vw, 116px) 0 clamp(48px, 7vw, 88px);
  align-items: end;
}

.hero-intro h1 {
  max-width: 13ch;
  margin: 0;
  font-size: clamp(3.15rem, 6vw, 5.8rem);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 0.98;
  text-wrap: balance;
}

.landing-page--en .hero-intro h1 {
  max-width: 14ch;
  font-size: clamp(3rem, 5.6vw, 5.5rem);
}

.hero-intro__support {
  padding-bottom: 4px;
}

.hero-intro__support > p {
  max-width: 60ch;
  margin: 0;
  color: var(--pm-graphite);
  font-size: 17px;
  line-height: 1.72;
}

.hero-actions,
.boundary-actions {
  display: flex;
  margin-top: 28px;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-intro__support small {
  display: block;
  margin-top: 14px;
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.5;
}

.audit-workbench {
  margin-bottom: clamp(88px, 10vw, 148px);
  border: 1px solid #1c2637;
  border-radius: var(--pm-radius-md);
  background: #101722;
  color: #edf1f7;
  box-shadow: 0 26px 64px rgba(11, 18, 32, 0.18);
}

.audit-workbench__topbar {
  display: flex;
  min-height: 54px;
  padding: 0 18px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid #2a3444;
  font-family: var(--pm-font-mono);
  font-size: 11px;
  letter-spacing: 0.05em;
}

.audit-workbench__topbar > div:first-child {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 16px;
}

.audit-workbench__topbar strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-workbench__topbar span {
  color: #9ba6b6;
}

.audit-phase {
  display: inline-flex;
  min-width: 170px;
  align-items: center;
  justify-content: flex-end;
  gap: 9px;
  color: #e1b45e;
  text-align: right;
  transition: color var(--pm-motion-base) ease-out;
}

.audit-phase[data-phase="1"] {
  color: #8db2ea;
}

.audit-phase[data-phase="2"] {
  color: #74ba8c;
}

.audit-note {
  margin: 0;
  padding: 10px 18px;
  border-bottom: 1px solid #2a3444;
  color: #aeb7c5;
  font-size: 12px;
  line-height: 1.5;
}

.audit-ledger__head,
.claim-row {
  display: grid;
  grid-template-columns: minmax(280px, 1.25fr) minmax(300px, 1fr) 168px;
}

.audit-ledger__head {
  min-height: 36px;
  align-items: center;
  border-bottom: 1px solid #2a3444;
  color: #7f8998;
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.09em;
}

.audit-ledger__head span,
.claim-row > * {
  padding: 0 16px;
}

.audit-ledger__head span + span,
.claim-row > * + * {
  border-left: 1px solid #2a3444;
}

.claim-row {
  width: 100%;
  min-height: 54px;
  padding: 0;
  align-items: stretch;
  border: 0;
  border-bottom: 1px solid #2a3444;
  background: transparent;
  color: inherit;
  cursor: default;
  text-align: left;
}

.claim-row > * {
  display: flex;
  min-width: 0;
  align-items: center;
}

.claim-row__claim {
  color: #e7ebf1;
  font-size: 13px;
  line-height: 1.4;
}

.claim-row code {
  color: #9eacbf;
  font-family: var(--pm-font-mono);
  font-size: 10px;
  overflow-wrap: anywhere;
}

.claim-row--selected {
  background: #162131;
  box-shadow: inset 2px 0 0 #6e9bd7;
}

.verdict {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--status-color);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.045em;
  white-space: nowrap;
}

.verdict i {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  border: 1px solid currentColor;
  background: var(--status-bg);
}

.verdict[data-status="supported"] { --status-color: #74ba8c; --status-bg: #28543a; }
.verdict[data-status="partial"] { --status-color: #e1b45e; --status-bg: #604a1f; }
.verdict[data-status="docOnly"] { --status-color: #8db2ea; --status-bg: #2e4c72; }
.verdict[data-status="noEvidence"] { --status-color: #aeb7c5; --status-bg: #48515f; }
.verdict[data-status="risky"] { --status-color: #ec8782; --status-bg: #6d3434; }

.evidence-inspector {
  display: grid;
  grid-template-columns: minmax(270px, 0.72fr) minmax(0, 1.3fr);
}

.evidence-inspector__meta {
  padding: 26px 22px;
  border-right: 1px solid #2a3444;
}

.evidence-inspector__meta > span,
.evidence-inspector dt {
  color: #7f8998;
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.evidence-inspector__meta > strong {
  display: block;
  max-width: 34ch;
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.45;
}

.evidence-inspector dl {
  margin: 26px 0 0;
}

.evidence-inspector dl > div {
  padding: 14px 0;
  border-top: 1px solid #2a3444;
}

.evidence-inspector dd {
  margin: 7px 0 0;
}

.evidence-inspector dd code {
  color: #b8c4d4;
  font-family: var(--pm-font-mono);
  font-size: 10px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.source-window {
  min-width: 0;
  background: #0c121c;
  transition: background var(--pm-motion-base) cubic-bezier(.16, 1, .3, 1);
}

.source-window--resolved {
  background: #0c1514;
}

.source-window__meta {
  display: flex;
  min-height: 40px;
  padding: 0 18px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #273241;
  color: #8894a4;
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.source-window pre {
  margin: 0;
  padding: 22px 18px 18px;
  overflow-x: auto;
  color: #d9e0e9;
  font-family: var(--pm-font-mono);
  font-size: 11px;
  line-height: 1.75;
}

.source-window pre span:not(.source-line) {
  display: inline-block;
  width: 28px;
  color: #657083;
  user-select: none;
}

.source-line {
  display: block;
  margin: 0 -18px;
  padding: 0 18px;
  background: rgba(44, 90, 160, 0.2);
}

.source-line b {
  display: inline-block;
  width: 28px;
  color: #8db2ea;
  font-weight: 400;
}

.source-window > p {
  margin: 0;
  padding: 14px 18px 18px;
  border-top: 1px solid #273241;
  color: #aeb8c7;
  font-size: 12px;
  line-height: 1.6;
}

.content-section {
  padding: clamp(72px, 8vw, 112px) 0;
}

.method-section {
  border-top: 1px solid var(--pm-stone-strong);
}

.section-heading {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.7fr);
  gap: clamp(36px, 8vw, 112px);
  align-items: start;
}

.section-heading h2,
.layers-section h2,
.boundary-section h2 {
  max-width: 19ch;
  margin: 0;
  font-size: clamp(2.2rem, 4.2vw, 4.4rem);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 1.05;
  text-wrap: balance;
}

.section-heading > p,
.layers-section__intro > p,
.boundary-section__copy > p {
  max-width: 60ch;
  margin: 4px 0 0;
  color: var(--pm-muted);
  font-size: 16px;
  line-height: 1.7;
}

.method-flow {
  display: grid;
  margin-top: clamp(48px, 6vw, 76px);
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.method-flow article {
  display: grid;
  min-height: 220px;
  padding: 30px 28px 34px 0;
  grid-template-columns: 44px 1fr;
  gap: 20px;
  align-content: start;
}

.method-flow article + article {
  padding-left: 28px;
  border-left: 1px solid var(--pm-stone-strong);
}

.method-flow__node {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid var(--pm-ink);
  border-radius: var(--pm-radius-sm);
  font-family: var(--pm-font-mono);
  font-size: 13px;
}

.method-flow h3,
.layers-ledger h3 {
  margin: 5px 0 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.015em;
}

.method-flow p,
.layers-ledger p {
  margin: 14px 0 0;
  color: var(--pm-muted);
  font-size: 14px;
  line-height: 1.7;
}

.taxonomy-section {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(520px, 1.1fr);
  gap: clamp(44px, 8vw, 112px);
  border-top: 1px solid var(--pm-stone-strong);
}

.section-heading--compact {
  display: block;
}

.section-heading--compact h2 {
  max-width: 15ch;
  font-size: clamp(2.1rem, 3.4vw, 3.6rem);
}

.section-heading--compact p {
  margin-top: 24px;
}

.status-table {
  border-top: 1px solid var(--pm-stone-strong);
}

.status-table__row {
  display: grid;
  min-height: 64px;
  grid-template-columns: 160px 1fr;
  align-items: center;
  gap: 20px;
  border-bottom: 1px solid var(--pm-stone);
}

.status-table__row .verdict[data-status="supported"] { --status-color: var(--pm-supported); --status-bg: var(--pm-supported-bg); }
.status-table__row .verdict[data-status="partial"] { --status-color: var(--pm-partial); --status-bg: var(--pm-partial-bg); }
.status-table__row .verdict[data-status="docOnly"] { --status-color: var(--pm-doc); --status-bg: var(--pm-doc-bg); }
.status-table__row .verdict[data-status="noEvidence"] { --status-color: var(--pm-none); --status-bg: var(--pm-none-bg); }
.status-table__row .verdict[data-status="risky"] { --status-color: var(--pm-risk); --status-bg: var(--pm-risk-bg); }

.status-table__row p {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.55;
}

.layers-section {
  display: grid;
  padding: clamp(80px, 9vw, 128px) max(24px, calc((100vw - 1320px) / 2));
  grid-template-columns: minmax(0, 0.75fr) minmax(560px, 1.25fr);
  gap: clamp(48px, 8vw, 128px);
  background: #e9ecef;
}

.layers-section h2 {
  max-width: 13ch;
  font-size: clamp(2.2rem, 3.9vw, 4.2rem);
}

.layers-section__intro > p {
  margin-top: 28px;
}

.layers-ledger {
  border-top: 1px solid var(--pm-stone-strong);
}

.layers-ledger article {
  display: grid;
  padding: 28px 0 32px;
  grid-template-columns: 152px 1fr;
  column-gap: 28px;
  border-bottom: 1px solid var(--pm-stone-strong);
}

.layers-ledger article > span {
  grid-row: 1 / span 2;
  color: var(--pm-primary-dark);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.layers-ledger h3 {
  margin-top: -5px;
}

.boundary-section {
  display: grid;
  padding: clamp(80px, 9vw, 132px) max(24px, calc((100vw - 1320px) / 2));
  grid-template-columns: minmax(0, 0.72fr) minmax(520px, 1fr);
  gap: clamp(52px, 9vw, 144px);
  background: var(--pm-ink);
  color: #f4f6f9;
}

.boundary-section h2 {
  max-width: 12ch;
}

.boundary-section__copy > p {
  margin-top: 26px;
  color: #aeb7c5;
}

.boundary-panel {
  align-self: start;
  padding-top: 22px;
  border-top: 1px solid #495260;
}

.boundary-panel > strong {
  display: block;
  color: #f0a29c;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.55;
}

.boundary-panel details {
  margin-top: 26px;
  border-top: 1px solid #343e4d;
  border-bottom: 1px solid #343e4d;
}

.boundary-panel summary {
  min-height: 50px;
  color: #dce2ea;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  line-height: 50px;
}

.boundary-panel ul {
  margin: 0;
  padding: 0 0 20px 20px;
  color: #aeb7c5;
  font-size: 13px;
  line-height: 1.7;
}

.secondary-action--dark {
  border-color: #5d6878;
  color: #e4e8ee;
}

.secondary-action--dark:hover {
  border-color: #9ba6b6;
  background: #1a2331;
}

:deep(.site-language.language-switch) {
  border-color: var(--pm-stone-strong);
}

@media (max-width: 1060px) {
  .site-system {
    display: none;
  }

  .hero-intro {
    gap: 48px;
  }

  .audit-ledger__head,
  .claim-row {
    grid-template-columns: minmax(240px, 1fr) minmax(240px, 0.9fr) 152px;
  }

  .taxonomy-section,
  .layers-section,
  .boundary-section {
    grid-template-columns: 1fr;
  }

  .taxonomy-section {
    gap: 48px;
  }

  .layers-section,
  .boundary-section {
    gap: 52px;
  }

  .layers-section__intro,
  .boundary-section__copy {
    display: grid;
    grid-template-columns: 1fr 0.7fr;
    gap: 42px;
  }
}

@media (max-width: 820px) {
  .site-header__inner,
  .hero-shell,
  .content-section {
    width: min(100% - 32px, 1320px);
  }

  .site-header__inner {
    min-height: 64px;
  }

  .text-action--login {
    display: none;
  }

  .utility-menu__login {
    display: block;
  }

  .hero-intro,
  .section-heading {
    grid-template-columns: 1fr;
  }

  .hero-intro {
    gap: 34px;
    align-items: start;
  }

  .hero-intro__support {
    max-width: 640px;
  }

  .audit-ledger__head {
    display: none;
  }

  .claim-row {
    grid-template-columns: 1fr 160px;
    min-height: 82px;
  }

  .claim-row__claim {
    padding-top: 13px;
    align-items: flex-start;
  }

  .claim-row code {
    grid-column: 1;
    padding-bottom: 13px;
    border-left: 0;
    align-items: flex-start;
  }

  .claim-row .verdict {
    grid-row: 1 / span 2;
    grid-column: 2;
  }

  .evidence-inspector {
    grid-template-columns: 1fr;
  }

  .evidence-inspector__meta {
    border-right: 0;
    border-bottom: 1px solid #2a3444;
  }

  .method-flow {
    grid-template-columns: 1fr;
  }

  .method-flow article {
    min-height: 0;
    padding: 28px 0;
  }

  .method-flow article + article {
    padding-left: 0;
    border-top: 1px solid var(--pm-stone-strong);
    border-left: 0;
  }

  .layers-section__intro,
  .boundary-section__copy {
    display: block;
  }
}

@media (max-width: 560px) {
  .site-header__inner {
    width: calc(100% - 24px);
    gap: 8px;
  }

  .site-brand :deep(.brand-logo__copy) {
    display: none;
  }

  .site-brand :deep(.brand-logo) {
    gap: 0;
  }

  .site-brand {
    display: flex;
    min-width: 44px;
    min-height: 44px;
    align-items: center;
  }

  .site-actions {
    gap: 3px;
  }

  .utility-menu summary {
    padding: 0 8px;
  }

  .primary-action--nav {
    min-width: 44px;
    max-width: 112px;
    min-height: 44px;
    padding: 5px 10px;
    line-height: 1.2;
  }

  .system-rail {
    grid-template-columns: 1fr 1fr;
    padding: 8px 0;
    row-gap: 8px;
  }

  .system-rail > span {
    min-height: 22px;
    padding: 0 8px;
    align-items: center;
  }

  .system-rail > span:nth-child(odd) {
    padding-left: 0;
  }

  .system-rail > span:nth-child(even) {
    padding-right: 0;
    border-right: 0;
    text-align: right;
  }

  .system-rail .system-rail__privacy {
    justify-self: stretch;
  }

  .hero-intro {
    padding: 54px 0 44px;
  }

  .hero-intro h1,
  .landing-page--en .hero-intro h1 {
    max-width: 13ch;
    font-size: clamp(2.65rem, 13vw, 3.55rem);
    line-height: 1.01;
  }

  .hero-intro__support > p {
    font-size: 15px;
    line-height: 1.65;
  }

  .hero-actions,
  .boundary-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .audit-workbench {
    margin-right: -8px;
    margin-bottom: 84px;
    margin-left: -8px;
    border-radius: var(--pm-radius-sm);
  }

  .audit-workbench__topbar {
    min-height: 72px;
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
  }

  .audit-workbench__topbar > div:first-child {
    width: 100%;
    justify-content: space-between;
  }

  .audit-phase {
    min-width: 0;
    justify-content: flex-start;
  }

  .claim-row {
    grid-template-columns: 1fr;
    padding: 14px 14px 16px;
    gap: 9px;
  }

  .claim-row > *,
  .claim-row__claim,
  .claim-row code,
  .claim-row .verdict {
    grid-row: auto;
    grid-column: auto;
    padding: 0;
    border-left: 0;
  }

  .source-window pre {
    font-size: 9.5px;
  }

  .content-section {
    padding: 72px 0;
  }

  .section-heading h2,
  .section-heading--compact h2,
  .layers-section h2,
  .boundary-section h2 {
    font-size: 2.35rem;
  }

  .status-table__row {
    grid-template-columns: 132px 1fr;
    gap: 12px;
  }

  .status-table__row p {
    font-size: 12px;
  }

  .layers-section,
  .boundary-section {
    padding: 76px 16px;
  }

  .layers-ledger article {
    display: block;
  }

  .layers-ledger article > span {
    display: block;
    margin-bottom: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .landing-page {
    scroll-behavior: auto !important;
  }

  .audit-phase,
  .source-window,
  .primary-action,
  .secondary-action {
    transition: none;
  }
}
</style>
