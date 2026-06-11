const Layout = () => import("@/layout/index.vue");

export default {
  path: "/automation",
  name: "Automation",
  component: Layout,
  meta: {
    icon: "ep/set-up",
    title: "自动化",
    rank: 11
  },
  children: [
    {
      path: "/automation/rules",
      name: "AutomationRules",
      component: () => import("@/views/docflow/automation/list.vue"),
      meta: {
        title: "自动化规则",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
