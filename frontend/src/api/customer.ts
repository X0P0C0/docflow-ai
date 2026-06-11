import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type CustomerInfo = {
  id: number;
  username: string;
  email?: string;
  company?: string;
  realName?: string;
};

export type CustomerLoginResponse = {
  token: string;
  expireSeconds: number;
  customer: CustomerInfo;
};

export type CustomerTicket = {
  id: number;
  ticketNo: string;
  title: string;
  content: string;
  type?: string;
  priority?: number;
  priorityLabel?: string;
  status?: number;
  statusLabel?: string;
  assigneeName?: string;
  satisfactionScore?: number;
  createTime?: string;
  updateTime?: string;
  timeline?: TimelineItem[];
};

export type TimelineItem = {
  actionType: string;
  operatorName: string;
  remark?: string;
  time?: string;
};

export const customerRegister = (data: {
  username: string;
  password: string;
  email?: string;
  phone?: string;
  company?: string;
  realName?: string;
}) => {
  return http.request<ApiResult<CustomerLoginResponse>>("post", "/api/customer/register", { data });
};

export const customerLogin = (data: { username: string; password: string }) => {
  return http.request<ApiResult<CustomerLoginResponse>>("post", "/api/customer/login", { data });
};

export const customerSubmitTicket = (data: {
  title: string;
  content: string;
  type?: string;
  priority?: number;
}) => {
  return http.request<ApiResult<CustomerTicket>>("post", "/api/customer/tickets", {
    data,
    headers: { "X-Customer-Id": getCustomerId() }
  });
};

export const customerListTickets = () => {
  return http.request<ApiResult<CustomerTicket[]>>("get", "/api/customer/tickets", {
    headers: { "X-Customer-Id": getCustomerId() }
  });
};

export const customerGetTicket = (ticketId: number) => {
  return http.request<ApiResult<CustomerTicket>>("get", `/api/customer/tickets/${ticketId}`, {
    headers: { "X-Customer-Id": getCustomerId() }
  });
};

export const customerRateSatisfaction = (ticketId: number, data: { score: number; comment?: string }) => {
  return http.request<ApiResult<CustomerTicket>>("post", `/api/customer/tickets/${ticketId}/satisfaction`, {
    data,
    headers: { "X-Customer-Id": getCustomerId() }
  });
};

function getCustomerId(): string {
  return localStorage.getItem("customer_id") || "0";
}

export function setCustomerSession(token: string, customer: CustomerInfo) {
  localStorage.setItem("customer_token", token);
  localStorage.setItem("customer_id", String(customer.id));
  localStorage.setItem("customer_info", JSON.stringify(customer));
}

export function clearCustomerSession() {
  localStorage.removeItem("customer_token");
  localStorage.removeItem("customer_id");
  localStorage.removeItem("customer_info");
}

export function getCustomerInfo(): CustomerInfo | null {
  const raw = localStorage.getItem("customer_info");
  return raw ? JSON.parse(raw) : null;
}

export function isCustomerLoggedIn(): boolean {
  return !!localStorage.getItem("customer_token");
}
