<template>
  <div class="page">
    <section class="stats-grid">
      <UCard
        v-for="item in statCards"
        :key="item.label"
        variant="soft"
        class="stat-card"
        @click="goToPage(item.path)"
      >
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value" :class="item.tone">{{ item.value }}</div>
      </UCard>
    </section>

    <section class="overview-grid">
      <UCard variant="soft" class="chart-card">
        <template #header>
          <div class="section-title">近 7 日考勤趋势</div>
        </template>
        <div ref="chartRef" class="chart-box" />
      </UCard>

      <UCard variant="soft">
        <template #header>
          <div class="section-head">
            <div class="section-title">个人信息</div>
            <UButton color="primary" variant="soft" size="sm" icon="i-lucide-user-pen" @click="openProfileEditor">编辑资料</UButton>
          </div>
        </template>

        <div class="profile-card">
          <div class="profile-line">
            <span>账号</span>
            <strong>{{ profile?.username || "-" }}</strong>
          </div>
          <div class="profile-line">
            <span>姓名</span>
            <strong>{{ profile?.realName || "-" }}</strong>
          </div>
          <div class="profile-line">
            <span>工号</span>
            <strong>{{ profile?.employeeNo || "-" }}</strong>
          </div>
          <div class="profile-line">
            <span>角色</span>
            <strong>{{ profile?.roleName || "-" }}</strong>
          </div>
          <div class="profile-line">
            <span>手机号</span>
            <strong>{{ profile?.phone || "-" }}</strong>
          </div>
          <div class="profile-line">
            <span>邮箱</span>
            <strong>{{ profile?.email || "-" }}</strong>
          </div>
        </div>
      </UCard>
    </section>

    <UModal v-model:open="profileVisible" title="编辑个人信息">
      <template #body>
        <div class="profile-form">
          <div class="field-block">
            <label class="field-label">账号</label>
            <UInput v-model="profileForm.username" />
          </div>
          <div class="field-block">
            <label class="field-label">姓名</label>
            <UInput v-model="profileForm.realName" />
          </div>
          <div class="field-block">
            <label class="field-label">新密码</label>
            <UInput v-model="profileForm.password" type="password" placeholder="不填写则保持原密码" />
          </div>
          <div class="field-block">
            <label class="field-label">手机号</label>
            <UInput v-model="profileForm.phone" />
          </div>
          <div class="field-block">
            <label class="field-label">邮箱</label>
            <UInput v-model="profileForm.email" />
          </div>
          <div class="field-block">
            <label class="field-label">生日</label>
            <UInput v-model="profileForm.birthday" type="date" size="lg" variant="subtle" icon="i-lucide-calendar-days" />
          </div>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="profileVisible = false">取消</UButton>
          <UButton color="primary" :loading="profileSaving" @click="saveProfile">保存</UButton>
        </div>
      </template>
    </UModal>

    <UCard v-if="canViewCare" variant="soft" class="care-card">
      <template #header>
        <div class="section-title">近期关怀提醒</div>
      </template>

      <UTable :data="careReminders" :columns="careColumns" :empty="'未来 30 天暂无生日或入职周年提醒'" class="care-table">
        <template #status-cell="{ row }">
          <UBadge :color="row.original.planned ? 'success' : 'warning'" variant="soft">
            {{ row.original.planned ? "已生成计划" : "待生成" }}
          </UBadge>
        </template>
        <template #actions-cell="{ row }">
          <UButton
            color="primary"
            variant="ghost"
            size="sm"
            :disabled="row.original.planned"
            @click="quickCreate(row.original)"
          >
            一键生成计划
          </UButton>
        </template>
      </UTable>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { init, use, type ECharts } from "echarts/core";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

type CareReminder = {
  userId: number;
  realName: string;
  employeeNo: string;
  careType: string;
  targetDate: string;
  daysLeft: number;
  planned: boolean;
};

type ProfileForm = {
  username: string;
  realName: string;
  password: string;
  phone: string;
  email: string;
  birthday: string;
};

const router = useRouter();
const store = useUserStore();
const toast = useToast();
const summary = ref<Record<string, number> | null>(null);
const chartRef = ref<HTMLElement | null>(null);
const profile = computed(() => store.profile as Record<string, string> | null);
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const canViewCare = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const careReminders = ref<CareReminder[]>([]);
const profileVisible = ref(false);
const profileSaving = ref(false);
const profileForm = reactive<ProfileForm>({
  username: "",
  realName: "",
  password: "",
  phone: "",
  email: "",
  birthday: "",
});
let chartInstance: ECharts | null = null;

const careColumns: TableColumn<CareReminder>[] = [
  { accessorKey: "realName", header: "员工" },
  { accessorKey: "employeeNo", header: "工号" },
  { accessorKey: "careType", header: "提醒类型" },
  { accessorKey: "targetDate", header: "日期" },
  { accessorKey: "daysLeft", header: "剩余天数" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "actions", header: "操作" },
];

const statCards = computed(() => {
  const common = [
    { label: "今日考勤记录", value: summary.value?.todayAttendanceCount ?? "-", path: "/attendance/records", tone: "" },
    { label: "今日已打卡", value: summary.value?.todayClockInCount ?? "-", path: "/attendance/records", tone: "ok" },
  ];

  if (roleCode.value === "ADMIN" || roleCode.value === "HR") {
    return [
      { label: "在职员工", value: summary.value?.employeeCount ?? "-", path: "/system/employees", tone: "" },
      { label: "待审批请假", value: summary.value?.pendingLeaveCount ?? "-", path: "/attendance/leaves", tone: "warn" },
      { label: "待审批出差", value: summary.value?.pendingTripCount ?? "-", path: "/attendance/trips", tone: "trip" },
      { label: "请假中员工", value: summary.value?.activeLeaveCount ?? "-", path: "/attendance/leaves", tone: "warn" },
      { label: "已通过出差", value: summary.value?.approvedTripCount ?? "-", path: "/attendance/trips", tone: "trip" },
      ...common,
      { label: "待执行关怀", value: summary.value?.pendingCarePlanCount ?? "-", path: "/care/plans", tone: "care" },
    ];
  }

  return [
    { label: "我的请假申请", value: summary.value?.myLeaveCount ?? "-", path: "/attendance/leaves", tone: "warn" },
    { label: "我的出差申请", value: summary.value?.myTripCount ?? "-", path: "/attendance/trips", tone: "trip" },
    { label: "待审批请假", value: summary.value?.myPendingLeaveCount ?? "-", path: "/attendance/leaves", tone: "warn" },
    { label: "待审批出差", value: summary.value?.myPendingTripCount ?? "-", path: "/attendance/trips", tone: "trip" },
    ...common,
  ];
});

function goToPage(path: string) {
  router.push(path);
}

function handleResize() {
  chartInstance?.resize();
}

async function openProfileEditor() {
  const userId = Number(store.profile?.id || 0);
  if (!userId) {
    toast.add({ title: "未获取到当前登录人信息", color: "error" });
    return;
  }
  try {
    const data = (await http.get(`/users/${userId}`)) as Record<string, unknown>;
    profileForm.username = String(data.username || "");
    profileForm.realName = String(data.realName || "");
    profileForm.password = "";
    profileForm.phone = String(data.phone || "");
    profileForm.email = String(data.email || "");
    profileForm.birthday = String(data.birthday || "");
    profileVisible.value = true;
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "加载个人信息失败", color: "error" });
  }
}

async function saveProfile() {
  if (!profileForm.username.trim()) {
    toast.add({ title: "请输入账号", color: "warning" });
    return;
  }
  if (!profileForm.realName.trim()) {
    toast.add({ title: "请输入姓名", color: "warning" });
    return;
  }
  profileSaving.value = true;
  try {
    await http.put("/users/profile", profileForm);
    await store.fetchProfile();
    profileVisible.value = false;
    toast.add({ title: "个人信息已更新", color: "success" });
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "保存失败", color: "error" });
  } finally {
    profileSaving.value = false;
  }
}

async function loadCareReminders() {
  if (!canViewCare.value) return;
  careReminders.value = (await http.get("/dashboard/care-reminders")) as CareReminder[];
}

async function quickCreate(row: CareReminder) {
  await http.post("/care/plans/quick-create", {
    userId: row.userId,
    careType: row.careType,
    targetDate: row.targetDate,
  });
  toast.add({ title: "关怀计划已生成", color: "success" });
  await loadCareReminders();
}

onMounted(async () => {
  summary.value = (await http.get("/dashboard/summary")) as Record<string, number>;
  const trend = (await http.get("/dashboard/attendance-trend")) as { labels: string[]; values: number[] };
  await loadCareReminders();
  if (chartRef.value) {
    chartInstance = init(chartRef.value);
    chartInstance.setOption({
      tooltip: { trigger: "axis" },
      xAxis: { type: "category", data: trend.labels },
      yAxis: {
        type: "value",
        name: "人次",
        minInterval: 1,
        axisLabel: {
          formatter: (value: number) => String(Math.trunc(value)),
        },
      },
      series: [{ type: "line", smooth: true, data: trend.values, areaStyle: {} }],
      color: ["#0ea5e9"],
    });
    window.addEventListener("resize", handleResize);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.stat-value.warn {
  color: #d97706;
}

.stat-value.ok {
  color: #059669;
}

.stat-value.care {
  color: #2563eb;
}

.stat-value.trip {
  color: #7c3aed;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #173127;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chart-box {
  height: 320px;
}

.profile-card {
  display: grid;
  gap: 12px;
}

.profile-line {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.54);
  border: 1px solid rgba(27, 67, 50, 0.08);
}

.profile-line span {
  color: #6b7280;
}

.profile-line strong {
  color: #13231a;
}

.profile-form {
  display: grid;
  gap: 14px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  color: #5b6b63;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.care-table {
  overflow: hidden;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
