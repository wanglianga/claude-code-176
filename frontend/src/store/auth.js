import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('vision_user') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.user,
    role: (s) => s.user?.role,
    isGov: (s) => ['ADMIN', 'GOV_EDU', 'GOV_HEALTH'].includes(s.user?.role),
    isSchool: (s) => ['SCHOOL_DOCTOR', 'TEACHER'].includes(s.user?.role),
    isMedical: (s) => ['HOSPITAL', 'CLINIC'].includes(s.user?.role),
    schoolId: (s) => s.user?.schoolId
  },
  actions: {
    setLogin(user) {
      this.user = user
      localStorage.setItem('vision_user', JSON.stringify(user))
    },
    logout() {
      this.user = null
      localStorage.removeItem('vision_user')
    }
  }
})
