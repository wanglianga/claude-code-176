<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="学校">
          <el-select v-model="query.schoolId" placeholder="全部" clearable style="width:180px" @change="load">
            <el-option v-for="o in schools" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="query.grade" clearable style="width:110px" @change="load" placeholder="如 三年级"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:150px" @change="load">
            <el-option v-for="(l,k) in STATUS_LABELS" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.kw" clearable style="width:180px" placeholder="姓名/学籍号/电话" @change="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="importVisible = true">批量导入名单</el-button>
          <el-button @click="editStudent(null)">新增学生</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" stripe @row-click="open" size="small" style="cursor:pointer">
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="grade" label="年级" width="80" />
        <el-table-column label="班级" width="80">
          <template #default="{ row }">{{ row.className || '-' }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学籍号" width="140" />
        <el-table-column prop="parentName" label="家长" width="90" />
        <el-table-column prop="parentPhone" label="家长电话" width="130" />
        <el-table-column prop="pastVisionNote" label="既往视力档案" show-overflow-tooltip />
        <el-table-column label="当前状态" width="130">
          <template #default="{ row }">
            <el-tag :type="STATUS_TYPES[row.status]">{{ STATUS_LABELS[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="open(row)">健康记录</el-button>
            <el-button link type="warning" @click.stop="editStudent(row)">修正</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:10px" layout="total, prev, pager, next"
        :total="total" :page-size="query.size" :current-page="query.page"
        @current-change="p => { query.page = p; load() }" />
    </el-card>

    <!-- 导入 -->
    <el-dialog v-model="importVisible" title="班主任批量导入学生名单" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        title="CSV 表头（UTF-8）：姓名,性别,出生日期,年级,班级,学籍号,家长姓名,家长电话,既往视力档案；年级班级不存在时自动创建；错误行会登记为“导入信息错误”问题。"/>
      <el-select v-model="importSchoolId" placeholder="选择导入学校" style="width:100%;margin-bottom:10px">
        <el-option v-for="o in schools.filter(s => s.orgType === 'SCHOOL')" :key="o.id" :label="o.name" :value="o.id" />
      </el-select>
      <el-upload :action="uploadUrl" :headers="uploadHeaders" :data="{ schoolId: importSchoolId }"
        name="file" accept=".csv" :on-success="onImported" :show-file-list="false" drag>
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div>将 CSV 文件拖到此处，或<em>点击上传</em></div>
      </el-upload>
      <div v-if="importResult" style="margin-top:12px">
        <el-result :icon="importResult.failed ? 'warning' : 'success'"
          :title="`成功 ${importResult.success} / ${importResult.total}，失败 ${importResult.failed}`" />
        <el-alert v-for="(e,i) in importResult.errors" :key="i" type="error" :title="e" :closable="false" style="margin-top:4px"/>
      </div>
    </el-dialog>

    <!-- 新增/修正 -->
    <el-dialog v-model="editVisible" :title="form.id ? '修正学生信息（导入错误更正）' : '新增学生'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学校">
          <el-select v-model="form.schoolId" style="width:100%">
            <el-option v-for="o in schools.filter(s => s.orgType === 'SCHOOL')" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="form.classId" style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.grade + c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender"><el-radio value="男">男</el-radio><el-radio value="女">女</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="学籍号"><el-input v-model="form.studentNo" /></el-form-item>
        <el-form-item label="家长姓名"><el-input v-model="form.parentName" /></el-form-item>
        <el-form-item label="家长电话"><el-input v-model="form.parentPhone" /></el-form-item>
        <el-form-item label="既往视力档案"><el-input v-model="form.pastVisionNote" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStudent">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { STATUS_LABELS, STATUS_TYPES } from '../store'

const router = useRouter()
const schools = ref([])
const classes = ref([])
const rows = ref([])
const total = ref(0)
const query = reactive({ schoolId: null, grade: '', status: '', kw: '', page: 1, size: 15 })
const importVisible = ref(false)
const importSchoolId = ref(null)
const importResult = ref(null)
const editVisible = ref(false)
const form = reactive({})
const uploadUrl = '/api/students/import'
const uploadHeaders = { Authorization: 'Bearer ' + localStorage.getItem('token') }

async function load() {
  const data = await api.get('/students', { params: query })
  const orgMap = new Map(schools.value.map(s => [s.id, s]))
  rows.value = data.items.map(s => ({
    ...s,
    schoolName: orgMap.get(s.schoolId)?.name
  }))
  await loadClasses()
  rows.value.forEach(s => {
    s.className = classes.value.find(c => c.id === s.classId)?.className
  })
  total.value = data.total
}
async function loadClasses() {
  const sid = query.schoolId
  classes.value = await api.get('/classes', sid ? { params: { schoolId: sid } } : undefined)
}
function open(row) { router.push('/students/' + row.id) }
function editStudent(row) {
  Object.keys(form).forEach(k => delete form[k])
  if (row) Object.assign(form, JSON.parse(JSON.stringify(row)))
  form.birthDate = form.birthDate?.slice(0, 10)
  editVisible.value = true
}
async function saveStudent() {
  if (form.id) await api.put('/students/' + form.id, form)
  else await api.post('/students', form)
  ElMessage.success('已保存，修正已记入学生健康管理时间线')
  editVisible.value = false
  load()
}
function onImported(res) {
  importResult.value = res
  if (res.success) ElMessage.success(`成功导入 ${res.success} 人`)
  load()
}
onMounted(async () => {
  schools.value = await api.get('/organizations')
  await load()
  if (!importSchoolId.value) {
    const s = schools.value.find(x => x.orgType === 'SCHOOL')
    if (s) importSchoolId.value = s.id
  }
})
</script>
