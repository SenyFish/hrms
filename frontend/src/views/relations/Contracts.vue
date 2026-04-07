<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>{{ canEdit ? "合同管理" : "我的合同" }}</span>
        <UButton v-if="canEdit" color="primary" icon="i-lucide-plus" @click="open()">新增</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        :placeholder="canEdit ? '请输入合同编号、员工姓名、合同类型、合同名称或状态' : '请输入合同编号、合同类型、合同名称或状态'"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #reminderStatus-cell="{ row }">
        <UBadge :color="reminderColor(row.original.reminderStatus)" variant="soft">{{ row.original.reminderStatus || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div v-if="canEdit" class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
        <span v-else class="readonly-text">只读</span>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu v-model="query.size" :items="pageSizeOptions" value-key="value" label-key="label" class="size-select" @update:model-value="handleSizeChange" />
    </div>

    <UModal v-if="canEdit" v-model:open="visible" :title="form.id ? '编辑合同' : '新增合同'">
      <template #body>
        <div class="form-grid">
          <div class="field-block field-block-full">
            <label class="field-label">员工</label>
            <USelectMenu v-model="form.employeeId" :items="employees" value-key="id" label-key="realName" searchable class="w-full" @update:model-value="syncEmployee" />
          </div>
          <div class="field-block">
            <label class="field-label">合同类型</label>
            <USelectMenu v-model="form.contractType" :items="contractTypeOptions" class="w-full" @update:model-value="syncContractType" />
          </div>
          <div class="field-block">
            <label class="field-label">合同名称</label>
            <UInput v-model="form.contractTitle" placeholder="例如：2026版劳动合同、保密协议A版" />
          </div>
          <div class="field-block">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="contractStatusOptions" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">到期提醒（提前天数）</label>
            <UInputNumber v-model="form.reminderDays" :min="0" :step="1" />
            <span class="field-hint">例如填写 30，表示到期前 30 天进入续签提醒。</span>
          </div>
          <div class="field-block">
            <label class="field-label">开始日期</label>
            <UInput v-model="form.startDate" type="date" size="lg" variant="subtle" icon="i-lucide-calendar-range" class="time-input" />
            <span class="field-hint">合同生效日期。</span>
          </div>
          <div class="field-block">
            <label class="field-label">结束日期</label>
            <UInput v-model="form.endDate" type="date" size="lg" variant="subtle" icon="i-lucide-calendar-clock" class="time-input" />
            <span class="field-hint">合同到期或终止日期。</span>
          </div>
          <div class="field-block">
            <label class="field-label">提醒日期</label>
            <UInput :model-value="previewReminderDate" readonly />
            <span class="field-hint">根据结束日期和提醒天数自动计算。</span>
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
import { useUserStore } from "@/stores/user";

type Employee = { id: number; realName: string; hireDate?: string };
type Row = {
  id?: number;
  serialNo?: string;
  employeeId?: number;
  employeeName?: string;
  contractType?: string;
  contractTitle?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  reminderDays?: number;
  reminderDate?: string;
  reminderStatus?: string;
  remark?: string;
};
type PageData = { list: Row[]; total: number };

const toast = useToast();
const userStore = useUserStore();
const canEdit = computed(() => {
  const roleCode = userStore.profile?.roleCode as string | undefined;
  return roleCode === "ADMIN" || roleCode === "HR";
});

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
  contractType: "劳动合同",
  contractTitle: "",
  startDate: "",
  endDate: "",
  status: "生效中",
  reminderDays: 30,
  reminderDate: "",
  remark: "",
});

const pageSizeOptions = [{ label: "10 条/页", value: 10 }, { label: "20 条/页", value: 20 }, { label: "50 条/页", value: 50 }, { label: "100 条/页", value: 100 }];
const contractTypeOptions = ["劳动合同", "保密协议", "竞业限制", "实习协议", "劳务合同", "续签合同", "其他协议"];
const contractStatusOptions = ["生效中", "即将到期", "已到期", "已终止"];
const columns: TableColumn<Row>[] = computed(() => {
  const base: TableColumn<Row>[] = [
    { accessorKey: "serialNo", header: "合同编号" },
    { accessorKey: "contractType", header: "合同类型" },
    { accessorKey: "contractTitle", header: "合同名称" },
    { accessorKey: "startDate", header: "开始日期" },
    { accessorKey: "endDate", header: "结束日期" },
    { accessorKey: "reminderDate", header: "提醒日期" },
    { accessorKey: "reminderStatus", header: "提醒状态" },
    { accessorKey: "status", header: "状态" },
  ];
  if (canEdit.value) {
    base.splice(1, 0, { accessorKey: "employeeName", header: "员工" });
    base.push({ accessorKey: "actions", header: "操作" });
  }
  return base;
}) as unknown as TableColumn<Row>[];

const previewReminderDate = computed(() => {
  if (!form.endDate) return "";
  const days = Number(form.reminderDays ?? 30);
  const end = new Date(`${form.endDate}T00:00:00`);
  if (Number.isNaN(end.getTime())) return "";
  end.setDate(end.getDate() - Math.max(days, 0));
  return end.toISOString().slice(0, 10);
});

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/relations/contracts", { params: query })) as PageData;
    list.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadEmployees() {
  if (!canEdit.value) return;
  employees.value = (await http.get("/users")) as Employee[];
}

function syncEmployee() {
  const employee = employees.value.find((item) => item.id === form.employeeId);
  form.employeeName = employee?.realName || "";
  if (employee?.hireDate) {
    form.startDate = employee.hireDate;
  }
}

function syncContractType() {
  const employee = employees.value.find((item) => item.id === form.employeeId);
  if (employee?.hireDate) {
    form.startDate = employee.hireDate;
  }
}

function resetForm() {
  form.id = undefined;
  form.employeeId = undefined;
  form.employeeName = "";
  form.contractType = "劳动合同";
  form.contractTitle = "";
  form.startDate = "";
  form.endDate = "";
  form.status = "生效中";
  form.reminderDays = 30;
  form.reminderDate = "";
  form.remark = "";
}

function open(row?: Row) {
  if (!canEdit.value) return;
  if (row) Object.assign(form, row);
  else resetForm();
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
  if (status === "生效中") return "success";
  if (status === "即将到期") return "warning";
  if (status === "已到期" || status === "已终止") return "error";
  return "neutral";
}

function reminderColor(status?: string) {
  if (status === "提醒中") return "warning";
  if (status === "已过期") return "error";
  if (status === "未到提醒期") return "success";
  return "neutral";
}

async function save() {
  const employee = employees.value.find((item) => item.id === form.employeeId);
  form.employeeName = employee?.realName || form.employeeName || "";
  if (!form.employeeId || !form.employeeName || !form.startDate || !form.endDate) {
    toast.add({ title: "请完善合同信息", color: "warning" });
    return;
  }
  const payload = { ...form, reminderDate: previewReminderDate.value };
  saving.value = true;
  try {
    if (form.id) await http.put(`/relations/contracts/${form.id}`, payload);
    else await http.post("/relations/contracts", payload);
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: Row) {
  const confirmed = window.confirm("确定删除这条合同记录吗？");
  if (!confirmed) return;
  await http.delete(`/relations/contracts/${row.id}`);
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
.toolbar-input { width: 360px; }
.table-wrap { overflow: hidden; }
.action-group { display: flex; gap: 6px; }
.readonly-text { color: #7b8a83; font-size: 13px; }
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
