<template>
  <el-container style="height:100vh">
    <el-aside width="220px" class="aside">
      <div class="logo-bar">
        <span style="font-size:22px">👁️</span>
        <span class="logo-text">县域近视防控平台</span>
      </div>
      <el-menu :default-active="$route.path" router class="menu"
               background-color="#164b5f" text-color="#cfe6ef" active-text-color="#ffffff">
        <template v-for="r in menus" :key="r.path">
          <el-menu-item :index="'/' + r.path">
            <el-icon><component :is="r.meta.icon" /></el-icon>
            <span>{{ r.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="page-name">{{ $route.name }}</div>
        <div class="user-box">
          <el-tag type="info" effect="plain">{{ auth.user?.roleLabel }}</el-tag>
          <span class="uname">{{ auth.user?.realName }}</span>
          <span v-if="auth.user?.schoolName" class="muted">{{ auth.user.schoolName }}</span>
          <el-button link type="primary" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const router = useRouter()

const menus = computed(() => {
  const root = router.options.routes.find(r => r.path === '/')
  return root.children.filter(c => {
    if (c.meta?.hidden) return false
    if (!c.meta?.roles) return true
    return c.meta.roles.includes(auth.role)
  }).map(c => ({ ...c, path: c.path }))
})

const logout = () => {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.aside { background: #164b5f; overflow-y: auto; }
.logo-bar {
  height: 58px; display: flex; align-items: center; gap: 8px;
  padding: 0 16px; color: #fff; font-weight: 700;
  background: #0f3a4b;
}
.logo-text { font-size: 15px; line-height: 1.3; }
.menu { border-right: none; }
:deep(.el-menu-item.is-active) { background: #1f6f8b !important; }
.header {
  background: #fff; display: flex; align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
}
.page-name { font-weight: 700; font-size: 16px; color: #1f3a4d; }
.user-box { display: flex; align-items: center; gap: 10px; }
.uname { font-size: 14px; }
.main { background: #f4f6f9; padding: 16px; overflow-y: auto; }
</style>
