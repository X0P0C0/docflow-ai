const Layout = () => import("@/layout/index.vue");

export default {
  path: "/sla",
  name: "Sla",
  component: Layout,
  meta: {
    icon: "ep/alarm-clock",
    title: "SLA 管理",
    rank: 7
  },
  children: [
    {
      path: "/sla/policies",
      name: "SlaPolicies",
      component: () => import("@/views/docflow/sla/list.vue"),
      meta: {
        title: "SLA 策略",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
