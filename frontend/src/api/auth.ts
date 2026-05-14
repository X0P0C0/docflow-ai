import { get, post } from './http'
import { PERMISSION_CODES, ROLE_CODES } from '../auth-constants'
import { CAPABILITY_CODES } from '../capability-constants'

export interface CurrentUser {
  id: number
  username: string
  nickname: string
  realName: string
  email: string
  phone: string
  avatar: string | null
  roles: string[]
  permissions: string[]
  capabilities: string[]
}

export interface LoginResponse {
  token: string
  expireSeconds: number
  user: CurrentUser
}

const demoUsers: Record<string, CurrentUser> = {
  admin: {
    id: 1,
    username: 'admin',
    nickname: '系统管理员',
    realName: '系统管理员',
    email: 'admin@docflow.ai',
    phone: '13800000000',
    avatar: null,
    roles: [ROLE_CODES.ADMIN],
    permissions: [
      PERMISSION_CODES.KNOWLEDGE_VIEW,
      PERMISSION_CODES.TICKET_VIEW,
      PERMISSION_CODES.SYSTEM_VIEW,
      PERMISSION_CODES.KNOWLEDGE_ARTICLE_CREATE,
      PERMISSION_CODES.TICKET_ASSIGN,
      PERMISSION_CODES.SYSTEM_USER_MANAGE,
      PERMISSION_CODES.SYSTEM_ROLE_MANAGE,
    ],
    capabilities: [
      CAPABILITY_CODES.TICKET_OPERATE,
      CAPABILITY_CODES.TICKET_ASSIGN,
      CAPABILITY_CODES.TICKET_TRANSITION,
      CAPABILITY_CODES.TICKET_INTERNAL_COMMENT,
      CAPABILITY_CODES.TICKET_VIEW_ALL,
      CAPABILITY_CODES.KNOWLEDGE_MANAGE,
      CAPABILITY_CODES.AI_CENTER_ACCESS,
      CAPABILITY_CODES.SYSTEM_MANAGE,
    ],
  },
  support01: {
    id: 2,
    username: 'support01',
    nickname: '支持小李',
    realName: '李晓安',
    email: 'support01@docflow.ai',
    phone: '13800000001',
    avatar: null,
    roles: [ROLE_CODES.SUPPORT],
    permissions: [
      PERMISSION_CODES.KNOWLEDGE_VIEW,
      PERMISSION_CODES.TICKET_VIEW,
      PERMISSION_CODES.KNOWLEDGE_ARTICLE_CREATE,
      PERMISSION_CODES.TICKET_ASSIGN,
    ],
    capabilities: [
      CAPABILITY_CODES.TICKET_OPERATE,
      CAPABILITY_CODES.TICKET_ASSIGN,
      CAPABILITY_CODES.TICKET_TRANSITION,
      CAPABILITY_CODES.TICKET_INTERNAL_COMMENT,
      CAPABILITY_CODES.TICKET_VIEW_ALL,
      CAPABILITY_CODES.KNOWLEDGE_MANAGE,
      CAPABILITY_CODES.AI_CENTER_ACCESS,
    ],
  },
  user01: {
    id: 3,
    username: 'user01',
    nickname: '业务小王',
    realName: '王晨',
    email: 'user01@docflow.ai',
    phone: '13800000002',
    avatar: null,
    roles: [ROLE_CODES.USER],
    permissions: [PERMISSION_CODES.KNOWLEDGE_VIEW, PERMISSION_CODES.TICKET_VIEW],
    capabilities: [],
  },
}

export function login(payload: { username: string; password: string }) {
  return post<LoginResponse>('/api/auth/login', payload, { skipAuth: true })
}

export function fetchCurrentUser() {
  return get<CurrentUser>('/api/auth/me')
}

export function createDemoSession(username: string, password: string): LoginResponse | null {
  if (password !== 'password') {
    return null
  }

  const user = demoUsers[username]
  if (!user) {
    return null
  }

  return {
    token: `demo-token:${username}`,
    expireSeconds: 60 * 60 * 24,
    user,
  }
}

export function isDemoToken(token: string) {
  return token.startsWith('demo-token:')
}
