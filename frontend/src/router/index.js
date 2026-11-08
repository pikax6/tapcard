import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginPad.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/Console.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('@/views/home/HomeBoard.vue'), meta: { title: '概览' } },
      { path: 'acct', name: 'acct', component: () => import('@/views/acct/AcctBook.vue'), meta: { title: '卡户册' } },
      { path: 'acct/:id', name: 'acctCard', component: () => import('@/views/acct/AcctCard.vue'), meta: { title: '卡户详情' } },
      { path: 'dev', name: 'dev', component: () => import('@/views/dev/DevDesk.vue'), meta: { title: '机具台' } },
      { path: 'bill', name: 'bill', component: () => import('@/views/bill/BillBook.vue'), meta: { title: '账单簿' } },
      { path: 'settle', name: 'settle', component: () => import('@/views/settle/SettleBoard.vue'), meta: { title: '对账盘' } },
      { path: 'stat', name: 'stat', component: () => import('@/views/stat/DayChart.vue'), meta: { title: '统计' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

/**
 * 每个路由名允许哪些角色。与 Console.vue 里的 MENUS 保持同一份口径。
 * 注意 acctCard 必须放学生进来：他看自己的详情要靠它，
 * 而手改地址去看别人的卡，由后端 AcctGuard 返回 4001 —— 这正是要演示的越权用例。
 */
const ROLE_OF = {
  home:     [1, 2, 3, 9],
  acct:     [1, 3, 9],
  acctCard: [1, 3, 9],
  dev:      [2, 3, 9],
  bill:     [1, 3, 9],
  settle:   [3, 9],
  stat:     [3, 9]
}

// 先建实例，再挂守卫 —— 顺序反了会抛 "Cannot access 'router' before initialization"
const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('tapcard_token')
  if (!to.meta.noAuth && !token) {
    return { path: '/login', query: { back: to.fullPath } }
  }

  const allow = ROLE_OF[to.name]
  if (allow) {
    const role = Number(localStorage.getItem('tapcard_role') || 0)
    if (!allow.includes(role)) return { path: '/home' }
  }
  return true
})

export default router
