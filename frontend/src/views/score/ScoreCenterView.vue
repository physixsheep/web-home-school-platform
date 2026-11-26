<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { assignmentApi } from '../../api/assignment'
import { scoreApi } from '../../api/score'
import ScoreCharts from '../../components/charts/ScoreCharts.vue'
import { useAuthStore } from '../../stores/auth'
import type { ScoreItem, ScoreReport, ScoreTrend } from '../../types/score'

const auth = useAuthStore()
const isParent = computed(() => auth.primaryRole === 'PARENT')
const children = ref<{ id: number; label: string; secondary?: string }[]>([])
const selectedStudentId = ref<number>()
const report = ref<ScoreReport | null>(null)
const loading = ref(false)
const selectedYear = ref('')

const academicYears = computed(() => {
  const years = new Set(report.value?.items.map((item) => item.academicYear) ?? [])
  return [...years].sort().reverse()
})

const filteredItems = computed(() =>
  (report.value?.items ?? []).filter(
    (item) => !selectedYear.value || item.academicYear === selectedYear.value,
  ),
)

const filteredTrends = computed(() =>
  (report.value?.trends ?? []).filter(
    (item) => !selectedYear.value || item.academicYear === selectedYear.value,
  ),
)

const latestItems = computed(() => {
  const first = filteredItems.value[0]
  return first
    ? filteredItems.value.filter((item) => item.examId === first.examId)
    : []
})

const latestTrend = computed(() => filteredTrends.value[filteredTrends.value.length - 1])
const latestFullScore = computed(() =>
  latestItems.value.reduce((sum, item) => sum + Number(item.fullScore), 0),
)
const latestPercentage = computed(() => {
  if (!latestTrend.value || latestFullScore.value <= 0) {
    return 0
  }
  return Math.min(
    100,
    Math.round((Number(latestTrend.value.totalScore) / latestFullScore.value) * 100),
  )
})

const examGroups = computed(() => {
  const groups = new Map<
    number,
    { trend: ScoreTrend; items: ScoreItem[] }
  >()
  for (const trend of filteredTrends.value) {
    groups.set(trend.examId, { trend, items: [] })
  }
  for (const item of filteredItems.value) {
    groups.get(item.examId)?.items.push(item)
  }
  return [...groups.values()].reverse()
})

async function loadOptions() {
  if (!isParent.value) {
    return
  }
  const options = await assignmentApi.options()
  children.value = options.children
  selectedStudentId.value = options.children[0]?.id
}

async function loadReport() {
  if (isParent.value && !selectedStudentId.value) {
    report.value = null
    return
  }
  loading.value = true
  try {
    report.value = await scoreApi.report(
      isParent.value ? selectedStudentId.value : undefined,
    )
    if (!academicYears.value.includes(selectedYear.value)) {
      selectedYear.value = academicYears.value[0] ?? ''
    }
  } finally {
    loading.value = false
  }
}

function formatScore(value?: number) {
  return value == null ? '—' : Number(value).toFixed(2)
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

onMounted(async () => {
  await loadOptions()
  await loadReport()
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>{{ isParent ? '孩子成绩' : '我的成绩' }}</h2>
        <p>查看考试成绩、学科得分和历次考试总分趋势。</p>
      </div>
      <div class="heading-controls">
        <ElSelect
          v-if="isParent"
          v-model="selectedStudentId"
          placeholder="选择孩子"
          style="width: 220px"
          @change="loadReport"
        >
          <ElOption
            v-for="child in children"
            :key="child.id"
            :label="`${child.label}（${child.secondary || child.id}）`"
            :value="child.id"
          />
        </ElSelect>
        <ElSelect
          v-model="selectedYear"
          placeholder="选择学年"
          style="width: 170px"
        >
          <ElOption
            v-for="year in academicYears"
            :key="year"
            :label="year"
            :value="year"
          />
        </ElSelect>
      </div>
    </header>

    <ElEmpty
      v-if="isParent && children.length === 0"
      description="当前账号尚未绑定学生"
    />

    <div v-else v-loading="loading" class="score-content">
      <section v-if="report" class="summary-card">
        <div>
          <p>{{ report.studentName }} · {{ report.studentNo }}</p>
          <h2>{{ latestTrend?.examName || '暂无考试成绩' }}</h2>
          <span v-if="latestTrend">
            {{ latestTrend.academicYear }} · {{ latestTrend.term }} ·
            {{ formatDate(latestTrend.examDate) }}
          </span>
        </div>
        <div class="summary-score">
          <strong>{{ formatScore(latestTrend?.totalScore) }}</strong>
          <span>/ {{ latestFullScore || '—' }}</span>
          <ElProgress
            :percentage="latestPercentage"
            :show-text="false"
            :stroke-width="8"
          />
        </div>
        <div class="summary-average">
          <span>学科平均分</span>
          <strong>{{ formatScore(latestTrend?.averageScore) }}</strong>
          <small>共 {{ latestTrend?.subjectCount || 0 }} 个学科</small>
        </div>
      </section>

      <ScoreCharts
        v-if="filteredTrends.length"
        :subject-scores="latestItems"
        :trends="filteredTrends"
      />
      <ElEmpty v-else description="当前学年暂无已发布成绩" />

      <section v-if="examGroups.length" class="exam-list">
        <article v-for="group in examGroups" :key="group.trend.examId" class="exam-card">
          <header>
            <div>
              <h3>{{ group.trend.examName }}</h3>
              <span>
                {{ group.trend.academicYear }} · {{ group.trend.term }} ·
                {{ formatDate(group.trend.examDate) }}
              </span>
            </div>
            <div>
              <strong>{{ formatScore(group.trend.totalScore) }}</strong>
              <small>总分</small>
            </div>
          </header>
          <ElTable :data="group.items" size="small">
            <ElTableColumn prop="subjectName" label="学科" width="120" />
            <ElTableColumn label="成绩" width="120">
              <template #default="{ row }">
                {{ row.isAbsent ? '缺考' : formatScore(row.score) }}
              </template>
            </ElTableColumn>
            <ElTableColumn label="满分" width="100">
              <template #default="{ row }">{{ row.fullScore }}</template>
            </ElTableColumn>
            <ElTableColumn prop="className" label="班级" min-width="120" />
            <ElTableColumn prop="remark" label="备注" min-width="160">
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </ElTableColumn>
          </ElTable>
        </article>
      </section>
    </div>
  </section>
</template>

<style scoped>
.heading-controls {
  display: flex;
  gap: 12px;
}

.score-content {
  display: grid;
  gap: 20px;
}

.summary-card {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(220px, 0.5fr) minmax(180px, 0.4fr);
  align-items: center;
  gap: 28px;
  padding: 30px 34px;
  border-radius: 18px;
  color: #fff;
  background:
    radial-gradient(circle at 92% 8%, rgba(111, 181, 255, 0.55), transparent 34%),
    linear-gradient(115deg, #0d397f, #1e6bce 68%, #3988e6);
  box-shadow: 0 18px 38px rgba(23, 80, 157, 0.18);
}

.summary-card p {
  margin: 0 0 10px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.summary-card h2 {
  margin: 0;
  font-size: 26px;
}

.summary-card > div > span {
  display: block;
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
}

.summary-score {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: baseline;
  gap: 6px;
}

.summary-score strong {
  font-size: 44px;
}

.summary-score span {
  color: rgba(255, 255, 255, 0.62);
}

.summary-score .el-progress {
  grid-column: 1 / -1;
  margin-top: 8px;
}

:deep(.summary-score .el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.16);
}

:deep(.summary-score .el-progress-bar__inner) {
  background: #fff;
}

.summary-average {
  display: grid;
  padding-left: 24px;
  border-left: 1px solid rgba(255, 255, 255, 0.18);
}

.summary-average span,
.summary-average small {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
}

.summary-average strong {
  margin: 6px 0;
  font-size: 34px;
}

.exam-list {
  display: grid;
  gap: 16px;
}

.exam-card {
  overflow: hidden;
  border: 1px solid #e7ecf4;
  border-radius: 15px;
  background: #fff;
}

.exam-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px;
  border-bottom: 1px solid #edf1f7;
}

.exam-card h3 {
  margin: 0 0 6px;
  color: #20324e;
  font-size: 17px;
}

.exam-card header span {
  color: #8a96a8;
  font-size: 12px;
}

.exam-card header > div:last-child {
  text-align: right;
}

.exam-card header strong,
.exam-card header small {
  display: block;
}

.exam-card header strong {
  color: #2463b8;
  font-size: 24px;
}

.exam-card header small {
  color: #8a96a8;
  font-size: 11px;
}
</style>

