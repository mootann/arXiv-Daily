import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, LoginResponse, RegisterRequest } from '@/types'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const refreshToken = ref<string>('')
  const user = ref<User | null>(null)
  const isLoading = ref(false)

  // 检查是否登录
  const isLoggedIn = computed(() => !!token.value && !!user.value)

  // 初始化用户状态
  const initUser = async () => {
    const savedToken = localStorage.getItem('token')
    const savedRefreshToken = localStorage.getItem('refreshToken')
    if (savedToken) {
      token.value = savedToken
      if (savedRefreshToken) {
        refreshToken.value = savedRefreshToken
      }
      try {
        const res = await authApi.getCurrentUser()
        if (res.data.code === 200) {
          user.value = res.data.data
        } else {
          logout()
        }
      } catch (error) {
        console.error('获取用户信息失败', error)
        logout()
      }
    }
  }

  // 登录
  const login = async (data: LoginRequest): Promise<boolean> => {
    isLoading.value = true
    try {
      const res = await authApi.login(data)
      if (res.data.code === 200) {
        const loginData: LoginResponse = res.data.data
        token.value = loginData.accessToken
        refreshToken.value = loginData.refreshToken
        user.value = {
          id: loginData.userId,
          username: loginData.username,
          email: '',
          role: loginData.role
        }
        localStorage.setItem('token', loginData.accessToken)
        localStorage.setItem('refreshToken', loginData.refreshToken)
        return true
      }
      return false
    } catch (error) {
      console.error('登录失败:', error)
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 注册
  const register = async (data: RegisterRequest): Promise<boolean> => {
    isLoading.value = true
    try {
      const res = await authApi.register(data)
      return res.data.code === 200
    } catch (error) {
      console.error('注册失败:', error)
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  // 更新Token
  const updateTokens = (newAccessToken: string, newRefreshToken?: string) => {
    token.value = newAccessToken
    localStorage.setItem('token', newAccessToken)
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
    }
  }

  return {
    token,
    refreshToken,
    user,
    isLoading,
    isLoggedIn,
    initUser,
    login,
    register,
    logout,
    updateTokens
  }
})
