const Layout = () => import("@/layout/index.vue");

export default {
  path: "/notifications",
  name: "Notifications",
  component: Layout,
  meta: {
    icon: "ep/bell",
    title: "通知中心",
    rank: 9
  },
  children: [
    {
      path: "/notifications/list",
      name: "NotificationList",
      component: () => import("@/views/docflow/notifications/list.vue"),
      meta: {
        title: "通知列表",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
