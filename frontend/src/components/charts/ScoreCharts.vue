<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

import type { ScoreItem, ScoreTrend } from '../../types/score'

use([
  BarChart,
  LineChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer,
])

const props = defineProps<{
  trends: ScoreTrend[]
  subjectScores: ScoreItem[]
}>()

const lineElement = ref<HTMLDivElement>()
const barElement = ref<HTMLDivElement>()
let lineChart: ECharts | null = null
let barChart: ECharts | null = null

const axisStyle = {
  axisLine: { lineStyle: { color: '#dce4ef' } },
  axisTick: { show: false },
  axisLabel: { color: '#7c899c', fontSize: 11 },
  splitLine: { lineStyle: { color: '#edf2f8' } },
}

function renderLine() {
  if (!lineChart) {
    return
  }
  const option: EChartsCoreOption = {
    tooltip: { trigger: 'axis' },
    grid: { top: 28, left: 42, right: 18, bottom: 36 },
    xAxis: {
      type: 'category',
      data: props.trends.map((item) => item.examName),
      ...axisStyle,
    },
    yAxis: {
      type: 'value',
      ...axisStyle,
    },
    series: [
      {
        name: '总分',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: props.trends.map((item) => Number(item.totalScore)),
        lineStyle: { width: 3, color: '#2876dc' },
        itemStyle: { color: '#2876dc' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(40,118,220,.24)' },
              { offset: 1, color: 'rgba(40,118,220,.02)' },
            ],
          },
        },
      },
    ],
  }
  lineChart.setOption(option, true)
}

function renderBar() {
  if (!barChart) {
    return
  }
  const option: EChartsCoreOption = {
    tooltip: { trigger: 'axis' },
    grid: { top: 28, left: 42, right: 18, bottom: 36 },
    xAxis: {
      type: 'category',
      data: props.subjectScores.map((item) => item.subjectName),
      ...axisStyle,
    },
    yAxis: {
      type: 'value',
      ...axisStyle,
    },
    series: [
      {
        name: '成绩',
        type: 'bar',
        barMaxWidth: 34,
        data: props.subjectScores.map((item) =>
          item.isAbsent ? 0 : Number(item.score ?? 0),
        ),
        itemStyle: {
          borderRadius: [8, 8, 2, 2],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#4b91ef' },
              { offset: 1, color: '#1e5fbd' },
            ],
          },
        },
      },
    ],
  }
  barChart.setOption(option, true)
}

function resizeCharts() {
  lineChart?.resize()
  barChart?.resize()
}

watch(
  () => [props.trends, props.subjectScores],
  async () => {
    await nextTick()
    renderLine()
    renderBar()
  },
  { deep: true },
)

onMounted(() => {
  if (lineElement.value && barElement.value) {
    lineChart = init(lineElement.value)
    barChart = init(barElement.value)
    renderLine()
    renderBar()
    window.addEventListener('resize', resizeCharts)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  lineChart?.dispose()
  barChart?.dispose()
})
</script>

<template>
  <div class="chart-grid">
    <article class="chart-card">
      <div class="chart-heading">
        <h3>成绩变化趋势</h3>
        <span>按考试日期展示个人总分</span>
      </div>
      <div ref="lineElement" class="chart-canvas" />
    </article>
    <article class="chart-card">
      <div class="chart-heading">
        <h3>最近一次学科成绩</h3>
        <span>各学科实际得分</span>
      </div>
      <div ref="barElement" class="chart-canvas" />
    </article>
  </div>
</template>

<style scoped>
.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  padding: 22px;
  border: 1px solid #e7ecf4;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(22, 53, 97, 0.05);
}

.chart-heading h3 {
  margin: 0;
  color: #20324e;
  font-size: 16px;
}

.chart-heading span {
  display: block;
  margin-top: 6px;
  color: #8a96a8;
  font-size: 12px;
}

.chart-canvas {
  height: 280px;
  margin-top: 14px;
}

@media (max-width: 1280px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>

