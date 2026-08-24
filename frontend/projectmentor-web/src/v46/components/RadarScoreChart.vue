<template>
  <div ref="chartRef" class="radar-chart" />
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

export interface RadarScores {
  runnabilityScore?: number
  authenticityScore?: number
  structureScore?: number
  readmeScore?: number
  securityScore?: number
  engineeringScore?: number
  interviewScore?: number
}

const props = defineProps<{
  scores: RadarScores
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const labels: Array<{ key: keyof RadarScores; name: string }> = [
  { key: 'runnabilityScore', name: '可运行性' },
  { key: 'authenticityScore', name: '真实性' },
  { key: 'structureScore', name: '结构' },
  { key: 'readmeScore', name: 'README' },
  { key: 'securityScore', name: '安全' },
  { key: 'engineeringScore', name: '工程化' },
  { key: 'interviewScore', name: '面试价值' }
]

function scoreValue(value: number | undefined) {
  if (!Number.isFinite(value)) {
    return 0
  }

  return Math.max(0, Math.min(100, Number(value)))
}

function renderChart() {
  if (!chartRef.value) {
    return
  }

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption({
    color: ['#1f6feb'],
    tooltip: {
      trigger: 'item'
    },
    radar: {
      radius: '68%',
      splitNumber: 4,
      axisName: {
        color: '#475467',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: ['#e8edf5']
        }
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(31, 111, 235, 0.03)', 'rgba(20, 184, 166, 0.05)']
        }
      },
      axisLine: {
        lineStyle: {
          color: '#d9e2ef'
        }
      },
      indicator: labels.map((label) => ({
        name: label.name,
        max: 100
      }))
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: labels.map((label) => scoreValue(props.scores[label.key])),
            name: '项目评分',
            areaStyle: {
              color: 'rgba(31, 111, 235, 0.18)'
            },
            lineStyle: {
              width: 2
            },
            symbolSize: 5
          }
        ]
      }
    ]
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(async () => {
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

watch(
  () => props.scores,
  () => {
    nextTick(renderChart)
  },
  { deep: true }
)

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.radar-chart {
  width: 100%;
  min-height: 360px;
}
</style>
