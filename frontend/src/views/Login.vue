<template>
  <div class="login-view">
    <div class="login-view__ambient login-view__ambient--gold"></div>
    <div class="login-view__ambient login-view__ambient--green"></div>

    <section class="login-view__hero">
      <div class="login-view__badge">
        <img src="/favicon.svg" alt="HRMS" />
      </div>
      <p class="login-view__eyebrow">HRMS / Frappe UI Rebuild</p>
      <h1>让当前 Spring 后端进入一套更轻、更统一的工作台界面。</h1>
      <p class="login-view__copy">
        登录后继续使用现有接口、权限和业务流程，只替换前端视觉层与交互编排。当前版本优先覆盖登录、驾驶舱、主壳层和核心管理页。
      </p>

      <div class="login-view__stats">
        <article>
          <span>兼容策略</span>
          <strong>接口不变</strong>
        </article>
        <article>
          <span>设计方向</span>
          <strong>Frappe-style desktop console</strong>
        </article>
        <article>
          <span>优先模块</span>
          <strong>系统 / 权限 / 考勤 / 薪资</strong>
        </article>
      </div>
    </section>

    <section class="login-card">
      <div class="login-card__header">
        <p class="login-card__eyebrow">Account Access</p>
        <h2>登录系统</h2>
        <p>继续访问当前人力资源管理平台。</p>
      </div>

      <form class="login-card__form" @submit.prevent="onSubmit">
        <label class="login-card__field">
          <span>用户名</span>
          <input v-model="form.username" class="frappe-input__control" type="text" placeholder="请输入用户名" />
        </label>

        <label class="login-card__field">
          <span>密码</span>
          <input v-model="form.password" class="frappe-input__control" type="password" placeholder="请输入密码" />
        </label>

        <button class="frappe-button login-card__submit" data-variant="solid" type="submit" :disabled="loading">
          {{ loading ? "登录中..." : "登录" }}
        </button>
      </form>

      <div class="login-card__footer">
        <span>兼容当前认证方式和 Token 存储逻辑</span>
        <span>建议先使用管理员账号验证核心流程</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const store = useUserStore();
const loading = ref(false);
const form = reactive({
  username: "",
  password: "",
});

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
.login-view {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 420px);
  gap: 32px;
  align-items: center;
  padding: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(203, 173, 103, 0.22), transparent 24%),
    linear-gradient(135deg, #113427 0%, #1a4b39 52%, #f5efe4 165%);
}

.login-view__ambient {
  position: absolute;
  border-radius: 999px;
  filter: blur(48px);
  opacity: 0.5;
  pointer-events: none;
}

.login-view__ambient--gold {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -80px;
  background: rgba(203, 173, 103, 0.24);
}

.login-view__ambient--green {
  width: 420px;
  height: 420px;
  right: -90px;
  bottom: -80px;
  background: rgba(101, 166, 132, 0.24);
}

.login-view__hero,
.login-card {
  position: relative;
  z-index: 1;
}

.login-view__hero {
  max-width: 760px;
  color: #f9f3e6;
}

.login-view__badge {
  width: 116px;
  height: 116px;
  margin-bottom: 20px;
  border-radius: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.08)),
    rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow: 0 28px 54px rgba(4, 10, 16, 0.26);
}

.login-view__badge img {
  width: 72px;
  height: 72px;
}

.login-view__eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(249, 243, 230, 0.62);
}

.login-view__hero h1 {
  margin: 0;
  font-size: 54px;
  line-height: 1.06;
}

.login-view__copy {
  margin: 20px 0 0;
  max-width: 620px;
  color: rgba(249, 243, 230, 0.76);
  line-height: 1.8;
  font-size: 17px;
}

.login-view__stats {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.login-view__stats article {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.1);
}

.login-view__stats span {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  color: rgba(249, 243, 230, 0.58);
}

.login-view__stats strong {
  line-height: 1.6;
}

.login-card {
  padding: 28px;
  border-radius: 30px;
  background: rgba(255, 252, 246, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.34);
  box-shadow: 0 30px 70px rgba(10, 20, 16, 0.24);
}

.login-card__header h2 {
  margin: 0;
  font-size: 30px;
}

.login-card__header p:last-child {
  margin: 10px 0 0;
  color: var(--hr-text-soft);
}

.login-card__eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #6b7d73;
}

.login-card__form {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.login-card__field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.frappe-input__control {
  width: 100%;
  min-height: 44px;
  padding: 10px 14px;
  border: 1px solid rgba(25, 50, 39, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--hr-text);
  outline: none;
}

.frappe-input__control:focus {
  border-color: rgba(36, 85, 65, 0.36);
  box-shadow: 0 0 0 4px rgba(36, 85, 65, 0.08);
}

.login-card__field span {
  font-size: 13px;
  color: var(--hr-text-soft);
}

.login-card__submit {
  width: 100%;
  justify-content: center;
}

.login-card__footer {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--hr-text-soft);
  font-size: 12px;
}

@media (max-width: 1040px) {
  .login-view {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .login-view__hero h1 {
    font-size: 40px;
  }

  .login-view__stats {
    grid-template-columns: 1fr;
  }

  .login-card {
    max-width: 480px;
  }
}
</style>
