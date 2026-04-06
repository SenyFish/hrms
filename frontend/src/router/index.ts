import { createRouter, createWebHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0, left: 0 };
  },
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
        { path: "/salary/performance", name: "salaryPerformance", component: () => import("@/views/salary/Performance.vue") },
        { path: "/attendance/leaves", name: "leaves", component: () => import("@/views/attendance/Leaves.vue") },
        { path: "/attendance/records", name: "attRecords", component: () => import("@/views/attendance/Records.vue") },
        { path: "/attendance/trips", name: "attendanceTrips", component: () => import("@/views/attendance/Trips.vue") },
        { path: "/recruitment/requirements", name: "recruitmentRequirements", component: () => import("@/views/recruitment/Requirements.vue") },
        { path: "/recruitment/positions", name: "recruitmentPositions", component: () => import("@/views/recruitment/Positions.vue") },
        { path: "/recruitment/candidates", name: "recruitmentCandidates", component: () => import("@/views/recruitment/Candidates.vue") },
        { path: "/recruitment/referrals", name: "recruitmentReferrals", component: () => import("@/views/recruitment/Candidates.vue") },
        { path: "/relations/contracts", name: "relationContracts", component: () => import("@/views/relations/Contracts.vue") },
        { path: "/relations/disputes", name: "relationDisputes", component: () => import("@/views/relations/Disputes.vue") },
        { path: "/training/sessions", name: "trainingSessions", component: () => import("@/views/training/Sessions.vue") },
        { path: "/training/promotions", name: "trainingPromotions", component: () => import("@/views/training/Promotions.vue") },
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
