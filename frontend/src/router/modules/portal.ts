const Layout = () => import("@/layout/index.vue");

export default {
  path: "/portal",
  name: "Portal",
  redirect: "/portal/login",
  meta: {
    title: "客户门户",
    rank: 100
  },
  children: [
    {
      path: "/portal/login",
      name: "PortalLogin",
      component: () => import("@/views/docflow/portal/login.vue"),
      meta: {
        title: "客户登录",
        showLink: false
      }
    },
    {
      path: "/portal/tickets",
      name: "PortalTickets",
      component: () => import("@/views/docflow/portal/tickets.vue"),
      meta: {
        title: "我的工单",
        showLink: true
      }
    },
    {
      path: "/portal/tickets/new",
      name: "PortalNewTicket",
      component: () => import("@/views/docflow/portal/new-ticket.vue"),
      meta: {
        title: "提交工单",
        showLink: false
      }
    },
    {
      path: "/portal/tickets/:id",
      name: "PortalTicketDetail",
      component: () => import("@/views/docflow/portal/ticket-detail.vue"),
      meta: {
        title: "工单详情",
        showLink: false
      }
    }
  ]
} satisfies RouteConfigsTable;
