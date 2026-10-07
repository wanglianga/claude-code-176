<template>
  <div>
    <el-row :gutter="14">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between">
              <b>机构管理</b>
              <el-button type="primary" size="small" @click="openOrg(null)">新增机构</el-button>
            </div>
          </template>
          <el-table :data="orgs" size="small">
            <el-table-column prop="name" label="机构名称" show-overflow-tooltip/>
            <el-table-column label="类型" width="140">
              <template #default="{ row }">{{ orgTypeLabel[row.orgType] }}</template>
            </el-table-column>
            <el-table-column prop="area" label="片区" width="100"/>
            <el-table-column prop="contactName" label="联系人" width="90"/>
            <el-table-column prop="contactPhone" label="联系电话" width="130"/>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="openOrg(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between">
              <b>账号管理</b>
              <el-button type="primary" size="small" @click="openUser(null)">新增账号</el-button>
            </div>
          </template>
          <el-table :data="users" size="small">
            <el-table-column prop="username" label="用户名" width="120"/>
            <el-table-column prop="realName" label="姓名" width="90"/>
            <el-table-column label="角色" width="100">
              <template #default="{ row }">{{ ROLE_LABELS[row.role] }}</template>
            </el-table-column>
            <el-table-column prop="organizationName" label="所属机构" show-overflow-tooltip/>
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.active?'success':'info'" size="small">{{ row.active ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="openUser(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="orgDialog" :title="orgForm.id?'编辑机构':'新增机构'" width="460px">
      <el-form :model="orgForm" label-width="90px">
        <el-form-item label="机构名称"><el-input v-model="orgForm.name"/></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="orgForm.orgType" style="width:100%">
            <el-option v-for="(l,k) in orgTypeLabel" :key="k" :label="l" :value="k"/>
          </el-select>
        </el-form-item>
        <el-form-item label="片区"><el-input v-model="orgForm.area"/></el-form-item>
        <el-form-item label="联系人"><el-input v-model="orgForm.contactName"/></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="orgForm.contactPhone"/></el-form-item>
        <el-form-item label="地址"><el-input v-model="orgForm.address"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orgDialog=false">取消</el-button>
        <el-button type="primary" @click="saveOrg">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userDialog" :title="userForm.id?'编辑账号':'新增账号'" width="460px">
      <el-form :model="userForm" label-width="90px">
        <el-form-item label="用户名"><el-input v-model="userForm.username" :disabled="!!userForm.id"/></el-form-item>
        <el-form-item label="姓名"><el-input v-model="userForm.realName"/></el-form-item>
        <el-form-item :label="userForm.id?'重置密码':'密码'">
          <el-input v-model="userForm.password" :placeholder="userForm.id?'留空则不修改':'初始密码'"/>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width:100%">
            <el-option v-for="(l,k) in ROLE_LABELS" :key="k" :label="l" :value="k"/>
          </el-select>
        </el-form-item>
        <el-form-item label="所属机构">
          <el-select v-model="userForm.organizationId" clearable style="width:100%">
            <el-option v-for="o in orgs" :key="o.id" :label="o.name" :value="o.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="电话"><el-input v-model="userForm.phone"/></el-form-item>
        <el-form-item v-if="userForm.id" label="启用">
          <el-switch v-model="userForm.active"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialog=false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { ROLE_LABELS } from '../store'

const orgs = ref([])
const users = ref([])
const orgDialog = ref(false)
const userDialog = ref(false)
const orgForm = reactive({})
const userForm = reactive({})
const orgTypeLabel = {
  SCHOOL: '学校', SCREENING_AGENCY: '筛查机构', HOSPITAL: '县医院',
  OPTOMETRY_CLINIC: '合作眼科门诊', OPTICAL_SHOP: '补助定点眼镜店',
  EDU_BUREAU: '县教体部门', HEALTH_COMMISSION: '县卫健部门'
}

async function load() {
  orgs.value = await api.get('/organizations')
  users.value = await api.get('/users')
}
function openOrg(row) {
  Object.keys(orgForm).forEach(k => delete orgForm[k])
  if (row) Object.assign(orgForm, JSON.parse(JSON.stringify(row)))
  orgDialog.value = true
}
function openUser(row) {
  Object.keys(userForm).forEach(k => delete userForm[k])
  if (row) Object.assign(userForm, JSON.parse(JSON.stringify(row)))
  userForm.password = ''
  if (!row) userForm.active = true
  userDialog.value = true
}
async function saveOrg() {
  if (!orgForm.name || !orgForm.orgType) return ElMessage.warning('名称与类型必填')
  await api.post('/organizations', orgForm)
  ElMessage.success('机构已保存')
  orgDialog.value = false
  load()
}
async function saveUser() {
  if (!userForm.username || !userForm.realName || !userForm.role) {
    return ElMessage.warning('用户名、姓名、角色必填')
  }
  if (userForm.id) await api.put('/users/' + userForm.id, userForm)
  else {
    if (!userForm.password) return ElMessage.warning('请设置初始密码')
    await api.post('/users', userForm)
  }
  ElMessage.success('账号已保存')
  userDialog.value = false
  load()
}
onMounted(load)
</script>
