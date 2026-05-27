const Layout = () => import("@/layout/index.vue");

export default {
  path: "/knowledge",
  name: "Knowledge",
  component: Layout,
  redirect: "/knowledge/articles",
  meta: {
    icon: "ri/book-open-line",
    title: "知识库",
    rank: 3
  },
  children: [
    {
      path: "/knowledge/articles",
      name: "KnowledgeArticles",
      component: () => import("@/views/docflow/knowledge/list.vue"),
      meta: {
        title: "知识文章",
        showLink: true
      }
    },
    {
      path: "/knowledge/articles/create",
      name: "KnowledgeCreate",
      component: () => import("@/views/docflow/knowledge/editor.vue"),
      meta: {
        title: "新建文章",
        showLink: false
      }
    },
    {
      path: "/knowledge/articles/:id",
      name: "KnowledgeDetail",
      component: () => import("@/views/docflow/knowledge/detail.vue"),
      meta: {
        title: "文章详情",
        showLink: false
      }
    },
    {
      path: "/knowledge/articles/:id/edit",
      name: "KnowledgeEdit",
      component: () => import("@/views/docflow/knowledge/editor.vue"),
      meta: {
        title: "编辑文章",
        showLink: false
      }
    }
  ]
} satisfies RouteConfigsTable;
