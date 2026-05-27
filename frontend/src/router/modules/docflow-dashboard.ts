const Layout = () => import("@/layout/index.vue");

export default {
  path: "/dashboard",
  name: "Dashboard",
  component: Layout,
  redirect: "/dashboard/index",
  meta: {
    icon: "ep/home-filled",
    title: "控制台",
    rank: 1
  },
  children: [
    {
      path: "/dashboard/index",
      name: "DashboardIndex",
      component: () => import("@/views/docflow/dashboard/index.vue"),
      meta: {
        title: "控制台",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
