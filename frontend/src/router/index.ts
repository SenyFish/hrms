import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: () => import("@/views/Login.vue") },
    {
      path: "/",
      component: () => import("@/layouts/MainLayout.vue"),
      meta: { requiresAuth: true },
      children: [
        { path: "", redirect: "/home" },
        { path: "/home", name: "home", component: () => import("@/views/Home.vue") },
        { path: "/system/files", name: "files", component: () => import("@/views/system/Files.vue") },
        { path: "/system/employees", name: "employees", component: () => import("@/views/system/Employees.vue") },
        { path: "/system/departments", name: "departments", component: () => import("@/views/system/Departments.vue") },
        { path: "/permission/roles", name: "roles", component: () => import("@/views/permission/Roles.vue") },
        { path: "/permission/menus", name: "menus", component: () => import("@/views/permission/Menus.vue") },
        { path: "/salary/records", name: "salaryRecords", component: () => import("@/views/salary/Records.vue") },
        { path: "/salary/cities", name: "cities", component: () => import("@/views/salary/Cities.vue") },
        { path: "/attendance/leaves", name: "leaves", component: () => import("@/views/attendance/Leaves.vue") },
        { path: "/attendance/records", name: "attRecords", component: () => import("@/views/attendance/Records.vue") },
        { path: "/finance/expenses", name: "financeExpenses", component: () => import("@/views/finance/Expenses.vue") },
        { path: "/finance/assets", name: "financeAssets", component: () => import("@/views/finance/Assets.vue") },
        { path: "/finance/approvals", name: "financeApprovals", component: () => import("@/views/finance/Approvals.vue") },
        { path: "/recruitment/requirements", name: "recruitmentRequirements", component: () => import("@/views/recruitment/Requirements.vue") },
        { path: "/recruitment/positions", name: "recruitmentPositions", component: () => import("@/views/recruitment/Positions.vue") },
        { path: "/recruitment/candidates", name: "recruitmentCandidates", component: () => import("@/views/recruitment/Candidates.vue") },
        { path: "/recruitment/referrals", name: "recruitmentReferrals", component: () => import("@/views/recruitment/Candidates.vue") },
        { path: "/care/plans", name: "carePlans", component: () => import("@/views/care/Plans.vue") },
        { path: "/care/stats", name: "careStats", component: () => import("@/views/care/Stats.vue") },
      ],
    },
  ],
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
