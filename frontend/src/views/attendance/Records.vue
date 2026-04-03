<template>
  <div class="app-page">
    <PageSection
      eyebrow="Attendance"
      title="考勤记录与统计"
      :description="isHr ? '按时间范围查看全量打卡记录，并处理导入导出。' : '查看个人打卡记录并快速完成上下班打卡。'"
      flush
    >
      <FilterToolbar>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" />
        <el-input
          v-model="query.keyword"
          class="page-field"
          :placeholder="isHr ? '搜索员工或状态' : '搜索状态'"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>

        <template #actions>
          <template v-if="isHr">
            <el-button type="success" @click="exportXlsx">导出月报</el-button>
            <el-upload :show-file-list="false" accept=".csv" :http-request="doImport" style="display: inline-block">
              <el-button type="warning">导入 CSV</el-button>
            </el-upload>
          </template>
          <template v-else>
            <button class="frappe-button" data-variant="solid" type="button" @click="clock('IN')">上班打卡</button>
            <button class="frappe-button" data-variant="outline" type="button" @click="clock('OUT')">下班打卡</button>
          </template>
        </template>
      </FilterToolbar>

      <div class="app-table">
        <el-table :data="rows" border>
          <el-table-column v-if="isHr" prop="user.realName" label="员工" width="120" />
          <el-table-column prop="attDate" label="日期" width="130" />
          <el-table-column label="上班" width="180">
            <template #default="{ row }">{{ fmt(row.clockIn) }}</template>
          </el-table-column>
          <el-table-column label="下班" width="180">
            <template #default="{ row }">{{ fmt(row.clockOut) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <span class="pill-tag" :class="statusClass(String(row.status || ''))">{{ row.status }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { downloadByFetch } from "@/utils/download";
import FilterToolbar from "@/components/ui/FilterToolbar.vue";
import PageSection from "@/components/ui/PageSection.vue";

type AttendanceRow = Record<string, unknown>;
type AttendanceListResponse = {
  list: AttendanceRow[];
  total: number;
};

const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const isHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const range = ref<[string, string] | null>(null);
const rows = ref<AttendanceRow[]>([]);
const total = ref(0);
const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});

function fmt(v: unknown) {
  if (!v) return "";
  return new Date(v as string).toLocaleString("zh-CN");
}

function statusClass(status: string) {
  if (status.includes("正常")) return "pill-tag--success";
  if (status.includes("迟到") || status.includes("早退")) return "pill-tag--warning";
  if (status.includes("缺勤")) return "pill-tag--danger";
  return "pill-tag--neutral";
}

async function load() {
  const params = {
    from: range.value?.[0],
    to: range.value?.[1],
    keyword: query.keyword || undefined,
    page: query.page,
    size: query.size,
  };

  const data = isHr.value
    ? ((await http.get("/attendance/records", { params })) as AttendanceListResponse)
    : ((await http.get("/attendance/records/my", { params })) as AttendanceListResponse);

  rows.value = data.list || [];
  total.value = data.total || 0;
}

function search() {
  query.page = 1;
  load();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);
  range.value = [start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)];
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

async function clock(type: string) {
  await http.post("/attendance/clock", { type });
  ElMessage.success(type === "IN" ? "上班打卡成功" : "下班打卡成功");
  await load();
}

async function exportXlsx() {
  const from = range.value?.[0];
  const to = range.value?.[1];
  if (!from || !to) {
    ElMessage.warning("请选择日期范围");
    return;
  }
  try {
    await downloadByFetch(`/api/attendance/export?from=${from}&to=${to}`, { headers: { Authorization: `Bearer ${store.token}` } }, `attendance-${from}-${to}.xlsx`);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : "导出失败");
  }
}

async function doImport(opt: UploadRequestOptions) {
  const fd = new FormData();
  fd.append("file", opt.file as File);
  await fetch("/api/attendance/import", {
    method: "POST",
    headers: { Authorization: `Bearer ${store.token}` },
    body: fd,
  });
  ElMessage.success("导入完成");
  await load();
}

onMounted(() => {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);
  range.value = [start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)];
  load();
});
</script>

<style scoped>
.page-field {
  min-width: 220px;
}

.page-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
