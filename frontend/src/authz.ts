import { getRouteRequiredCapability } from './access-policy'
import { authState } from './auth'
import { CAPABILITY_CODES } from './capability-constants'
import {
  AI_CENTER_ROLE_CODES,
  KNOWLEDGE_MANAGER_PERMISSION_CODES,
  KNOWLEDGE_MANAGER_ROLE_CODES,
  SYSTEM_ADMIN_ROLE_CODES,
  SYSTEM_MANAGER_PERMISSION_CODES,
  TICKET_OPERATOR_PERMISSION_CODES,
  TICKET_OPERATOR_ROLE_CODES,
} from './auth-constants'

// authz.ts 的职责是把“当前登录用户拥有什么”翻译成前端可直接消费的布尔能力判断。
// 它不替代后端鉴权，只负责页面、按钮和交互层的可见性控制。
export function hasCapability(capability: string) {
  const currentCapabilities = authState.user?.capabilities || []
  return currentCapabilities.includes(capability)
}

export function hasPermission(permission: string) {
  const currentPermissions = authState.user?.permissions || []
  return currentPermissions.includes(permission)
}

export function hasAnyPermission(permissions: string[]) {
  return permissions.some((permission) => hasPermission(permission))
}

export function hasAnyRole(roles: string[]) {
  const currentRoles = authState.user?.roles || []
  return roles.some((role) => currentRoles.includes(role))
}

export function canManageKnowledgeArticles() {
  // 优先信任后端返回的 capabilities，同时保留 role / permission 兜底，
  // 这样前后端协议演进时页面不会因为单一字段缺失而全部失效。
  return hasCapability(CAPABILITY_CODES.KNOWLEDGE_MANAGE)
    || hasAnyPermission(KNOWLEDGE_MANAGER_PERMISSION_CODES)
    || hasAnyRole(KNOWLEDGE_MANAGER_ROLE_CODES)
}

export function canOperateTickets() {
  return hasCapability(CAPABILITY_CODES.TICKET_OPERATE)
    || hasAnyPermission(TICKET_OPERATOR_PERMISSION_CODES)
    || hasAnyRole(TICKET_OPERATOR_ROLE_CODES)
}

export function canAssignTickets() {
  return hasCapability(CAPABILITY_CODES.TICKET_ASSIGN) || canOperateTickets()
}

export function canTransitionTickets() {
  return hasCapability(CAPABILITY_CODES.TICKET_TRANSITION) || canOperateTickets()
}

export function canUseInternalTicketComments() {
  return hasCapability(CAPABILITY_CODES.TICKET_INTERNAL_COMMENT) || canOperateTickets()
}

export function canViewAllTickets() {
  return hasCapability(CAPABILITY_CODES.TICKET_VIEW_ALL) || canOperateTickets()
}

export function canAccessAiCenter() {
  return hasCapability(CAPABILITY_CODES.AI_CENTER_ACCESS) || hasAnyRole(AI_CENTER_ROLE_CODES)
}

export function canManageSystem() {
  return hasCapability(CAPABILITY_CODES.SYSTEM_MANAGE)
    || hasAnyPermission(SYSTEM_MANAGER_PERMISSION_CODES)
    || hasAnyRole(SYSTEM_ADMIN_ROLE_CODES)
}

export function canAccessCapability(capability?: string | null) {
  // 这里把 capability code 收口成统一分发入口，
  // route 守卫和页面按钮都不需要自己维护一份映射规则。
  if (!capability) {
    return true
  }
  if (capability === CAPABILITY_CODES.KNOWLEDGE_MANAGE) {
    return canManageKnowledgeArticles()
  }
  if (capability === CAPABILITY_CODES.AI_CENTER_ACCESS) {
    return canAccessAiCenter()
  }
  if (capability === CAPABILITY_CODES.SYSTEM_MANAGE) {
    return canManageSystem()
  }
  if (capability === CAPABILITY_CODES.TICKET_OPERATE) {
    return canOperateTickets()
  }
  if (capability === CAPABILITY_CODES.TICKET_VIEW_ALL) {
    return canViewAllTickets()
  }
  if (capability === CAPABILITY_CODES.TICKET_ASSIGN) {
    return canAssignTickets()
  }
  if (capability === CAPABILITY_CODES.TICKET_TRANSITION) {
    return canTransitionTickets()
  }
  if (capability === CAPABILITY_CODES.TICKET_INTERNAL_COMMENT) {
    return canUseInternalTicketComments()
  }
  return hasCapability(capability)
}

export function canAccessRoute(target: string) {
  // 路由层只关心“这个地址需要什么能力”，具体怎么算权限交给 canAccessCapability。
  return canAccessCapability(getRouteRequiredCapability(target))
}
