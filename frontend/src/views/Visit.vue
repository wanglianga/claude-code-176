<template>
  <div class="page-container">
    <el-card>
      <el-alert type="success" :closable="false" style="margin-bottom:10px"
        title="医院/门诊回写复诊结果后，处方参数、复查周期、补助资格与差异情况自动进入学生健康管理档案。" />
      <el-table :data="rows" stripe size="small">
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column label="学校/班级" min-width="190">
          <template #default="{ row }">{{ row.schoolName }} {{ row.grade }}{{ row.className }}</template>
        </el-table-column>
        <el-table-column prop="parentPhone" label="家长电话" width="140" />
        <el-table-column prop="stageLabel" label="状态" width="110" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="pick(row)">回写复诊处方</el-button>
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
const sid = ref(null)
const dialogs = ref()
const load = async () => {
  const wait = await api.students({ stage: 'AWAIT_VISIT' })
  const revisit = await api.students({ stage: 'REVIEW_DUE' })
  rows.value = [...wait, ...revisit]
}
const pick = (row) => { sid.value = row.id; dialogs.value.open('visit') }
const detail = (row) => router.push(`/students/${row.id}`)
onMounted(load)
</script>
