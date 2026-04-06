<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>考勤记录与统计</span>
        <div class="header-actions">
          <template v-if="isHr">
            <UInput v-model="rangeStart" type="date" />
            <span class="range-sep">至</span>
            <UInput v-model="rangeEnd" type="date" />
            <UInput
              v-model="query.keyword"
              class="toolbar-input"
              size="md"
              variant="subtle"
              icon="i-lucide-search"
              placeholder="请输入员工或状态关键字"
              @keyup.enter="search"
            />
            <UButton color="neutral" variant="soft" @click="search">查询</UButton>
            <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
            <UButton color="success" icon="i-lucide-file-spreadsheet" @click="exportXlsx">导出月报表</UButton>
            <label class="import-btn">
              <input type="file" accept=".csv" class="file-input" @change="onImportChange" />
              <UButton color="warning" icon="i-lucide-upload">导入 CSV</UButton>
            </label>
          </template>
          <template v-else>
            <UInput
              v-model="query.keyword"
              class="toolbar-input-sm"
              size="md"
              variant="subtle"
              icon="i-lucide-search"
              placeholder="请输入状态关键字"
              @keyup.enter="search"
            />
            <UButton color="neutral" variant="soft" @click="search">查询</UButton>
            <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
            <UButton color="primary" icon="i-lucide-log-in" @click="clock('IN')">上班打卡</UButton>
            <UButton color="success" icon="i-lucide-log-out" @click="clock('OUT')">下班打卡</UButton>
          </template>
        </div>
      </div>
    </template>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #user-cell="{ row }">{{ row.original.user?.realName || "-" }}</template>
      <template #clockIn-cell="{ row }">{{ fmt(row.original.clockIn) }}</template>
      <template #clockOut-cell="{ row }">{{ fmt(row.original.clockOut) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu
        v-model="query.size"
        :items="pageSizeOptions"
        value-key="value"
        label-key="label"
        class="size-select"
        @update:model-value="handleSizeChange"
      />
    </div>
  </UCard>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { downloadByFetch } from "@/utils/download";

type AttendanceRow = Record<string, unknown> & {
  user?: { realName?: string };
  attDate?: string;
  clockIn?: string;
  clockOut?: string;
  status?: string;
};
type AttendanceListResponse = {
  list: AttendanceRow[];
  total: number;
  page: number;
  size: number;
};

const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const isHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const columns = computed<TableColumn<AttendanceRow>[]>(() => {
  const base: TableColumn<AttendanceRow>[] = [];
  if (isHr.value) {
    base.push({ accessorKey: "user", header: "员工" });
  }
  base.push(
    { accessorKey: "attDate", header: "日期" },
    { accessorKey: "clockIn", header: "上班" },
    { accessorKey: "clockOut", header: "下班" },
    { accessorKey: "status", header: "状态" }
  );
  return base;
});

const rangeStart = ref("");
const rangeEnd = ref("");
const rows = ref<AttendanceRow[]>([]);
const total = ref(0);
const loading = ref(false);
const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});

function fmt(v: unknown) {
  if (!v) return "";
  return new Date(v as string).toLocaleString("zh-CN");
}

function statusColor(status?: string) {
  if (!status) return "neutral";
  if (status.includes("正常")) return "success";
  if (status.includes("迟到") || status.includes("早退")) return "warning";
  return "primary";
}

async function load() {
  loading.value = true;
  try {
    const params = {
      from: rangeStart.value || undefined,
      to: rangeEnd.value || undefined,
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    };

    const data = isHr.value
      ? ((await http.get("/attendance/records", { params })) as AttendanceListResponse)
      : ((await http.get("/attendance/records/my", { params })) as AttendanceListResponse);

    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
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
  rangeStart.value = start.toISOString().slice(0, 10);
  rangeEnd.value = end.toISOString().slice(0, 10);
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

async function clock(type: string) {
  await http.post("/attendance/clock", { type });
  toast.add({ title: type === "IN" ? "上班打卡成功" : "下班打卡成功", color: "success" });
  await load();
}

async function exportXlsx() {
  const from = rangeStart.value;
  const to = rangeEnd.value;
  if (!from || !to) {
    toast.add({ title: "请选择日期范围", color: "warning" });
    return;
  }
  try {
    await downloadByFetch(
      `/api/attendance/export?from=${from}&to=${to}`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      `attendance-${from}-${to}.xlsx`
    );
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "导出失败", color: "error" });
  }
}

async function onImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  const fd = new FormData();
  fd.append("file", file);
  await fetch("/api/attendance/import", {
    method: "POST",
    headers: { Authorization: `Bearer ${store.token}` },
    body: fd,
  });
  toast.add({ title: "导入完成", color: "success" });
  input.value = "";
  await load();
}

onMounted(() => {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);
  rangeStart.value = start.toISOString().slice(0, 10);
  rangeEnd.value = end.toISOString().slice(0, 10);
  load();
});
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-input {
  width: 240px;
}

.toolbar-input-sm {
  width: 220px;
}

.range-sep {
  color: #6b7280;
  font-size: 14px;
}

.import-btn {
  display: inline-flex;
}

.file-input {
  display: none;
}

.table-wrap {
  overflow: hidden;
}

.pager {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.pager-total {
  color: #6b7280;
  font-size: 14px;
}

.size-select {
  width: 120px;
}

@media (max-width: 1100px) {
  .header-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .pager {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
