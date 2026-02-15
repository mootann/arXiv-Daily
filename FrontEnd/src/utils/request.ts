import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse, LoginResponse } from '@/types'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
})

// 是否正在刷新token
let isRefreshing = false
// 重试队列
let requests: Function[] = []

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code === 200) {
      return response
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  async (error) => {
    const originalRequest = error.config
    
    // 如果是401错误，且不是刷新token的请求，则尝试刷新
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/refresh')) {
      if (isRefreshing) {
        // 如果正在刷新，将请求加入队列
        return new Promise((resolve) => {
          requests.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(service(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          // 使用新的axios实例请求刷新token，避免循环拦截
          const { data } = await axios.post<ApiResponse<LoginResponse>>('/api/v1/auth/refresh', { refreshToken })
          
          if (data.code === 200) {
            const { accessToken, refreshToken: newRefreshToken } = data.data
            localStorage.setItem('token', accessToken)
            if (newRefreshToken) {
              localStorage.setItem('refreshToken', newRefreshToken)
            }
            
            // 执行队列中的请求
            requests.forEach((cb) => cb(accessToken))
            requests = []
            
            // 重试当前请求
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            return service(originalRequest)
          }
        } catch (refreshError) {
          console.error('刷新Token失败', refreshError)
          // 刷新失败，清除token并跳转登录
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          router.push('/login')
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      } else {
        // 无refresh token，直接跳转登录
        localStorage.removeItem('token')
        router.push('/login')
      }
    }

    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
