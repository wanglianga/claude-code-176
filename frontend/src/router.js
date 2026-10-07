import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue') },
  {
    path: '/',
    component: () => import('./views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: '总览看板', component: () => import('./views/Dashboard.vue') },
      { path: 'students', name: '学生档案', component: () => import('./views/Students.vue') },
      { path: 'students/:id', name: '学生健康管理记录', component: () => import('./views/StudentDetail.vue') },
      { path: 'screening', name: '筛查组织', component: () => import('./views/Screening.vue') },
      { path: 'follow', name: '告知与复核', component: () => import('./views/Follow.vue') },
      { path: 'revisit', name: '复诊回写', component: () => import('./views/Revisit.vue') },
      { path: 'glasses', name: '配镜与补助', component: () => import('./views/Glasses.vue') },
      { path: 'interventions', name: '学校干预', component: () => import('./views/Interventions.vue') },
      { path: 'issues', name: '异常跟进清单', component: () => import('./views/Issues.vue') },
      { path: 'stats', name: '趋势与监管', component: () => import('./views/Stats.vue') },
      { path: 'admin', name: '机构与账号', component: () => import('./views/Admin.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') return '/login'
})

export default router
