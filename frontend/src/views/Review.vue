<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="tab">
        <el-tab-pane label="待健康老师复核" name="review">
          <el-table :data="reviewRows" stripe size="small">
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column label="学校/班级" min-width="200">
              <template #default="{ row }">{{ row.schoolName }} {{ row.grade }}{{ row.className }}</template>
            </el-table-column>
            <el-table-column label="异常标记" min-width="160">
              <template #default="{ row }">
                <el-tag v-for="(f,i) in row.flagLabels" :key="i" type="danger" size="small" style="margin:2px">{{ f }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="pick(row,'review')">复核</el-button>
                <el-button link size="small" @click="detail(row)">查看档案</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="学校日常干预（座位/户外/提醒）" name="intervene">
          <el-alert type="info" :closable="false" style="margin-bottom:10px"
            title="把医疗复诊与学校日常干预连接起来：对异常及已配镜学生持续安排座位调整、户外活动、家长提醒和用眼指导。" />
          <el-table :data="manageRows" stripe size="small">
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column label="班级" width="140">
              <template #default="{ row }">{{ row.grade }}{{ row.className }}</template>
            </el-table-column>
            <el-table-column prop="stageLabel" label="当前状态" width="120" />
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button size="small" @click="pick(row,'intervention')">登记干预</el-button>
                <el-button size="small" plain @click="quick(row,'SEAT_ADJUST','座位调整至教室前排')">座位调整</el-button>
                <el-button size="small" plain type="success" @click="quick(row,'OUTDOOR_ACTIVITY','每天保证2小时户外活动')">户外活动</el-button>
                <el-button size="small" plain type="warning" @click="quick(row,'PARENT_REMIND','向家长推送用眼与复查提醒')">家长提醒</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <action-dialogs ref="dialogs" :student-id="sid" @done="load" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import ActionDialogs from '../components/ActionDialogs.vue'

const router = useRouter()
const tab = ref('review')
const reviewRows = ref([])
const manageRows = ref([])
const sid = ref(null)
const dialogs = ref()

const load = async () => {
  reviewRows.value = await api.students({ stage: 'ABNORMAL_REVIEW' })
  const retest = await api.students({ stage: 'SCREENED' })
  // 已进入管理链路、需要学校持续干预的学生
  const managed = await api.students({})
  manageRows.value = managed.filter(s => ['GLASSES_SUBSIDY','MANAGING','REVIEW_DUE','CLOSED_LOOP',
    'VISITED','AWAIT_VISIT','AWAIT_PARENT'].includes(s.stage))
}
const pick = (row, name) => { sid.value = row.id; dialogs.value.open(name) }
const quick = async (row, type, content) => {
  await api.intervention({ studentId: row.id, type, content, executed: true })
  ElMessage.success(`已为 ${row.name} 登记干预`)
}
const detail = (row) => router.push(`/students/${row.id}`)
onMounted(load)
</script>
