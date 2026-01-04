import { createRouter, createWebHistory } from 'vue-router';
import Home from '@/views/Home.vue';
import PaperDetail from '@/views/PaperDetail.vue';
import UserProfile from '@/views/UserProfile.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
    },
    {
      path: '/paper/:id',
      name: 'paper-detail',
      component: PaperDetail,
    },
    {
      path: '/profile',
      name: 'profile',
      component: UserProfile,
      meta: {
        requiresAuth: true
      }
    },
  ],
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token');
  
  if (to.meta.requiresAuth && !token) {
    next('/');
  } else {
    next();
  }
});

export default router;
