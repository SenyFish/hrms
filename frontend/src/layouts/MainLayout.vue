<template>
  <el-container class="layout">
    <el-aside width="236px" class="aside">
      <div class="logo-wrap">
        <div class="logo-badge" aria-label="系统图标">
          <img src="/favicon.svg" alt="系统图标" class="logo-badge-icon" />
        </div>
        <div class="logo-copy">
          <strong>人力资源管理系统</strong>
          <span>Human Resource Platform</span>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        router
        class="menu"
        background-color="transparent"
        text-color="rgba(246,243,234,0.78)"
        active-text-color="#f6f3ea"
      >
        <template v-for="m in topMenus" :key="m.id">
          <el-sub-menu v-if="childrenOf(m.id).length" :index="String(m.id)">
            <template #title>
              <el-icon><component :is="iconMap(m.icon)" /></el-icon>
              <span>{{ m.title }}</span>
            </template>
            <el-menu-item v-for="c in childrenOf(m.id)" :key="c.id" :index="c.path">
              {{ c.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.path">
            <el-icon><component :is="iconMap(m.icon)" /></el-icon>
            <span>{{ m.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="content-shell">
      <el-header class="header">
        <div>
          <p class="header-kicker">组织管理工作台</p>
          <span class="title">{{ pageTitle }}</span>
        </div>
        <div class="right">
          <div class="user-chip">
            <span class="user-label">当前用户</span>
            <strong class="name">{{ displayName }}</strong>
          </div>
          <el-button type="primary" class="logout-btn" @click="logout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import {
  HomeFilled,
  Setting,
  Lock,
  Money,
  Clock,
  Folder,
  User,
  OfficeBuilding,
  Avatar,
  Menu,
  Document,
  Location,
  Calendar,
  List,
  DataAnalysis,
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const store = useUserStore();

const menus = computed(() => store.menus || []);

const topMenus = computed(() => menus.value.filter((m) => m.parentId == null || m.parentId === 0));

function childrenOf(pid: number) {
  return menus.value.filter((x) => x.parentId === pid).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
}

function iconMap(name?: string) {
  const map: Record<string, unknown> = {
    HomeFilled,
    Setting,
    Lock,
    Money,
    Clock,
    Folder,
    User,
    OfficeBuilding,
    Avatar,
    Menu,
    Document,
    Location,
    Calendar,
    List,
    DataAnalysis,
  };
  return map[name || ""] || HomeFilled;
}

const pageTitle = computed(() => {
  const menu = menus.value.find((x) => x.path === route.path);
  return menu?.title || "首页";
});

const displayName = computed(() => (store.profile?.realName as string) || (store.profile?.username as string) || "");

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
.layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(215, 194, 139, 0.12), transparent 22%),
    linear-gradient(180deg, #eef4ef 0%, #e8efe8 100%);
}

.aside {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(12, 23, 32, 0.98) 0%, rgba(27, 67, 50, 0.97) 100%);
  color: #f6f3ea;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 18px 0 36px rgba(15, 23, 32, 0.14);
}

.aside::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(215, 194, 139, 0.18), transparent 24%),
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: auto, 34px 34px, 34px 34px;
  opacity: 0.5;
  pointer-events: none;
}

.logo-wrap {
  position: relative;
  z-index: 1;
  padding: 24px 20px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-badge {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #d7c28b 0%, #b6cdaa 100%);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.16);
}

.logo-badge-icon {
  width: 32px;
  height: 32px;
  display: block;
}

.logo-copy strong {
  display: block;
  font-size: 17px;
  line-height: 1.3;
}

.logo-copy span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: rgba(246, 243, 234, 0.58);
}

.menu {
  position: relative;
  z-index: 1;
  border-right: none;
  padding: 8px 12px 20px;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  margin-bottom: 6px;
  border-radius: 14px;
  height: 46px;
  line-height: 46px;
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(215, 194, 139, 0.28), rgba(135, 191, 163, 0.22));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.content-shell {
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  background: rgba(250, 247, 239, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(27, 67, 50, 0.08);
}

.header-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #6d8e7b;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #13231a;
}

.right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-chip {
  min-width: 148px;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(27, 67, 50, 0.08);
}

.user-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #6b7280;
}

.name {
  color: #13231a;
  font-size: 14px;
}

.logout-btn {
  min-height: 40px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #1b4332 0%, #2d6a4f 100%);
}

.main {
  min-height: calc(100vh - 84px);
  padding: 24px;
  background: transparent;
}

@media (max-width: 900px) {
  .layout {
    display: block;
  }

  .aside {
    width: 100% !important;
  }

  .header {
    padding: 16px 18px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .right {
    width: 100%;
    justify-content: space-between;
  }

  .main {
    padding: 16px;
  }
}
</style>
