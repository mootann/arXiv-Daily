<template>
  <div class="user-layout">
    <aside class="sidebar">
      <div class="logo-area" @click="goToHome">
        <div class="logo-icon">
          <i class="fas fa-robot"></i>
        </div>
        <span>派聪明</span>
      </div>
      
      <nav class="nav-menu">
        <div 
          v-for="item in menuItems" 
          :key="item.id"
          class="nav-item"
          :class="{ active: currentTab === item.id }"
          @click="currentTab = item.id"
        >
          <i :class="item.icon"></i>
          <span>{{ item.label }}</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="collapse-btn">
          <i class="fas fa-outdent"></i>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <header class="top-bar">
        <div class="page-title">
          <i :class="currentMenuItem?.icon" v-if="currentMenuItem"></i>
          <span>{{ currentMenuItem?.label }}</span>
        </div>
        <div class="user-actions">
          <div class="action-btn" title="搜索"><i class="fas fa-search"></i></div>
          <div class="action-btn" title="全屏"><i class="fas fa-expand"></i></div>
          <div class="action-btn" title="切换语言"><i class="fas fa-language"></i></div>
          <div class="user-profile-badge">
            <i class="fas fa-user-circle"></i>
            <span>{{ userInfo?.username || 'Guest' }}</span>
          </div>
        </div>
      </header>

      <div class="content-area">
        <component :is="currentComponent" :userInfo="userInfo" />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentUser } from '@/api/auth';
import type { UserInfo } from '@/api/auth';

import ProfileInfo from '@/components/user/ProfileInfo.vue';
import ChatAssistant from '@/components/user/ChatAssistant.vue';
import KnowledgeBase from '@/components/user/KnowledgeBase.vue';

// 简单的占位组件
const ComingSoon = {
  template: `
    <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 400px; color: #bdc3c7;">
      <i class="fas fa-tools" style="font-size: 48px; margin-bottom: 20px;"></i>
      <h3 style="font-weight: 500;">功能正在开发中</h3>
    </div>
  `
};

const router = useRouter();
const userInfo = ref<UserInfo | null>(null);
const currentTab = ref('profile');

const menuItems = [
  { id: 'chat', label: '聊天助手', icon: 'fas fa-comments' },
  { id: 'history', label: '聊天记录', icon: 'fas fa-history' },
  { id: 'knowledge', label: '知识库', icon: 'fas fa-folder' },
  { id: 'orgs', label: '组织标签', icon: 'fas fa-tags' },
  { id: 'users', label: '用户管理', icon: 'fas fa-users-cog' },
  { id: 'profile', label: '个人中心', icon: 'fas fa-user-astronaut' }
];

const currentMenuItem = computed(() => menuItems.find(item => item.id === currentTab.value));

const currentComponent = computed(() => {
  switch (currentTab.value) {
    case 'chat': return ChatAssistant;
    case 'knowledge': return KnowledgeBase;
    case 'profile': return ProfileInfo;
    default: return ComingSoon;
  }
});

const loadUserInfo = async () => {
  try {
    const res = await getCurrentUser();
    if (res.data.code === 200 && res.data.data) {
      userInfo.value = res.data.data;
    }
  } catch (error) {
    console.error('加载用户信息失败:', error);
    router.push('/login');
  }
};

const goToHome = () => {
  router.push('/');
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.user-layout {
  display: flex;
  height: 100vh;
  background: #f0f2f5;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.logo-area span {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-menu {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.nav-item:hover {
  background: #f8f9fa;
  color: #764ba2;
}

.nav-item.active {
  background: #f0f7ff; /* 淡紫色背景，类似截图 */
  color: #764ba2;
  font-weight: 600;
}

.nav-item.active i {
  color: #764ba2;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  cursor: pointer;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-bar {
  height: 64px;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.page-title i {
  color: #764ba2;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  cursor: pointer;
  border-radius: 50%;
  transition: background 0.3s;
}

.action-btn:hover {
  background: #f5f5f5;
  color: #2c3e50;
}

.user-profile-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f8f9fa;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #e0e0e0;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #bdc3c7;
}
</style>
