<template>
  <div class="profile-content-inner">
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
</template>

<script setup lang="ts">
import type { UserInfo } from '@/api/auth';

defineProps<{
  userInfo: UserInfo | null;
}>();

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
</script>

<style scoped>
.profile-content-inner {
  padding: 20px;
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

.section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}

.section h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: 13px;
  color: #95a5a6;
  font-weight: 500;
}

.info-item span {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 500;
}

.org-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.org-tag {
  padding: 4px 12px;
  background: #f0f2f5;
  border-radius: 16px;
  font-size: 13px;
  color: #666;
}

.org-tag.empty {
  color: #999;
  font-style: italic;
  background: none;
  padding: 0;
}
</style>
