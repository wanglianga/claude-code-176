<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-radio-group v-model="status" @change="load">
            <el-radio-button value="OPEN">待处理</el-radio-button>
            <el-radio-button value="PROCESSING">处理中</el-radio-button>
            <el-radio-button value="RESOLVED">已处理</el-radio-button>
            <el-radio-button value="">全部</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="issueType" clearable placeholder="全部类型" style="width:170px" @change="load">
            <el-option v-for="(l,k) in ISSUE_LABELS" :key="k" :label="l" :value="k"/>
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="rows" size="small" stripe>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">
            <el-tag :type="ISSUE_TYPES[row.issue.issueType]">
              {{ ISSUE_LABELS[row.issue.issueType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学生" width="100">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push('/students/'+row.issue.studentId)">{{ row.studentName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="issue.title" label="标题" width="220" show-overflow-tooltip/>
        <el-table-column prop="issue.description" label="情况说明" show-overflow-tooltip/>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.issue.status==='RESOLVED'?'success':(row.issue.status==='PROCESSING'?'warning':'danger')" size="small">
              {{ row.issue.status === 'RESOLVED' ? '已处理' : row.issue.status === 'PROCESSING' ? '处理中' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issue.resolution" label="处理结果" show-overflow-tooltip/>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="row.issue.status==='OPEN'" link type="warning" @click="mark(row,'process')">开始处理</el-button>
            <el-button v-if="row.issue.status!=='RESOLVED'" link type="success" @click="mark(row,'resolve')">处理完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { ISSUE_LABELS, ISSUE_TYPES } from '../store'

const rows = ref([])
const status = ref('OPEN')
const issueType = ref('')

async function load() {
  rows.value = await api.get('/issues', {
    params: { status: status.value || undefined, issueType: issueType.value || undefined }
  })
}
async function mark(row, action) {
  if (action === 'process') {
    await api.post(`/issues/${row.issue.id}/process`)
    ElMessage.success('已标记处理中')
  } else {
    const { value } = await ElMessageBox.prompt('处理结果说明（将写入学生健康管理记录）', '完成处理', {
      confirmButtonText: '提交', inputType: 'textarea'
    }).catch(() => ({}))
    if (value === undefined) return
    await api.post(`/issues/${row.issue.id}/resolve`, { resolution: value, resolve: true })
    ElMessage.success('问题已闭环')
  }
  load()
}
onMounted(load)
</script>
