const Layout = () => import("@/layout/index.vue");

export default {
  path: "/fields",
  name: "CustomFields",
  component: Layout,
  meta: {
    icon: "ep/edit",
    title: "自定义字段",
    rank: 12
  },
  children: [
    {
      path: "/fields/list",
      name: "FieldList",
      component: () => import("@/views/docflow/fields/list.vue"),
      meta: {
        title: "字段管理",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
