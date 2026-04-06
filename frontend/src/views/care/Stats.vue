<template>
  <div class="page">
    <div class="stats-grid">
      <UCard v-for="item in statCards" :key="item.label" variant="soft" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value" :class="item.className">{{ item.value }}</div>
      </UCard>
    </div>

    <UCard variant="soft" class="summary-card">
      <template #header>
        <div class="summary-title">关怀概览</div>
      </template>
      <div class="summary-grid">
        <div class="summary-item">
          <span class="summary-label">关怀记录总数</span>
          <strong>{{ stats.totalRecords }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">计划完成率</span>
          <strong>{{ completionRate }}%</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">当前重点</span>
          <strong>{{ stats.pendingPlans > 0 ? "优先执行待执行计划" : "持续补充关怀记录" }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">建议动作</span>
          <strong>{{ stats.pendingPlans > 0 ? "安排负责人跟进员工关怀计划" : "梳理新的关怀名单与场景" }}</strong>
        </div>
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from "vue";
import http from "@/api/http";

const stats = reactive({
  totalPlans: 0,
  totalRecords: 0,
  pendingPlans: 0,
  completedPlans: 0,
  monthRecords: 0,
});

const completionRate = computed(() => {
  if (!stats.totalPlans) return 0;
  return Math.round((stats.completedPlans / stats.totalPlans) * 100);
});

const statCards = computed(() => [
  { label: "关怀计划总数", value: stats.totalPlans, className: "" },
  { label: "待执行计划", value: stats.pendingPlans, className: "warn" },
  { label: "已完成计划", value: stats.completedPlans, className: "ok" },
  { label: "本月关怀记录", value: stats.monthRecords, className: "" },
]);

async function load() {
  const data = (await http.get("/care/records/stats")) as typeof stats;
  Object.assign(stats, data);
}

onMounted(load);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card,
.summary-card {
  border-radius: 18px;
}

.stat-label,
.summary-label {
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

.summary-title {
  font-weight: 600;
  color: #0f172a;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(38, 67, 52, 0.08);
}

@media (max-width: 900px) {
  .stats-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
