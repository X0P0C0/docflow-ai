const Layout = () => import("@/layout/index.vue");

export default {
  path: "/system",
  name: "System",
  component: Layout,
  redirect: "/system/users",
  meta: {
    icon: "ep/setting",
    title: "系统管理",
    rank: 5
  },
  children: [
    {
      path: "/system/users",
      name: "SystemUsers",
      component: () => import("@/views/docflow/system/users.vue"),
      meta: {
        title: "用户管理",
        showLink: true
      }
    },
    {
      path: "/system/roles",
      name: "SystemRoles",
      component: () => import("@/views/docflow/system/roles.vue"),
      meta: {
        title: "角色管理",
        showLink: true
      }
    },
    {
      path: "/system/depts",
      name: "SystemDepts",
      component: () => import("@/views/docflow/system/depts.vue"),
      meta: {
        title: "部门管理",
        showLink: true
      }
    },
    {
      path: "/system/health",
      name: "SystemHealth",
      component: () => import("@/views/docflow/system/health.vue"),
      meta: {
        title: "系统健康",
        showLink: true
      }
    },
    {
      path: "/system/feature-flags",
      name: "FeatureFlags",
      component: () => import("@/views/docflow/system/feature-flags.vue"),
      meta: {
        title: "功能开关",
        showLink: true
      }
    },
    {
      path: "/system/api-docs",
      name: "ApiDocs",
      component: () => import("@/views/docflow/system/api-docs.vue"),
      meta: {
        title: "API 文档",
        showLink: true
      }
    }
  ]
} satisfies RouteConfigsTable;
