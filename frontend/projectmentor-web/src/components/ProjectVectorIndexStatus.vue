<template>
  <section class="vector-index-status" aria-labelledby="vector-index-title">
    <div>
      <span class="index-state-marker" :class="`is-${normalizedStatus.toLowerCase()}`" aria-hidden="true" />
      <div>
        <h3 id="vector-index-title">{{ copy.title }}</h3>
        <p>{{ statusDescription }}</p>
        <code v-if="status?.lastErrorCode">{{ status.lastErrorCode }}</code>
      </div>
    </div>
    <dl v-if="status?.status === 'READY'">
      <div><dt>{{ copy.files }}</dt><dd>{{ status.indexedFileCount }}</dd></div>
      <div><dt>{{ copy.chunks }}</dt><dd>{{ status.chunkCount }}</dd></div>
    </dl>
    <el-button :loading="rebuilding || loading" :disabled="isActive" @click="rebuild">
      {{ isActive ? `${copy.indexing} ${status?.progress ?? 0}%` : copy.rebuild }}
    </el-button>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getVectorIndexStatus, rebuildVectorIndex } from '@/api/project'
import type { VectorIndexStatus } from '@/types/api'

const props = defineProps<{ projectId: number }>()
const { locale } = useI18n()
const status = ref<VectorIndexStatus>()
const loading = ref(false)
const rebuilding = ref(false)
let timer: number | undefined
const zh = computed(() => locale.value.startsWith('zh'))
const copy = computed(() => zh.value ? {
  title: '项目证据索引', files: '文件', chunks: '证据块', rebuild: '重新建立索引', indexing: '正在索引',
  unavailable: '混合检索尚未配置；问答将继续使用基础检索。', failed: '索引失败；问答已自动降级为基础检索。'
} : {
  title: 'Project evidence index', files: 'Files', chunks: 'Chunks', rebuild: 'Rebuild index', indexing: 'Indexing',
  unavailable: 'Hybrid retrieval is not configured; Q&A continues with basic retrieval.', failed: 'Indexing failed; Q&A has fallen back to basic retrieval.'
})
const normalizedStatus = computed(() => status.value?.status || 'NOT_INDEXED')
const isActive = computed(() => ['PENDING', 'RUNNING'].includes(normalizedStatus.value))
const statusDescription = computed(() => {
  if (!status.value) return copy.value.unavailable
  if (status.value.status === 'FAILED') return copy.value.failed
  if (status.value.status === 'DEGRADED') return zh.value ? '内容已变化；旧索引仍可查询，建议重建。' : 'Content changed; the previous index remains queryable. Rebuild is recommended.'
  if (status.value.status === 'READY') return zh.value ? '混合证据检索可用，引用会显示文件和真实行号。' : 'Hybrid evidence retrieval is ready with file and line citations.'
  return status.value.displayStatus || copy.value.unavailable
})

async function load() {
  loading.value = !status.value
  try {
    status.value = await getVectorIndexStatus(props.projectId)
    if (isActive.value) schedulePoll()
  } catch { status.value = undefined }
  finally { loading.value = false }
}
async function rebuild() {
  rebuilding.value = true
  try {
    status.value = await rebuildVectorIndex(props.projectId)
    ElMessage.success(zh.value ? '索引任务已提交' : 'Index task submitted')
    schedulePoll()
  } catch { ElMessage.warning(copy.value.unavailable) }
  finally { rebuilding.value = false }
}
function schedulePoll() { window.clearTimeout(timer); timer = window.setTimeout(load, 2000) }
onMounted(load)
onBeforeUnmount(() => window.clearTimeout(timer))
</script>

<style scoped>
.vector-index-status { display:flex; align-items:center; justify-content:space-between; gap:24px; padding:14px 0; border-bottom:1px solid var(--pm-stone-strong); }
.vector-index-status > div:first-child { display:flex; min-width:0; align-items:flex-start; gap:12px; }
.index-state-marker { width:9px; height:9px; flex:0 0 9px; margin-top:6px; background:var(--pm-muted); }
.index-state-marker.is-ready { background:var(--pm-supported); }
.index-state-marker.is-pending,.index-state-marker.is-running { background:var(--pm-partial); }
.index-state-marker.is-failed { background:var(--pm-risk); }
.vector-index-status h3 { margin:0; color:var(--pm-ink); font-size:14px; }
.vector-index-status p { max-width:68ch; margin:4px 0 0; color:var(--pm-muted); font-size:12px; line-height:1.55; }
.vector-index-status code { display:block; margin-top:5px; color:var(--pm-risk); font-size:10px; }
.vector-index-status dl { display:flex; gap:20px; margin:0; }
.vector-index-status dl div { display:grid; gap:2px; }
.vector-index-status dt { color:var(--pm-muted); font-size:10px; }
.vector-index-status dd { margin:0; color:var(--pm-ink); font-family:var(--pm-font-mono); font-variant-numeric:tabular-nums; }
.vector-index-status :deep(.el-button) { min-height:44px; flex:0 0 auto; }
@media (max-width:700px) { .vector-index-status { align-items:stretch; flex-direction:column; gap:12px; } .vector-index-status dl { border-top:1px solid var(--pm-stone); padding-top:10px; } .vector-index-status :deep(.el-button) { width:100%; } }
</style>
