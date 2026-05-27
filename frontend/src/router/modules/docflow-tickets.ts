const Layout = () => import("@/layout/index.vue");

export default {
  path: "/tickets",
  name: "Tickets",
  component: Layout,
  redirect: "/tickets/list",
  meta: {
    icon: "ri/coupon-3-line",
    title: "工单中心",
    rank: 2
  },
  children: [
    {
      path: "/tickets/list",
      name: "TicketList",
      component: () => import("@/views/docflow/tickets/list.vue"),
      meta: {
        title: "工单列表",
        showLink: true
      }
    },
    {
      path: "/tickets/create",
      name: "TicketCreate",
      component: () => import("@/views/docflow/tickets/create.vue"),
      meta: {
        title: "新建工单",
        showLink: false
      }
    },
    {
      path: "/tickets/:id",
      name: "TicketDetail",
      component: () => import("@/views/docflow/tickets/detail.vue"),
      meta: {
        title: "工单详情",
        showLink: false
      }
    }
  ]
} satisfies RouteConfigsTable;
