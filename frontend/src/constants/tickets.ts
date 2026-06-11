export const ticketStatusOptions = [
  { label: '新建', value: 1 },
  { label: '处理中', value: 2 },
  { label: '已解决', value: 3 },
  { label: '已关闭', value: 4 }
];

export const ticketPriorityOptions = [
  { label: 'P4 - 紧急', value: 4 },
  { label: 'P3 - 高', value: 3 },
  { label: 'P2 - 普通', value: 2 },
  { label: 'P1 - 低', value: 1 }
];

export const ticketTypeOptions = [
  { label: '故障', value: 'INCIDENT' },
  { label: '任务', value: 'TASK' },
  { label: '咨询', value: 'QUESTION' }
];

export const ticketCategoryOptions = [
  { label: '使用支持', value: 1 },
  { label: '通用处理', value: 2 },
  { label: '支付与订单', value: 3 }
];

export function getTicketStatusLabel(status?: number | null) {
  return (
    ticketStatusOptions.find(option => option.value === status)?.label ??
    '未知状态'
  );
}

export function getTicketPriorityLabel(priority?: number | null) {
  return (
    ticketPriorityOptions.find(option => option.value === priority)?.label ??
    '未配置'
  );
}

export function getTicketTypeLabel(type?: string | null) {
  return (
    ticketTypeOptions.find(option => option.value === type)?.label ?? '其它'
  );
}

export function getTicketCategoryLabel(categoryId?: number | null) {
  return (
    ticketCategoryOptions.find(option => option.value === categoryId)?.label ??
    '未分类'
  );
}
