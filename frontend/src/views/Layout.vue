<template>
  <el-container style="height:100vh">
    <el-aside width="220px" class="aside">
      <div class="logo"><el-icon :size="22"><View /></el-icon><span>明眸护瞳管理平台</span></div>
      <el-menu :default-active="active" router background-color="#1f2d3d" text-color="#c0ccda"
               active-text-color="#ffd04b">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon><span>总览看板</span></el-menu-item>
        <el-menu-item index="/students"><el-icon><User /></el-icon><span>学生档案</span></el-menu-item>
        <el-menu-item index="/screening"><el-icon><Document /></el-icon><span>筛查组织/录入</span></el-menu-item>
        <el-menu-item index="/follow"><el-icon><Bell /></el-icon><span>家长确认/学校复核</span></el-menu-item>
        <el-menu-item index="/revisit"><el-icon><FirstAidKit /></el-icon><span>复诊回写/复查</span></el-menu-item>
        <el-menu-item index="/glasses"><el-icon><ShoppingCart /></el-icon><span>配镜与补助</span></el-menu-item>
        <el-menu-item index="/interventions"><el-icon><School /></el-icon><span>学校干预</span></el-menu-item>
        <el-menu-item index="/issues"><el-icon><Warning /></el-icon>
          <span>异常跟进清单</span>
          <el-badge v-if="openCount" :value="openCount" class="badge" />
        </el-menu-item>
        <el-menu-item index="/stats"><el-icon><TrendCharts /></el-icon><span>趋势与监管</span></el-menu-item>
        <el-menu-item v-if="isCounty" index="/admin"><el-icon><Setting /></el-icon><span>机构与账号</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="title">{{ route.meta.name || route.name }}</div>
        <div class="right">
          <el-tag effect="plain">{{ roleLabel }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="28">{{ auth.user?.realName?.[0] }}</el-avatar>
              {{ auth.user?.realName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { useAuthStore, ROLE_LABELS } from '../store'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const active = computed(() => route.path)
const roleLabel = computed(() => ROLE_LABELS[auth.role] || auth.role)
const isCounty = computed(() => auth.isCounty)
const openCount = ref(0)

async function loadOpen() {
  try {
    const data = await api.get('/issues', { params: { status: 'OPEN' } })
    openCount.value = data.length
  } catch (e) { /* ignore */ }
}
onMounted(loadOpen)

function onCommand(c) {
  if (c === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.aside { background: #1f2d3d; overflow-x: hidden; }
.logo {
  height: 60px; display: flex; align-items: center; gap: 8px;
  color: #fff; font-weight: 600; padding: 0 18px; font-size: 15px;
  white-space: nowrap;
}
.aside :deep(.el-menu) { border-right: none; }
.badge { margin-left: 8px; }
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}
.title { font-weight: 600; font-size: 16px; }
.right { display: flex; align-items: center; gap: 14px; }
.user { display: flex; align-items: center; gap: 6px; cursor: pointer; outline: none; }
.main { background: #f4f6fa; padding: 16px; }
</style>
