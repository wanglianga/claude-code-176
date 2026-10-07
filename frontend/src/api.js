import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

api.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.error || err.message
    if (status === 401) {
      localStorage.removeItem('token')
      if (location.hash !== '#/login') location.hash = '#/login'
    } else {
      ElMessage.error(msg || '请求失败')
    }
    return Promise.reject(err)
  }
)

export default api
