<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2 class="auth-title">Login</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>Username</label>
          <input type="text" v-model="form.username" class="form-input" required>
        </div>
        <div class="form-group">
          <label>Password</label>
          <input type="password" v-model="form.password" class="form-input" required>
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? 'Logging in...' : 'Login' }}
        </button>
      </form>
      <div class="auth-link">
        Don't have an account? <router-link to="/register">Register</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  loading.value = true;
  try {
    const loginResponse = await login(form);
    console.log('登录成功:', loginResponse);
    // 保存 token
    if (loginResponse.token) {
      localStorage.setItem('token', loginResponse.token);
    }
    alert('登录成功！');
    router.push('/');
  } catch (error: any) {
    console.error('登录失败:', error);
    alert('登录失败: ' + (error?.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};
</script>
