<template>
  <div class="page-container">
    <el-card>
      <el-form :inline="true">
        <el-form-item label="片区" v-if="!auth.isSchool">
          <el-select v-model="q.regionId" placeholder="全部" clearable style="width:130px"
                     @change="onRegionChange">
            <el-option v-for="r in regions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学校" v-if="!auth.isSchool">
          <el-select v-model="q.schoolId" placeholder="全部" clearable style="width:200px">
            <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="q.grade" placeholder="如 三年级" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.stage" placeholder="全部" clearable style="width:140px">
            <el-option v-for="(l,k) in stageLabels" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="姓名/学号" clearable style="width:140px"
                    @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="q.onlyImportError">仅看导入错误</el-checkbox>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <div style="margin-bottom:10px">
        <el-button type="success" :icon="Upload" @click="importDialog = true">批量导入名单</el-button>
        <el-button :icon="Download" tag="a" :href="templateUrl" target="_blank">下载导入模板</el-button>
        <el-button :icon="Plus" @click="openAdd">手工新增学生</el-button>
      </div>

      <el-table :data="rows" stripe size="small" border>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="80" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column label="学校/班级" min-width="200">
          <template #default="{ row }">
            {{ row.schoolName }}<br/>
            <span class="muted">{{ row.grade }}{{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="parentName" label="家长" width="80" />
        <el-table-column prop="parentPhone" label="家长电话" width="130" />
        <el-table-column label="当前状态" width="110">
          <template #default="{ row }">
            <el-tag :type="stageType(row.stage)" size="small">{{ row.stageLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常标记" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="(f,i) in row.flagLabels" :key="i" type="danger" size="small"
                    style="margin:2px">{{ f }}</el-tag>
            <el-tag v-if="row.importError" type="warning" size="small" style="margin:2px">
              导入错误
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">健康档案</el-button>
            <el-button v-if="row.importError" link type="warning" @click="openFix(row)">修正</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importDialog" title="批量导入学生名单" width="560px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        title="支持 .xlsx / .csv（UTF-8）。列为：年级,班级,学号,姓名,性别,出生日期,家长姓名,家长电话,既往视力档案。电话格式错误等不会阻断导入，将登记为“批量导入信息错误”异常事项，由班主任修正后闭环。" />
      <el-upload drag :auto-upload="false" :show-file-list="true" accept=".xlsx,.csv"
                 :on-change="onFileChange" :limit="1">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
      </el-upload>
      <el-select v-if="!auth.isSchool" v-model="importSchoolId" placeholder="导入到哪所学校"
                 style="width:100%;margin-top:12px">
        <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <template #footer>
        <el-button @click="importDialog=false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果 -->
    <el-dialog v-model="resultDialog" title="导入结果" width="600px">
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="总行数">{{ importResult.total }}</el-descriptions-item>
        <el-descriptions-item label="新增">{{ importResult.imported }}</el-descriptions-item>
        <el-descriptions-item label="更新">{{ importResult.updated }}</el-descriptions-item>
        <el-descriptions-item label="阻断错误">{{ importResult.errorCount }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="importResult.errors?.length" style="margin-top:12px">
        <div class="section-title">阻断错误明细</div>
        <el-table :data="importResult.errors" size="small" border max-height="220">
          <el-table-column prop="row" label="行号" width="70" />
          <el-table-column prop="reason" label="原因" />
        </el-table>
      </div>
      <el-alert v-else type="success" :closable="false" style="margin-top:12px"
        title="所有行均已写入；电话等软错误以“导入错误”标记和异常事项形式跟踪，可在列表中修正闭环。" />
    </el-dialog>

    <!-- 修正导入错误 -->
    <el-dialog v-model="fixDialog" title="修正导入信息" width="460px">
      <el-form label-width="90px">
        <el-form-item label="学生">{{ fixRow?.name }}</el-form-item>
        <el-form-item label="家长电话"><el-input v-model="fixForm.parentPhone" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="fixForm.gender" style="width:100%">
            <el-option label="男" value="男" /><el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="学号"><el-input v-model="fixForm.studentNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fixDialog=false">取消</el-button>
        <el-button type="primary" @click="doFix">保存并闭环异常事项</el-button>
      </template>
    </el-dialog>

    <!-- 手工新增 -->
    <el-dialog v-model="addDialog" title="手工新增学生" width="520px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="学校" v-if="!auth.isSchool">
          <el-select v-model="addForm.schoolId" style="width:100%">
            <el-option v-for="s in schools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级"><el-input v-model="addForm.grade" /></el-form-item>
        <el-form-item label="班级"><el-input v-model="addForm.className" placeholder="如 2班" /></el-form-item>
        <el-form-item label="学号"><el-input v-model="addForm.studentNo" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="addForm.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="addForm.gender">
            <el-radio value="男">男</el-radio><el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="addForm.birthDate" value-format="YYYY-MM-DD" style="width:100%"/></el-form-item>
        <el-form-item label="家长姓名"><el-input v-model="addForm.parentName" /></el-form-item>
        <el-form-item label="家长电话"><el-input v-model="addForm.parentPhone" /></el-form-item>
        <el-form-item label="既往视力档案"><el-input v-model="addForm.priorVisionArchive" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog=false">取消</el-button>
        <el-button type="primary" @click="doAdd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Upload, Download, Plus, UploadFilled } from '@element-plus/icons-vue'
import { api } from '../api'
import { useAuthStore } from '../store/auth'

const STAGE_LABELS = {
  PENDING_SCREEN: '待筛查', SCREENED: '已筛查', ABNORMAL_REVIEW: '异常复核中',
  AWAIT_PARENT: '待家长确认', AWAIT_VISIT: '待复诊', VISITED: '已复诊',
  GLASSES_SUBSIDY: '配镜补助中', MANAGING: '持续管理', REVIEW_DUE: '复查到期',
  CLOSED_LOOP: '本学年闭环', DEFERRED: '家长暂缓', LOST_FOLLOW: '失访跟进'
}
const stageLabels = STAGE_LABELS
const stageType = (s) => ({
  CLOSED_LOOP: 'success', MANAGING: 'success', VISITED: '', GLASSES_SUBSIDY: 'warning',
  DEFERRED: 'info', LOST_FOLLOW: 'danger', REVIEW_DUE: 'danger', AWAIT_VISIT: 'warning',
  AWAIT_PARENT: 'warning', ABNORMAL_REVIEW: 'warning'
}[s] || 'info')

const router = useRouter()
const auth = useAuthStore()
const regions = ref([])
const schools = ref([])
const rows = ref([])
const q = reactive({ regionId: null, schoolId: auth.isSchool ? auth.schoolId : null,
  grade: '', stage: '', keyword: '', onlyImportError: false })

const templateUrl = api.importTemplateUrl

const load = async () => {
  rows.value = await api.students({
    regionId: q.regionId, schoolId: q.schoolId, grade: q.grade || null,
    stage: q.stage || null, keyword: q.keyword || null, onlyImportError: q.onlyImportError
  })
}
const onRegionChange = async () => {
  schools.value = await api.schools(q.regionId)
}
const openDetail = (row) => router.push(`/students/${row.id}`)

/* 导入 */
const importDialog = ref(false)
const importing = ref(false)
const uploading = ref(false)
const importFile = ref(null)
const importSchoolId = ref(null)
const resultDialog = ref(false)
const importResult = reactive({ total: 0, imported: 0, updated: 0, errorCount: 0, errors: [] })
const onFileChange = (file) => { importFile.value = file.raw }
const doImport = async () => {
  if (!importFile.value) return ElMessage.warning('请先选择文件')
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    if (importSchoolId.value) fd.append('schoolId', importSchoolId.value)
    const r = await api.importStudents(fd)
    Object.assign(importResult, r)
    importDialog.value = false
    resultDialog.value = true
    load()
  } finally { uploading.value = false }
}

/* 修正 */
const fixDialog = ref(false)
const fixRow = ref(null)
const fixForm = reactive({ studentId: null, parentPhone: '', gender: '', studentNo: '' })
const openFix = (row) => {
  fixRow.value = row
  Object.assign(fixForm, { studentId: row.id, parentPhone: row.parentPhone, gender: row.gender, studentNo: row.studentNo })
  fixDialog.value = true
}
const doFix = async () => {
  await api.fixImport({ ...fixForm })
  ElMessage.success('已保存并闭环该导入错误事项')
  fixDialog.value = false
  load()
}

/* 新增 */
const addDialog = ref(false)
const addForm = reactive({ schoolId: null, grade: '', className: '', studentNo: '', name: '',
  gender: '男', birthDate: '', parentName: '', parentPhone: '', priorVisionArchive: '' })
const openAdd = () => {
  Object.assign(addForm, { schoolId: auth.schoolId, grade: '', className: '', studentNo: '', name: '',
    gender: '男', birthDate: '', parentName: '', parentPhone: '', priorVisionArchive: '' })
  addDialog.value = true
}
const doAdd = async () => {
  await api.saveStudent({ ...addForm })
  ElMessage.success('已新增')
  addDialog.value = false
  load()
}

onMounted(async () => {
  regions.value = await api.regions()
  schools.value = await api.schools()
  load()
})
</script>
