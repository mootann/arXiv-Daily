<template>
  <div class="dialog-overlay" v-if="show" @click="handleOverlayClick">
    <div class="dialog-container" @click.stop>
      <div class="dialog-header">
        <h2>登录</h2>
        <button class="close-btn" @click="$emit('close')">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="dialog-body">
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label>用户名</label>
            <input 
              type="text" 
              v-model="form.username" 
              class="form-input" 
              required
              placeholder="请输入用户名"
            >
          </div>
          <div class="form-group">
            <label>密码</label>
            <input 
              type="password" 
              v-model="form.password" 
              class="form-input" 
              required
              placeholder="请输入密码"
            >
          </div>
          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
        <div class="dialog-footer">
          <span>还没有账号？</span>
          <a @click="switchToRegister" class="link">立即注册</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '@/api/auth';

interface Props {
  show: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
  switchToRegister: [];
  loginSuccess: [token: string];
}>();

const router = useRouter();
const loading = ref(false);
const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  loading.value = true;
  try {
    const data = await login(form);
    // login 函数已经解析了响应，直接返回 LoginResponse 数据
    localStorage.setItem('token', data.token);
    localStorage.setItem('userId', String(data.userId));
    localStorage.setItem('username', data.username);
    localStorage.setItem('role', data.role);
    localStorage.setItem('primaryOrg', data.primaryOrg);
    localStorage.setItem('orgTags', JSON.stringify(Array.from(data.orgTags || [])));
    
    emit('loginSuccess', data.token);
    emit('close');
    
    router.push('/');
  } catch (error: any) {
    alert('登录失败: ' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

const handleOverlayClick = () => {
  emit('close');
};

const switchToRegister = () => {
  emit('close');
  emit('switchToRegister');
};
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.dialog-container {
  background: linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 100%);
  border-radius: 16px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  overflow: hidden;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid #333;
}

.dialog-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}

.close-btn {
  background: none;
  border: none;
  color: #999;
  font-size: 20px;
  cursor: pointer;
  transition: color 0.3s;
  padding: 4px;
}

.close-btn:hover {
  color: #fff;
}

.dialog-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #ccc;
  font-size: 14px;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #444;
  border-radius: 8px;
  background: #1a1a1a;
  color: #fff;
  font-size: 14px;
  transition: all 0.3s;
  outline: none;
}

.form-input:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.2);
}

.form-input::placeholder {
  color: #666;
}

.btn-primary {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dialog-footer {
  margin-top: 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.dialog-footer .link {
  color: #3498db;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.3s;
}

.dialog-footer .link:hover {
  color: #2980b9;
  text-decoration: underline;
}
</style>
