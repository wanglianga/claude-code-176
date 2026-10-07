<template>
  <div v-if="data">
    <el-page-header @back="$router.back()" title="返回" :content="data.student.name + ' 的健康管理记录'" style="margin-bottom:12px"/>
    <el-row :gutter="14">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>全流程健康管理时间线</b>
              <el-tag :type="STATUS_TYPES[data.student.status]" size="large">
                {{ STATUS_LABELS[data.student.status] || data.student.status }}
              </el-tag>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item v-for="e in data.timeline" :key="e.id" :timestamp="fmt(e.eventTime)"
              :type="dotType(e.eventType)" placement="top">
              <el-card shadow="never">
                <div style="display:flex;justify-content:space-between">
                  <b>{{ e.title }}</b>
                  <el-tag size="small" effect="plain">{{ roleLabel(e.actorRole) }} · {{ e.actorName }}</el-tag>
                </div>
                <div class="detail">{{ e.detail }}</div>
              </el-card>
            </el-timeline-item>
            <el-empty v-if="!data.timeline.length" description="暂无记录" />
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card style="margin-bottom:14px">
          <template #header><b>档案信息</b></template>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="学校班级">{{ data.schoolName }} {{ data.grade }}{{ data.className }}</el-descriptions-item>
            <el-descriptions-item label="学籍号">{{ data.student.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ data.student.birthDate }}</el-descriptions-item>
            <el-descriptions-item label="家长/电话">{{ data.student.parentName }} {{ data.student.parentPhone }}</el-descriptions-item>
            <el-descriptions-item label="既往档案">{{ data.student.pastVisionNote || '无' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card style="margin-bottom:14px">
          <template #header><b>快捷处置</b></template>
          <div style="display:flex;flex-direction:column;gap:8px">
            <el-button type="primary" @click="goScreening">筛查现场录入</el-button>
            <el-button type="warning" @click="parentDialog = true">家长决策（县医院/门诊/暂缓）</el-button>
            <el-button type="success" @click="goRevisit">复诊结果回写</el-button>
            <el-button @click="ivDialog = true">登记学校干预</el-button>
            <el-button type="danger" plain @click="issueDialog = true">登记异常问题</el-button>
          </div>
        </el-card>
        <el-card>
          <template #header><b>关联问题（{{ data.issues.length }}）</b></template>
          <el-table :data="data.issues" size="small">
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="ISSUE_TYPES[row.issueType]" size="small">{{ ISSUE_LABELS[row.issueType] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" show-overflow-tooltip/>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status==='RESOLVED'?'success':(row.status==='PROCESSING'?'warning':'danger')">
                  {{ row.status === 'RESOLVED' ? '已处理' : row.status === 'PROCESSING' ? '处理中' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 家长决策 -->
    <el-dialog v-model="parentDialog" title="家长决策" width="460px">
      <el-radio-group v-model="decision.choice" style="display:flex;flex-direction:column;gap:10px;margin-bottom:12px">
        <el-radio value="COUNTY_HOSPITAL">到县人民医院复诊</el-radio>
        <el-radio value="PARTNER_CLINIC">到合作眼科门诊复诊</el-radio>
        <el-radio value="POSTPONE">暂缓处理（需填写原因）</el-radio>
      </el-radio-group>
      <el-input v-model="decision.reason" type="textarea" :rows="3" placeholder="暂缓原因 / 备注"/>
      <template #footer>
        <el-button @click="parentDialog=false">取消</el-button>
        <el-button type="primary" @click="submitDecision">提交</el-button>
      </template>
    </el-dialog>

    <!-- 干预 -->
    <el-dialog v-model="ivDialog" title="登记学校干预" width="460px">
      <el-form label-width="90px">
        <el-form-item label="干预类型">
          <el-select v-model="intervention.type" style="width:100%">
            <el-option label="座位调整" value="SEAT_ADJUSTMENT"/>
            <el-option label="户外活动" value="OUTDOOR_ACTIVITY"/>
            <el-option label="用眼习惯提醒" value="EYE_HABIT_REMINDER"/>
            <el-option label="家长提醒" value="PARENT_NOTICE"/>
            <el-option label="定期检查" value="PERIODIC_CHECK"/>
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="intervention.title"/></el-form-item>
        <el-form-item label="详情"><el-input v-model="intervention.detail" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ivDialog=false">取消</el-button>
        <el-button type="primary" @click="submitIv">提交</el-button>
      </template>
    </el-dialog>

    <!-- 问题登记 -->
    <el-dialog v-model="issueDialog" title="登记异常问题" width="460px">
      <el-form label-width="90px">
        <el-form-item label="问题类型">
          <el-select v-model="issue.issueType" style="width:100%">
            <el-option v-for="(l,k) in ISSUE_LABELS" :key="k" :label="l" :value="k"/>
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="issue.title"/></el-form-item>
        <el-form-item label="描述"><el-input v-model="issue.description" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialog=false">取消</el-button>
        <el-button type="danger" @click="submitIssue">提交并跟进</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { STATUS_LABELS, STATUS_TYPES, ISSUE_LABELS, ISSUE_TYPES, ROLE_LABELS } from '../store'

const route = useRoute()
const router = useRouter()
const data = ref(null)
const parentDialog = ref(false)
const ivDialog = ref(false)
const issueDialog = ref(false)
const decision = reactive({ choice: 'COUNTY_HOSPITAL', reason: '' })
const intervention = reactive({ type: 'SEAT_ADJUSTMENT', title: '', detail: '' })
const issue = reactive({ issueType: 'PHONE_UNREACHABLE', title: '', description: '' })

const EVENT_TYPES = ['danger', 'warning', 'primary', 'success']
function dotType(t) {
  if (t.includes('ABNORMAL') || t.includes('MISSED') || t.includes('DISPUTE') || t.includes('DISCOMFORT') || t.includes('DIVERGENCE')) return 'danger'
  if (t.includes('CONFIRM') || t.includes('REVIEW') || t.includes('REMINDED') || t.includes('POSTPONE') || t.includes('ISSUE')) return 'warning'
  if (t.includes('PAID') || t.includes('APPROVED') || t.includes('DONE') || t.includes('RESOLVED') || t.includes('VERIFIED') || t.includes('CONFIRMED') || t.includes('NORMAL')) return 'success'
  return 'primary'
}
function roleLabel(r) { return ROLE_LABELS[r] || r }
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }
function goScreening() { router.push('/screening?studentId=' + route.params.id) }
function goRevisit() { router.push('/revisit?studentId=' + route.params.id) }

async function load() {
  data.value = await api.get('/students/' + route.params.id)
}
async function submitDecision() {
  const pc = (await api.get('/screening/confirmations'))
    .map(x => x.confirmation).find(c => c.studentId === Number(route.params.id) &&
      (c.status === 'PENDING' || c.status === 'ON_HOLD'))
  if (!pc) return ElMessage.warning('该学生没有待决策的确认单')
  await api.post(`/screening/confirmations/${pc.id}/decision`, decision)
  ElMessage.success('家长决策已提交，待学校健康老师复核')
  parentDialog.value = false
  load()
}
async function submitIv() {
  await api.post('/interventions', { ...intervention, studentId: Number(route.params.id) })
  ElMessage.success('干预已安排，进入健康管理记录')
  ivDialog.value = false
  load()
}
async function submitIssue() {
  await api.post('/issues', { ...issue, studentId: Number(route.params.id) })
  ElMessage.success('问题已登记并进入跟进清单')
  issueDialog.value = false
  load()
}
onMounted(load)
</script>

<style scoped>
.detail { color: #606266; font-size: 13px; margin-top: 6px; line-height: 1.6; }
</style>
