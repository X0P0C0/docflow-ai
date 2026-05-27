import "@/utils/sso";
import Cookies from "js-cookie";
import { getConfig } from "@/config";
import NProgress from "@/utils/progress";
import { transformI18n } from "@/plugins/i18n";
import { buildHierarchyTree } from "@/utils/tree";
import remainingRouter from "./modules/remaining";
import { useMultiTagsStoreHook } from "@/store/modules/multiTags";
import { usePermissionStoreHook } from "@/store/modules/permission";
import {
  isUrl,
  openLink,
  cloneDeep,
  isAllEmpty,
  storageLocal
} from "@pureadmin/utils";
import {
  ascending,
  getTopMenu,
  initRouter,
  isOneOfArray,
  getHistoryMode,
  findRouteByPath,
  handleAliveRoute,
  formatTwoStageRoutes,
  formatFlatteningRoutes
} from "./utils";
import {
  type Router,
  type RouteRecordRaw,
  type RouteComponent,
  createRouter
} from "vue-router";
import {
  type DataInfo,
  userKey,
  removeToken,
  multipleTabsKey
} from "@/utils/auth";

/** Auto-load static route modules except `remaining.ts`. */
const modules: Record<string, any> = import.meta.glob(
  ["./modules/**/*.ts", "!./modules/**/remaining.ts"],
  {
    eager: true
  }
);

/** Raw static routes before flattening and hierarchy transforms. */
const routes = [];

function hideMenuTree(route: any) {
  if (!route?.meta) route.meta = {};
  route.meta.showLink = false;
  route.children?.forEach((child: any) => hideMenuTree(child));
  return route;
}

Object.keys(modules).forEach(key => {
  const route = modules[key].default;
  if (
    !key.includes("docflow-") &&
    !key.endsWith("/home.ts") &&
    !key.endsWith("/error.ts")
  ) {
    // Keep non-DocFlow route modules available but out of the main menu.
    routes.push(hideMenuTree(route));
    return;
  }
  routes.push(route);
});

/** Export normalized static routes for the active router instance. */
export const constantRoutes: Array<RouteRecordRaw> = formatTwoStageRoutes(
  formatFlatteningRoutes(buildHierarchyTree(ascending(routes.flat(Infinity))))
);

/** Snapshot used when resetting routes after logout. */
const initConstantRoutes: Array<RouteRecordRaw> = cloneDeep(constantRoutes);

/** Menu routes kept in their original hierarchy. */
export const constantMenus: Array<RouteComponent> = ascending(
  routes.flat(Infinity)
).concat(...remainingRouter);

/** Paths excluded from the visible menu tree. */
export const remainingPaths = Object.keys(remainingRouter).map(v => {
  return remainingRouter[v].path;
});

/** Create the router instance. */
export const router: Router = createRouter({
  history: getHistoryMode(import.meta.env.VITE_ROUTER_HISTORY),
  routes: constantRoutes.concat(...(remainingRouter as any)),
  strict: true,
  scrollBehavior(to, from, savedPosition) {
    return new Promise(resolve => {
      if (savedPosition) {
        return savedPosition;
      } else {
        if (from.meta.saveSrollTop) {
          const top: number =
            document.documentElement.scrollTop || document.body.scrollTop;
          resolve({ left: 0, top });
        }
      }
    });
  }
});

/** Track pages that have already completed an initial load. */
const loadedPaths = new Set<string>();

/** Clear the page-load cache. */
export function resetLoadedPaths() {
  loadedPaths.clear();
}

/** Reset the router to its initial static state. */
export function resetRouter() {
  router.clearRoutes();
  for (const route of initConstantRoutes.concat(...(remainingRouter as any))) {
    router.addRoute(route);
  }
  router.options.routes = formatTwoStageRoutes(
    formatFlatteningRoutes(buildHierarchyTree(ascending(routes.flat(Infinity))))
  );
  usePermissionStoreHook().clearAllCachePage();
  resetLoadedPaths();
}

/** Route whitelist. */
const whiteList = ["/login"];

const { VITE_HIDE_HOME } = import.meta.env;

router.beforeEach((to: ToRouteType, _from) => {
  to.meta.loaded = loadedPaths.has(to.path);

  if (!to.meta.loaded) {
    NProgress.start();
  }

  if (to.meta?.keepAlive) {
    handleAliveRoute(to, "add");
    // Handle full-page refresh and tag-click refresh cases.
    if (_from.name === undefined || _from.name === "Redirect") {
      handleAliveRoute(to);
    }
  }
  const userInfo = storageLocal().getItem<DataInfo<number>>(userKey);
  const externalLink = isUrl(to?.name as string);
  if (!externalLink) {
    to.matched.some(item => {
      if (!item.meta.title) return "";
      const Title = getConfig().Title;
      if (Title)
        document.title = `${transformI18n(item.meta.title)} | ${Title}`;
      else document.title = transformI18n(item.meta.title);
    });
  }
  /** Keep logged-in users on their current route instead of sending them back to `/login`. */
  function toCorrectRoute() {
    return whiteList.includes(to.fullPath) ? _from.fullPath : undefined;
  }
  if (Cookies.get(multipleTabsKey) && userInfo) {
    // Route to 403 when role access is insufficient.
    if (to.meta?.roles && !isOneOfArray(to.meta?.roles, userInfo?.roles)) {
      return { path: "/error/403" };
    }
    // If the home page is hidden, direct manual `/welcome` access to 404.
    if (VITE_HIDE_HOME === "true" && to.fullPath === "/welcome") {
      return { path: "/error/404" };
    }
    if (_from?.name) {
      // Handle external-link routes.
      if (externalLink) {
        openLink(to?.name as string);
        NProgress.done();
        return false;
      } else {
        return toCorrectRoute();
      }
    } else {
      // First-load refresh path: rebuild dynamic routes if needed.
      if (
        usePermissionStoreHook().wholeMenus.length === 0 &&
        to.path !== "/login"
      ) {
        initRouter().then((router: Router) => {
          if (!useMultiTagsStoreHook().getMultiTagsCache) {
            const { path } = to;
            const route = findRouteByPath(
              path,
              router.options.routes[0].children
            );
            getTopMenu(true);
            // Tags with query/params payloads are not restored here.
            if (route && route.meta?.title) {
              if (isAllEmpty(route.parentId) && route.meta?.backstage) {
                // Dynamic top-level route directory case.
                const { path, name, meta } = route.children[0];
                useMultiTagsStoreHook().handleTags("push", {
                  path,
                  name,
                  meta
                });
              } else {
                const { path, name, meta } = route;
                useMultiTagsStoreHook().handleTags("push", {
                  path,
                  name,
                  meta
                });
              }
            }
          }
          // Ensure dynamic routes are ready before re-entering the target path.
          if (isAllEmpty(to.name)) router.push(to.fullPath);
        });
      }
      return toCorrectRoute();
    }
  } else {
    if (to.path !== "/login") {
      if (whiteList.indexOf(to.path) !== -1) {
        return true;
      } else {
        removeToken();
        return { path: "/login" };
      }
    } else {
      return true;
    }
  }
});

router.afterEach(to => {
  loadedPaths.add(to.path);
  NProgress.done();
});

export default router;
