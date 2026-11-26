<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { scoreApi } from '../../api/score'
import type { TeachingOption } from '../../types/assignment'
import type {
  ExamOption,
  ScoreEntryRow,
  ScoreStats,
} from '../../types/score'

const exams = ref<ExamOption[]>([])
const teachings = ref<TeachingOption[]>([])
const selectedExamId = ref<number>()
const selectedTeachingKey = ref('')
const rows = ref<ScoreEntryRow[]>([])
const stats = ref<ScoreStats | null>(null)
const loading = ref(false)
const saving = ref(false)

const compatibleExams = computed(() => {
  const teaching = selectedTeaching.value
  if (!teaching) {
    return exams.value
  }
  return exams.value.filter(
    (exam) =>
      (exam.classId && exam.classId === teaching.classId) ||
      (!exam.classId && exam.gradeId === teaching.gradeId),
  )
})

const selectedTeaching = computed(() =>
  teachings.value.find(
    (item) => `${item.classId}:${item.subjectId}` === selectedTeachingKey.value,
  ),
)

async function loadOptions() {
  const result = await scoreApi.workspaceOptions()
  exams.value = result.exams
  teachings.value = result.teachings
  selectedTeachingKey.value = teachings.value[0]
    ? `${teachings.value[0].classId}:${teachings.value[0].subjectId}`
    : ''
  selectedExamId.value = compatibleExams.value[0]?.id
  if (selectedExamId.value && selectedTeaching.value) {
    await loadEntry()
  }
}

function onTeachingChange() {
  if (
    selectedExamId.value &&
    !compatibleExams.value.some((item) => item.id === selectedExamId.value)
  ) {
    selectedExamId.value = compatibleExams.value[0]?.id
  }
  void loadEntry()
}

async function loadEntry() {
  if (!selectedExamId.value || !selectedTeaching.value) {
    rows.value = []
    stats.value = null
    return
  }
  loading.value = true
  try {
    const teaching = selectedTeaching.value
    const [entryRows, statistics] = await Promise.all([
      scoreApi.entryRows(
        selectedExamId.value,
        teaching.classId,
        teaching.subjectId,
      ),
      scoreApi.statistics(
        selectedExamId.value,
        teaching.classId,
        teaching.subjectId,
      ),
    ])
    rows.value = entryRows.map((item) => ({
      ...item,
      score: item.score == null ? undefined : Number(item.score),
    }))
    stats.value = statistics
  } finally {
    loading.value = false
  }
}

function onAbsentChange(row: ScoreEntryRow) {
  if (row.isAbsent === 1) {
    row.score = undefined
  }
}

async function saveScores() {
  if (!selectedExamId.value || !selectedTeaching.value) {
    return
  }
  const records = rows.value
    .filter((row) => row.isAbsent === 1 || row.score !== undefined)
    .map((row) => ({
      studentId: row.studentId,
      score: row.isAbsent === 1 ? undefined : row.score,
      absent: row.isAbsent === 1,
      remark: row.remark || undefined,
    }))
  if (records.length === 0) {
    ElMessage.warning('请至少填写一名学生的成绩或标记缺考')
    return
  }
  saving.value = true
  try {
    const teaching = selectedTeaching.value
    await scoreApi.batchSave({
      examId: selectedExamId.value,
      classId: teaching.classId,
      subjectId: teaching.subjectId,
      records,
    })
    ElMessage.success('成绩已保存')
    await loadEntry()
  } finally {
    saving.value = false
  }
}

function formatAverage(value?: number) {
  return value == null ? '—' : Number(value).toFixed(2)
}

onMounted(loadOptions)
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>成绩管理</h2>
        <p>选择已发布考试和本人任教学科，录入当前班级学生成绩。</p>
      </div>
      <ElButton :disabled="!rows.length" :loading="saving" type="primary" @click="saveScores">
        保存成绩
      </ElButton>
    </header>

    <div class="score-toolbar">
      <div>
        <label>考试</label>
        <ElSelect
          v-model="selectedExamId"
          placeholder="选择考试"
          style="width: 320px"
          @change="loadEntry"
        >
          <ElOption
            v-for="exam in compatibleExams"
            :key="exam.id"
            :label="`${exam.name}（${exam.examDate}）`"
            :value="exam.id"
          />
        </ElSelect>
      </div>
      <div>
        <label>班级学科</label>
        <ElSelect
          v-model="selectedTeachingKey"
          placeholder="选择班级和学科"
          style="width: 300px"
          @change="onTeachingChange"
        >
          <ElOption
            v-for="item in teachings"
            :key="`${item.classId}:${item.subjectId}`"
            :label="`${item.gradeName}${item.className} · ${item.subjectName}`"
            :value="`${item.classId}:${item.subjectId}`"
          />
        </ElSelect>
      </div>
    </div>

    <div class="stats-grid">
      <article>
        <span>已录入</span>
        <strong>{{ stats?.enteredCount ?? 0 }}</strong>
      </article>
      <article>
        <span>平均分</span>
        <strong>{{ formatAverage(stats?.averageScore) }}</strong>
      </article>
      <article>
        <span>最高分</span>
        <strong>{{ formatAverage(stats?.highestScore) }}</strong>
      </article>
      <article>
        <span>最低分</span>
        <strong>{{ formatAverage(stats?.lowestScore) }}</strong>
      </article>
      <article>
        <span>缺考</span>
        <strong>{{ stats?.absentCount ?? 0 }}</strong>
      </article>
    </div>

    <div class="data-card">
      <div class="table-wrap">
        <ElTable
          v-loading="loading"
          :data="rows"
          empty-text="请选择考试和任教班级学科"
          stripe
        >
          <ElTableColumn label="学生" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.studentName }}</strong>
              <div class="muted-text">{{ row.studentNo }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="缺考" width="110">
            <template #default="{ row }">
              <ElSwitch
                v-model="row.isAbsent"
                :active-value="1"
                :inactive-value="0"
                @change="onAbsentChange(row)"
              />
            </template>
          </ElTableColumn>
          <ElTableColumn label="成绩" width="190">
            <template #default="{ row }">
              <ElInputNumber
                v-model="row.score"
                :disabled="row.isAbsent === 1"
                :min="0"
                :max="selectedTeaching?.fullScore || 999"
                :precision="2"
                placeholder="未录入"
              />
              <span class="full-score">/ {{ selectedTeaching?.fullScore }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="备注" min-width="240">
            <template #default="{ row }">
              <ElInput v-model="row.remark" maxlength="255" placeholder="可选" />
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>
  </section>
</template>

<style scoped>
.score-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 20px 22px;
  border: 1px solid #e7ecf4;
  border-radius: 15px;
  background: #fff;
}

.score-toolbar label {
  display: block;
  margin-bottom: 9px;
  color: #66758b;
  font-size: 12px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}

.stats-grid article {
  padding: 20px;
  border: 1px solid #e7ecf4;
  border-radius: 13px;
  background: linear-gradient(145deg, #fff, #f6faff);
}

.stats-grid span,
.stats-grid strong {
  display: block;
}

.stats-grid span {
  color: #8895a7;
  font-size: 12px;
}

.stats-grid strong {
  margin-top: 8px;
  color: #1d5fb9;
  font-size: 24px;
}

.full-score {
  margin-left: 7px;
  color: #94a0b1;
  font-size: 12px;
}
</style>

