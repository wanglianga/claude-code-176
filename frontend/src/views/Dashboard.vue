<template>
  <div>
    <el-row :gutter="14">
      <el-col :span="6" v-for="k in cards" :key="k.label">
        <el-card shadow="hover" class="kpi">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{ color: k.color }">{{ k.value }}</div>
          <div class="kpi-sub">{{ k.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="14">
        <el-card>
          <template #header><b>异常学生健康管理闭环漏斗</b></template>
          <div ref="funnelEl" style="height:340px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><b>关键监管指标</b></template>
          <div class="rate-row" v-for="r in rates" :key="r.label">
            <span>{{ r.label }}</span>
            <el-progress :percentage="r.value" :color="r.color" style="flex:1;margin:0 14px" />
            <b>{{ r.value }}%</b>
          </div>
          <el-alert type="warning" :closable="false" style="margin-top:10px"
            :title="'当前未结问题 ' + ov.openIssueCount + ' 条；复查逾期 ' + ov.followUpMissed + ' 人，需学校持续跟进'"/>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="12">
        <el-card>
          <template #header><b>各年级近视异常率</b></template>
          <div ref="gradeEl" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><b>片区分布</b></template>
          <div ref="areaEl" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../api'

const ov = ref({})
const grades = ref([])
const areas = ref([])
const funnelEl = ref(), gradeEl = ref(), areaEl = ref()

const cards = computed(() => [
  { label: '在管学生', value: ov.value.studentCount ?? '-', sub: '已建档人数', color: '#2f6fdb' },
  { label: '筛查异常', value: ov.value.abnormalCount ?? '-', sub: '需家长确认与复诊', color: '#e65d4f' },
  { label: '真实复诊完成', value: ov.value.revisitCompletedCount ?? '-', sub: '医院/门诊回写', color: '#14a44d' },
  { label: '补助已发放', value: ov.value.subsidyPaidCount ?? '-', sub: '符合补助 ' + (ov.value.subsidyEligibleCount ?? 0) + ' 人', color: '#f0a020' }
])
const rates = computed(() => [
  { label: '真实复诊率', value: ov.value.realRevisitRate ?? 0, color: '#2f6fdb' },
  { label: '学校干预执行率', value: ov.value.interventionExecRate ?? 0, color: '#14a44d' },
  { label: '周期复查到场率', value: ov.value.followUpRate ?? 0, color: '#f0a020' }
])

onMounted(async () => {
  ov.value = await api.get('/stats/overview')
  grades.value = await api.get('/stats/by-grade')
  areas.value = await api.get('/stats/by-area')
  await nextTick()
  const fn = echarts.init(funnelEl.value)
  fn.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel', left: '8%', width: '84%', minSize: '20%',
      label: { formatter: '{b}: {c}', fontSize: 13 },
      data: (ov.value.funnel || []).map(f => ({ name: f.name, value: f.value })),
      color: ['#e65d4f', '#f0a020', '#e6c84f', '#47b3f5', '#2f6fdb', '#14a44d']
    }]
  })
  const g = echarts.init(gradeEl.value)
  g.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: grades.value.map(x => x.grade) },
    yAxis: { type: 'value', max: 100, name: '%' },
    series: [{
      type: 'bar', barWidth: 36,
      data: grades.value.map(x => x.rate),
      itemStyle: { color: '#2f6fdb', borderRadius: [4, 4, 0, 0] },
      label: { show: true, formatter: '{c}%' }
    }]
  })
  const a = echarts.init(areaEl.value)
  a.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '46%'],
      data: areas.value.map(x => ({ name: x.area + '(' + x.abnormal + '/' + x.total + ')', value: x.total }))
    }]
  })
  window.addEventListener('resize', () => { fn.resize(); g.resize(); a.resize() })
})
</script>

<style scoped>
.kpi-label { color: #909399; font-size: 13px; }
.kpi-value { font-size: 34px; font-weight: 700; margin: 4px 0; }
.kpi-sub { color: #a8abb2; font-size: 12px; }
.rate-row { display: flex; align-items: center; margin: 14px 0; font-size: 13px; }
</style>
