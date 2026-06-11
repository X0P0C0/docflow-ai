const Layout = () => import("@/layout/index.vue");

export default {
  path: "/audit",
  name: "Audit",
  component: Layout,
  meta: {
    icon: "ep/document-checked",
    title: "审计日志",
    rank: 10
  },
  children: [
    {
      path: "/audit/logs",
      name: "AuditLogs",
      component: () => import("@/views/docflow/audit/list.vue"),
      meta: {
        title: "操作日志",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
