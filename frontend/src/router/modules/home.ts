import { home } from "@/router/enums";

const Layout = () => import("@/layout/index.vue");

export default {
  path: "/",
  name: "Home",
  component: Layout,
  redirect: "/dashboard/index",
  meta: {
    icon: "ep/home-filled",
    title: "DocFlow AI",
    rank: home,
    showLink: false
  },
  children: [
    {
      path: "/welcome",
      name: "Welcome",
      component: () => import("@/views/docflow/dashboard/index.vue"),
      meta: {
        title: "控制台",
        showLink: false
      }
    }
  ]
} satisfies RouteConfigsTable;
