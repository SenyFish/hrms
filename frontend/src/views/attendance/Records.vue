<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>考勤记录与统计</span>
        <div class="header-actions">
          <template v-if="isHr">
            <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" />
            <el-input
              v-model="query.keyword"
              placeholder="请输入员工或状态关键字"
              clearable
              style="width: 240px"
              @keyup.enter="search"
            />
            <el-button @click="search">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button type="success" @click="exportXlsx">导出月报表</el-button>
            <el-upload :show-file-list="false" accept=".csv" :http-request="doImport" style="display: inline-block">
              <el-button type="warning">导入 CSV</el-button>
            </el-upload>
          </template>
          <template v-else>
            <el-input
              v-model="query.keyword"
              placeholder="请输入状态关键字"
              clearable
              style="width: 220px"
              @keyup.enter="search"
            />
            <el-button @click="search">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button type="primary" @click="clock('IN')">上班打卡</el-button>
            <el-button type="success" @click="clock('OUT')">下班打卡</el-button>
          </template>
        </div>
      </div>
    </template>

    <el-table :data="rows" border>
      <el-table-column prop="user.realName" label="员工" v-if="isHr" />
      <el-table-column prop="attDate" label="日期" width="120" />
      <el-table-column label="上班" width="170">
        <template #default="{ row }">{{ fmt(row.clockIn) }}</template>
      </el-table-column>
      <el-table-column label="下班" width="170">
        <template #default="{ row }">{{ fmt(row.clockOut) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110" />
    </el-table>

    <div class="pager">
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
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, reactive } from "vue";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import { downloadByFetch } from "@/utils/download";

type AttendanceRow = Record<string, unknown>;
type AttendanceListResponse = {
  list: AttendanceRow[];
  total: number;
  page: number;
  size: number;
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

async function load() {
  const from = range.value?.[0];
  const to = range.value?.[1];
  const params = {
    from,
    to,
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
    await downloadByFetch(
      `/api/attendance/export?from=${from}&to=${to}`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      `attendance-${from}-${to}.xlsx`
    );
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
