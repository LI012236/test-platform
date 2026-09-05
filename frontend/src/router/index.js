import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/projects' },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/projects', component: () => import('../views/Projects.vue') },
  { path: '/projects/:id', component: () => import('../views/ProjectDetail.vue') },
  { path: '/cases/:id', component: () => import('../views/CaseEditor.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/projects'
  }
  return true
})

export default router
