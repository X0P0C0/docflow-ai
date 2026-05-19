import { getRouteRequiredCapability } from '../access-policy'
import { createRouter, createWebHistory } from 'vue-router'
import { setAuthFailureHandler } from '../api/http'
import { clearSession, isAuthenticated, restoreSession } from '../auth'
import { canAccessCapability } from '../authz'
import { CAPABILITY_CODES } from '../capability-constants'
import { buildAuthFailureRedirect, shouldSkipAuthFailureRedirect } from '../utils/authFailure'

const LoginView = () => import('../views/LoginView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const KnowledgeArticleListView = () => import('../views/KnowledgeArticleListView.vue')
const KnowledgeArticleEditorView = () => import('../views/KnowledgeArticleEditorView.vue')
const KnowledgeArticleDetailView = () => import('../views/KnowledgeArticleDetailView.vue')
const AiCenterView = () => import('../views/AiCenterView.vue')
const NotificationCenterView = () => import('../views/NotificationCenterView.vue')
const SystemManageView = () => import('../views/SystemManageView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const TicketListView = () => import('../views/TicketListView.vue')
const TicketCreateView = () => import('../views/TicketCreateView.vue')
const TicketDetailView = () => import('../views/TicketDetailView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        guestOnly: true,
      },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/knowledge/articles',
      name: 'knowledge-article-list',
      component: KnowledgeArticleListView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/knowledge/articles/create',
      name: 'knowledge-article-create',
      component: KnowledgeArticleEditorView,
      meta: {
        requiresAuth: true,
        requiredCapability: CAPABILITY_CODES.KNOWLEDGE_MANAGE,
      },
    },
    {
      path: '/knowledge/articles/:id',
      name: 'knowledge-article-detail',
      component: KnowledgeArticleDetailView,
      props: true,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/knowledge/articles/:id/edit',
      name: 'knowledge-article-edit',
      component: KnowledgeArticleEditorView,
      props: true,
      meta: {
        requiresAuth: true,
        requiredCapability: CAPABILITY_CODES.KNOWLEDGE_MANAGE,
      },
    },
    {
      path: '/ai-center',
      name: 'ai-center',
      component: AiCenterView,
      meta: {
        requiresAuth: true,
        requiredCapability: CAPABILITY_CODES.AI_CENTER_ACCESS,
      },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: NotificationCenterView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/settings',
      name: 'settings',
      component: SystemManageView,
      meta: {
        requiresAuth: true,
        requiredCapability: CAPABILITY_CODES.SYSTEM_MANAGE,
      },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/tickets',
      name: 'ticket-list',
      component: TicketListView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/tickets/create',
      name: 'ticket-create',
      component: TicketCreateView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/tickets/:id',
      name: 'ticket-detail',
      component: TicketDetailView,
      props: true,
      meta: {
        requiresAuth: true,
      },
    },
  ],
})

setAuthFailureHandler((error) => {
  // 把 401/403 的跳转决策放在 router 层，而不是 http 层，
  // 这样可以结合当前页面状态决定是否真的要跳走。
  const currentRoute = router.currentRoute.value
  if (shouldSkipAuthFailureRedirect(currentRoute, error)) {
    return
  }
  if (error.status === 401) {
    // 401 说明 token 基本已经不可用了，先清本地会话再跳登录页。
    clearSession()
  }
  router.replace(buildAuthFailureRedirect(currentRoute.fullPath, error))
})

router.beforeEach(async (to) => {
  // 路由守卫统一承担两件事：先恢复会话，再判断页面级能力是否足够。
  if (to.meta.requiresAuth) {
    // 先恢复一次会话，再决定这条路由到底能不能进。
    await restoreSession()
  }

  if (to.meta.requiresAuth && !isAuthenticated()) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  const requiredCapability = typeof to.meta.requiredCapability === 'string'
    ? to.meta.requiredCapability
    : getRouteRequiredCapability(to.path)

  if (requiredCapability && !canAccessCapability(requiredCapability)) {
    // 权限不足时尽量把用户送回“最接近当前意图”的可访问页面，而不是直接白屏。
    if (requiredCapability === CAPABILITY_CODES.KNOWLEDGE_MANAGE) {
      return {
        path: '/knowledge/articles',
        query: {
          reason: 'forbidden',
          from: to.fullPath,
        },
      }
    }
    return {
      path: '/dashboard',
      query: {
        reason: 'forbidden',
        from: to.fullPath,
      },
    }
  }

  if (to.meta.guestOnly && isAuthenticated()) {
    return '/dashboard'
  }

  return true
})

export default router
