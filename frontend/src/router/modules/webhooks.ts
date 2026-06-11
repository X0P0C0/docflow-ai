const Layout = () => import("@/layout/index.vue");

export default {
  path: "/webhooks",
  name: "Webhooks",
  component: Layout,
  meta: {
    icon: "ep/link",
    title: "Webhook 管理",
    rank: 13
  },
  children: [
    {
      path: "/webhooks/list",
      name: "WebhookList",
      component: () => import("@/views/docflow/webhooks/list.vue"),
      meta: {
        title: "Webhook 管理",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
