<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="40" color="#2f6fdb"><View /></el-icon>
        <h2>县域儿童青少年近视<br/>筛查复诊配镜补助平台</h2>
        <p>筛查 · 告知 · 复诊 · 处方 · 配镜 · 补助 · 复查 · 学校干预 一体化健康管理</p>
      </div>
      <el-form :model="form" @keyup.enter="submit">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" placeholder="密码（演示账号统一 123456）" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <el-divider>演示账号（点击填充）</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc-tag" @click="fill(a.u)">
          {{ a.label }}：{{ a.u }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: 'teacher.wang', password: '123456' })
const loading = ref(false)

const accounts = [
  { u: 'admin', label: '平台管理员' },
  { u: 'edu', label: '县教体' },
  { u: 'health', label: '县卫健' },
  { u: 'teacher.wang', label: '健康老师' },
  { u: 'head.li', label: '班主任' },
  { u: 'screener.chen', label: '筛查员' },
  { u: 'doctor.zhou', label: '眼科医生' },
  { u: 'optom.wu', label: '视光师' },
  { u: 'optician.sun', label: '眼镜店' }
]
const fill = u => { form.username = u; form.password = '123456' }

async function submit() {
  loading.value = true
  try {
    const res = await api.post('/auth/login', form)
    auth.setAuth(res.token, res.user)
    ElMessage.success('欢迎，' + res.user.realName)
    router.push('/dashboard')
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2f6fdb 0%, #47b3f5 100%);
}
.login-card {
  width: 460px; background: #fff; border-radius: 14px; padding: 36px 40px;
  box-shadow: 0 18px 50px rgba(0,0,0,.2);
}
.brand { text-align: center; margin-bottom: 18px; }
.brand h2 { font-size: 20px; line-height: 1.5; margin: 12px 0 8px; color: #1f2d3d; }
.brand p { font-size: 12px; color: #909399; margin: 0; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acc-tag { cursor: pointer; }
</style>
