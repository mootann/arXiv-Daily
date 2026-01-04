import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/papers'
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    children: [
      {
        path: 'papers',
        name: 'PaperList',
        component: () => import('@/views/PaperList.vue'),
        meta: { title: '论文列表' }
      },
      {
        path: 'papers/:id/edit',
        name: 'PaperEdit',
        component: () => import('@/views/PaperEdit.vue'),
        meta: { title: '编辑论文' }
      },
      {
        path: 'sync',
        name: 'DataSync',
        component: () => import('@/views/DataSync.vue'),
        meta: { title: '数据同步' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
