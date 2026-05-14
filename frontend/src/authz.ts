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
  return canAccessCapability(getRouteRequiredCapability(target))
}
