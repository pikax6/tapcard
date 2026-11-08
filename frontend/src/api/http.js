import axios from 'axios'
import { ElMessage } from 'element-plus'

// 一个 axios 实例，全项目只用它。baseURL 留 /api，开发期走 vite proxy
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('tapcard_token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

// 后端统一返回 {code, msg, data}：code === 0 才算成功
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(body)
    }
    return body
  },
  (err) => {
    const status = err.response && err.response.status
    if (status === 401) {
      localStorage.removeItem('tapcard_token')
      ElMessage.warning('登录已过期，请重新登录')
      location.hash = '#/login'
    } else if (status === 403) {
      ElMessage.error('没有权限访问该数据')
    } else {
      ElMessage.error((err.response && err.response.data && err.response.data.msg) || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default http
