<template>
  <div>
    <el-card>
      <div style="margin-bottom:10px;display:flex;justify-content:space-between">
        <el-radio-group v-model="filter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PLANNED">已安排</el-radio-button>
          <el-radio-button value="DONE">已执行</el-radio-button>
          <el-radio-button value="VERIFIED">部门已核验</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="dialog = true">安排干预（支持批量）</el-button>
      </div>
      <el-table :data="rows" size="small" stripe>
        <el-table-column label="学生" width="100">
          <template #default="{ row }">{{ row.studentName }}</template>
        </el-table-column>
        <el-table-column prop="schoolName" label="学校" show-overflow-tooltip/>
        <el-table-column label="干预类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ typeLabel(row.intervention.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="intervention.title" label="内容" show-overflow-tooltip/>
        <el-table-column prop="intervention.plannedDate" label="计划日期" width="110"/>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.intervention.status==='VERIFIED'?'success':(row.intervention.status==='DONE'?'primary':'info')">
              {{ row.intervention.status === 'VERIFIED' ? '已核验' : row.intervention.status === 'DONE' ? '已执行' : '已安排' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="intervention.effectNote" label="效果/核验记录" show-overflow-tooltip/>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.intervention.status==='PLANNED'" link type="primary" @click="execute(row)">登记执行</el-button>
            <el-button v-if="row.intervention.status==='DONE'" link type="success" @click="verify(row)">部门核验</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="安排学校日常干预" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="干预类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="座位调整" value="SEAT_ADJUSTMENT"/>
            <el-option label="户外活动" value="OUTDOOR_ACTIVITY"/>
            <el-option label="用眼习惯提醒" value="EYE_HABIT_REMINDER"/>
            <el-option label="家长提醒" value="PARENT_NOTICE"/>
            <el-option label="定期检查" value="PERIODIC_CHECK"/>
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="详情"><el-input v-model="form.detail" type="textarea" :rows="3"/></el-form-item>
        <el-form-item label="计划日期"><el-date-picker v-model="form.plannedDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="干预对象">
          <el-select v-model="form.studentIds" multiple filterable collapse-tags style="width:100%">
            <el-option v-for="s in students" :key="s.id"
              :label="s.grade + ' ' + s.name + '（' + (statusLabel(s.status)) + '）'" :value="s.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">批量安排</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { STATUS_LABELS } from '../store'

const filter = ref('')
const rows = ref([])
const students = ref([])
const dialog = ref(false)
const form = reactive({ type: 'SEAT_ADJUSTMENT', title: '', detail: '', plannedDate: '', studentIds: [] })

const typeLabel = t => ({ SEAT_ADJUSTMENT:'座位调整', OUTDOOR_ACTIVITY:'户外活动',
  EYE_HABIT_REMINDER:'用眼习惯提醒', PARENT_NOTICE:'家长提醒', PERIODIC_CHECK:'定期检查' }[t])
const statusLabel = s => STATUS_LABELS[s] || s
async function load() {
  rows.value = await api.get('/interventions', { params: filter.value ? { status: filter.value } : {} })
}
async function execute(row) {
  const { value } = await ElMessageBox.prompt('执行情况与效果记录（可留空）', '登记干预执行', {
    confirmButtonText: '提交', inputType: 'textarea'
  }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/interventions/${row.intervention.id}/execute`, { effectNote: value })
  ElMessage.success('已登记执行')
  load()
}
async function verify(row) {
  const { value } = await ElMessageBox.prompt('核验意见（可留空）', '部门核验干预落实', {
    confirmButtonText: '核验通过', inputType: 'textarea'
  }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/interventions/${row.intervention.id}/verify`, { effectNote: value })
  ElMessage.success('核验完成')
  load()
}
async function submit() {
  if (!form.studentIds.length) return ElMessage.warning('请选择干预对象')
  const res = await api.post('/interventions', form)
  ElMessage.success(`已为 ${res.created} 名学生安排干预`)
  dialog.value = false
  load()
}
onMounted(async () => {
  students.value = (await api.get('/students', { params: { size: 200 } })).items
  form.plannedDate = new Date().toISOString().slice(0, 10)
  load()
})
</script>
