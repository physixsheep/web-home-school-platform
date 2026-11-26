<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { adminApi } from '../../api/admin'
import type {
  EntityOption,
  TeacherAssignmentRow,
} from '../../types/organization'

const rows = ref<TeacherAssignmentRow[]>([])
const teachers = ref<EntityOption[]>([])
const classes = ref<EntityOption[]>([])
const subjects = ref<EntityOption[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const query = reactive({
  keyword: '',
  relationType: '',
  status: '',
  page: 1,
  size: 10,
})
const form = reactive({
  teacherId: undefined as number | undefined,
  classId: undefined as number | undefined,
  subjectId: undefined as number | undefined,
  relationType: 'SUBJECT' as 'HOMEROOM' | 'SUBJECT',
  startDate: new Date().toISOString().slice(0, 10),
  endDate: '',
})
const rules: FormRules<typeof form> = {
  teacherId: [{ required: true, message: '请选择教师', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关联类型', trigger: 'change' }],
  subjectId: [
    {
      validator: (_rule, value, callback) => {
        if (form.relationType === 'SUBJECT' && !value) {
          callback(new Error('任课关联必须选择学科'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const result = await adminApi.teacherAssignments({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      relationType: query.relationType || undefined,
      status: query.status || undefined,
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [teacherOptions, classOptions, subjectOptions] = await Promise.all([
    adminApi.teacherOptions(),
    adminApi.classOptions(),
    adminApi.subjectOptions(),
  ])
  teachers.value = teacherOptions
  classes.value = classOptions
  subjects.value = subjectOptions
}

function search() {
  query.page = 1
  void loadData()
}

function resetFilters() {
  query.keyword = ''
  query.relationType = ''
  query.status = ''
  search()
}

function openCreate() {
  Object.assign(form, {
    teacherId: undefined,
    classId: undefined,
    subjectId: undefined,
    relationType: 'SUBJECT',
    startDate: new Date().toISOString().slice(0, 10),
    endDate: '',
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.teacherId || !form.classId) {
    return
  }
  saving.value = true
  try {
    await adminApi.createTeacherAssignment({
      teacherId: form.teacherId,
      classId: form.classId,
      subjectId: form.relationType === 'SUBJECT' ? form.subjectId : undefined,
      relationType: form.relationType,
      startDate: form.startDate,
      endDate: form.endDate || undefined,
    })
    ElMessage.success('教师班级关联已创建')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function endAssignment(row: TeacherAssignmentRow) {
  try {
    await ElMessageBox.confirm(
      `确认结束 ${row.teacherName} 与 ${row.gradeName}${row.className} 的当前关联吗？`,
      '结束关联',
      {
        type: 'warning',
        confirmButtonText: '结束',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  await adminApi.endTeacherAssignment(row.id)
  ElMessage.success('教师班级关联已结束')
  await loadData()
}

function optionLabel(options: EntityOption[], id?: number) {
  if (!id) {
    return '—'
  }
  const option = options.find((item) => item.id === id)
  return option ? `${option.label}（${option.secondary || id}）` : `ID ${id}`
}

watch(
  () => form.relationType,
  (value) => {
    if (value === 'HOMEROOM') {
      form.subjectId = undefined
    }
  },
)

onMounted(async () => {
  await Promise.all([loadData(), loadOptions()])
})
</script>

<template>
  <section class="management-page">
    <header class="page-heading">
      <div>
        <h2>教师任课</h2>
        <p>班级任课教师可发布本班通知；班主任负责请假审批，二者在数据权限中分开校验。</p>
      </div>
      <ElButton type="primary" @click="openCreate">新增关联</ElButton>
    </header>

    <div class="data-card">
      <div class="filter-bar">
        <ElInput
          v-model="query.keyword"
          clearable
          placeholder="教师 / 工号 / 班级"
          @keyup.enter="search"
        />
        <ElSelect v-model="query.relationType" clearable placeholder="全部类型">
          <ElOption label="班主任" value="HOMEROOM" />
          <ElOption label="任课教师" value="SUBJECT" />
        </ElSelect>
        <ElSelect v-model="query.status" clearable placeholder="全部状态">
          <ElOption label="有效" value="ACTIVE" />
          <ElOption label="已结束" value="ENDED" />
        </ElSelect>
        <ElButton type="primary" @click="search">查询</ElButton>
        <ElButton @click="resetFilters">重置</ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="rows" empty-text="暂无教师班级关联" stripe>
          <ElTableColumn label="教师" min-width="150">
            <template #default="{ row }">
              <strong>{{ row.teacherName }}</strong>
              <div class="muted-text">{{ row.employeeNo }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="班级" min-width="130">
            <template #default="{ row }">
              {{ row.gradeName }}{{ row.className }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="类型" width="110">
            <template #default="{ row }">
              <ElTag :type="row.relationType === 'HOMEROOM' ? 'warning' : 'primary'" effect="light">
                {{ row.relationType === 'HOMEROOM' ? '班主任' : '任课教师' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="subjectName" label="学科" width="110">
            <template #default="{ row }">{{ row.subjectName || '—' }}</template>
          </ElTableColumn>
          <ElTableColumn label="有效期" width="220">
            <template #default="{ row }">
              {{ row.startDate }} 至 {{ row.endDate || '现在' }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
                {{ row.status === 'ACTIVE' ? '有效' : '已结束' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <ElButton
                v-if="row.status === 'ACTIVE'"
                link
                type="danger"
                @click="endAssignment(row)"
              >
                结束
              </ElButton>
              <span v-else class="muted-text">已完成</span>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>

      <div class="pagination-row">
        <ElPagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="search"
        />
      </div>
    </div>

    <ElDialog v-model="dialogVisible" title="新增教师班级关联" width="580px">
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="104px">
        <ElFormItem label="教师" prop="teacherId">
          <ElSelect
            v-model="form.teacherId"
            filterable
            placeholder="选择教师"
            style="width: 100%"
          >
            <ElOption
              v-for="item in teachers"
              :key="item.id"
              :label="optionLabel(teachers, item.id)"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="班级" prop="classId">
          <ElSelect
            v-model="form.classId"
            filterable
            placeholder="选择班级"
            style="width: 100%"
          >
            <ElOption
              v-for="item in classes"
              :key="item.id"
              :label="optionLabel(classes, item.id)"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="关联类型" prop="relationType">
          <ElRadioGroup v-model="form.relationType">
            <ElRadio value="SUBJECT">任课教师</ElRadio>
            <ElRadio value="HOMEROOM">班主任</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem v-if="form.relationType === 'SUBJECT'" label="学科" prop="subjectId">
          <ElSelect
            v-model="form.subjectId"
            filterable
            placeholder="选择学科"
            style="width: 100%"
          >
            <ElOption
              v-for="item in subjects"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="开始日期" prop="startDate">
          <ElDatePicker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="结束日期">
          <ElDatePicker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可空，表示长期有效"
            style="width: 100%"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton :loading="saving" type="primary" @click="submit">创建</ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

