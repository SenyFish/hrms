<template>
  <div class="login-page">
    <div class="bg-curtain curtain-a"></div>
    <div class="bg-curtain curtain-b"></div>
    <div class="bg-curtain curtain-c"></div>
    <div class="bg-aurora aurora-a"></div>
    <div class="bg-aurora aurora-b"></div>
    <div class="bg-grid"></div>
    <div class="bg-wave wave-a"></div>
    <div class="bg-wave wave-b"></div>
    <div class="bg-orb orb-a"></div>
    <div class="bg-orb orb-b"></div>

    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-hero">
          <div class="brand-glow glow-a"></div>
          <div class="brand-glow glow-b"></div>
          <div class="brand-badge" aria-label="系统图标">
            <img src="/favicon.svg" alt="系统图标" class="brand-badge-icon" />
          </div>
        </div>

        <h1>人力资源管理系统</h1>
        <p class="brand-copy">
          统一覆盖组织人事、考勤薪资、财务资产、招聘协同与员工关怀场景，让日常管理、审批流转与经营数据更清晰。
        </p>

        <div class="brand-cards">
          <div class="info-card">
            <span class="info-label">组织人事</span>
            <strong>集中管理员工档案、部门岗位、权限菜单与城市参保规则</strong>
          </div>
          <div class="info-card">
            <span class="info-label">业务协同</span>
            <strong>联动考勤打卡、请假审批、资产申请、资产审核与月报导出</strong>
          </div>
          <div class="info-card">
            <span class="info-label">经营支持</span>
            <strong>打通薪资社保、财务支出、招聘内推和员工关怀计划数据</strong>
          </div>
        </div>
      </section>

      <el-card class="login-card" shadow="always">
        <div class="card-head">
          <p class="card-kicker">账号登录</p>
          <h2>欢迎进入系统</h2>
          <p class="card-copy">请输入账号信息，继续访问人力资源管理平台</p>
        </div>

        <el-form :model="form" class="login-form" @submit.prevent="onSubmit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              prefix-icon="Lock"
            />
          </el-form-item>
          <el-button type="primary" class="submit-btn" native-type="submit" :loading="loading">登录</el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";

const router = useRouter();
const store = useUserStore();
const loading = ref(false);
const form = reactive({ username: "", password: "" });

async function onSubmit() {
  loading.value = true;
  try {
    const data = (await http.post("/auth/login", form)) as { token: string };
    store.setToken(data.token);
    await store.fetchProfile();
    ElMessage.success("登录成功");
    router.push("/home");
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  --bg-deep: #0f1720;
  --bg-mid: #1b4332;
  --brand-accent: #87bfa3;
  --brand-gold: #d7c28b;
  --text-main: #f6f3ea;
  --text-soft: rgba(246, 243, 234, 0.76);
  --card-border: rgba(255, 255, 255, 0.18);
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(215, 194, 139, 0.22), transparent 28%),
    linear-gradient(135deg, var(--bg-deep) 0%, var(--bg-mid) 52%, #f3efe5 160%);
  isolation: isolate;
}

.bg-curtain {
  position: absolute;
  inset: auto;
  pointer-events: none;
  border-radius: 50%;
  filter: blur(56px);
  opacity: 0.52;
  mix-blend-mode: screen;
  will-change: transform, opacity;
}

.curtain-a {
  width: 92vw;
  height: 84vh;
  left: -26vw;
  top: -18vh;
  background:
    radial-gradient(
      ellipse at 34% 42%,
      rgba(215, 194, 139, 0.34) 0%,
      rgba(215, 194, 139, 0.18) 24%,
      rgba(215, 194, 139, 0.08) 42%,
      rgba(215, 194, 139, 0) 74%
    );
  animation: curtainFlowA 32s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.curtain-b {
  width: 96vw;
  height: 86vh;
  right: -28vw;
  top: 2vh;
  background:
    radial-gradient(
      ellipse at 58% 48%,
      rgba(135, 191, 163, 0.3) 0%,
      rgba(135, 191, 163, 0.16) 28%,
      rgba(135, 191, 163, 0.08) 46%,
      rgba(135, 191, 163, 0) 76%
    );
  animation: curtainFlowB 36s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.curtain-c {
  width: 88vw;
  height: 76vh;
  left: 10vw;
  bottom: -26vh;
  background:
    radial-gradient(
      ellipse at 50% 50%,
      rgba(246, 243, 234, 0.2) 0%,
      rgba(246, 243, 234, 0.1) 26%,
      rgba(246, 243, 234, 0.04) 44%,
      rgba(246, 243, 234, 0) 78%
    );
  animation: curtainFlowC 40s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.bg-aurora {
  position: absolute;
  inset: auto;
  pointer-events: none;
  mix-blend-mode: screen;
  opacity: 0.18;
  filter: blur(42px);
}

.aurora-a {
  width: 72vw;
  height: 42vh;
  top: -10%;
  left: -12%;
  background:
    linear-gradient(
      120deg,
      rgba(215, 194, 139, 0) 0%,
      rgba(215, 194, 139, 0.22) 28%,
      rgba(135, 191, 163, 0.18) 56%,
      rgba(215, 194, 139, 0) 100%
    );
  transform: rotate(-10deg);
  animation: auroraSweepA 24s ease-in-out infinite;
}

.aurora-b {
  width: 68vw;
  height: 38vh;
  right: -18%;
  bottom: -8%;
  background:
    linear-gradient(
      135deg,
      rgba(135, 191, 163, 0) 0%,
      rgba(135, 191, 163, 0.22) 34%,
      rgba(246, 243, 234, 0.12) 62%,
      rgba(135, 191, 163, 0) 100%
    );
  transform: rotate(12deg);
  animation: auroraSweepB 28s ease-in-out infinite;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.9), transparent 85%);
  animation: gridFloat 28s linear infinite;
  opacity: 0.72;
}

.bg-wave {
  position: absolute;
  inset: auto;
  border-radius: 999px;
  filter: blur(44px);
  opacity: 0.26;
  pointer-events: none;
}

.wave-a {
  width: 720px;
  height: 720px;
  top: -240px;
  left: 8%;
  background: radial-gradient(circle, rgba(215, 194, 139, 0.2) 0%, rgba(215, 194, 139, 0) 68%);
  animation: waveDriftA 26s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.wave-b {
  width: 680px;
  height: 680px;
  right: -180px;
  bottom: -220px;
  background: radial-gradient(circle, rgba(135, 191, 163, 0.2) 0%, rgba(135, 191, 163, 0) 70%);
  animation: waveDriftB 30s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

.bg-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(14px);
  opacity: 0.7;
  will-change: transform;
}

.orb-a {
  width: 320px;
  height: 320px;
  top: -80px;
  right: 12%;
  background: rgba(135, 191, 163, 0.22);
  animation: orbFloatA 12s ease-in-out infinite;
}

.orb-b {
  width: 280px;
  height: 280px;
  left: -90px;
  bottom: 8%;
  background: rgba(215, 194, 139, 0.18);
  animation: orbFloatB 16s ease-in-out infinite;
}

.login-shell {
  min-height: 100vh;
  max-width: 1140px;
  margin: 0 auto;
  padding: 20px 24px 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) 420px;
  gap: 34px;
  align-items: center;
  position: relative;
  z-index: 1;
}

.brand-panel {
  color: var(--text-main);
  max-width: 640px;
  padding: 0 10px 0 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100%;
}

.brand-hero {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 18px;
}

.brand-glow {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(6px);
}

.glow-a {
  width: 188px;
  height: 188px;
  top: -30px;
  left: -20px;
  background: radial-gradient(circle, rgba(215, 194, 139, 0.36) 0%, rgba(215, 194, 139, 0) 72%);
}

.glow-b {
  width: 148px;
  height: 148px;
  top: 20px;
  left: 48px;
  background: radial-gradient(circle, rgba(135, 191, 163, 0.28) 0%, rgba(135, 191, 163, 0) 76%);
}

.brand-badge {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 150px;
  height: 150px;
  border-radius: 40px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0.08)),
    rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow:
    0 34px 68px rgba(4, 10, 16, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 0 0 14px rgba(215, 194, 139, 0.08);
  margin-bottom: 16px;
  backdrop-filter: blur(14px);
  animation: heroBreath 6s ease-in-out infinite;
}

.brand-badge-icon {
  width: 102px;
  height: 102px;
  display: block;
  filter:
    drop-shadow(0 18px 26px rgba(6, 14, 22, 0.26))
    drop-shadow(0 0 14px rgba(246, 243, 234, 0.18));
}

.brand-panel h1 {
  margin: 0;
  font-size: 54px;
  line-height: 1.04;
  letter-spacing: 0.02em;
}

.brand-copy {
  max-width: 560px;
  margin: 20px 0 0;
  font-size: 17px;
  line-height: 1.8;
  color: var(--text-soft);
}

.brand-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
}

.info-card {
  padding: 18px 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(8px);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.12);
}

.info-label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--brand-accent);
}

.info-card strong {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.7;
  color: var(--text-main);
}

.login-card {
  align-self: center;
  border: 1px solid var(--card-border);
  border-radius: 28px;
  overflow: hidden;
  background: rgba(251, 248, 241, 0.92);
  box-shadow: 0 24px 60px rgba(9, 16, 24, 0.28);
}

.card-head {
  margin-bottom: 24px;
}

.card-kicker {
  margin: 0 0 10px;
  color: #3f6b56;
  font-size: 12px;
  letter-spacing: 0.12em;
}

.card-head h2 {
  margin: 0;
  font-size: 30px;
  color: #13231a;
}

.card-copy {
  margin: 10px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(19, 35, 26, 0.08) inset;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 14px;
  margin-top: 6px;
  background: linear-gradient(135deg, #1b4332 0%, #2d6a4f 100%);
  font-size: 15px;
  font-weight: 600;
}

@keyframes curtainFlowA {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.04) rotate(-10deg);
    opacity: 0.5;
  }
  33% {
    transform: translate3d(9vw, 5vh, 0) scale(1.12) rotate(-6deg);
  }
  66% {
    transform: translate3d(16vw, 13vh, 0) scale(1.08) rotate(-2deg);
    opacity: 0.58;
  }
}

@keyframes curtainFlowB {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.02) rotate(12deg);
    opacity: 0.48;
  }
  50% {
    transform: translate3d(-18vw, -9vh, 0) scale(1.14) rotate(18deg);
    opacity: 0.56;
  }
}

@keyframes curtainFlowC {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.02) rotate(-4deg);
    opacity: 0.34;
  }
  50% {
    transform: translate3d(-10vw, -14vh, 0) scale(1.16) rotate(-8deg);
    opacity: 0.42;
  }
}

@keyframes orbFloatA {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-20px, 22px, 0) scale(1.06);
  }
}

@keyframes auroraSweepA {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(-10deg) scale(1);
    opacity: 0.16;
  }
  50% {
    transform: translate3d(6%, 8%, 0) rotate(-7deg) scale(1.12);
    opacity: 0.24;
  }
}

@keyframes auroraSweepB {
  0%,
  100% {
    transform: translate3d(0, 0, 0) rotate(12deg) scale(1);
    opacity: 0.15;
  }
  50% {
    transform: translate3d(-8%, -7%, 0) rotate(16deg) scale(1.14);
    opacity: 0.22;
  }
}

@keyframes orbFloatB {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(18px, -18px, 0) scale(1.08);
  }
}

@keyframes waveDriftA {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.04);
  }
  50% {
    transform: translate3d(44px, 34px, 0) scale(1.12);
  }
}

@keyframes waveDriftB {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1.03);
  }
  50% {
    transform: translate3d(-40px, -38px, 0) scale(1.1);
  }
}

@keyframes gridFloat {
  0% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 10px, 0);
  }
  100% {
    transform: translate3d(0, 0, 0);
  }
}

@keyframes heroBreath {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
    box-shadow:
      0 34px 68px rgba(4, 10, 16, 0.3),
      inset 0 1px 0 rgba(255, 255, 255, 0.24),
      0 0 0 14px rgba(215, 194, 139, 0.08);
  }
  50% {
    transform: translate3d(0, -4px, 0);
    box-shadow:
      0 42px 82px rgba(4, 10, 16, 0.34),
      inset 0 1px 0 rgba(255, 255, 255, 0.28),
      0 0 0 18px rgba(215, 194, 139, 0.1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .bg-curtain,
  .bg-aurora,
  .bg-grid,
  .bg-wave,
  .bg-orb,
  .brand-badge {
    animation: none !important;
  }
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    padding: 18px 18px 28px;
  }

  .brand-panel {
    padding-right: 0;
  }

  .brand-badge {
    width: 118px;
    height: 118px;
    border-radius: 30px;
  }

  .brand-badge-icon {
    width: 78px;
    height: 78px;
  }

  .brand-panel h1 {
    font-size: 36px;
  }

  .brand-cards {
    grid-template-columns: 1fr;
  }

  .login-card {
    max-width: 480px;
    width: 100%;
    margin: 0 auto;
  }
}
</style>
