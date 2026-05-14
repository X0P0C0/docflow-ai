import { getRouteRequiredCapability } from '../access-policy'
import { createRouter, createWebHistory } from 'vue-router'
import { setAuthFailureHandler } from '../api/http'
import AiCenterView from '../views/AiCenterView.vue'
import { clearSession, isAuthenticated, restoreSession } from '../auth'
import { canAccessCapability } from '../authz'
import { CAPABILITY_CODES } from '../capability-constants'
import { buildAuthFailureRedirect, shouldSkipAuthFailureRedirect } from '../utils/authFailure'
import DashboardView from '../views/DashboardView.vue'
import KnowledgeArticleDetailView from '../views/KnowledgeArticleDetailView.vue'
import KnowledgeArticleEditorView from '../views/KnowledgeArticleEditorView.vue'
import KnowledgeArticleListView from '../views/KnowledgeArticleListView.vue'
import LoginView from '../views/LoginView.vue'
import NotificationCenterView from '../views/NotificationCenterView.vue'
import ProfileView from '../views/ProfileView.vue'
import SystemManageView from '../views/SystemManageView.vue'
import TicketCreateView from '../views/TicketCreateView.vue'
import TicketDetailView from '../views/TicketDetailView.vue'
import TicketListView from '../views/TicketListView.vue'

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
  const currentRoute = router.currentRoute.value
  if (shouldSkipAuthFailureRedirect(currentRoute, error)) {
    return
  }
  if (error.status === 401) {
    clearSession()
  }
  router.replace(buildAuthFailureRedirect(currentRoute.fullPath, error))
})

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth) {
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
