<template>
  <header class="header">
    <div class="header-left">
      <div id="header-categories" class="header-categories"></div>
    </div>
    
    <div class="header-center" @click="goToHome">
      <h1>arXiv-Daily</h1>
      <a :href="repositoryUrl" target="_blank" class="github-btn" v-if="repositoryUrl">
          <i class="fab fa-github"></i>
          <span>GitHub</span>
          <span class="stat"><i class="fas fa-star"></i> {{ starsCount ?? '-' }}</span>
          <span class="stat"><i class="fas fa-code-branch"></i> {{ forksCount ?? '-' }}</span>
      </a>
      <a href="#" class="github-btn" v-else>
          <i class="fab fa-github"></i>
          <span>GitHub</span>
          <span class="stat"><i class="fas fa-star"></i> 2239</span>
          <span class="stat"><i class="fas fa-code-branch"></i> 745</span>
      </a>
    </div>

    <div class="header-right">
      <div id="header-filters" class="header-filters"></div>
      <div class="header-actions">
        <button class="icon-btn" title="Settings">
            <i class="fas fa-cog"></i>
        </button>
        <div v-if="isLoggedIn" class="user-menu">
            <button class="user-btn" @click="showUserDropdown = !showUserDropdown">
                <i class="fas fa-user-circle"></i>
                <span>{{ username }}</span>
                <i class="fas fa-chevron-down dropdown-arrow" :class="{ rotated: showUserDropdown }"></i>
            </button>
            <div class="dropdown-menu" v-if="showUserDropdown">
                <div class="dropdown-item" @click="goToProfile">
                    <i class="fas fa-user"></i>
                    <span>个人主页</span>
                </div>
                <div class="dropdown-item" @click="handleLogout">
                    <i class="fas fa-sign-out-alt"></i>
                    <span>退出登录</span>
                </div>
            </div>
        </div>
        <button v-else class="login-btn" @click="showLoginDialog = true">
            <i class="fas fa-user"></i>
        </button>
      </div>
    </div>

    <LoginDialog 
      :show="showLoginDialog" 
      @close="showLoginDialog = false"
      @switchToRegister="handleSwitchToRegister"
      @loginSuccess="handleLoginSuccess"
    />

    <RegisterDialog 
      :show="showRegisterDialog" 
      @close="showRegisterDialog = false"
      @switchToLogin="handleSwitchToLogin"
      @loginSuccess="handleLoginSuccess"
    />
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { getRepositoryInfo } from '@/api/github';
import { logout } from '@/api/auth';
import LoginDialog from './LoginDialog.vue';
import RegisterDialog from './RegisterDialog.vue';
import type { GitHubRepositoryInfo } from '@/types';

const starsCount = ref<number | null>(null);
const forksCount = ref<number | null>(null);
const repositoryUrl = ref<string>('');

const showLoginDialog = ref(false);
const showRegisterDialog = ref(false);
const showUserDropdown = ref(false);
const isLoggedIn = ref(false);
const username = ref('');

const router = useRouter();

const loadRepositoryInfo = async () => {
  try {
    const response = await getRepositoryInfo();
    if (response.code === 200 && response.data) {
      const info = response.data as GitHubRepositoryInfo;
      starsCount.value = info.stargazers_count;
      forksCount.value = info.forks_count;
      repositoryUrl.value = info.html_url;
    }
  } catch (error) {
    console.error('加载仓库信息失败:', error);
  }
};

const checkLoginStatus = () => {
  const token = localStorage.getItem('token');
  const user = localStorage.getItem('username');
  isLoggedIn.value = !!token;
  username.value = user || '';
};

const handleLoginSuccess = () => {
  isLoggedIn.value = true;
  username.value = localStorage.getItem('username') || '';
};

const handleLogout = async () => {
  try {
    const token = localStorage.getItem('token');
    if (token) {
      await logout(token);
    }
  } catch (error) {
    console.error('退出登录失败:', error);
  } finally {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('primaryOrg');
    localStorage.removeItem('orgTags');
    isLoggedIn.value = false;
    username.value = '';
    showUserDropdown.value = false;
    router.push('/');
  }
};

const goToProfile = () => {
  showUserDropdown.value = false;
  router.push('/profile');
};

const goToHome = () => {
  router.push('/');
};

const handleSwitchToRegister = () => {
  showLoginDialog.value = false;
  showRegisterDialog.value = true;
};

const handleSwitchToLogin = () => {
  showRegisterDialog.value = false;
  showLoginDialog.value = true;
};

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  if (!target.closest('.user-menu')) {
    showUserDropdown.value = false;
  }
};

onMounted(() => {
  loadRepositoryInfo();
  checkLoginStatus();
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 100%);
  border-bottom: 1px solid #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  max-width: 30%;
}

.github-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #24292e 0%, #1a1a1a 100%);
  color: #fff;
  text-decoration: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.github-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(36, 41, 46, 0.4);
}

.github-btn i {
  font-size: 18px;
}

.github-btn .stat {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.logo {
  font-size: 28px;
  color: #3498db;
}

.logo i {
  animation: spin 10s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.header-center h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #fff 0%, #ccc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-center h1 .highlight {
  color: #3498db;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
  max-width: 30%;
}

.header-categories {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.header-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.login-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}

.user-menu {
  position: relative;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.user-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.user-btn i {
  font-size: 18px;
}

.dropdown-arrow {
  font-size: 12px;
  transition: transform 0.3s;
}

.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 160px;
  background: linear-gradient(135deg, #2a2a2a 0%, #1e1e1e 100%);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  border: 1px solid #333;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: #ccc;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.dropdown-item:hover {
  background: rgba(52, 152, 219, 0.2);
  color: #fff;
}

.dropdown-item i {
  font-size: 16px;
  width: 16px;
  text-align: center;
}
</style>
