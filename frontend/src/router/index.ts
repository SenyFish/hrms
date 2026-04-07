import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";
import { appRoutes } from "./routes";

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0, left: 0 };
  },
  routes: appRoutes,
});

router.beforeEach(async (to, _from, next) => {
  const store = useUserStore();
  if (to.path === "/login") {
    if (store.token) {
      return next("/home");
    }
    return next();
  }
  if (to.meta.requiresAuth && !store.token) {
    return next("/login");
  }
  if (store.token && !store.profile) {
    try {
      await store.fetchProfile();
    } catch {
      store.clear();
      return next("/login");
    }
  }
  next();
});

export default router;
