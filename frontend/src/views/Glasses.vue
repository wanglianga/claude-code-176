<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="配镜登记" name="glasses">
        <el-card>
          <div style="margin-bottom:10px">
            <el-button type="primary" @click="openFit(false)">合作店补助配镜</el-button>
            <el-button type="warning" plain @click="openFit(true)">家长自行外配登记</el-button>
          </div>
          <el-table :data="glasses" size="small" stripe>
            <el-table-column label="学生" width="100">
              <template #default="{ row }">{{ nameMap[row.studentId] }}</template>
            </el-table-column>
            <el-table-column prop="orgName" label="配镜机构" show-overflow-tooltip/>
            <el-table-column label="来源" width="120">
              <template #default="{ row }">
                <el-tag :type="row.source==='SUBSIDY_SHOP'?'success':'info'">
                  {{ row.source === 'SUBSIDY_SHOP' ? '补助定点店' : '自行外配' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="gType(row.status)">{{ gLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="价格(元)" width="90"/>
            <el-table-column prop="subsidyAmount" label="补助(元)" width="90"/>
            <el-table-column prop="fittedDate" label="配镜日期" width="110"/>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button v-if="row.status==='FITTED'" link type="danger" @click="discomfort(row)">配镜不适</el-button>
                <el-button v-if="row.status==='DISCOMFORT_REPORTED'" link type="warning" @click="readjust(row)">店方调试</el-button>
                <el-button v-if="row.status!=='CONFIRMED_OK'&&row.source==='SUBSIDY_SHOP'" link type="success" @click="confirm(row)">家长确认舒适</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="配镜补助审核" name="subsidy">
        <el-card>
          <el-radio-group v-model="subFilter" style="margin-bottom:10px" @change="loadSub">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING">待审核</el-radio-button>
            <el-radio-button value="APPROVED">已通过</el-radio-button>
            <el-radio-button value="DISPUTED">资格争议</el-radio-button>
            <el-radio-button value="REJECTED">已驳回</el-radio-button>
            <el-radio-button value="PAID">已发放</el-radio-button>
          </el-radio-group>
          <el-table :data="subsidies" size="small" stripe>
            <el-table-column label="学生" width="100">
              <template #default="{ row }">{{ row.studentName }}</template>
            </el-table-column>
            <el-table-column prop="schoolName" label="学校" show-overflow-tooltip/>
            <el-table-column label="金额(元)" width="100">
              <template #default="{ row }">{{ row.subsidy.amount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="subType(row.subsidy.status)">{{ subLabel(row.subsidy.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="subsidy.decisionNote" label="审核/争议意见" show-overflow-tooltip/>
            <el-table-column prop="subsidy.decidedByName" label="经办人" width="100"/>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <template v-if="row.subsidy.status==='PENDING'||row.subsidy.status==='DISPUTED'">
                  <el-button link type="success" @click="decide(row,'APPROVE')">通过</el-button>
                  <el-button link type="danger" @click="decide(row,'REJECT')">驳回</el-button>
                </template>
                <el-button v-if="row.subsidy.status==='APPROVED'" link type="primary" @click="pay(row)">发放补助</el-button>
                <el-button v-if="row.subsidy.status!=='PAID'&&row.subsidy.status!=='DISPUTED'" link type="warning" @click="dispute(row)">发起争议复核</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="fitDialog" :title="external ? '家长自行外配登记' : '合作店补助配镜'" width="460px">
      <el-form :model="fit" label-width="100px">
        <el-form-item label="学生">
          <el-select v-model="fit.studentId" filterable style="width:100%">
            <el-option v-for="s in students" :key="s.id" :label="s.grade + ' ' + s.name" :value="s.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="配镜机构"><el-input v-model="fit.orgName" :placeholder="external?'如 网上自购成品眼镜':'默认当前定点店'"/></el-form-item>
        <el-form-item label="价格(元)"><el-input-number v-model="fit.price" :min="0" :step="10"/></el-form-item>
        <el-form-item label="配镜日期"><el-date-picker v-model="fit.fittedDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-alert v-if="!external" type="info" :closable="false" title="复诊医生判定符合补助资格时，保存后自动发起补助申请（封顶 300 元/副）"/>
        <el-alert v-else type="warning" :closable="false" title="自行外配不享受补助，系统将登记问题由学校跟进佩戴与复查"/>
      </el-form>
      <template #footer>
        <el-button @click="fitDialog=false">取消</el-button>
        <el-button type="primary" @click="submitFit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const tab = ref('glasses')
const glasses = ref([])
const subsidies = ref([])
const students = ref([])
const nameMap = reactive({})
const subFilter = ref('PENDING')
const fitDialog = ref(false)
const external = ref(false)
const fit = reactive({})

const gLabel = s => ({ ORDERED:'已下单', FITTED:'已配镜', DISCOMFORT_REPORTED:'不适反馈', READJUSTED:'已调试', CONFIRMED_OK:'确认舒适' }[s])
const gType = s => ({ ORDERED:'info', FITTED:'primary', DISCOMFORT_REPORTED:'danger', READJUSTED:'warning', CONFIRMED_OK:'success' }[s])
const subLabel = s => ({ PENDING:'待审核', APPROVED:'已通过', REJECTED:'已驳回', DISPUTED:'争议中', PAID:'已发放' }[s])
const subType = s => ({ PENDING:'warning', APPROVED:'success', REJECTED:'danger', DISPUTED:'danger', PAID:'primary' }[s])

async function loadGlasses() { glasses.value = await api.get('/glasses') }
async function loadSub() {
  subsidies.value = await api.get('/subsidies', { params: subFilter.value ? { status: subFilter.value } : {} })
}
function openFit(ext) {
  external.value = ext
  Object.keys(fit).forEach(k => delete fit[k])
  Object.assign(fit, { studentId: null, orgName: '', price: ext ? 200 : 480, fittedDate: new Date().toISOString().slice(0, 10) })
  fitDialog.value = true
}
async function submitFit() {
  if (external.value) await api.post('/glasses/external', fit)
  else await api.post('/glasses', fit)
  ElMessage.success('配镜记录已保存')
  fitDialog.value = false
  loadGlasses()
}
async function discomfort(row) {
  const { value } = await ElMessageBox.prompt('描述配镜后不适症状', '配镜后不适', {
    confirmButtonText: '提交', inputType: 'textarea', inputPlaceholder: '如头晕、看地面变形、眼胀'
  }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/glasses/${row.id}/discomfort`, { note: value })
  ElMessage.success('已登记，进入异常跟进清单')
  loadGlasses()
}
async function readjust(row) {
  const { value } = await ElMessageBox.prompt('调试处理说明', '眼镜店调试', { confirmButtonText: '完成' }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/glasses/${row.id}/readjust`, { note: value })
  ElMessage.success('调试完成')
  loadGlasses()
}
async function confirm(row) {
  await api.post(`/glasses/${row.id}/confirm`)
  ElMessage.success('家长已确认佩戴舒适')
  loadGlasses()
}
async function decide(row, action) {
  const { value } = await ElMessageBox.prompt(action === 'APPROVE' ? '审核意见' : '驳回原因',
    action === 'APPROVE' ? '通过补助申请' : '驳回补助申请', { confirmButtonText: '确定', inputType: 'textarea' }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/subsidies/${row.subsidy.id}/decision`, { action, note: value })
  ElMessage.success('已审核')
  loadSub()
}
async function pay(row) {
  await ElMessageBox.confirm(`确认向 ${row.studentName} 发放补助 ${row.subsidy.amount} 元？`, '补助发放', { type: 'warning' })
  await api.post(`/subsidies/${row.subsidy.id}/pay`)
  ElMessage.success('补助已发放')
  loadSub()
}
async function dispute(row) {
  const { value } = await ElMessageBox.prompt('争议复核理由', '补助资格争议', {
    confirmButtonText: '提交复核', inputType: 'textarea'
  }).catch(() => ({}))
  if (value === undefined) return
  await api.post(`/subsidies/${row.subsidy.id}/dispute`, { note: value })
  ElMessage.success('争议已提交，进入跟进清单')
  loadSub()
}
onMounted(async () => {
  const all = await api.get('/students', { params: { size: 200 } })
  students.value = all.items
  all.items.forEach(s => { nameMap[s.id] = s.name })
  await loadGlasses()
  await loadSub()
})
</script>
