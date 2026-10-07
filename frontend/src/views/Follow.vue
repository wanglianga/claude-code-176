<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="家长确认与学校复核" name="confirm">
        <el-card>
          <el-radio-group v-model="filter" style="margin-bottom:10px" @change="load">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING">待家长确认</el-radio-button>
            <el-radio-button value="CONFIRMED">家长已确认</el-radio-button>
            <el-radio-button value="ON_HOLD">家长暂缓</el-radio-button>
          </el-radio-group>
          <el-table :data="rows" stripe size="small">
            <el-table-column label="学生" width="100">
              <template #default="{ row }">{{ row.studentName }}</template>
            </el-table-column>
            <el-table-column prop="schoolName" label="学校" show-overflow-tooltip/>
            <el-table-column prop="grade" label="年级" width="80"/>
            <el-table-column prop="parentPhone" label="家长电话" width="130"/>
            <el-table-column label="家长选择" width="130">
              <template #default="{ row }">
                <el-tag v-if="row.confirmation.choice==='COUNTY_HOSPITAL'" type="primary">县医院复诊</el-tag>
                <el-tag v-else-if="row.confirmation.choice==='PARTNER_CLINIC'" type="success">合作眼科门诊</el-tag>
                <el-tag v-else-if="row.confirmation.choice==='POSTPONE'" type="info">暂缓处理</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="confirmation.reason" label="暂缓原因/备注" show-overflow-tooltip/>
            <el-table-column label="学校复核" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.confirmation.schoolReviewStatus==='APPROVED'" type="success">已通过</el-tag>
                <el-tag v-else-if="row.confirmation.schoolReviewStatus==='REJECTED'" type="danger">已退回</el-tag>
                <el-tag v-else type="warning">待复核</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button link type="primary" @click="openStudent(row)">健康记录</el-button>
                <el-button v-if="row.confirmation.status==='PENDING'||row.confirmation.status==='ON_HOLD'"
                  link type="warning" @click="remind(row)">再次提醒</el-button>
                <el-button v-if="row.confirmation.status==='CONFIRMED'&&row.confirmation.schoolReviewStatus==='PENDING'"
                  link type="success" @click="openReview(row)">复核转诊</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="转诊单跟踪" name="referral">
        <el-card>
          <el-table :data="referrals" stripe size="small">
            <el-table-column label="学生" width="110">
              <template #default="{ row }">{{ row.studentName }}</template>
            </el-table-column>
            <el-table-column label="复诊机构">
              <template #default="{ row }">
                {{ row.targetOrgName || '-' }}
                <el-tag size="small" style="margin-left:6px" :type="row.targetType==='COUNTY_HOSPITAL'?'primary':'success'">
                  {{ row.targetType === 'COUNTY_HOSPITAL' ? '县医院' : '合作门诊' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status==='COMPLETED'?'success':'warning'">
                  {{ row.status === 'COMPLETED' ? '已完成复诊' : '待复诊' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="issuedAt" label="开单时间" width="170">
              <template #default="{ row }">{{ fmt(row.issuedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="reviewDialog" title="学校健康老师复核" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        :title="`学生 ${active?.studentName} 家长选择：${active?.confirmation.choice === 'COUNTY_HOSPITAL' ? '县人民医院' : '合作眼科门诊'}`"/>
      <el-input v-model="reviewNote" type="textarea" :rows="3" placeholder="复核备注（可留空）"/>
      <template #footer>
        <el-button @click="reviewDialog=false">取消</el-button>
        <el-button type="danger" plain @click="doReview(false)">退回补充</el-button>
        <el-button type="success" @click="doReview(true)">复核通过并转诊</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const tab = ref('confirm')
const filter = ref('')
const rows = ref([])
const referrals = ref([])
const reviewDialog = ref(false)
const active = ref(null)
const reviewNote = ref('')

function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }
function openStudent(row) { router.push('/students/' + row.confirmation.studentId) }
async function load() {
  const params = filter.value ? { status: filter.value } : {}
  rows.value = await api.get('/screening/confirmations', { params })
  referrals.value = await api.get('/screening/referrals')
}
function openReview(row) { active.value = row; reviewNote.value = ''; reviewDialog.value = true }
async function doReview(approved) {
  await api.post(`/screening/confirmations/${active.value.confirmation.id}/review`,
    { approved, note: reviewNote.value })
  ElMessage.success(approved ? '复核通过，转诊单已生成' : '已退回')
  reviewDialog.value = false
  load()
}
async function remind(row) {
  await ElMessageBox.confirm('向家长再次发送短信提醒？（短信经 Redis 网关队列下发）', '提醒确认', { type: 'warning' })
  await api.post(`/screening/confirmations/${row.confirmation.id}/remind`)
  ElMessage.success('提醒已发送')
  load()
}
onMounted(load)
</script>
