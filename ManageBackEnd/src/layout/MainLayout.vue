<template>
  <el-container class="main-layout">
    <el-aside width="200px">
      <div class="logo">
        <h2>arXiv管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="el-menu-vertical"
      >
        <el-menu-item index="/papers">
          <el-icon><Document /></el-icon>
          <span>论文列表</span>
        </el-menu-item>
        <el-menu-item index="/sync">
          <el-icon><Refresh /></el-icon>
          <span>数据同步</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-title">{{ pageTitle }}</div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { Document, Refresh } from '@element-plus/icons-vue';

const route = useRoute();

const activeMenu = computed(() => route.path);
const pageTitle = computed(() => route.meta.title as string || 'arXiv管理后台');
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #1f2d3d;
}

.logo h2 {
  font-size: 18px;
  color: #fff;
  font-weight: 500;
}

.el-menu-vertical {
  border-right: none;
  background-color: #304156;
}

.el-menu-vertical :deep(.el-menu-item) {
  color: #bfcbd9;
}

.el-menu-vertical :deep(.el-menu-item:hover) {
  background-color: #263445;
}

.el-menu-vertical :deep(.el-menu-item.is-active) {
  background-color: #1f2d3d;
  color: #409eff;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
