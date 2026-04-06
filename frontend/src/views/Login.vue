<template>
  <div class="login-page">
    <div class="curtain curtain-a"></div>
    <div class="curtain curtain-b"></div>
    <div class="curtain curtain-c"></div>
    <div class="halo halo-a"></div>
    <div class="halo halo-b"></div>
    <div class="grid-overlay"></div>

    <div class="login-shell">
      <section class="brand-panel">
        <div class="hero-mark">
          <div class="hero-ring"></div>
          <div class="hero-badge">
            <img src="/favicon.svg" alt="系统图标" class="hero-icon" />
          </div>
        </div>

        <UBadge color="primary" variant="soft" class="brand-tag">HRMS 一体化工作台</UBadge>
        <h1>人力资源管理系统</h1>
        <p class="brand-copy">
          覆盖组织人事、薪资社保、考勤审批、招聘内推、员工关系、培训发展与员工关怀，
          为管理员、人事和员工提供统一入口与在线协同能力。
        </p>

        <div class="brand-grid">
          <UCard variant="soft" class="feature-card">
            <template #header>
              <span class="feature-label">组织与人员</span>
            </template>
            支持员工档案、部门岗位、角色权限、合同资料、个人信息维护等核心人事管理场景。
          </UCard>
          <UCard variant="soft" class="feature-card">
            <template #header>
              <span class="feature-label">流程与协同</span>
            </template>
            统一处理请假、出差、资产申请、内推、纠纷申请、审批待办和首页业务提醒。
          </UCard>
          <UCard variant="soft" class="feature-card">
            <template #header>
              <span class="feature-label">经营与发展</span>
            </template>
            提供工资明细、月报导出、绩效考核、培训组织、晋升规划和关怀计划等持续运营能力。
          </UCard>
        </div>
      </section>

      <UCard variant="soft" class="login-card">
        <template #header>
          <div class="card-head">
            <p class="card-kicker">账号登录</p>
            <h2>进入业务工作台</h2>
            <span>输入用户名和密码后继续访问系统。</span>
          </div>
        </template>

        <form class="login-form" @submit.prevent="onSubmit">
          <div class="field-block">
            <label class="field-label">用户名</label>
            <UInput
              v-model="form.username"
              size="xl"
              variant="subtle"
              placeholder="请输入用户名"
              icon="i-lucide-user-round"
            />
          </div>

          <div class="field-block">
            <label class="field-label">密码</label>
            <UInput
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              size="xl"
              variant="subtle"
              placeholder="请输入密码"
              icon="i-lucide-lock-keyhole"
            >
              <template #trailing>
                <UButton
                  color="neutral"
                  variant="ghost"
                  :icon="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                  @click.prevent="showPassword = !showPassword"
                />
              </template>
            </UInput>
          </div>

          <UButton type="submit" size="xl" block color="primary" variant="solid" :loading="loading" class="submit-btn">
            登录系统
          </UButton>
        </form>
      </UCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const store = useUserStore();
const toast = useToast();
const loading = ref(false);
const showPassword = ref(false);
const form = reactive({ username: "", password: "" });

async function onSubmit() {
  loading.value = true;
  try {
    const data = (await http.post("/auth/login", form)) as { token: string };
    store.setToken(data.token);
    await store.fetchProfile();
    toast.add({ title: "登录成功", color: "success" });
    router.push("/home");
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "登录失败",
      color: "error",
    });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(215, 194, 139, 0.22), transparent 24%),
    radial-gradient(circle at 80% 18%, rgba(135, 191, 163, 0.28), transparent 22%),
    linear-gradient(135deg, #09131c 0%, #123024 48%, #eef2eb 150%);
}

.curtain,
.halo,
.grid-overlay {
  pointer-events: none;
  position: absolute;
}

.curtain {
  border-radius: 999px;
  filter: blur(60px);
  opacity: 0.48;
  mix-blend-mode: screen;
}

.curtain-a {
  width: 76vw;
  height: 78vh;
  top: -20vh;
  left: -18vw;
  background: radial-gradient(circle, rgba(215, 194, 139, 0.3) 0%, rgba(215, 194, 139, 0) 72%);
  animation: driftA 22s ease-in-out infinite;
}

.curtain-b {
  width: 68vw;
  height: 72vh;
  top: -8vh;
  right: -20vw;
  background: radial-gradient(circle, rgba(135, 191, 163, 0.24) 0%, rgba(135, 191, 163, 0) 74%);
  animation: driftB 26s ease-in-out infinite;
}

.curtain-c {
  width: 88vw;
  height: 74vh;
  bottom: -30vh;
  left: 12vw;
  background: radial-gradient(circle, rgba(248, 244, 236, 0.16) 0%, rgba(248, 244, 236, 0) 76%);
  animation: driftC 28s ease-in-out infinite;
}

.halo {
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.52;
}

.halo-a {
  width: 320px;
  height: 320px;
  right: 14%;
  top: 6%;
  background: rgba(135, 191, 163, 0.16);
}

.halo-b {
  width: 280px;
  height: 280px;
  left: -80px;
  bottom: 10%;
  background: rgba(215, 194, 139, 0.16);
}

.grid-overlay {
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.9), transparent 86%);
}

.login-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
  padding: 22px 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) 430px;
  align-items: center;
  gap: 38px;
}

.brand-panel {
  color: #f8f4ec;
}

.hero-mark {
  position: relative;
  width: 184px;
  height: 184px;
  margin-bottom: 20px;
}

.hero-ring {
  position: absolute;
  inset: 6px;
  border-radius: 42px;
  background: radial-gradient(circle, rgba(215, 194, 139, 0.24) 0%, rgba(215, 194, 139, 0) 72%);
  filter: blur(8px);
}

.hero-badge {
  position: absolute;
  inset: 18px;
  border-radius: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0.08)),
    rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow:
    0 30px 60px rgba(4, 10, 16, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 0 0 18px rgba(215, 194, 139, 0.08);
}

.hero-icon {
  width: 98px;
  height: 98px;
  filter: drop-shadow(0 20px 28px rgba(6, 14, 22, 0.24));
}

.brand-tag {
  margin-bottom: 14px;
}

.brand-panel h1 {
  margin: 0;
  font-size: 56px;
  line-height: 1.04;
}

.brand-copy {
  max-width: 620px;
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.85;
  color: rgba(248, 244, 236, 0.76);
}

.brand-grid {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.feature-card {
  min-height: 176px;
  color: #eef4ef;
  background: rgba(255, 255, 255, 0.07);
  border-color: rgba(255, 255, 255, 0.12);
}

.feature-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #9fd0b5;
}

.login-card {
  border-radius: 28px;
  background: rgba(250, 247, 239, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow: 0 24px 60px rgba(9, 16, 24, 0.28);
  backdrop-filter: blur(18px);
}

.card-head {
  display: grid;
  gap: 8px;
}

.card-kicker {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #3f6b56;
}

.card-head h2 {
  margin: 0;
  font-size: 30px;
  color: #13231a;
}

.card-head span {
  color: #6b7280;
  font-size: 14px;
}

.login-form {
  display: grid;
  gap: 18px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  color: #264334;
  font-weight: 600;
}

.submit-btn {
  margin-top: 4px;
}

@keyframes driftA {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.04);
  }
  50% {
    transform: translate3d(48px, 22px, 0) scale(1.12);
  }
}

@keyframes driftB {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.04);
  }
  50% {
    transform: translate3d(-56px, 26px, 0) scale(1.14);
  }
}

@keyframes driftC {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.02);
  }
  50% {
    transform: translate3d(-18px, -34px, 0) scale(1.08);
  }
}

@media (max-width: 1024px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 26px;
  }

  .brand-panel h1 {
    font-size: 40px;
  }

  .brand-grid {
    grid-template-columns: 1fr;
  }

  .login-card {
    max-width: 480px;
    width: 100%;
    margin: 0 auto;
  }
}
</style>
