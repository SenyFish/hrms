<template>
  <div class="app-page">
    <div class="stat-grid">
      <StatPanel label="在职员工" :value="summary?.employeeCount ?? '-'" hint="员工档案">
        当前组织正式纳入系统的人员数量
      </StatPanel>
      <StatPanel label="待审批请假" :value="summary?.pendingLeaveCount ?? '-'" hint="待办">
        待 HR 或管理员处理的请假申请
      </StatPanel>
      <StatPanel label="今日考勤记录" :value="summary?.todayAttendanceCount ?? '-'" hint="实时">
        今天已生成的打卡记录总数
      </StatPanel>
      <StatPanel label="今日已打卡" :value="summary?.todayClockInCount ?? '-'" hint="上班">
        已完成上班或下班登记的员工
      </StatPanel>
      <StatPanel label="待执行关怀" :value="summary?.pendingCarePlanCount ?? '-'" hint="提醒">
        生日、周年等关怀任务待处理数量
      </StatPanel>
    </div>

    <div class="split-grid">
      <PageSection
        eyebrow="Attendance"
        title="近 7 日考勤趋势"
        description="保留当前后端统计接口，以更紧凑的桌面图表呈现近期波动。"
      >
        <div ref="chartRef" class="home-chart"></div>
      </PageSection>

      <PageSection
        eyebrow="Profile"
        title="当前用户"
        description="显示当前登录用户的身份、工号和角色信息。"
      >
        <div class="home-profile">
          <div class="home-profile__hero">
            <div class="home-profile__avatar">{{ initials }}</div>
            <div>
              <strong>{{ profile?.realName || profile?.username || "未命名用户" }}</strong>
              <p>{{ profile?.roleName || profile?.roleCode || "未分配角色" }}</p>
            </div>
          </div>

          <div class="info-list">
            <div class="info-list__item">
              <span class="info-list__label">工号</span>
              <strong>{{ profile?.employeeNo || "-" }}</strong>
            </div>
            <div class="info-list__item">
              <span class="info-list__label">账号</span>
              <strong>{{ profile?.username || "-" }}</strong>
            </div>
            <div class="info-list__item">
              <span class="info-list__label">角色编码</span>
              <strong>{{ profile?.roleCode || "-" }}</strong>
            </div>
          </div>
        </div>
      </PageSection>
    </div>

    <div class="split-grid home-secondary">
      <PageSection
        eyebrow="Quick Access"
        title="核心模块入口"
        description="优先跳转到系统、权限、考勤和薪资这些首轮重构覆盖的模块。"
      >
        <div class="home-links">
          <router-link v-for="link in quickLinks" :key="link.path" :to="link.path" class="home-link">
            <span class="home-link__title">{{ link.title }}</span>
            <small>{{ link.description }}</small>
          </router-link>
        </div>
      </PageSection>

      <PageSection
        v-if="canViewCare"
        eyebrow="Care"
        title="近期关怀提醒"
        description="保持当前后端提醒数据，集中呈现未来 30 天的员工关怀任务。"
        flush
      >
        <div v-if="careReminders.length" class="home-reminders">
          <article v-for="row in careReminders" :key="`${row.userId}-${row.careType}-${row.targetDate}`" class="home-reminder">
            <div>
              <strong>{{ row.realName }}</strong>
              <p>{{ row.employeeNo }} · {{ row.careType }} · {{ row.targetDate }}</p>
            </div>
            <div class="home-reminder__meta">
              <span class="pill-tag" :class="row.planned ? 'pill-tag--success' : 'pill-tag--warning'">
                {{ row.planned ? "已生成计划" : `${row.daysLeft} 天后` }}
              </span>
              <el-button type="primary" link :disabled="row.planned" @click="quickCreate(row)">一键生成</el-button>
            </div>
          </article>
        </div>
        <AppEmptyState v-else title="暂无近期关怀提醒" description="未来 30 天没有生日或入职周年提醒。" />
      </PageSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { init, use, type ECharts } from "echarts/core";
import { ElMessage } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import AppEmptyState from "@/components/ui/AppEmptyState.vue";
import PageSection from "@/components/ui/PageSection.vue";
import StatPanel from "@/components/ui/StatPanel.vue";

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

const store = useUserStore();
const summary = ref<Record<string, number> | null>(null);
const chartRef = ref<HTMLElement | null>(null);
const careReminders = ref<CareReminder[]>([]);
let chartInstance: ECharts | null = null;

const profile = computed(() => (store.profile || {}) as Record<string, string>);
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const canViewCare = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const initials = computed(() => {
  const source = String(profile.value.realName || profile.value.username || "HR");
  return source.slice(0, 2).toUpperCase();
});

const quickLinks = [
  { path: "/system/employees", title: "员工管理", description: "维护档案、角色和组织归属" },
  { path: "/permission/roles", title: "角色权限", description: "集中配置角色和菜单访问" },
  { path: "/attendance/records", title: "考勤记录", description: "查看打卡、导入导出和统计" },
  { path: "/salary/records", title: "薪资记录", description: "处理月度薪资与参保规则" },
];

function handleResize() {
  chartInstance?.resize();
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
  ElMessage.success("关怀计划已生成");
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
      grid: { top: 18, left: 18, right: 18, bottom: 18, containLabel: true },
      xAxis: {
        type: "category",
        data: trend.labels,
        axisLine: { lineStyle: { color: "rgba(25, 50, 39, 0.14)" } },
        axisLabel: { color: "#65756d" },
      },
      yAxis: {
        type: "value",
        name: "人次",
        nameTextStyle: { color: "#65756d" },
        splitLine: { lineStyle: { color: "rgba(25, 50, 39, 0.08)" } },
        axisLabel: { color: "#65756d" },
      },
      series: [
        {
          type: "line",
          smooth: true,
          data: trend.values,
          lineStyle: { width: 3, color: "#245541" },
          itemStyle: { color: "#cbad67" },
          areaStyle: {
            color: {
              type: "linear",
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: "rgba(36, 85, 65, 0.28)" },
                { offset: 1, color: "rgba(36, 85, 65, 0.04)" },
              ],
            },
          },
        },
      ],
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
.home-chart {
  height: 320px;
}

.home-profile {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.home-profile__hero {
  display: flex;
  align-items: center;
  gap: 14px;
}

.home-profile__avatar {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--hr-primary) 0%, var(--hr-primary-strong) 100%);
  color: #f7f2e7;
  font-size: 20px;
  font-weight: 700;
}

.home-profile__hero p {
  margin: 6px 0 0;
  color: var(--hr-text-soft);
}

.home-secondary {
  align-items: start;
}

.home-links {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.home-link {
  display: block;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.54));
  border: 1px solid rgba(24, 49, 38, 0.08);
  color: inherit;
  text-decoration: none;
  box-shadow: var(--hr-shadow-soft);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.home-link:hover {
  transform: translateY(-2px);
  box-shadow: var(--hr-shadow);
}

.home-link__title {
  display: block;
  font-size: 16px;
  font-weight: 700;
}

.home-link small {
  display: block;
  margin-top: 8px;
  color: var(--hr-text-soft);
  line-height: 1.6;
}

.home-reminders {
  display: flex;
  flex-direction: column;
}

.home-reminder {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid rgba(24, 49, 38, 0.08);
}

.home-reminder:first-child {
  padding-top: 0;
}

.home-reminder:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.home-reminder p {
  margin: 6px 0 0;
  color: var(--hr-text-soft);
}

.home-reminder__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 900px) {
  .home-links {
    grid-template-columns: 1fr;
  }

  .home-reminder {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
