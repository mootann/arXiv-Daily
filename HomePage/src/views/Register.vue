<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2 class="auth-title">Register</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>Username</label>
          <input type="text" v-model="form.username" class="form-input" required>
        </div>
        <div class="form-group">
          <label>Email</label>
          <input type="email" v-model="form.email" class="form-input" required>
        </div>
        <div class="form-group">
          <label>Password</label>
          <input type="password" v-model="form.password" class="form-input" required>
        </div>
        <div class="form-group">
          <label>Confirm Password</label>
          <input type="password" v-model="form.confirmPassword" class="form-input" required>
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? 'Registering...' : 'Register' }}
        </button>
      </form>
      <div class="auth-link">
        Already have an account? <router-link to="/login">Login</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { register } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const handleRegister = async () => {
  if (form.password !== form.confirmPassword) {
    alert('密码不匹配');
    return;
  }
  
  loading.value = true;
  try {
    const user = await register({
        username: form.username,
        email: form.email,
        password: form.password
    });
    console.log('注册成功:', user);
    alert('注册成功！');
    router.push('/login');
  } catch (error: any) {
    console.error('注册失败:', error);
    alert('注册失败: ' + (error?.message || '未知错误'));
  } finally {
    loading.value = false;
  }
};
</script>
