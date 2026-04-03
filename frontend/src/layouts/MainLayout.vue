<template>
  <div class="shell">
    <aside class="shell__aside">
      <div class="shell__brand">
        <div class="shell__brand-badge">
          <img src="/favicon.svg" alt="HRMS" class="shell__brand-icon" />
        </div>
        <div>
          <strong class="shell__brand-title">HRMS Console</strong>
          <p class="shell__brand-copy">Frappe-style workspace for the current Spring backend</p>
        </div>
      </div>

      <div class="shell__nav-copy">
        <p class="shell__nav-kicker">{{ currentMeta.sectionEn }}</p>
        <h2>{{ currentMeta.section }}</h2>
        <p>{{ currentMeta.description }}</p>
      </div>

      <nav class="shell__nav">
        <section v-for="group in menuGroups" :key="group.id" class="shell__nav-group">
          <header class="shell__nav-group-title">
            <component :is="iconMap(group.icon)" />
            <span>{{ group.title }}</span>
          </header>
          <router-link
            v-for="item in group.children"
            :key="item.id"
            :to="item.path"
            class="shell__nav-link"
            :class="{ 'is-active': route.path === item.path }"
          >
            <span>{{ item.title }}</span>
            <small>{{ shortPath(item.path) }}</small>
          </router-link>
          <router-link
            v-if="!group.children.length && group.path"
            :to="group.path"
            class="shell__nav-link"
            :class="{ 'is-active': route.path === group.path }"
          >
            <span>{{ group.title }}</span>
            <small>{{ shortPath(group.path) }}</small>
          </router-link>
        </section>
      </nav>
    </aside>

    <main class="shell__content">
      <header class="shell__topbar">
        <div class="shell__topbar-copy">
          <p class="shell__topbar-kicker">{{ currentMeta.section }} / {{ currentMeta.sectionEn }}</p>
          <h1>{{ pageTitle }}</h1>
        </div>

        <div class="shell__topbar-actions">
          <div class="shell__identity">
            <span class="shell__identity-label">当前用户</span>
            <strong>{{ displayName }}</strong>
          </div>
          <button class="frappe-button" data-variant="outline" type="button" @click="logout">退出登录</button>
        </div>
      </header>

      <section class="app-page">
        <div class="page-hero" :class="accentClass">
          <div class="page-hero__content">
            <p class="page-hero__eyebrow">{{ currentMeta.section }} / {{ currentMeta.sectionEn }}</p>
            <h2 class="page-hero__title">{{ pageTitle }}</h2>
            <p class="page-hero__description">{{ currentMeta.description }}</p>
          </div>
          <div class="page-hero__metrics">
            <div class="page-pill">
              <span class="page-pill__label">Route</span>
              <span class="page-pill__value">{{ route.path }}</span>
            </div>
            <div class="page-pill">
              <span class="page-pill__label">Role</span>
              <span class="page-pill__value">{{ displayRole }}</span>
            </div>
          </div>
        </div>

        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "@/api/http";
import { useUserStore, type MenuItem } from "@/stores/user";
import { accentClassMap, menuMeta } from "@/config/navigation";
import {
  Avatar,
  Calendar,
  Clock,
  Collection,
  DataAnalysis,
  Document,
  Files,
  HomeFilled,
  Location,
  Lock,
  Money,
  OfficeBuilding,
  Setting,
  User,
} from "@element-plus/icons-vue";

type MenuNode = MenuItem & {
  sortOrder?: number;
  children: MenuItem[];
};

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const menus = computed(() => store.menus || []);

const menuGroups = computed<MenuNode[]>(() => {
  const roots = menus.value
    .filter((item) => item.parentId == null || item.parentId === 0)
    .sort((a, b) => ((a as MenuNode).sortOrder || 0) - ((b as MenuNode).sortOrder || 0));

  return roots.map((root) => ({
    ...(root as MenuNode),
    children: menus.value
      .filter((item) => item.parentId === root.id)
      .sort((a, b) => ((a as MenuNode).sortOrder || 0) - ((b as MenuNode).sortOrder || 0)),
  }));
});

function iconMap(name?: string) {
  const map: Record<string, unknown> = {
    HomeFilled,
    Setting,
    Lock,
    Money,
    Clock,
    Files,
    User,
    OfficeBuilding,
    Avatar,
    Collection,
    Document,
    Location,
    Calendar,
    DataAnalysis,
  };
  return map[name || ""] || HomeFilled;
}

const currentMeta = computed(() => {
  return (
    menuMeta[route.path] || {
      section: "工作台",
      sectionEn: "Workspace",
      description: "在统一壳层内继续处理当前业务流程。",
      accent: "overview",
    }
  );
});

const pageTitle = computed(() => {
  const menu = menus.value.find((item) => item.path === route.path);
  return menu?.title || "首页";
});

const accentClass = computed(() => accentClassMap[currentMeta.value.accent] || accentClassMap.overview);
const displayName = computed(() => (store.profile?.realName as string) || (store.profile?.username as string) || "未知用户");
const displayRole = computed(() => (store.profile?.roleName as string) || (store.profile?.roleCode as string) || "未分配");

function shortPath(path: string) {
  return path.replace(/^\//, "");
}

async function logout() {
  try {
    await http.post("/auth/logout");
  } catch {
    // ignore logout failure and clear local session anyway
  }
  store.clear();
  router.push("/login");
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
}

.shell__aside {
  position: sticky;
  top: 0;
  min-height: 100vh;
  padding: 24px 20px;
  background:
    radial-gradient(circle at top right, rgba(203, 173, 103, 0.18), transparent 22%),
    linear-gradient(180deg, rgba(18, 54, 41, 0.98) 0%, rgba(23, 58, 46, 0.98) 100%);
  color: #f6f2e8;
}

.shell__aside::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 30px 30px;
  opacity: 0.34;
  pointer-events: none;
}

.shell__brand,
.shell__nav-copy,
.shell__nav {
  position: relative;
  z-index: 1;
}

.shell__brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.shell__brand-badge {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: linear-gradient(135deg, #cbad67 0%, #f4e7c5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.2);
}

.shell__brand-icon {
  width: 34px;
  height: 34px;
}

.shell__brand-title {
  display: block;
  font-size: 18px;
}

.shell__brand-copy {
  margin: 4px 0 0;
  color: rgba(246, 242, 232, 0.64);
  line-height: 1.5;
  font-size: 12px;
}

.shell__nav-copy {
  margin-top: 26px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
}

.shell__nav-kicker {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(246, 242, 232, 0.56);
}

.shell__nav-copy h2 {
  margin: 0;
  font-size: 24px;
}

.shell__nav-copy p:last-child {
  margin: 10px 0 0;
  color: rgba(246, 242, 232, 0.68);
  line-height: 1.7;
}

.shell__nav {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shell__nav-group {
  padding: 14px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.shell__nav-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: rgba(246, 242, 232, 0.72);
  font-size: 13px;
}

.shell__nav-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 16px;
  color: rgba(246, 242, 232, 0.76);
  text-decoration: none;
  transition:
    background 0.18s ease,
    transform 0.18s ease;
}

.shell__nav-link small {
  color: rgba(246, 242, 232, 0.38);
}

.shell__nav-link:hover,
.shell__nav-link.is-active {
  background: rgba(255, 255, 255, 0.12);
  transform: translateX(2px);
}

.shell__nav-link.is-active {
  color: #fff8ea;
}

.shell__content {
  min-width: 0;
  padding: 24px;
}

.shell__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.shell__topbar-kicker {
  margin: 0 0 8px;
  color: #6b7d73;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shell__topbar h1 {
  margin: 0;
  font-size: 28px;
}

.shell__topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shell__identity {
  min-width: 160px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.64);
  border: 1px solid rgba(24, 49, 38, 0.08);
  box-shadow: var(--hr-shadow-soft);
}

.shell__identity-label {
  display: block;
  margin-bottom: 6px;
  color: var(--hr-text-soft);
  font-size: 12px;
}

@media (max-width: 1120px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .shell__aside {
    position: relative;
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .shell__content {
    padding: 16px;
  }

  .shell__topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .shell__topbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .shell__identity {
    min-width: 0;
    flex: 1;
  }
}
</style>
