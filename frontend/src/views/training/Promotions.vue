<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>规划员工晋升</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        placeholder="请输入员工姓名、当前岗位、目标岗位或状态"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">
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

    <UModal v-model:open="visible" :title="form.id ? '编辑晋升规划' : '新增晋升规划'">
      <template #body>
        <div class="form-grid">
          <div class="field-block field-block-full">
            <label class="field-label">员工</label>
            <USelectMenu
              v-model="form.employeeId"
              :items="employeeOptions"
              value-key="id"
              label-key="label"
              searchable
              class="w-full"
              @update:model-value="syncEmployee"
            />
            <span class="field-hint">仅可从最近一次绩效考核等级为 A 且状态为已确认或已归档的员工中选择。</span>
          </div>
          <div class="field-block">
            <label class="field-label">当前岗位</label>
            <UInput v-model="form.currentPosition" />
            <span class="field-hint">选择员工后会自动带入当前角色岗位，仍可手动调整。</span>
          </div>
          <div class="field-block">
            <label class="field-label">目标岗位</label>
            <UInput v-model="form.targetPosition" />
          </div>
          <div class="field-block">
            <label class="field-label">计划日期</label>
            <UInput
              v-model="form.plannedDate"
              type="date"
              size="lg"
              variant="subtle"
              icon="i-lucide-calendar-fold"
              class="time-input"
            />
            <span class="field-hint">用于安排评估和晋升落地节奏。</span>
          </div>
          <div class="field-block">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="promotionStatusOptions" class="w-full" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">备注</label>
            <UTextarea v-model="form.remark" :rows="3" />
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

type Employee = {
  id: number;
  realName: string;
  employeeNo?: string;
  currentPosition?: string;
  latestGrade?: string;
  latestAssessmentPeriod?: string;
};

type Row = {
  id?: number;
  employeeId?: number;
  employeeName?: string;
  currentPosition?: string;
  targetPosition?: string;
  plannedDate?: string;
  status?: string;
  remark?: string;
};

type PageData = {
  list: Row[];
  total: number;
};

const toast = useToast();
const list = ref<Row[]>([]);
const total = ref(0);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const employees = ref<Employee[]>([]);

const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});

const form = reactive<Row>({
  id: undefined,
  employeeId: undefined,
  employeeName: "",
  currentPosition: "",
  targetPosition: "",
  plannedDate: "",
  status: "待评估",
  remark: "",
});

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const promotionStatusOptions = ["待评估", "培养中", "已通过", "已落地"];

const employeeOptions = computed(() =>
  employees.value.map((item) => ({
    id: item.id,
    label: `${item.realName}${item.employeeNo ? `（${item.employeeNo}）` : ""} · ${item.currentPosition || "未设置岗位"} · 绩效${item.latestGrade || "A"}${item.latestAssessmentPeriod ? ` · ${item.latestAssessmentPeriod}` : ""}`,
  }))
);

const columns: TableColumn<Row>[] = [
  { accessorKey: "employeeName", header: "员工" },
  { accessorKey: "currentPosition", header: "当前岗位" },
  { accessorKey: "targetPosition", header: "目标岗位" },
  { accessorKey: "plannedDate", header: "计划日期" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "remark", header: "备注" },
  { accessorKey: "actions", header: "操作" },
];

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/training/promotions", { params: query })) as PageData;
    list.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadEmployees() {
  employees.value = (await http.get("/training/promotions/eligible-employees")) as Employee[];
}

function syncEmployee() {
  const employee = employees.value.find((item) => item.id === form.employeeId);
  form.employeeName = employee?.realName || "";
  form.currentPosition = employee?.currentPosition || "";
}

function resetForm() {
  form.id = undefined;
  form.employeeId = undefined;
  form.employeeName = "";
  form.currentPosition = "";
  form.targetPosition = "";
  form.plannedDate = "";
  form.status = "待评估";
  form.remark = "";
}

function open(row?: Row) {
  if (row) {
    Object.assign(form, row);
  } else {
    resetForm();
  }
  visible.value = true;
}

function search() {
  query.page = 1;
  load();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

function statusColor(status?: string) {
  if (status === "已通过" || status === "已落地") return "success";
  if (status === "培养中") return "primary";
  return "warning";
}

async function save() {
  syncEmployee();
  if (!form.employeeId || !form.employeeName || !form.targetPosition || !form.plannedDate) {
    toast.add({ title: "请完善晋升规划信息", color: "warning" });
    return;
  }
  const payload = { ...form };
  saving.value = true;
  try {
    if (form.id) await http.put(`/training/promotions/${form.id}`, payload);
    else await http.post("/training/promotions", payload);
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: Row) {
  const confirmed = window.confirm("确定删除这条晋升规划吗？");
  if (!confirmed) return;
  await http.delete(`/training/promotions/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  if (list.value.length === 1 && query.page > 1) query.page -= 1;
  await load();
}

onMounted(async () => {
  await Promise.all([load(), loadEmployees()]);
});
</script>

<style scoped>
.header-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.toolbar-input { width: 340px; }
.table-wrap { overflow: hidden; }
.action-group { display: flex; gap: 6px; }
.pager { margin-top: 16px; display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.pager-total { color: #6b7280; font-size: 14px; }
.size-select { width: 120px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.field-block { display: grid; gap: 8px; }
.field-block-full { grid-column: 1 / -1; }
.field-label { font-size: 14px; font-weight: 600; color: #264334; }
.field-hint { font-size: 12px; color: #7b8a83; }
.time-input :deep(input) { min-height: 44px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }

@media (max-width: 900px) {
  .toolbar-input { width: 100%; }
  .pager { flex-wrap: wrap; justify-content: flex-start; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
