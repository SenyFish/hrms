<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>{{ isAdminOrHr ? "处理纠纷" : "我的纠纷申请" }}</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">{{ isAdminOrHr ? "新增" : "申请" }}</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        :placeholder="isAdminOrHr ? '请输入纠纷编号、员工姓名、纠纷类型、状态或说明' : '请输入纠纷编号、纠纷类型、状态或说明'"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <template v-if="isAdminOrHr">
            <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
            <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
          </template>
          <template v-else>
            <span class="apply-tag">已提交申请</span>
          </template>
        </div>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu v-model="query.size" :items="pageSizeOptions" value-key="value" label-key="label" class="size-select" @update:model-value="handleSizeChange" />
    </div>

    <UModal v-model:open="visible" :title="form.id ? '编辑纠纷' : (isAdminOrHr ? '新增纠纷' : '申请纠纷')">
      <template #body>
        <div class="form-grid">
          <div v-if="isAdminOrHr" class="field-block field-block-full">
            <label class="field-label">员工</label>
            <USelectMenu v-model="form.employeeId" :items="employees" value-key="id" label-key="realName" searchable class="w-full" @update:model-value="syncEmployee" />
          </div>
          <div v-else class="field-block field-block-full readonly-card">
            <label class="field-label">员工</label>
            <UInput :model-value="selfName" readonly />
          </div>
          <div class="field-block">
            <label class="field-label">纠纷类型</label>
            <UInput v-model="form.disputeType" />
          </div>
          <div class="field-block">
            <label class="field-label">纠纷日期</label>
            <UInput v-model="form.disputeDate" type="date" size="lg" variant="subtle" icon="i-lucide-calendar-warning" class="time-input" />
            <span class="field-hint">记录纠纷实际发生日期。</span>
          </div>
          <div v-if="isAdminOrHr" class="field-block field-block-full">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="disputeStatusOptions" class="w-full" />
          </div>
          <div v-else class="field-block field-block-full readonly-card">
            <label class="field-label">状态</label>
            <UInput :model-value="form.status || '待处理'" readonly />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">纠纷说明</label>
            <UTextarea v-model="form.description" :rows="3" />
          </div>
          <div v-if="isAdminOrHr" class="field-block field-block-full">
            <label class="field-label">处理结果</label>
            <UTextarea v-model="form.resolution" :rows="3" />
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
import { useUserStore } from "@/stores/user";

type Employee = { id: number; realName: string };
type Row = {
  id?: number;
  serialNo?: string;
  employeeId?: number;
  employeeName?: string;
  disputeType?: string;
  disputeDate?: string;
  status?: string;
  description?: string;
  resolution?: string;
};
type PageData = { list: Row[]; total: number };

const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const isAdminOrHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const selfName = computed(() => String(store.profile?.realName || store.profile?.username || ""));
const list = ref<Row[]>([]);
const total = ref(0);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const employees = ref<Employee[]>([]);
const query = reactive({ keyword: "", page: 1, size: 10 });
const form = reactive<Row>({
  id: undefined,
  employeeId: undefined,
  employeeName: "",
  disputeType: "",
  disputeDate: "",
  status: "待处理",
  description: "",
  resolution: "",
});

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];
const disputeStatusOptions = ["待处理", "处理中", "已和解", "已结案"];
const columns: TableColumn<Row>[] = [
  { accessorKey: "serialNo", header: "纠纷编号" },
  { accessorKey: "employeeName", header: "员工" },
  { accessorKey: "disputeType", header: "纠纷类型" },
  { accessorKey: "disputeDate", header: "纠纷日期" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "description", header: "纠纷说明" },
  { accessorKey: "resolution", header: "处理结果" },
  { accessorKey: "actions", header: "操作" },
];

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/relations/disputes", { params: query })) as PageData;
    list.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadEmployees() {
  if (!isAdminOrHr.value) return;
  employees.value = (await http.get("/users")) as Employee[];
}

function syncEmployee() {
  const employee = employees.value.find((item) => item.id === form.employeeId);
  form.employeeName = employee?.realName || "";
}

function resetForm() {
  form.id = undefined;
  form.employeeId = isAdminOrHr.value ? undefined : Number(store.profile?.id || 0) || undefined;
  form.employeeName = isAdminOrHr.value ? "" : selfName.value;
  form.disputeType = "";
  form.disputeDate = "";
  form.status = "待处理";
  form.description = "";
  form.resolution = "";
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
  if (status === "已结案" || status === "已和解") return "success";
  if (status === "处理中") return "warning";
  return "error";
}

async function save() {
  if (isAdminOrHr.value) {
    syncEmployee();
  } else {
    form.employeeId = Number(store.profile?.id || 0) || undefined;
    form.employeeName = selfName.value;
    form.status = "待处理";
    form.resolution = "";
  }
  if (!form.employeeId || !form.employeeName || !form.disputeType || !form.disputeDate || !form.description) {
    toast.add({ title: "请完善纠纷信息", color: "warning" });
    return;
  }
  const payload = { ...form };
  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/relations/disputes/${form.id}`, payload);
    } else {
      await http.post("/relations/disputes", payload);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: Row) {
  const confirmed = window.confirm("确定删除这条纠纷记录吗？");
  if (!confirmed) return;
  await http.delete(`/relations/disputes/${row.id}`);
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
.action-group { display: flex; align-items: center; gap: 6px; }
.apply-tag { font-size: 12px; color: #6b7280; }
.pager { margin-top: 16px; display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.pager-total { color: #6b7280; font-size: 14px; }
.size-select { width: 120px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.field-block { display: grid; gap: 8px; }
.field-block-full { grid-column: 1 / -1; }
.field-label { font-size: 14px; font-weight: 600; color: #264334; }
.field-hint { font-size: 12px; color: #7b8a83; }
.readonly-card { opacity: 0.92; }
.time-input :deep(input) { min-height: 44px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 900px) {
  .toolbar-input { width: 100%; }
  .pager { flex-wrap: wrap; justify-content: flex-start; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
