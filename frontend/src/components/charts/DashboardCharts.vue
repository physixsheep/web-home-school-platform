<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

import type {
  NameValue,
  TeacherClassStat,
} from '../../types/dashboard'

use([
  BarChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer,
])

const props = defineProps<{
  mode: 'admin' | 'teacher'
  primary: NameValue[] | TeacherClassStat[]
  secondary: NameValue[]
}>()

const firstElement = ref<HTMLDivElement>()
const secondElement = ref<HTMLDivElement>()
let firstChart: ECharts | null = null
let secondChart: ECharts | null = null

const axis = {
  axisLine: { lineStyle: { color: '#dce4ef' } },
  axisTick: { show: false },
  axisLabel: { color: '#7c899c', fontSize: 11 },
  splitLine: { lineStyle: { color: '#edf2f8' } },
}

function renderFirst() {
  if (!firstChart) {
    return
  }
  const option: EChartsCoreOption =
    props.mode === 'admin'
      ? {
          tooltip: { trigger: 'item' },
          legend: { bottom: 0, textStyle: { color: '#6e7c91' } },
          series: [
            {
              type: 'pie',
              radius: ['46%', '70%'],
              center: ['50%', '44%'],
              data: (props.primary as NameValue[]).map((item) => ({
                name: item.name,
                value: item.value,
              })),
              label: { color: '#53627a', formatter: '{b}\n{c}人' },
              itemStyle: {
                borderColor: '#fff',
                borderWidth: 3,
                borderRadius: 5,
              },
              color: ['#2d73d3', '#51a2e8', '#78c8c0', '#e3a34c', '#8a79dc'],
            },
          ],
        }
      : {
          tooltip: { trigger: 'axis' },
          legend: { top: 0, textStyle: { color: '#6e7c91' } },
          grid: { top: 42, left: 44, right: 18, bottom: 40 },
          xAxis: {
            type: 'category',
            data: (props.primary as TeacherClassStat[]).map(
              (item) => item.gradeName + item.className,
            ),
            ...axis,
          },
          yAxis: { type: 'value', ...axis },
          series: [
            {
              name: '学生人数',
              type: 'bar',
              barMaxWidth: 30,
              data: (props.primary as TeacherClassStat[]).map(
                (item) => item.studentCount,
              ),
              itemStyle: { color: '#2d73d3', borderRadius: [7, 7, 2, 2] },
            },
            {
              name: '已交作业',
              type: 'bar',
              barMaxWidth: 30,
              data: (props.primary as TeacherClassStat[]).map(
                (item) => item.submittedCount,
              ),
              itemStyle: { color: '#6bb9aa', borderRadius: [7, 7, 2, 2] },
            },
          ],
        }
  firstChart.setOption(option, true)
}

function renderSecond() {
  if (!secondChart) {
    return
  }
  const data = props.secondary
  const option: EChartsCoreOption = {
    tooltip: { trigger: 'axis' },
    grid: { top: 22, left: 42, right: 18, bottom: 36 },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      ...axis,
    },
    yAxis: { type: 'value', ...axis },
    series: [
      {
        type: 'bar',
        barMaxWidth: 34,
        data: data.map((item) => item.value),
        itemStyle: {
          borderRadius: [8, 8, 2, 2],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#5d9bea' },
              { offset: 1, color: '#1f60bc' },
            ],
          },
        },
      },
    ],
  }
  secondChart.setOption(option, true)
}

function resize() {
  firstChart?.resize()
  secondChart?.resize()
}

watch(
  () => [props.primary, props.secondary, props.mode],
  async () => {
    await nextTick()
    renderFirst()
    renderSecond()
  },
  { deep: true },
)

onMounted(() => {
  if (firstElement.value && secondElement.value) {
    firstChart = init(firstElement.value)
    secondChart = init(secondElement.value)
    renderFirst()
    renderSecond()
    window.addEventListener('resize', resize)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  firstChart?.dispose()
  secondChart?.dispose()
})
</script>

<template>
  <div class="dashboard-charts">
    <article class="chart-card">
      <div class="chart-heading">
        <h3>{{ mode === 'admin' ? '各年级学生分布' : '班级学生与作业提交' }}</h3>
        <span>{{ mode === 'admin' ? '当前在读学生按年级统计' : '各班级当前学生和累计提交量' }}</span>
      </div>
      <div ref="firstElement" class="chart-canvas" />
    </article>
    <article class="chart-card">
      <div class="chart-heading">
        <h3>{{ mode === 'admin' ? '已发布通知范围' : '待处理事项' }}</h3>
        <span>{{ mode === 'admin' ? '全校、年级和班级通知数量' : '待批改作业与待审批请假' }}</span>
      </div>
      <div ref="secondElement" class="chart-canvas" />
    </article>
  </div>
</template>

<style scoped>
.dashboard-charts {
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
  height: 300px;
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .dashboard-charts {
    grid-template-columns: 1fr;
  }
}
</style>

