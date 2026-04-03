<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">关怀计划总数</div>
          <div class="stat-value">{{ stats.totalPlans }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">待执行计划</div>
          <div class="stat-value warn">{{ stats.pendingPlans }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">已完成计划</div>
          <div class="stat-value ok">{{ stats.completedPlans }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">本月关怀记录</div>
          <div class="stat-value">{{ stats.monthRecords }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="summary-card">
      <template #header>关怀概览</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="关怀记录总数">{{ stats.totalRecords }}</el-descriptions-item>
        <el-descriptions-item label="计划完成率">{{ completionRate }}%</el-descriptions-item>
        <el-descriptions-item label="当前重点">{{ stats.pendingPlans > 0 ? '优先执行待执行计划' : '持续补充关怀记录' }}</el-descriptions-item>
        <el-descriptions-item label="建议动作">{{ stats.pendingPlans > 0 ? '安排负责人跟进员工关怀计划' : '梳理新的关怀名单与场景' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
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

.stat-card,
.summary-card {
  border-radius: 18px;
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
</style>
