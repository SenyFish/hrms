<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="4">
        <el-card shadow="hover" class="stat">
          <div class="stat-label">在职员工</div>
          <div class="stat-value">{{ summary?.employeeCount ?? "-" }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat">
          <div class="stat-label">待审批请假</div>
          <div class="stat-value warn">{{ summary?.pendingLeaveCount ?? "-" }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat">
          <div class="stat-label">今日考勤记录</div>
          <div class="stat-value">{{ summary?.todayAttendanceCount ?? "-" }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat">
          <div class="stat-label">今日已打卡</div>
          <div class="stat-value ok">{{ summary?.todayClockInCount ?? "-" }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat">
          <div class="stat-label">待执行关怀</div>
          <div class="stat-value care">{{ summary?.pendingCarePlanCount ?? "-" }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>近 7 日考勤趋势</template>
          <div ref="chartRef" style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>个人信息</template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="姓名">{{ profile?.realName }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ profile?.employeeNo }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ profile?.roleName }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="canViewCare" shadow="hover" class="care-card">
      <template #header>近期关怀提醒</template>
      <el-table :data="careReminders" border empty-text="未来 30 天暂无生日或入职周年提醒">
        <el-table-column prop="realName" label="员工" width="120" />
        <el-table-column prop="employeeNo" label="工号" width="120" />
        <el-table-column prop="careType" label="提醒类型" width="120" />
        <el-table-column prop="targetDate" label="日期" width="120" />
        <el-table-column prop="daysLeft" label="剩余天数" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">{{ row.planned ? "已生成计划" : "待生成" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :disabled="row.planned" @click="quickCreate(row)">一键生成计划</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { LineChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { init, use, type ECharts } from "echarts/core";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";

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
const profile = computed(() => store.profile);
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const canViewCare = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const careReminders = ref<CareReminder[]>([]);
let chartInstance: ECharts | null = null;

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
      xAxis: { type: "category", data: trend.labels },
      yAxis: { type: "value", name: "人次" },
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
  padding: 4px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin-top: 8px;
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

.care-card {
  margin-top: 16px;
}
</style>
