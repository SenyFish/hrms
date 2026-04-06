<template>
  <div class="layout-shell">
    <aside class="sidebar hr-shell-surface">
      <div class="brand-box">
        <div class="brand-mark">
          <img src="/favicon.svg" alt="系统图标" class="brand-icon" />
        </div>
        <div class="brand-copy">
          <strong>人力资源管理系统</strong>
          <span>Nuxt UI Admin Workspace</span>
        </div>
      </div>

      <div class="menu-stack">
        <section v-for="menu in topMenus" :key="menu.id" class="menu-section">
          <div v-if="childrenOf(menu.id).length" class="menu-group-title">
            <span>{{ menuLabel(menu.title, menu.path) }}</span>
            <UBadge color="neutral" variant="soft" size="sm">{{ childrenOf(menu.id).length }}</UBadge>
          </div>

          <div class="menu-items">
            <button
              v-if="!childrenOf(menu.id).length"
              type="button"
              class="menu-link"
              :class="{ active: route.path === menu.path }"
              @click="router.push(menu.path)"
            >
              <UIcon :name="iconMap(menu.icon)" class="menu-icon" />
              <span>{{ menuLabel(menu.title, menu.path) }}</span>
            </button>

            <button
              v-for="child in childrenOf(menu.id)"
              :key="child.id"
              type="button"
              class="menu-link"
              :class="{ active: route.path === child.path }"
              @click="router.push(child.path)"
            >
              <UIcon :name="iconMap(child.icon)" class="menu-icon" />
              <span>{{ menuLabel(child.title, child.path) }}</span>
            </button>
          </div>
        </section>
      </div>
    </aside>

    <main class="workspace">
      <header class="workspace-header hr-shell-surface">
        <div>
          <p class="workspace-kicker">组织运营中台</p>
          <h1 class="workspace-title">{{ pageTitle }}</h1>
        </div>

        <div class="workspace-actions">
          <div class="user-panel">
            <UAvatar size="lg" :text="displayInitial" class="user-avatar" />
            <div>
              <div class="user-name">{{ displayName }}</div>
              <div class="user-role">{{ displayRole }}</div>
            </div>
          </div>
          <UButton color="primary" variant="solid" icon="i-lucide-log-out" @click="logout">退出登录</UButton>
        </div>
      </header>

      <div class="workspace-body">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const menus = computed(() => store.menus || []);
const topMenus = computed(() => menus.value.filter((m) => m.parentId == null || m.parentId === 0));

function childrenOf(pid: number) {
  return menus.value.filter((item) => item.parentId === pid).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
}

function iconMap(name?: string) {
  const map: Record<string, string> = {
    HomeFilled: "i-lucide-house",
    Setting: "i-lucide-settings-2",
    Lock: "i-lucide-shield-check",
    Money: "i-lucide-wallet",
    Clock: "i-lucide-clock-3",
    Folder: "i-lucide-folder-kanban",
    User: "i-lucide-users",
    OfficeBuilding: "i-lucide-building-2",
    Avatar: "i-lucide-user-round-check",
    Menu: "i-lucide-panels-top-left",
    Document: "i-lucide-file-text",
    Location: "i-lucide-map-pinned",
    Calendar: "i-lucide-calendar-range",
    List: "i-lucide-list-todo",
    DataAnalysis: "i-lucide-chart-column-big",
  };
  return map[name || ""] || "i-lucide-square-chart-gantt";
}

const pageTitle = computed(() => {
  const current = menus.value.find((item) => item.path === route.path);
  return current?.title || "首页";
});

const displayName = computed(() => String(store.profile?.realName || store.profile?.username || ""));
const displayRole = computed(() => String(store.profile?.roleName || store.profile?.roleCode || ""));
const displayInitial = computed(() => displayName.value.slice(0, 1) || "H");

function menuLabel(title: string, path: string) {
  const roleCode = String(store.profile?.roleCode || "");
  if (roleCode === "EMP" && path === "/relations/contracts") {
    return "我的合同";
  }
  return title;
}

async function logout() {
  try {
    await http.post("/auth/logout");
  } catch {
    /* ignore */
  }
  store.clear();
  router.push("/login");
}
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(215, 194, 139, 0.22), transparent 24%),
    radial-gradient(circle at bottom right, rgba(135, 191, 163, 0.22), transparent 26%),
    linear-gradient(180deg, #edf3ee 0%, #e4ece6 100%);
}

.sidebar {
  border-radius: 28px;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(10, 18, 27, 0.94) 0%, rgba(27, 67, 50, 0.96) 100%),
    rgba(10, 18, 27, 0.78);
  color: #f8f4ec;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 36px);
}

.brand-box {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 8px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, rgba(215, 194, 139, 0.98), rgba(150, 194, 166, 0.96));
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.22);
}

.brand-icon {
  width: 34px;
  height: 34px;
}

.brand-copy strong,
.brand-copy span {
  display: block;
}

.brand-copy strong {
  font-size: 18px;
  line-height: 1.3;
}

.brand-copy span {
  margin-top: 4px;
  color: rgba(248, 244, 236, 0.62);
  font-size: 12px;
}

.menu-stack {
  margin-top: 18px;
  display: grid;
  gap: 16px;
}

.menu-section {
  display: grid;
  gap: 10px;
}

.menu-group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(248, 244, 236, 0.6);
}

.menu-items {
  display: grid;
  gap: 8px;
}

.menu-link {
  width: 100%;
  border: 0;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  color: rgba(248, 244, 236, 0.8);
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, color 0.2s ease;
}

.menu-link:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  transform: translateY(-1px);
}

.menu-link.active {
  background: linear-gradient(135deg, rgba(215, 194, 139, 0.3), rgba(135, 191, 163, 0.26));
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.menu-icon {
  width: 18px;
  height: 18px;
}

.workspace {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 16px;
}

.workspace-header {
  min-height: 88px;
  padding: 18px 22px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.workspace-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #527164;
}

.workspace-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
  color: #13231a;
}

.workspace-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(27, 67, 50, 0.08);
}

.user-name {
  font-weight: 700;
  color: #13231a;
}

.user-role {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

.workspace-body {
  min-width: 0;
}

@media (max-width: 1100px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
  }

  .workspace-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
}
</style>
