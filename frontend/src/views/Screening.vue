<template>
  <div>
    <el-row :gutter="14">
      <el-col :span="9">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>筛查批次</b>
              <el-button type="primary" size="small" @click="batchDialog = true">新建批次</el-button>
            </div>
          </template>
          <el-table :data="batches" size="small" highlight-current-row @current-change="pickBatch" height="560">
            <el-table-column prop="name" label="批次名称" show-overflow-tooltip/>
            <el-table-column prop="screeningDate" label="日期" width="110"/>
            <el-table-column label="异常/总数" width="90">
              <template #default="{ row }">
                <span style="color:#e65d4f">{{ row.abnormalCount }}</span>/{{ row.totalCount }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="15">
        <el-card v-if="current">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>{{ current.batch.name }}（设备校准编号将随记录留存）</b>
              <el-button type="primary" size="small" @click="openRecord(null)">现场录入</el-button>
            </div>
          </template>
          <el-table :data="current.records" size="small">
            <el-table-column label="学生" width="100">
              <template #default="{ row }">{{ nameMap[row.studentId] }}</template>
            </el-table-column>
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.absent" type="info">请假</el-tag>
                <el-tag v-else-if="row.result==='ABNORMAL'" type="danger">异常</el-tag>
                <el-tag v-else-if="row.result==='SUSPECTED'" type="warning">可疑/复测</el-tag>
                <el-tag v-else type="success">正常</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="裸眼(右/左)" width="110">
              <template #default="{ row }">{{ row.absent ? '-' : row.nakedOd + '/' + row.nakedOs }}</template>
            </el-table-column>
            <el-table-column label="球镜(右/左)" width="120">
              <template #default="{ row }">{{ row.absent ? '-' : row.sphOd + '/' + row.sphOs }}</template>
            </el-table-column>
            <el-table-column prop="deviceCalibNo" label="设备校准号" width="130"/>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRecord(row)">查看/补录</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-alert v-if="current.absentCount" type="warning" :closable="false" style="margin-top:8px"
            :title="`本批有 ${current.absentCount} 人请假未筛，已进入异常跟进清单等待补筛`"/>
        </el-card>
        <el-empty v-else description="请选择左侧批次" />
      </el-col>
    </el-row>

    <!-- 新建批次 -->
    <el-dialog v-model="batchDialog" title="新建年度筛查批次" width="480px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="批次名称"><el-input v-model="batchForm.name" placeholder="如 2026 年阳光小学秋季视力筛查"/></el-form-item>
        <el-form-item label="学校">
          <el-select v-model="batchForm.schoolId" style="width:100%">
            <el-option v-for="o in schools" :key="o.id" :label="o.name" :value="o.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="年级"><el-input v-model="batchForm.grade"/></el-form-item>
        <el-form-item label="筛查日期"><el-date-picker v-model="batchForm.screeningDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="batchForm.notes" type="textarea"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog=false">取消</el-button>
        <el-button type="primary" @click="createBatch">创建</el-button>
      </template>
    </el-dialog>

    <!-- 现场录入 -->
    <el-dialog v-model="recordDialog" title="筛查现场记录" width="720px">
      <el-form :model="rec" label-width="130px">
        <el-form-item label="学生">
          <el-select v-model="rec.studentId" filterable style="width:100%" placeholder="选择学生">
            <el-option v-for="s in studentOptions" :key="s.id"
              :label="s.grade + s.className + ' ' + s.name" :value="s.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="当日请假">
          <el-switch v-model="rec.absent"/>
          <span v-if="rec.absent" style="margin-left:10px;color:#e65d4f">请假将登记问题并安排补筛</span>
        </el-form-item>
        <template v-if="rec.absent">
          <el-form-item label="请假原因"><el-input v-model="rec.absentReason"/></el-form-item>
        </template>
        <template v-else>
          <el-divider content-position="left">视力与屈光</el-divider>
          <el-row>
            <el-col :span="12"><el-form-item label="裸眼视力 右"><el-input-number v-model="rec.nakedOd" :min="4.0" :max="5.3" :step="0.1"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="裸眼视力 左"><el-input-number v-model="rec.nakedOs" :min="4.0" :max="5.3" :step="0.1"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="球镜(D) 右"><el-input-number v-model="rec.sphOd" :min="-10" :max="6" :step="0.25"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="球镜(D) 左"><el-input-number v-model="rec.sphOs" :min="-10" :max="6" :step="0.25"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="散光(D) 右"><el-input-number v-model="rec.cylOd" :min="-6" :max="6" :step="0.25"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="散光(D) 左"><el-input-number v-model="rec.cylOs" :min="-6" :max="6" :step="0.25"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="轴位 右/左">
              <el-input-number v-model="rec.axisOd" :min="0" :max="180"/>
              <el-input-number v-model="rec.axisOs" :min="0" :max="180" style="margin-left:8px"/>
            </el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="是否戴镜"><el-switch v-model="rec.wearingGlasses"/></el-form-item>
            </el-col>
          </el-row>
          <el-divider content-position="left">用眼习惯问卷</el-divider>
          <el-row>
            <el-col :span="12"><el-form-item label="户外活动(h/天)"><el-input-number v-model="rec.hoursOutdoor" :min="0" :max="8" :step="0.5"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="电子屏幕(h/天)"><el-input-number v-model="rec.screenHours" :min="0" :max="12" :step="0.5"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="读写距离合格"><el-switch v-model="rec.readingDistanceOk"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="每日眼保健操"><el-switch v-model="rec.eyeExerciseDaily"/></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="父母近视"><el-switch v-model="rec.parentalMyopia"/></el-form-item></el-col>
          </el-row>
          <el-form-item label="问卷备注"><el-input v-model="rec.questionnaireNote"/></el-form-item>
          <el-divider content-position="left">设备与复测</el-divider>
          <el-row>
            <el-col :span="12">
              <el-form-item label="设备校准编号"><el-input v-model="rec.deviceCalibNo" placeholder="如 CAL-2026-018"/></el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="需要复测"><el-switch v-model="rec.needRetest"/></el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="recordDialog=false">取消</el-button>
        <el-button type="primary" @click="submitRecord">提交（异常将自动推送家长+学校复核）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const schools = ref([])
const batches = ref([])
const current = ref(null)
const nameMap = reactive({})
const studentOptions = ref([])
const batchDialog = ref(false)
const recordDialog = ref(false)
const batchForm = reactive({ name: '', schoolId: null, grade: '', screeningDate: '', notes: '' })
const rec = reactive({})

function resetRec() {
  Object.keys(rec).forEach(k => delete rec[k])
  Object.assign(rec, {
    absent: false, absentReason: '', nakedOd: 5.0, nakedOs: 5.0, sphOd: 0, sphOs: 0,
    cylOd: 0, cylOs: 0, axisOd: 180, axisOs: 180, wearingGlasses: false,
    hoursOutdoor: 1, screenHours: 2, readingDistanceOk: true, eyeExerciseDaily: true,
    parentalMyopia: false, questionnaireNote: '', needRetest: false, deviceCalibNo: 'CAL-2026-018'
  })
}
async function loadBatches() {
  batches.value = await api.get('/screening/batches')
  const sid = route.query.schoolId
  if (sid && batches.value.length) {
    const b = batches.value.find(x => String(x.id) === String(batches.value[0].id))
    if (b) pickBatch(b)
  }
}
async function pickBatch(b) {
  if (!b) return
  const detail = await api.get('/screening/batches/' + b.id)
  current.value = detail
  const students = await api.get('/students', { params: { schoolId: b.schoolId, size: 200 } })
  studentOptions.value = students.items
  const classes = await api.get('/classes', { params: { schoolId: b.schoolId } })
  const cmap = new Map(classes.map(c => [c.id, c.grade + c.className]))
  students.items.forEach(s => { nameMap[s.id] = s.name; s.className = cmap.get(s.classId) })
  if (route.query.studentId) {
    rec.batchId = b.id
    rec.studentId = Number(route.query.studentId)
  }
}
function openRecord(row) {
  resetRec()
  rec.batchId = current.value.batch.id
  if (row) Object.assign(rec, JSON.parse(JSON.stringify(row)))
  if (route.query.studentId && !row) rec.studentId = Number(route.query.studentId)
  recordDialog.value = true
}
async function submitRecord() {
  await api.post('/screening/records', rec)
  ElMessage.success(rec.absent ? '已登记请假与补筛问题' :
    (rec.needRetest ? '已记录并标记需复测' : '筛查记录已提交'))
  recordDialog.value = false
  pickBatch(current.value.batch)
  loadBatches()
}
async function createBatch() {
  await api.post('/screening/batches', batchForm)
  ElMessage.success('批次已创建')
  batchDialog.value = false
  loadBatches()
}
onMounted(async () => {
  schools.value = (await api.get('/organizations')).filter(o => o.orgType === 'SCHOOL')
  batchForm.schoolId = schools.value[0]?.id
  batchForm.screeningDate = new Date().toISOString().slice(0, 10)
  await loadBatches()
  if (batches.value.length) pickBatch(batches.value[0])
})
</script>
