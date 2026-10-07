<template>
  <div class="page-container">
    <el-row :gutter="12" style="margin-bottom:12px">
      <el-col :span="6"><el-card class="kpi-card"><div class="num">{{ stat.total }}</div><div class="label">在册学生</div></el-card></el-col>
      <el-col :span="6"><el-card class="kpi-card"><div class="num" style="color:#e6a23c">{{ stat.pending }}</div><div class="label">待筛查</div></el-card></el-col>
      <el-col :span="6"><el-card class="kpi-card"><div class="num" style="color:#f56c6c">{{ stat.absent }}</div><div class="label">请假待补检</div></el-card></el-col>
      <el-col :span="6"><el-card class="kpi-card"><div class="num" style="color:#67c23a">{{ stat.done }}</div><div class="label">已完成筛查</div></el-card></el-col>
    </el-row>

    <el-card>
      <el-radio-group v-model="stage" style="margin-bottom:10px" @change="load">
        <el-radio-button value="PENDING_SCREEN">待筛查</el-radio-button>
        <el-radio-button value="LOST_FOLLOW">请假/失访待补检</el-radio-button>
        <el-radio-button value="">全部学生</el-radio-button>
      </el-radio-group>
      <el-table :data="rows" stripe size="small">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="schoolName" label="学校" min-width="180" />
        <el-table-column label="班级" width="130">
          <template #default="{ row }">{{ row.grade }}{{ row.className }}</template>
        </el-table-column>
        <el-table-column prop="stageLabel" label="状态" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="pick(row)">录入筛查/补检</el-button>
            <el-button link size="small" @click="detail(row)">档案</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <action-dialogs ref="dialogs" :student-id="sid" @done="load" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import ActionDialogs from '../components/ActionDialogs.vue'

const router = useRouter()
const rows = ref([])
const stage = ref('PENDING_SCREEN')
const sid = ref(null)
const dialogs = ref()
const stat = ref({ total: 0, pending: 0, absent: 0, done: 0 })

const load = async () => {
  rows.value = await api.students({ stage: stage.value || null })
  const all = await api.students({})
  stat.value = {
    total: all.length,
    pending: all.filter(s => s.stage === 'PENDING_SCREEN').length,
    absent: all.filter(s => s.stage === 'LOST_FOLLOW').length,
    done: all.filter(s => s.stage !== 'PENDING_SCREEN').length
  }
}
const pick = (row) => { sid.value = row.id; dialogs.value.open('screen') }
const detail = (row) => router.push(`/students/${row.id}`)
onMounted(load)
</script>
