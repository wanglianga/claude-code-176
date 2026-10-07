<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="复诊结果回写" name="diagnosis">
        <el-card>
          <div style="margin-bottom:10px;display:flex;justify-content:space-between">
            <el-radio-group v-model="diagFilter" @change="loadDiag">
              <el-radio-button value="PENDING">待复诊（转诊单）</el-radio-button>
              <el-radio-button value="DONE">已回写诊断</el-radio-button>
            </el-radio-group>
            <el-button type="primary" @click="openDiag(null)">回写复诊结果</el-button>
          </div>
          <el-table v-if="diagFilter==='PENDING'" :data="pendingRefs" size="small" stripe>
            <el-table-column label="学生" prop="studentName" width="100"/>
            <el-table-column label="年级" prop="grade" width="80"/>
            <el-table-column label="目标机构" prop="targetOrgName"/>
            <el-table-column label="家长选择" width="120">
              <template #default="{ row }">
                {{ row.targetType === 'COUNTY_HOSPITAL' ? '县医院' : '合作眼科门诊' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDiag(row)">录入复诊结果</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-table v-else :data="diagnoses" size="small" stripe>
            <el-table-column label="学生" width="100">
              <template #default="{ row }">{{ nameMap[row.studentId] }}</template>
            </el-table-column>
            <el-table-column prop="doctorName" label="医生" width="90"/>
            <el-table-column prop="visitDate" label="复诊日期" width="110"/>
            <el-table-column label="真性近视" width="90">
              <template #default="{ row }">
                <el-tag :type="row.trueMyopia?'danger':'success'">{{ row.trueMyopia ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="散瞳" width="100">
              <template #default="{ row }">
                {{ row.needMydriasis ? (row.mydriasisDone ? '需要(已做)' : '需要') : '不需要' }}
              </template>
            </el-table-column>
            <el-table-column label="处方(右 球/散)" width="130">
              <template #default="{ row }">{{ row.rxSphOd }} / {{ row.rxCylOd }}</template>
            </el-table-column>
            <el-table-column label="处方(左 球/散)" width="130">
              <template #default="{ row }">{{ row.rxSphOs }} / {{ row.rxCylOs }}</template>
            </el-table-column>
            <el-table-column label="复查周期" width="90">
              <template #default="{ row }">{{ row.revisitCycleMonths }}个月</template>
            </el-table-column>
            <el-table-column label="补助资格" width="100">
              <template #default="{ row }">
                <el-tag :type="row.subsidyEligible?'success':'info'">{{ row.subsidyEligible ? '符合' : '不符合' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="advice" label="医嘱/差异说明" show-overflow-tooltip/>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="周期复查（半年）" name="followup">
        <el-card>
          <div style="margin-bottom:10px;display:flex;justify-content:space-between">
            <el-radio-group v-model="fuFilter" @change="loadFu">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="PENDING">待提醒</el-radio-button>
              <el-radio-button value="NOTIFIED">已提醒</el-radio-button>
              <el-radio-button value="DONE">已到场</el-radio-button>
              <el-radio-button value="MISSED">逾期未到</el-radio-button>
            </el-radio-group>
            <el-button type="warning" plain @click="scanOverdue">立即扫描逾期（定时任务每日07:30）</el-button>
          </div>
          <el-table :data="followUps" size="small" stripe>
            <el-table-column label="学生" width="100">
              <template #default="{ row }">{{ row.studentName }}</template>
            </el-table-column>
            <el-table-column label="应复查日期" width="120">
              <template #default="{ row }">
                <span :style="row.overdue?'color:#e65d4f;font-weight:600':''">{{ row.followUp.dueDate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="fuType(row.followUp.status)">{{ fuLabel(row.followUp.status) }}</el-tag>
                <el-tag v-if="row.overdue" type="danger" size="small" style="margin-left:4px">已逾期</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="parentPhone" label="家长电话" width="130"/>
            <el-table-column prop="followUp.resultNote" label="复查结果" show-overflow-tooltip/>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button v-if="row.followUp.status!=='DONE'" link type="warning" @click="remind(row)">发送提醒</el-button>
                <el-button v-if="row.followUp.status!=='DONE'" link type="success" @click="markDone(row)">登记到场</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 复诊回写弹窗 -->
    <el-dialog v-model="diagDialog" title="复诊结果回写学生档案" width="720px">
      <el-form :model="d" label-width="130px">
        <el-form-item label="学生">
          <el-select v-model="d.studentId" filterable style="width:100%">
            <el-option v-for="s in students" :key="s.id" :label="s.grade + ' ' + s.name" :value="s.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="复诊日期">
          <el-date-picker v-model="d.visitDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-row>
          <el-col :span="8"><el-form-item label="真性近视"><el-switch v-model="d.trueMyopia"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="需要散瞳"><el-switch v-model="d.needMydriasis"/></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="已散瞳验光"><el-switch v-model="d.mydriasisDone"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="近视程度">
          <el-radio-group v-model="d.degree">
            <el-radio value="NONE">无</el-radio><el-radio value="LOW">轻度</el-radio>
            <el-radio value="MEDIUM">中度</el-radio><el-radio value="HIGH">高度</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="left">处方参数（D）</el-divider>
        <el-row>
          <el-col :span="12"><el-form-item label="球镜 右"><el-input-number v-model="d.rxSphOd" :min="-15" :max="8" :step="0.25"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="球镜 左"><el-input-number v-model="d.rxSphOs" :min="-15" :max="8" :step="0.25"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="散光 右"><el-input-number v-model="d.rxCylOd" :min="-6" :max="6" :step="0.25"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="散光 左"><el-input-number v-model="d.rxCylOs" :min="-6" :max="6" :step="0.25"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="瞳距(mm)"><el-input-number v-model="d.pd" :min="40" :max="80" :step="0.5"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="复查周期(月)"><el-input-number v-model="d.revisitCycleMonths" :min="1" :max="24"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="符合配镜补助"><el-switch v-model="d.subsidyEligible"/></el-form-item>
        <el-form-item label="补助/资格说明"><el-input v-model="d.subsidyNote"/></el-form-item>
        <el-form-item label="与筛查差异说明"><el-input v-model="d.divergenceNote" type="textarea" :rows="2" placeholder="差异较大时系统将自动登记问题"/></el-form-item>
        <el-form-item label="医嘱"><el-input v-model="d.advice" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diagDialog=false">取消</el-button>
        <el-button type="primary" @click="submitDiag">回写并生成复查计划</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const tab = ref('diagnosis')
const diagFilter = ref('PENDING')
const fuFilter = ref('')
const pendingRefs = ref([])
const diagnoses = ref([])
const followUps = ref([])
const students = ref([])
const nameMap = reactive({})
const diagDialog = ref(false)
const d = reactive({})

function resetD() {
  Object.keys(d).forEach(k => delete d[k])
  Object.assign(d, {
    studentId: null, visitDate: new Date().toISOString().slice(0, 10),
    trueMyopia: true, needMydriasis: false, mydriasisDone: false, degree: 'LOW',
    rxSphOd: -1, rxSphOs: -1, rxCylOd: 0, rxCylOs: 0, pd: 58,
    revisitCycleMonths: 6, subsidyEligible: true, subsidyNote: '', divergenceNote: '', advice: ''
  })
}
async function loadDiag() {
  pendingRefs.value = await api.get('/screening/referrals', { params: { status: 'PENDING' } })
  diagnoses.value = await api.get('/revisit/diagnoses')
}
async function loadFu() {
  followUps.value = await api.get('/followups', { params: fuFilter.value ? { status: fuFilter.value } : {} })
}
function openDiag(row) {
  resetD()
  if (row) { d.studentId = row.studentId; d.referralId = undefined }
  if (route.query.studentId) d.studentId = Number(route.query.studentId)
  diagDialog.value = true
}
async function submitDiag() {
  await api.post('/revisit/diagnoses', d)
  ElMessage.success('复诊结果已回写档案；差异预警、复查计划已联动处理')
  diagDialog.value = false
  loadDiag()
}
function fuLabel(s) { return { PENDING: '待提醒', NOTIFIED: '已提醒', DONE: '已到场', MISSED: '逾期未到' }[s] }
function fuType(s) { return { PENDING: 'info', NOTIFIED: 'warning', DONE: 'success', MISSED: 'danger' }[s] }
async function remind(row) {
  await api.post(`/followups/${row.followUp.id}/remind`)
  ElMessage.success('复查提醒短信已发送')
  loadFu()
}
async function markDone(row) {
  const { value } = await ElMessageBox.prompt('登记复查结果（视力/度数变化等）', '到场复查', {
    confirmButtonText: '确认', inputType: 'textarea'
  }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/followups/${row.followUp.id}/done`, { resultNote: value })
  ElMessage.success('已登记按期复查')
  loadFu()
}
async function scanOverdue() {
  const res = await api.post('/followups/scan-overdue')
  ElMessage.success(`扫描完成，新增逾期 ${res.newlyMissed} 人，已登记问题并通知学校跟进`)
  loadFu()
}
onMounted(async () => {
  const all = await api.get('/students', { params: { size: 200 } })
  students.value = all.items
  all.items.forEach(s => { nameMap[s.id] = s.name })
  await loadDiag()
  await loadFu()
})
</script>
