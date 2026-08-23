<template>
  <section class="evidence-surface" aria-labelledby="claim-evidence-title">
    <header class="evidence-surface-header">
      <div>
        <h2 id="claim-evidence-title">{{ t('projects.v5.evidence.title') }}</h2>
        <p>{{ t('projects.v5.evidence.description') }}</p>
      </div>
      <div v-if="reportCreatedAt || reportId" class="evidence-surface-meta">
        <span v-if="reportCreatedAt">{{ t('projects.v5.evidence.reportTimestamp', { time: formatDate(reportCreatedAt) }) }}</span>
        <el-button v-if="reportId" text @click="emit('view-report', reportId)">
          {{ t('projects.v5.evidence.viewReport') }}
        </el-button>
      </div>
    </header>

    <div v-if="loading" class="evidence-loading" aria-live="polite">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="error" class="section-error" role="alert">
      <div>
        <strong>{{ t('projects.v5.errors.evidence') }}</strong>
        <p>{{ t('projects.v5.evidence.description') }}</p>
      </div>
      <el-button @click="emit('retry')">{{ t('projects.v5.retry') }}</el-button>
    </div>

    <div v-else class="evidence-workbench">
      <aside class="repository-pane" aria-labelledby="repository-context-title">
        <div class="pane-header">
          <div>
            <h3 id="repository-context-title">{{ t('projects.v5.evidence.repository') }}</h3>
            <p>{{ t('projects.v5.evidence.repositoryDescription') }}</p>
          </div>
          <span class="pane-count">{{ t('projects.v5.filesCount', { count: files.length }) }}</span>
        </div>

        <div v-if="filesLoading" class="pane-skeleton">
          <el-skeleton :rows="6" animated />
        </div>
        <div v-else-if="filesError" class="repository-error" role="status">
          <p>{{ t('projects.v5.evidence.repositoryUnavailable') }}</p>
          <el-button text @click="emit('retry-files')">{{ t('projects.v5.retry') }}</el-button>
        </div>
        <template v-else-if="repositoryFiles.length">
          <ul class="repository-files">
            <li v-for="file in repositoryFiles" :key="file.id || file.filePath">
              <span class="repository-role">{{ fileRole(file) }}</span>
              <strong>{{ fileName(file.filePath) }}</strong>
              <code>{{ directoryPath(file.filePath) }}</code>
            </li>
          </ul>
          <button class="text-action repository-more" type="button" @click="emit('open-sources')">
            {{ t('projects.v5.evidence.openSources') }}
          </button>
        </template>
        <EmptyState
          v-else
          variant="compact"
          :title="t('projects.v5.evidence.noSources')"
          :description="t('projects.v5.evidence.noSourcesDescription')"
        >
          <el-button @click="emit('open-sources')">{{ t('projects.v5.evidence.openSources') }}</el-button>
        </EmptyState>
      </aside>

      <section class="claim-pane" aria-labelledby="claim-list-title">
        <div class="pane-header claim-pane-header">
          <div>
            <h3 id="claim-list-title">{{ t('projects.v5.evidence.claims') }}</h3>
            <p>{{ t('projects.v5.evidence.claimCount', { count: orderedClaims.length }) }}</p>
          </div>
          <div v-if="orderedClaims.length" class="claim-status-summary" :aria-label="t('projects.v5.evidence.statusSummary')">
            <StatusLabel
              v-for="item in visibleStatusSummary"
              :key="item.status"
              :status="item.status"
              :label="`${statusLabel(item.status)} ${item.count}`"
            />
          </div>
        </div>

        <div
          v-if="orderedClaims.length"
          ref="claimListRef"
          class="claim-list"
          role="listbox"
          :aria-label="t('projects.v5.evidence.claims')"
        >
          <button
            v-for="(claim, index) in orderedClaims"
            :key="claimKey(claim, index)"
            type="button"
            class="claim-row"
            :class="{ 'is-selected': index === selectedClaimIndex }"
            role="option"
            :aria-selected="index === selectedClaimIndex"
            :tabindex="index === selectedClaimIndex ? 0 : -1"
            @click="selectClaim(index)"
            @keydown="handleClaimKeydown($event, index)"
          >
            <span class="claim-row-main">
              <span class="claim-row-title">{{ claim.claimText }}</span>
              <span class="claim-row-source">
                {{ sourceLabel(claim.sourceType) }} · {{ categoryLabel(claim.category) }}
              </span>
            </span>
            <span class="claim-row-verdict">
              <StatusLabel :status="claim.status" :label="statusLabel(claim.status)" />
              <span>{{ t('projects.v5.evidence.evidenceCount', { count: claim.evidenceFiles?.length || 0 }) }}</span>
            </span>
          </button>
        </div>

        <EmptyState
          v-else
          variant="compact"
          :title="t('projects.v5.evidence.noClaims')"
          :description="t('projects.v5.evidence.noClaimsDescription')"
        >
          <el-button type="primary" @click="emit('run-audit')">{{ t('projects.v5.runAudit') }}</el-button>
        </EmptyState>
      </section>

      <aside
        ref="inspectorRef"
        class="inspector-pane"
        aria-labelledby="evidence-inspector-title"
        tabindex="-1"
      >
        <div class="inspector-header">
          <div>
            <h3 id="evidence-inspector-title">{{ t('projects.v5.evidence.inspector') }}</h3>
            <StatusLabel
              v-if="selectedClaim"
              :status="selectedClaim.status"
              :label="statusLabel(selectedClaim.status)"
            />
          </div>
          <span v-if="selectedClaim?.confidenceScore !== undefined" class="confidence-value">
            {{ t('projects.v5.evidence.confidence') }} {{ selectedClaim.confidenceScore }}
          </span>
        </div>

        <Transition name="inspector-swap" mode="out-in">
          <div
            v-if="selectedClaim"
            :key="`${selectedClaimIndex}-${selectedEvidenceIndex}`"
            class="inspector-content"
          >
          <div class="inspector-claim">
            <h4>{{ selectedClaim.claimText }}</h4>
            <dl>
              <div>
                <dt>{{ t('projects.v5.evidence.sourceClaim') }}</dt>
                <dd>{{ sourceLabel(selectedClaim.sourceType) }}</dd>
              </div>
              <div v-if="selectedClaim.reason">
                <dt>{{ t('projects.v5.evidence.claimReason') }}</dt>
                <dd>{{ selectedClaim.reason }}</dd>
              </div>
              <div v-if="selectedClaim.sourceSnippet">
                <dt>{{ t('projects.v5.evidence.source') }}</dt>
                <dd>{{ selectedClaim.sourceSnippet }}</dd>
              </div>
            </dl>
          </div>

          <div v-if="selectedEvidence" class="evidence-document">
            <div class="evidence-document-heading">
              <div>
                <span>{{ t('common.evidence') }} {{ String(selectedEvidenceIndex + 1).padStart(2, '0') }}</span>
                <h4>{{ fileName(selectedEvidence.filePath) }}</h4>
              </div>
              <div class="evidence-document-actions">
                <button type="button" @click="wrapCode = !wrapCode">
                  {{ wrapCode ? t('projects.v5.evidence.scrollCode') : t('projects.v5.evidence.wrapCode') }}
                </button>
                <button type="button" @click="copySelectedEvidence">
                  {{ t('projects.v5.evidence.copyEvidence') }}
                </button>
              </div>
            </div>

            <code class="evidence-path">{{ selectedEvidence.filePath }}</code>

            <div class="evidence-metadata">
              <span>{{ fileRole(selectedEvidence) }}</span>
              <span>{{ evidenceLevelLabel(selectedEvidence.evidenceLevel) }}</span>
            </div>

            <pre v-if="selectedEvidence.snippet" :class="{ 'is-wrapped': wrapCode }" tabindex="0"><code>{{ selectedEvidence.snippet }}</code></pre>

            <section v-if="selectedEvidence.reason" class="evidence-explanation">
              <h5>{{ t('projects.v5.evidence.reason') }}</h5>
              <p>{{ selectedEvidence.reason }}</p>
            </section>

            <section v-if="selectedEvidence.matchedKeywords?.length" class="evidence-keywords">
              <h5>{{ t('projects.v5.evidence.matchedKeywords') }}</h5>
              <code>{{ selectedEvidence.matchedKeywords.join(' · ') }}</code>
            </section>
          </div>

          <div
            v-if="evidenceOptions.length > 1"
            ref="evidenceListRef"
            class="evidence-source-list"
            role="listbox"
            :aria-label="t('projects.v5.evidence.relatedEvidence')"
          >
            <button
              v-for="(evidence, index) in evidenceOptions"
              :key="`${evidence.fileId || evidence.filePath}-${index}`"
              type="button"
              :class="{ 'is-selected': index === selectedEvidenceIndex }"
              role="option"
              :aria-selected="index === selectedEvidenceIndex"
              :tabindex="index === selectedEvidenceIndex ? 0 : -1"
              @click="selectEvidence(index)"
              @keydown="handleEvidenceKeydown($event, index)"
            >
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <strong>{{ fileName(evidence.filePath) }}</strong>
              <code>{{ evidence.filePath }}</code>
            </button>
          </div>

          <EmptyState
            v-if="!selectedEvidence"
            variant="compact"
            :title="t('projects.v5.evidence.noEvidence')"
            :description="t('projects.v5.evidence.noEvidenceDescription')"
          />

          <div v-if="selectedClaim.resumeAdvice || selectedClaim.interviewQuestion" class="claim-next-steps">
            <section v-if="selectedClaim.resumeAdvice">
              <h5>{{ t('projects.v5.evidence.resumeAdvice') }}</h5>
              <p>{{ selectedClaim.resumeAdvice }}</p>
            </section>
            <section v-if="selectedClaim.interviewQuestion">
              <h5>{{ t('projects.v5.evidence.interviewFollowUp') }}</h5>
              <p>{{ selectedClaim.interviewQuestion }}</p>
            </section>
          </div>
          </div>

          <p v-else key="empty" class="inspector-placeholder">{{ t('projects.v5.evidence.selectClaim') }}</p>
        </Transition>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type {
  ClaimEvidenceFile,
  ClaimEvidenceItem,
  ClaimEvidenceStatus,
  ProjectFile
} from '@/types/api'

const props = withDefaults(defineProps<{
  files?: ProjectFile[]
  claims?: ClaimEvidenceItem[]
  loading?: boolean
  error?: boolean
  filesLoading?: boolean
  filesError?: boolean
  reportId?: number
  reportCreatedAt?: string
}>(), {
  files: () => [],
  claims: () => [],
  loading: false,
  error: false,
  filesLoading: false,
  filesError: false,
  reportId: undefined,
  reportCreatedAt: undefined
})

const emit = defineEmits<{
  (event: 'retry'): void
  (event: 'retry-files'): void
  (event: 'run-audit'): void
  (event: 'open-sources'): void
  (event: 'view-report', reportId: number): void
}>()

const { t } = useI18n()
const selectedClaimIndex = ref(0)
const selectedEvidenceIndex = ref(0)
const wrapCode = ref(false)
const inspectorRef = ref<HTMLElement>()
const claimListRef = ref<HTMLElement>()
const evidenceListRef = ref<HTMLElement>()

const statusPriority: Record<ClaimEvidenceStatus, number> = {
  RISKY: 0,
  NO_EVIDENCE: 1,
  DOC_ONLY: 2,
  PARTIAL: 3,
  SUPPORTED: 4
}

const orderedClaims = computed(() => (props.claims || []).slice().sort((left, right) => {
  const priority = statusPriority[left.status] - statusPriority[right.status]
  return priority || (left.confidenceScore ?? 100) - (right.confidenceScore ?? 100)
}))

const selectedClaim = computed(() => orderedClaims.value[selectedClaimIndex.value])
const evidenceOptions = computed(() => selectedClaim.value?.evidenceFiles || [])
const selectedEvidence = computed(() => evidenceOptions.value[selectedEvidenceIndex.value])

const repositoryFiles = computed(() => (props.files || [])
  .slice()
  .sort((left, right) => {
    const leftReadme = /(^|\/)readme/i.test(left.filePath) ? 0 : 1
    const rightReadme = /(^|\/)readme/i.test(right.filePath) ? 0 : 1
    return leftReadme - rightReadme || left.filePath.localeCompare(right.filePath)
  })
  .slice(0, 24))

const visibleStatusSummary = computed(() => {
  const countMap = new Map<ClaimEvidenceStatus, number>()
  for (const claim of orderedClaims.value) {
    countMap.set(claim.status, (countMap.get(claim.status) || 0) + 1)
  }
  return (['RISKY', 'NO_EVIDENCE', 'PARTIAL', 'SUPPORTED'] as ClaimEvidenceStatus[])
    .map((status) => ({ status, count: countMap.get(status) || 0 }))
    .filter((item) => item.count > 0)
    .slice(0, 3)
})

watch(orderedClaims, () => {
  if (selectedClaimIndex.value >= orderedClaims.value.length) {
    selectedClaimIndex.value = 0
  }
})

watch(selectedClaim, () => {
  selectedEvidenceIndex.value = 0
  wrapCode.value = false
})

function selectClaim(index: number) {
  selectedClaimIndex.value = index
  if (window.matchMedia('(max-width: 700px)').matches) {
    nextTick(() => {
      inspectorRef.value?.focus({ preventScroll: true })
      inspectorRef.value?.scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
        block: 'start'
      })
    })
  }
}

function selectEvidence(index: number) {
  selectedEvidenceIndex.value = index
}

function listboxTargetIndex(event: KeyboardEvent, index: number, length: number) {
  if (event.key === 'ArrowDown' || event.key === 'ArrowRight') return (index + 1) % length
  if (event.key === 'ArrowUp' || event.key === 'ArrowLeft') return (index - 1 + length) % length
  if (event.key === 'Home') return 0
  if (event.key === 'End') return length - 1
  return undefined
}

function focusListboxOption(container: HTMLElement | undefined, index: number) {
  nextTick(() => {
    const options = container?.querySelectorAll<HTMLElement>('[role="option"]')
    options?.[index]?.focus()
  })
}

function handleClaimKeydown(event: KeyboardEvent, index: number) {
  const targetIndex = listboxTargetIndex(event, index, orderedClaims.value.length)
  if (targetIndex === undefined) return
  event.preventDefault()
  selectedClaimIndex.value = targetIndex
  focusListboxOption(claimListRef.value, targetIndex)
}

function handleEvidenceKeydown(event: KeyboardEvent, index: number) {
  const targetIndex = listboxTargetIndex(event, index, evidenceOptions.value.length)
  if (targetIndex === undefined) return
  event.preventDefault()
  selectEvidence(targetIndex)
  focusListboxOption(evidenceListRef.value, targetIndex)
}

function claimKey(claim: ClaimEvidenceItem, index: number) {
  return `${claim.sourceType}-${claim.claimText}-${index}`
}

function statusLabel(status: ClaimEvidenceStatus) {
  return t(`projects.v5.claimStatus.${status}`)
}

function sourceLabel(sourceType: string) {
  const knownSources = ['PROJECT_DESCRIPTION', 'TECH_STACK', 'README']
  return knownSources.includes(sourceType)
    ? t(`projects.v5.claimSource.${sourceType}`)
    : sourceType.replace(/_/g, ' ')
}

function categoryLabel(category: string) {
  return category.replace(/_/g, ' ')
}

function fileName(path: string) {
  return path.split(/[\\/]/).filter(Boolean).pop() || path
}

function directoryPath(path: string) {
  const normalized = path.replace(/\\/g, '/')
  const index = normalized.lastIndexOf('/')
  return index > -1 ? normalized.slice(0, index) : './'
}

function fileRole(file: { filePath: string; fileType?: string }) {
  const type = file.fileType?.toUpperCase() || ''
  const path = file.filePath.toLowerCase()
  let role = 'OTHER'

  if (type === 'README' || path.endsWith('.md')) {
    role = 'DOC'
  } else if (
    ['CONFIG', 'POM', 'PACKAGE', 'DOCKER', 'DOCKER_COMPOSE', 'SQL', 'GITIGNORE'].includes(type)
    || ['.xml', '.yml', '.yaml', '.properties', '.sql', '.json'].some((extension) => path.endsWith(extension))
    || path.endsWith('dockerfile')
  ) {
    role = 'CONFIG'
  } else if (
    ['CODE', 'CONTROLLER', 'SERVICE', 'MAPPER', 'ENTITY', 'UTIL'].includes(type)
    || ['.java', '.kt', '.js', '.jsx', '.ts', '.tsx', '.vue', '.css', '.scss', '.html', '.py', '.go', '.rs', '.c', '.cpp', '.cs', '.sh'].some((extension) => path.endsWith(extension))
  ) {
    role = 'CODE'
  }

  return t(`projects.v5.evidence.fileTypes.${role}`)
}

function evidenceLevelLabel(level: string) {
  return ['STRONG', 'WEAK'].includes(level)
    ? t(`projects.v5.evidence.evidenceLevels.${level}`)
    : level.replace(/_/g, ' ')
}

function formatDate(value: string) {
  return String(value).replace('T', ' ').slice(0, 19)
}

async function copySelectedEvidence() {
  if (!selectedClaim.value || !selectedEvidence.value) {
    return
  }

  const evidence = selectedEvidence.value
  const content = [
    selectedClaim.value.claimText,
    evidence.filePath,
    evidence.snippet || '',
    evidence.reason || ''
  ].filter(Boolean).join('\n\n')

  const copied = await copyText(content)
  ElMessage[copied ? 'success' : 'error'](
    copied ? t('projects.v5.evidence.copied') : t('projects.v5.evidence.copyFailed')
  )
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // Continue with the textarea fallback on non-secure origins.
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  let copied = false
  try {
    copied = document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
  }
  return copied
}
</script>

<style scoped>
.evidence-surface {
  min-width: 0;
}

.evidence-surface-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--pm-space-6);
  margin-bottom: var(--pm-space-5);
}

.evidence-surface-header h2,
.pane-header h3,
.inspector-header h3,
.inspector-claim h4,
.evidence-document h4 {
  margin: 0;
}

.evidence-surface-header h2 {
  color: var(--pm-ink);
  font-size: var(--pm-type-section-title);
  letter-spacing: -0.02em;
}

.evidence-surface-header p {
  max-width: 68ch;
  margin: 7px 0 0;
  color: var(--pm-muted);
  font-size: 14px;
  line-height: 1.6;
}

.evidence-surface-meta {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.evidence-loading,
.section-error {
  min-height: 360px;
  padding: 28px;
  border: 1px solid var(--pm-stone);
  background: var(--pm-surface);
}

.section-error {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.section-error strong {
  color: var(--pm-ink);
}

.section-error p {
  margin: 6px 0 0;
  color: var(--pm-muted);
  line-height: 1.6;
}

.evidence-workbench {
  display: grid;
  min-width: 0;
  grid-template-columns: 220px minmax(300px, 0.9fr) minmax(360px, 1.15fr);
  align-items: start;
  overflow: clip;
  border: 1px solid var(--pm-stone-strong);
  border-radius: var(--pm-radius-md);
  background: var(--pm-surface);
  box-shadow: 0 26px 64px rgba(11, 18, 32, 0.18);
}

.repository-pane,
.claim-pane,
.inspector-pane {
  min-width: 0;
}

.repository-pane,
.claim-pane {
  min-height: 660px;
  background: var(--pm-surface);
}

.repository-pane {
  border-right: 1px solid var(--pm-stone);
}

.claim-pane {
  border-right: 1px solid var(--pm-stone);
}

.pane-header,
.inspector-header {
  display: flex;
  min-height: 86px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid var(--pm-stone);
}

.pane-header h3,
.inspector-header h3 {
  color: inherit;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.pane-header p {
  margin: 5px 0 0;
  color: var(--pm-muted);
  font-size: 11px;
  line-height: 1.5;
}

.pane-count {
  flex: 0 0 auto;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.pane-skeleton,
.repository-error {
  padding: 18px;
}

.repository-error p {
  margin: 0 0 8px;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.55;
}

.repository-files {
  max-height: 515px;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
  scrollbar-color: var(--pm-stone-strong) transparent;
  scrollbar-width: thin;
}

.repository-files li {
  display: grid;
  min-width: 0;
  gap: 4px;
  padding: 11px 14px;
  border-bottom: 1px solid var(--pm-stone);
}

.repository-role {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.repository-files strong {
  overflow: hidden;
  color: var(--pm-graphite);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.repository-files code,
.evidence-source-list code {
  overflow: hidden;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-action {
  min-height: 44px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--pm-primary-dark);
  cursor: pointer;
  font: 600 13px var(--pm-font-sans);
  text-align: left;
}

.repository-more {
  width: 100%;
  padding: 0 14px;
  border-top: 1px solid var(--pm-stone);
}

.claim-pane-header {
  align-items: flex-start;
}

.claim-status-summary {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.claim-list {
  max-height: 574px;
  overflow: auto;
  scrollbar-color: var(--pm-stone-strong) transparent;
  scrollbar-width: thin;
}

.claim-row {
  position: relative;
  display: grid;
  width: 100%;
  min-height: 92px;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 16px;
  padding: 16px 18px;
  border: 0;
  border-bottom: 1px solid var(--pm-stone);
  background: var(--pm-surface);
  color: var(--pm-ink);
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: background-color var(--pm-motion-base) var(--pm-ease-standard);
}

.claim-row::before {
  position: absolute;
  inset: 0 auto 0 0;
  width: 1px;
  background: var(--pm-primary);
  content: '';
  opacity: 0;
  transition: opacity var(--pm-motion-base) var(--pm-ease-standard);
}

.claim-row:hover {
  background: var(--pm-surface-hover);
}

.claim-row.is-selected {
  background: var(--pm-primary-soft);
}

.claim-row.is-selected::before {
  opacity: 1;
}

.claim-row-main,
.claim-row-verdict {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.claim-row-title {
  color: var(--pm-ink);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.claim-row-source,
.claim-row-verdict > span:last-child {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  line-height: 1.45;
  text-transform: uppercase;
}

.claim-row-verdict {
  justify-items: end;
}

.inspector-pane {
  position: sticky;
  top: 143px;
  min-height: 660px;
  max-height: calc(100vh - 164px);
  overflow: auto;
  background: var(--pm-inspection);
  color: var(--pm-inspection-text);
  scrollbar-color: var(--pm-inspection-rule) transparent;
  scrollbar-width: thin;
}

.inspector-pane:focus {
  outline-offset: -3px;
}

.inspector-content {
  min-width: 0;
}

.inspector-swap-enter-active,
.inspector-swap-leave-active {
  transition:
    opacity var(--pm-motion-fast) ease,
    transform var(--pm-motion-fast) var(--pm-ease-out);
}

.inspector-swap-enter-from,
.inspector-swap-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

.inspector-header {
  position: sticky;
  z-index: 2;
  top: 0;
  min-height: 76px;
  border-color: var(--pm-inspection-rule);
  background: var(--pm-inspection);
}

.inspector-header > div {
  display: grid;
  gap: 8px;
}

.confidence-value {
  color: var(--pm-inspection-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.inspector-claim {
  padding: 20px;
  border-bottom: 1px solid var(--pm-inspection-rule);
}

.inspector-claim h4 {
  max-width: 34ch;
  color: var(--pm-inspection-text);
  font-size: 19px;
  letter-spacing: -0.018em;
  line-height: 1.45;
}

.inspector-claim dl {
  display: grid;
  gap: 14px;
  margin: 18px 0 0;
}

.inspector-claim dl div {
  display: grid;
  gap: 5px;
}

.inspector-claim dt,
.evidence-document-heading span,
.evidence-explanation h5,
.evidence-keywords h5,
.claim-next-steps h5 {
  color: var(--pm-inspection-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  font-weight: 400;
  letter-spacing: 0.08em;
  line-height: 1.4;
  text-transform: uppercase;
}

.inspector-claim dd {
  margin: 0;
  color: var(--pm-inspection-text);
  font-size: 13px;
  line-height: 1.65;
}

.evidence-document {
  padding: 20px;
  border-bottom: 1px solid var(--pm-inspection-rule);
  background: var(--pm-inspection-deep);
}

.evidence-document-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.evidence-document-heading h4 {
  margin-top: 5px;
  color: var(--pm-inspection-text);
  font-size: 15px;
  line-height: 1.45;
}

.evidence-document-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.evidence-document-actions button {
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--pm-inspection-rule);
  border-radius: var(--pm-radius-sm);
  background: transparent;
  color: var(--pm-inspection-text);
  cursor: pointer;
  font: 600 10px var(--pm-font-sans);
  transition:
    border-color var(--pm-motion-fast) ease,
    background-color var(--pm-motion-fast) ease;
}

.evidence-document-actions button:hover {
  border-color: var(--pm-inspection-muted);
}

.evidence-path {
  display: block;
  margin-top: 14px;
  color: var(--pm-inspection-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.evidence-metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 10px;
  color: var(--pm-inspection-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.evidence-document pre {
  max-width: 100%;
  margin: 18px -20px 0;
  padding: 18px 20px;
  overflow: auto;
  border-top: 1px solid var(--pm-inspection-rule);
  border-bottom: 1px solid var(--pm-inspection-rule);
  background: var(--pm-inspection);
  color: var(--pm-inspection-text);
  font: 11px/1.75 var(--pm-font-mono);
  tab-size: 2;
  white-space: pre;
  scrollbar-color: var(--pm-inspection-rule) transparent;
  scrollbar-width: thin;
}

.evidence-document pre.is-wrapped {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.evidence-explanation,
.evidence-keywords,
.claim-next-steps section {
  margin-top: 18px;
}

.evidence-explanation h5,
.evidence-keywords h5,
.claim-next-steps h5 {
  margin: 0;
}

.evidence-explanation p,
.claim-next-steps p {
  margin: 7px 0 0;
  color: var(--pm-inspection-text);
  font-size: 13px;
  line-height: 1.7;
}

.evidence-keywords code {
  display: block;
  margin-top: 7px;
  color: var(--pm-inspection-text);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  line-height: 1.65;
}

.evidence-source-list {
  display: grid;
  border-bottom: 1px solid var(--pm-inspection-rule);
}

.evidence-source-list button {
  display: grid;
  min-width: 0;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 3px 8px;
  padding: 11px 20px;
  border: 0;
  border-bottom: 1px solid var(--pm-inspection-rule);
  background: transparent;
  color: var(--pm-inspection-text);
  cursor: pointer;
  text-align: left;
  transition: background-color var(--pm-motion-fast) ease;
}

.evidence-source-list button:last-child {
  border-bottom: 0;
}

.evidence-source-list button:hover,
.evidence-source-list button.is-selected {
  background: rgba(232, 239, 249, 0.06);
}

.evidence-source-list button > span {
  grid-row: 1 / 3;
  color: var(--pm-inspection-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
}

.evidence-source-list strong {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.claim-next-steps {
  padding: 2px 20px 22px;
}

.inspector-placeholder {
  margin: 0;
  padding: 32px 20px;
  color: var(--pm-inspection-muted);
  line-height: 1.65;
}

.inspector-pane :deep(.empty-state) {
  border-color: var(--pm-inspection-rule);
  background: var(--pm-inspection);
  color: var(--pm-inspection-muted);
}

.inspector-pane :deep(.empty-state h3) {
  color: var(--pm-inspection-text);
}

@media (max-width: 1180px) {
  .evidence-workbench {
    grid-template-columns: minmax(300px, 0.82fr) minmax(360px, 1.18fr);
  }

  .repository-pane {
    min-height: auto;
    grid-column: 1 / -1;
    border-right: 0;
    border-bottom: 1px solid var(--pm-stone);
  }

  .repository-files {
    display: grid;
    max-height: 184px;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .repository-files li {
    border-right: 1px solid var(--pm-stone);
  }
}

@media (max-width: 700px) {
  .evidence-surface-header,
  .evidence-document-heading,
  .section-error {
    align-items: flex-start;
    flex-direction: column;
  }

  .evidence-workbench {
    display: block;
    overflow: visible;
    border-right: 0;
    border-left: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .repository-files {
    grid-template-columns: 1fr;
  }

  .claim-pane,
  .inspector-pane {
    min-height: auto;
    border-right: 0;
  }

  .claim-list {
    max-height: none;
  }

  .claim-row {
    min-height: 104px;
    padding-right: 14px;
    padding-left: 14px;
  }

  .inspector-pane {
    position: relative;
    top: auto;
    max-height: none;
    margin-top: 18px;
    scroll-margin-top: 118px;
  }

  .inspector-header {
    position: static;
  }

  .evidence-document-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 460px) {
  .claim-row {
    grid-template-columns: 1fr;
  }

  .claim-row-verdict,
  .claim-status-summary {
    justify-items: start;
  }

  .claim-status-summary {
    display: none;
  }

  .pane-header,
  .inspector-header,
  .inspector-claim,
  .evidence-document {
    padding-right: 16px;
    padding-left: 16px;
  }

  .evidence-document pre {
    margin-right: -16px;
    margin-left: -16px;
    padding-right: 16px;
    padding-left: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .claim-row,
  .claim-row::before,
  .inspector-swap-enter-active,
  .inspector-swap-leave-active,
  .evidence-document-actions button,
  .evidence-source-list button {
    transition: none;
  }

  .inspector-swap-enter-from,
  .inspector-swap-leave-to {
    opacity: 1;
    transform: none;
  }
}
</style>
