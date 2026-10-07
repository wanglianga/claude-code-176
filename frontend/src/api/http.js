import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/',
  timeout: 20000
})

http.interceptors.request.use(config => {
  const raw = localStorage.getItem('vision_user')
  if (raw) {
    const user = JSON.parse(raw)
    config.headers.Authorization = `Bearer ${user.token}`
  }
  return config
})

http.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('vision_user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default http
