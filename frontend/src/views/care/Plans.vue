<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>关怀计划</span>
        <div class="actions">
          <UButton color="success" icon="i-lucide-download" :loading="exporting" @click="exportPlans">导出</UButton>
          <UButton color="primary" icon="i-lucide-plus" @click="open()">新增计划</UButton>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        placeholder="请输入计划编号、员工姓名或关怀类型"
        @keyup.enter="search"
      />
      <USelectMenu v-model="query.status" :items="statusOptions" class="status-select" @update:model-value="search" />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #user-cell="{ row }">{{ row.original.user?.realName || "-" }}</template>
      <template #employeeNo-cell="{ row }">{{ row.original.user?.employeeNo || "-" }}</template>
      <template #plannedTime-cell="{ row }">{{ formatDateTime(row.original.plannedTime) }}</template>
      <template #budgetAmount-cell="{ row }">¥{{ Number(row.original.budgetAmount || 0).toFixed(2) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="row.original.status === '已完成' ? 'success' : 'warning'" variant="soft">
          {{ row.original.status || "-" }}
        </UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination
        v-model:page="query.page"
        :total="total"
        :items-per-page="query.size"
        show-edges
        @update:page="load"
      />
      <USelectMenu
        v-model="query.size"
        :items="pageSizeOptions"
        value-key="value"
        label-key="label"
        class="size-select"
        @update:model-value="handleSizeChange"
      />
    </div>

    <UModal v-model:open="visible" :title="form.id ? '编辑关怀计划' : '新增关怀计划'">
      <template #body>
        <div class="form-grid">
          <div class="field-block field-block-full">
            <label class="field-label">关怀员工</label>
            <USelectMenu
              v-model="form.userId"
              :items="userOptions"
              value-key="id"
              label-key="label"
              searchable
              class="w-full"
            />
          </div>
          <div class="field-block">
            <label class="field-label">关怀类型</label>
            <USelectMenu v-model="form.careType" :items="careTypeOptions" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">计划时间</label>
            <UInput v-model="form.plannedTime" type="datetime-local" size="lg" variant="subtle" icon="i-lucide-calendar-heart" class="time-input" />
            <span class="field-hint">选择计划执行的具体时间。</span>
          </div>
          <div class="field-block">
            <label class="field-label">预算金额</label>
            <UInputNumber v-model="form.budgetAmount" :min="0" :step="100" />
          </div>
          <div class="field-block">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="['待执行', '已完成']" class="w-full" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">关怀内容</label>
            <UTextarea v-model="form.content" :rows="3" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">备注</label>
            <UTextarea v-model="form.remark" :rows="2" />
          </div>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="visible = false">取消</UButton>
          <UButton color="primary" :loading="saving" @click="save">保存</UButton>
        </div>
      </template>
    </UModal>
  </UCard>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { downloadByFetch } from "@/utils/download";
import { useUserStore } from "@/stores/user";

type UserOption = { id: number; realName: string; employeeNo: string };
type PlanRow = Record<string, any> & { id?: number };
type PageData = { list: PlanRow[]; total: number };

const toast = useToast();
const store = useUserStore();
const rows = ref<PlanRow[]>([]);
const total = ref(0);
const users = ref<UserOption[]>([]);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const exporting = ref(false);
const query = reactive({ keyword: "", status: "", page: 1, size: 10 });
const form = reactive({
  id: undefined as number | undefined,
  userId: undefined as number | undefined,
  careType: "日常关怀",
  plannedTime: "",
  status: "待执行",
  content: "",
  remark: "",
  budgetAmount: 0,
});

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const statusOptions = ["", "待执行", "已完成"];
const careTypeOptions = ["生日慰问", "节日关怀", "入职回访", "困难帮扶", "日常关怀"];

const columns: TableColumn<PlanRow>[] = [
  { accessorKey: "planCode", header: "计划编号" },
  { accessorKey: "user", header: "关怀员工" },
  { accessorKey: "employeeNo", header: "工号" },
  { accessorKey: "careType", header: "关怀类型" },
  { accessorKey: "plannedTime", header: "计划时间" },
  { accessorKey: "budgetAmount", header: "预算金额" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "content", header: "关怀内容" },
  { accessorKey: "actions", header: "操作" },
];

const userOptions = computed(() =>
  users.value.map((item) => ({
    id: item.id,
    label: `${item.realName}（${item.employeeNo}）`,
  }))
);

function resetForm() {
  form.id = undefined;
  form.userId = undefined;
  form.careType = "日常关怀";
  form.plannedTime = "";
  form.status = "待执行";
  form.content = "";
  form.remark = "";
  form.budgetAmount = 0;
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/care/plans", {
      params: { ...query, keyword: query.keyword || undefined, status: query.status || undefined },
    })) as PageData;
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadUsers() {
  users.value = (await http.get("/users")) as UserOption[];
}

function search() {
  query.page = 1;
  load();
}

function resetSearch() {
  query.keyword = "";
  query.status = "";
  query.page = 1;
  query.size = 10;
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

function open(row?: PlanRow) {
  if (row) {
    form.id = row.id as number;
    form.userId = row.user?.id;
    form.careType = row.careType || "日常关怀";
    form.plannedTime = row.plannedTime ? String(row.plannedTime).slice(0, 16) : "";
    form.status = row.status || "待执行";
    form.content = row.content || "";
    form.remark = row.remark || "";
    form.budgetAmount = Number(row.budgetAmount) || 0;
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.userId) {
    toast.add({ title: "请选择关怀员工", color: "warning" });
    return;
  }
  if (!form.content.trim()) {
    toast.add({ title: "请输入关怀内容", color: "warning" });
    return;
  }
  const payload = { ...form, plannedTime: form.plannedTime ? `${form.plannedTime}:00Z` : undefined };
  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/care/plans/${form.id}`, payload);
    } else {
      await http.post("/care/plans", payload);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: PlanRow) {
  const confirmed = window.confirm("确定删除这条关怀计划吗？");
  if (!confirmed) return;
  await http.delete(`/care/plans/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  await load();
}

async function exportPlans() {
  exporting.value = true;
  try {
    const params = new URLSearchParams();
    if (query.keyword) params.set("keyword", query.keyword);
    if (query.status) params.set("status", query.status);
    await downloadByFetch(
      `/api/care/plans/export?${params.toString()}`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      "care-plans.xlsx"
    );
    toast.add({ title: "导出已开始", color: "success" });
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : "导出失败",
      color: "error",
    });
  } finally {
    exporting.value = false;
  }
}

onMounted(async () => {
  await Promise.all([load(), loadUsers()]);
});
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.actions {
  display: flex;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-input {
  width: 320px;
}

.status-select {
  width: 140px;
}

.table-wrap {
  overflow: hidden;
}

.action-group {
  display: flex;
  gap: 6px;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block-full {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: #264334;
}

.field-hint {
  font-size: 12px;
  color: #7b8a83;
}

.time-input :deep(input) {
  min-height: 44px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .toolbar-input,
  .status-select {
    width: 100%;
  }

  .pager {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
