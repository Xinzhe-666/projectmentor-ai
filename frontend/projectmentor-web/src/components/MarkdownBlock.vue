<template>
  <div class="markdown-block" v-html="html" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const props = defineProps<{
  content?: string
}>()

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code: string, language: string): string {
    if (language && hljs.getLanguage(language)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(code, { language }).value}</code></pre>`
      } catch {
        return ''
      }
    }

    return `<pre class="hljs"><code>${escapeHtml(code)}</code></pre>`
  }
})

const html = computed(() => md.render(props.content?.trim() || '-'))
</script>

<style scoped>
.markdown-block {
  color: #344054;
  line-height: 1.8;
}

.markdown-block :deep(p:first-child) {
  margin-top: 0;
}

.markdown-block :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-block :deep(pre) {
  overflow: auto;
  padding: 14px;
  border-radius: 8px;
}

.markdown-block :deep(code) {
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
}

.markdown-block :deep(a) {
  color: var(--pm-primary);
  font-weight: 600;
}
</style>
