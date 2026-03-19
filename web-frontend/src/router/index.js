import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/project'
  },
  {
    path: '/project',
    name: 'Project',
    component: () => import('../views/ProjectView.vue')
  },
  {
    path: '/analysis',
    name: 'Analysis',
    component: () => import('../views/AnalysisView.vue')
  },
  {
    path: '/routes',
    name: 'Routes',
    component: () => import('../views/RoutesView.vue')
  },
  {
    path: '/callgraph',
    name: 'CallGraph',
    component: () => import('../views/CallGraphView.vue')
  },
  {
    path: '/callgraph/:method',
    name: 'CallGraphDetail',
    component: () => import('../views/CallGraphView.vue')
  },
  {
    path: '/taint',
    name: 'Taint',
    component: () => import('../views/TaintView.vue')
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
