<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="openGitHub">
          <div class="logo-icon">
            <svg height="20" aria-hidden="true" viewBox="0 0 16 16" version="1.1" width="20" data-view-component="true" fill="currentColor">
              <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.46-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
            </svg>
          </div>
          <span class="logo-text">PaperRepro</span>
          <span class="github-stats">
            <span class="stat-item"><span class="star-icon">★</span> {{ githubStars }}</span>
            <span class="stat-item"><span class="fork-icon">⑂</span> {{ githubForks }}</span>
          </span>
        </div>
        
        <nav class="nav-menu">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
            <el-icon><HomeFilled /></el-icon>
            首页
          </router-link>
          <router-link to="/follow" class="nav-item" :class="{ active: $route.path === '/follow' }">
            <el-icon><Star /></el-icon>
            关注
          </router-link>
          <router-link to="/collect" class="nav-item" :class="{ active: $route.path === '/collect' }">
            <el-icon><Collection /></el-icon>
            收藏
          </router-link>
        </nav>
        
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="hover" @command="handleCommand">
              <span class="user-info" @click="goToCollect">
                <el-avatar :size="32" :src="`https://api.dicebear.com/7.x/initials/svg?seed=${userStore.user?.username}&backgroundColor=2563eb`" />
                <span class="username">{{ userStore.user?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="collect">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="follow">我的关注</el-dropdown-item>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button text @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { githubApi } from '@/api'

const userStore = useUserStore()
const router = useRouter()

const githubStars = ref(0)
const githubForks = ref(0)

// 初始化用户状态
userStore.initUser()

// 获取GitHub仓库信息
const fetchGitHubStats = async () => {
  try {
    const res = await githubApi.getRepositoryInfo()
    if (res.data.code === 200 && res.data.data) {
      githubStars.value = res.data.data.starsCount || 0
      githubForks.value = res.data.data.forksCount || 0
    }
  } catch (error) {
    console.error('获取GitHub仓库信息失败', error)
  }
}

// 打开GitHub仓库
const openGitHub = () => {
  window.open('https://github.com/mootann/PaperRepro', '_blank')
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'collect') {
    router.push('/collect')
  } else if (command === 'follow') {
    router.push('/follow')
  }
}

const goToCollect = () => {
  router.push('/collect')
}

onMounted(() => {
  fetchGitHubStats()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
  color: #333;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.3s;
}

.logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: #24292f;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.5px;
}

.github-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.github-stats .stat-item {
  display: flex;
  align-items: center;
  gap: 2px;
}

.github-stats .star-icon {
  color: #eab308;
}

.github-stats .fork-icon {
  color: #666;
}

.nav-menu {
  display: flex;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  color: #666;
  text-decoration: none;
  font-size: 15px;
  transition: all 0.3s;
  position: relative;
}

.nav-item:hover {
  color: #1a1a1a;
  background: #f5f5f5;
}

.nav-item.active {
  color: #1a1a1a;
  font-weight: 500;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 2px;
  background: #1a1a1a;
  border-radius: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right .el-button {
  color: #1a1a1a;
}

.header-right .el-button--primary {
  background: #1a1a1a;
  border-color: #1a1a1a;
}

.header-right .el-button--primary:hover {
  background: #333;
  border-color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1a1a1a;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f0f0f0;
}

.username {
  font-size: 14px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}

/* 路由过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 修复登录注册页输入框自动填充背景色未覆盖的问题 */
.login-page .el-input__wrapper,
.register-page .el-input__wrapper {
  padding: 0;
  overflow: hidden;
  height: 40px;
}

.login-page .el-input__inner,
.register-page .el-input__inner {
  padding: 0 15px;
  height: 100%;
  line-height: 40px;
}
</style>
