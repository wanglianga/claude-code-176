import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    role: s => s.user?.role,
    isCounty: s => ['PLATFORM_ADMIN', 'EDU_BUREAU', 'HEALTH_COMMISSION'].includes(s.user?.role)
  },
  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})

export const STATUS_LABELS = {
  PENDING_SCREENING: '待筛查',
  SCREENED_ABNORMAL: '可疑待复测',
  AWAIT_CONFIRM: '待家长确认',
  AWAIT_REVIEW: '待学校复核',
  AWAIT_REVISIT: '待复诊',
  IN_REVISIT: '复诊中',
  REVISIT_DONE: '已复诊',
  GLASSES_FITTED: '已配镜',
  SUBSIDY_PAID: '补助已发放',
  ON_HOLD: '家长暂缓',
  MISSED_REVIEW: '复查逾期',
  CLOSED: '本周期闭环'
}

export const STATUS_TYPES = {
  PENDING_SCREENING: 'info',
  SCREENED_ABNORMAL: 'warning',
  AWAIT_CONFIRM: 'danger',
  AWAIT_REVIEW: 'warning',
  AWAIT_REVISIT: 'warning',
  REVISIT_DONE: 'primary',
  GLASSES_FITTED: 'primary',
  SUBSIDY_PAID: 'success',
  ON_HOLD: 'info',
  MISSED_REVIEW: 'danger',
  CLOSED: 'success'
}

export const ISSUE_LABELS = {
  ABSENT: '筛查请假',
  PHONE_UNREACHABLE: '联系不畅/暂缓',
  IMPORT_ERROR: '导入信息错误',
  RESULT_DIVERGENCE: '筛查复诊差异',
  SELF_GLASSES: '自行外配眼镜',
  SUBSIDY_DISPUTE: '补助资格争议',
  GLASSES_DISCOMFORT: '配镜不适',
  MISSED_REVIEW: '复查未到场'
}

export const ISSUE_TYPES = {
  ABSENT: 'warning',
  PHONE_UNREACHABLE: 'danger',
  IMPORT_ERROR: 'warning',
  RESULT_DIVERGENCE: 'danger',
  SELF_GLASSES: 'info',
  SUBSIDY_DISPUTE: 'danger',
  GLASSES_DISCOMFORT: 'warning',
  MISSED_REVIEW: 'danger'
}

export const ROLE_LABELS = {
  PLATFORM_ADMIN: '平台管理员',
  EDU_BUREAU: '县教体部门',
  HEALTH_COMMISSION: '县卫健部门',
  SCHOOL_HEALTH_TEACHER: '学校健康老师',
  HEAD_TEACHER: '班主任',
  SCREENER: '筛查人员',
  DOCTOR: '眼科医生',
  OPTOMETRIST: '视光师',
  OPTICIAN: '眼镜店',
  PARENT: '家长'
}
