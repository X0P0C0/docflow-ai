import type {
  ActivityItem,
  KnowledgeArticleItem,
  MetricItem,
  NavItem,
  TicketItem,
} from '../types/dashboard'

export const workspaceNav: NavItem[] = [
  { name: '控制台', badge: '08', to: '/dashboard' },
  { name: '知识库', badge: '24', to: '/knowledge/articles' },
  { name: '工单中心', badge: '16', to: '/tickets' },
  { name: 'AI 助手', badge: 'AI', to: '/ai-center' },
]

export const manageNav: NavItem[] = [
  { name: '通知中心', badge: '3', to: '/notifications' },
  { name: '系统管理', badge: 'Admin', to: '/settings' },
  { name: '个人中心', badge: 'Me', to: '/profile' },
]

export const metrics: MetricItem[] = [
  {
    tag: '总工单',
    value: '128',
    title: '本周新增工单数量',
    desc: '系统已经具备真实业务数据感',
    chipClass: 'chip-blue',
  },
  {
    tag: '已解决',
    value: '94',
    title: '平均处理时长 2.8 小时',
    desc: '后面这里可以继续接图表统计',
    chipClass: 'chip-green',
  },
  {
    tag: '待分配',
    value: '11',
    title: '建议优先处理高优问题',
    desc: '很适合映射工单列表和筛选逻辑',
    chipClass: 'chip-orange',
  },
  {
    tag: '知识文章',
    value: '342',
    title: '本周新增 18 篇知识沉淀',
    desc: '内容系统会是这个项目的核心亮点之一',
    chipClass: 'chip-blue',
  },
]

export const tickets: TicketItem[] = [
  {
    id: 1,
    ticketNo: 'INC-2031',
    title: '支付回调接口偶发超时',
    meta: '#INC-2031 · 订单中心 · 更新于 10 分钟前',
    content: '支付完成后，少量订单没有及时收到回调，需要结合签名校验、重试机制和幂等逻辑继续排查。',
    priorityLevel: 'P1',
    priority: '高优先级',
    status: '处理中',
    priorityClass: 'chip-red',
    assignee: '李晓安',
    submitter: '王晨',
    updatedAt: '10 分钟前',
    tags: ['支付', '回调', '高并发'],
    comments: [
      {
        author: '李晓安',
        content: '已初步定位到支付回调重试逻辑，需要结合日志继续确认。',
        typeLabel: 'Handling Note',
        internal: false,
        createdAt: '今天 10:24',
      },
      {
        author: '李晓安',
        content: '建议先检查 Redis 幂等键有效期配置是否过短。',
        typeLabel: 'Internal Memo',
        internal: true,
        createdAt: '今天 10:37',
      },
    ],
    timeline: [
      { title: 'Ticket Created', desc: '用户反馈支付完成后订单状态更新延迟。', operator: '王晨', createdAt: '今天 09:58' },
      { title: 'Ticket Assigned', desc: '已指派给技术支持处理。', operator: '李晓安', createdAt: '今天 10:08' },
    ],
  },
  {
    id: 2,
    ticketNo: 'INC-2027',
    title: '员工无法访问内部文档附件',
    meta: '#INC-2027 · 知识平台 · 负责人待分配',
    content: '点击知识文章中的附件时提示 403，需要排查权限控制、文件访问地址以及网关拦截规则。',
    priorityLevel: 'P2',
    priority: '待分配',
    status: '新建',
    priorityClass: 'chip-orange',
    assignee: '待分配',
    submitter: '王晨',
    updatedAt: '30 分钟前',
    tags: ['权限', '附件', '知识库'],
    comments: [
      {
        author: '王晨',
        content: '附件打开时报 403，请协助处理。',
        typeLabel: 'Comment',
        internal: false,
        createdAt: '今天 11:06',
      },
    ],
    timeline: [
      { title: 'Ticket Created', desc: '用户提交工单并附带访问异常截图。', operator: '王晨', createdAt: '今天 11:05' },
    ],
  },
  {
    id: 3,
    ticketNo: 'TASK-881',
    title: '客服后台批量导出速度较慢',
    meta: '#TASK-881 · 运营支持 · 预计 16:30 前处理',
    content: '客服后台批量导出数据时耗时较长，后续适合引出异步导出、任务队列、缓存和下载优化。',
    priorityLevel: 'P3',
    priority: '普通',
    status: '排查中',
    priorityClass: 'chip-green',
    assignee: '林哲',
    submitter: '运营支持组',
    updatedAt: '1 小时前',
    tags: ['导出', '异步', '性能'],
    comments: [],
    timeline: [
      { title: 'Ticket Created', desc: '运营提交优化工单。', operator: '运营支持组', createdAt: '今天 08:36' },
      { title: 'Status Updated', desc: '已进入性能排查阶段。', operator: '林哲', createdAt: '今天 09:12' },
    ],
  },
]

export const fallbackArticles: KnowledgeArticleItem[] = [
  {
    id: 1,
    tag: '支付系统 · 指南',
    title: '支付回调失败排查手册',
    summary: '从签名校验、重试机制、幂等判断到日志链路追踪，帮助支持人员快速定位问题并形成统一处理方案。',
    author: '作者：系统团队',
    time: '今天 09:20',
    metric: '浏览量：1,284',
    tagClass: 'chip-blue',
  },
  {
    id: 2,
    tag: '运维流程 · SOP',
    title: '生产环境发布异常应急处理流程',
    summary: '覆盖回滚判断、服务降级、缓存预热、异常上报与复盘记录，适合沉淀成团队统一操作标准。',
    author: '作者：运维组',
    time: '昨天',
    metric: '命中率：79%',
    tagClass: 'chip-green',
  },
]

export const activities: ActivityItem[] = [
  { title: '林哲', desc: '将工单 #INC-2031 状态更新为“处理中”' },
  { title: 'AI 助手', desc: '为《支付回调失败排查手册》生成了新的摘要版本' },
  { title: '产品组', desc: '新增知识分类“结算与对账”' },
]
