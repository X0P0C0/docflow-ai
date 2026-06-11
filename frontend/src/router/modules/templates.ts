const Layout = () => import("@/layout/index.vue");

export default {
  path: "/templates",
  name: "Templates",
  component: Layout,
  meta: {
    icon: "ep/document-copy",
    title: "工单模板",
    rank: 8
  },
  children: [
    {
      path: "/templates/list",
      name: "TemplateList",
      component: () => import("@/views/docflow/templates/list.vue"),
      meta: {
        title: "模板管理",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
