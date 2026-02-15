<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <h2>注册</h2>
        <p>创建您的账号</p>
      </div>
      <el-form :model="form" @submit.prevent="handleRegister">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱" type="email" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">注册</el-button>
      </el-form>
      <div class="footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
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
const form = reactive({ username: '', email: '', password: '' })

const handleRegister = async () => {
  if (!form.username || !form.email || !form.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  const success = await userStore.register(form)
  loading.value = false
  if (success) {
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.register-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.register-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(74, 144, 226, 0.15);
}
.register-header {
  text-align: center;
  margin-bottom: 24px;
}
.register-header h2 { font-size: 24px; color: #1a1a1a; margin: 0 0 8px; }
.register-header p { color: #666; font-size: 14px; margin: 0; }
.footer { text-align: center; margin-top: 16px; color: #666; font-size: 14px; }
.footer a { color: #4A90E2; text-decoration: none; }
.footer a:hover { text-decoration: underline; }
</style>
