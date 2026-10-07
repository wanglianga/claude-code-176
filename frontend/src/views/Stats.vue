<template>
  <div>
    <el-card style="margin-bottom:14px">
      <el-form inline>
        <el-form-item label="学校">
          <el-select v-model="schoolId" clearable placeholder="全县" style="width:220px" @change="loadAll">
            <el-option v-for="o in schools" :key="o.id" :label="o.name" :value="o.id"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-tag type="info">数据来源：学生健康管理全流程记录（筛查→告知→复诊→配镜→补助→复查→干预）</el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="14">
      <el-col :span="24">
        <el-card style="margin-bottom:14px">
          <template #header><b>近视筛查异常率月度趋势</b></template>
          <div ref="trendEl" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card style="margin-bottom:14px">
          <template #header><b>各学校对比：异常率 / 真实复诊率 / 干预执行率</b></template>
          <el-table :data="ranking" size="small">
            <el-table-column prop="schoolName" label="学校" show-overflow-tooltip/>
            <el-table-column prop="area" label="片区" width="90"/>
            <el-table-column label="异常率" width="90">
              <template #default="{ row }">
                <span :style="{color: row.abnormalRate > 30 ? '#e65d4f' : ''}">{{ row.abnormalRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="真实复诊率" width="100">
              <template #default="{ row }">
                <span :style="{color: row.revisitRate < 60 ? '#e65d4f' : '#14a44d'}">{{ row.revisitRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="干预执行率" width="100">
              <template #default="{ row }">{{ row.interventionExecRate }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card style="margin-bottom:14px">
          <template #header><b>学生管理状态分布（持续跟进）</b></template>
          <div ref="distEl" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card>
          <template #header><b>分年级异常率</b></template>
          <div ref="gradeEl" style="height:280px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../api'
import { STATUS_LABELS } from '../store'

const schools = ref([])
const schoolId = ref(null)
const ranking = ref([])
const trendEl = ref(), distEl = ref(), gradeEl = ref()
let charts = []

async function loadAll() {
  const params = schoolId.value ? { schoolId: schoolId.value } : {}
  const [trend, dist, grades, rank] = await Promise.all([
    api.get('/stats/trend', { params }),
    api.get('/stats/status-distribution', { params }),
    api.get('/stats/by-grade', { params }),
    api.get('/stats/school-ranking')
  ])
  ranking.value = rank
  await nextTick()
  charts.forEach(c => c.dispose())
  charts = []

  const t = echarts.init(trendEl.value)
  t.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['筛查人数', '异常人数', '异常率%'] },
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.monthly.map(x => x.month) },
    yAxis: [{ type: 'value', name: '人数' }, { type: 'value', name: '异常率%', max: 100 }],
    series: [
      { name: '筛查人数', type: 'bar', data: trend.monthly.map(x => x.total), itemStyle: { color: '#47b3f5' } },
      { name: '异常人数', type: 'bar', data: trend.monthly.map(x => x.abnormal), itemStyle: { color: '#e65d4f' } },
      { name: '异常率%', type: 'line', yAxisIndex: 1, smooth: true, data: trend.monthly.map(x => x.rate), itemStyle: { color: '#f0a020' } }
    ]
  })
  charts.push(t)

  const d = echarts.init(distEl.value)
  d.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: dist.map(x => ({ name: STATUS_LABELS[x.status] || x.status, value: x.count }))
    }]
  })
  charts.push(d)

  const g = echarts.init(gradeEl.value)
  g.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 30, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: grades.map(x => x.grade) },
    yAxis: { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'bar', barWidth: 40, data: grades.map(x => x.rate),
      itemStyle: { color: '#2f6fdb', borderRadius: [4, 4, 0, 0] },
      label: { show: true, formatter: '{c}%' },
      markLine: { data: [{ yAxis: 50, name: '关注线' }], lineStyle: { color: '#e65d4f' } }
    }]
  })
  charts.push(g)
  window.addEventListener('resize', () => charts.forEach(c => c.resize()))
}

onMounted(async () => {
  schools.value = (await api.get('/organizations')).filter(o => o.orgType === 'SCHOOL')
  loadAll()
})
</script>
