<template>
  <div class="profile-container">
    <button class="back-to-home-btn" @click="goToHome" title="返回首页">
      <i class="fas fa-home"></i>
      <span>返回首页</span>
    </button>
    <div class="profile-header">
      <div class="avatar">
        <i class="fas fa-user-circle"></i>
      </div>
      <div class="user-info">
        <h2>{{ userInfo?.username || '未登录' }}</h2>
        <p class="email">{{ userInfo?.email || '' }}</p>
        <div class="role-badge" :class="userInfo?.role?.toLowerCase()">
          {{ userInfo?.role || 'USER' }}
        </div>
      </div>
    </div>

    <div class="profile-content">
      <div class="section">
        <h3>基本信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <label>用户ID</label>
            <span>{{ userInfo?.id || '-' }}</span>
          </div>
          <div class="info-item">
            <label>用户名</label>
            <span>{{ userInfo?.username || '-' }}</span>
          </div>
          <div class="info-item">
            <label>邮箱</label>
            <span>{{ userInfo?.email || '-' }}</span>
          </div>
          <div class="info-item">
            <label>角色</label>
            <span>{{ userInfo?.role || '-' }}</span>
          </div>
          <div class="info-item">
            <label>注册时间</label>
            <span>{{ formatDate(userInfo?.createdTime) || '-' }}</span>
          </div>
          <div class="info-item">
            <label>更新时间</label>
            <span>{{ formatDate(userInfo?.updatedTime) || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3>组织信息</h3>
        <div class="info-grid">
          <div class="info-item full-width">
            <label>主组织</label>
            <span>{{ userInfo?.primaryOrg || '未设置' }}</span>
          </div>
          <div class="info-item full-width">
            <label>所属组织</label>
            <div class="org-tags">
              <span v-if="!userInfo?.orgTags || userInfo.orgTags.size === 0" class="empty">暂无组织</span>
              <span v-for="tag in Array.from(userInfo?.orgTags || [])" :key="tag" class="org-tag">
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentUser } from '@/api/auth';
import type { UserInfo } from '@/api/auth';

const router = useRouter();
const userInfo = ref<UserInfo | null>(null);

const loadUserInfo = async () => {
  try {
    const res = await getCurrentUser();
    if (res.data.code === 200 && res.data.data) {
      userInfo.value = res.data.data;
    }
  } catch (error: any) {
    console.error('加载用户信息失败:', error);
    router.push('/login');
  }
};

const formatDate = (dateString?: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const goToHome = () => {
  router.push('/');
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
  position: relative;
}

.back-to-home-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.back-to-home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}

.back-to-home-btn i {
  font-size: 16px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  background: linear-gradient(135deg, #2a2a2a 0%, #1e1e1e 100%);
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.avatar {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #fff;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.user-info h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}

.email {
  margin: 0 0 12px 0;
  color: #999;
  font-size: 14px;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.role-badge.user {
  background: rgba(52, 152, 219, 0.2);
  color: #3498db;
}

.role-badge.admin {
  background: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
}

.profile-content {
  background: linear-gradient(135deg, #2a2a2a 0%, #1e1e1e 100%);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.section {
  margin-bottom: 24px;
}

.section:last-child {
  margin-bottom: 0;
}

.section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  padding-bottom: 12px;
  border-bottom: 1px solid #333;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item label {
  font-size: 12px;
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
}

.info-item > span {
  font-size: 14px;
  color: #ccc;
}

.org-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.org-tag {
  padding: 4px 12px;
  background: rgba(52, 152, 219, 0.2);
  color: #3498db;
  border-radius: 4px;
  font-size: 13px;
}

.empty {
  color: #666;
  font-style: italic;
}
</style>
