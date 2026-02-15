<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h2>登录</h2>
        <p>登录您的账号</p>
      </div>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">登录</el-button>
      </el-form>
      <div class="footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const success = await userStore.login(form)
    if (success) {
      ElMessage.success('登录成功')
      await router.replace('/')
    }
  } catch (error) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(74, 144, 226, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.login-header h2 { font-size: 24px; color: #1a1a1a; margin: 0 0 8px; }
.login-header p { color: #666; font-size: 14px; margin: 0; }
.footer { text-align: center; margin-top: 16px; color: #666; font-size: 14px; }
.footer a { color: #4A90E2; text-decoration: none; }
.footer a:hover { text-decoration: underline; }
</style>
