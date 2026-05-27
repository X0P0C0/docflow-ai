const Layout = () => import("@/layout/index.vue");

export default {
  path: "/ai-center",
  name: "AiCenter",
  component: Layout,
  redirect: "/ai-center/index",
  meta: {
    icon: "ri/robot-2-line",
    title: "AI 中心",
    rank: 4
  },
  children: [
    {
      path: "/ai-center/index",
      name: "AiCenterIndex",
      component: () => import("@/views/docflow/ai/index.vue"),
      meta: {
        title: "AI 协作",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
