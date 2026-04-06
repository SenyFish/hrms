<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>关怀记录</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增记录</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        placeholder="请输入记录编号、员工姓名、类型或执行人"
        @keyup.enter="search"
      />
      <USelectMenu v-model="query.status" :items="statusOptions" class="status-select" @update:model-value="search" />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #user-cell="{ row }">{{ row.original.user?.realName || "-" }}</template>
      <template #plan-cell="{ row }">{{ row.original.plan?.planCode || "-" }}</template>
      <template #careTime-cell="{ row }">{{ formatDateTime(row.original.careTime) }}</template>
      <template #costAmount-cell="{ row }">¥{{ Number(row.original.costAmount || 0).toFixed(2) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="row.original.status === '已执行' ? 'success' : 'primary'" variant="soft">
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
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu v-model="query.size" :items="pageSizeOptions" value-key="value" label-key="label" class="size-select" @update:model-value="handleSizeChange" />
    </div>

    <UModal v-model:open="visible" :title="form.id ? '编辑关怀记录' : '新增关怀记录'">
      <template #body>
        <div class="form-grid">
          <div class="field-block field-block-full">
            <label class="field-label">关联计划</label>
            <USelectMenu v-model="form.planId" :items="planOptions" value-key="id" label-key="label" searchable class="w-full" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">关怀员工</label>
            <USelectMenu v-model="form.userId" :items="userOptions" value-key="id" label-key="label" searchable class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">关怀类型</label>
            <USelectMenu v-model="form.careType" :items="careTypeOptions" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">执行时间</label>
            <UInput v-model="form.careTime" type="datetime-local" size="lg" variant="subtle" icon="i-lucide-clock-3" class="time-input" />
            <span class="field-hint">记录本次关怀实际执行时间。</span>
          </div>
          <div class="field-block">
            <label class="field-label">执行人</label>
            <UInput v-model="form.handlerName" />
          </div>
          <div class="field-block">
            <label class="field-label">实际花费</label>
            <UInputNumber v-model="form.costAmount" :min="0" :step="100" />
          </div>
          <div class="field-block">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="recordStatusOptions" class="w-full" />
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

type UserOption = { id: number; realName: string; employeeNo: string };
type PlanOption = Record<string, any> & { id: number; planCode: string };
type RecordRow = Record<string, any> & { id?: number };
type PageData = { list: RecordRow[]; total: number };

const toast = useToast();
const rows = ref<RecordRow[]>([]);
const total = ref(0);
const users = ref<UserOption[]>([]);
const plans = ref<PlanOption[]>([]);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);

const query = reactive({ keyword: "", status: "", page: 1, size: 10 });
const form = reactive({
  id: undefined as number | undefined,
  planId: undefined as number | undefined,
  userId: undefined as number | undefined,
  careType: "日常关怀",
  careTime: "",
  handlerName: "",
  status: "已执行",
  content: "",
  remark: "",
  costAmount: 0,
});

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const statusOptions = ["", "已执行", "已跟进"];
const careTypeOptions = ["生日慰问", "节日关怀", "入职回访", "困难帮扶", "日常关怀"];
const recordStatusOptions = ["已执行", "已跟进"];

const columns: TableColumn<RecordRow>[] = [
  { accessorKey: "recordCode", header: "记录编号" },
  { accessorKey: "user", header: "关怀员工" },
  { accessorKey: "plan", header: "关联计划" },
  { accessorKey: "careType", header: "关怀类型" },
  { accessorKey: "careTime", header: "执行时间" },
  { accessorKey: "handlerName", header: "执行人" },
  { accessorKey: "costAmount", header: "实际花费" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "content", header: "关怀内容" },
  { accessorKey: "actions", header: "操作" },
];

const userOptions = computed(() => users.value.map((item) => ({ id: item.id, label: `${item.realName}（${item.employeeNo}）` })));
const planOptions = computed(() => plans.value.map((item) => ({ id: item.id, label: `${item.planCode} ${item.user?.realName || ""}` })));

function resetForm() {
  form.id = undefined;
  form.planId = undefined;
  form.userId = undefined;
  form.careType = "日常关怀";
  form.careTime = "";
  form.handlerName = "";
  form.status = "已执行";
  form.content = "";
  form.remark = "";
  form.costAmount = 0;
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
    const data = (await http.get("/care/records", { params: { ...query, keyword: query.keyword || undefined, status: query.status || undefined } })) as PageData;
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  const [userList, planPage] = await Promise.all([http.get("/users"), http.get("/care/plans", { params: { page: 1, size: 200 } })]);
  users.value = userList as UserOption[];
  plans.value = (planPage as { list: PlanOption[] }).list || [];
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

function open(row?: RecordRow) {
  if (row) {
    form.id = row.id as number;
    form.planId = row.plan?.id;
    form.userId = row.user?.id;
    form.careType = row.careType || "日常关怀";
    form.careTime = row.careTime ? String(row.careTime).slice(0, 16) : "";
    form.handlerName = row.handlerName || "";
    form.status = row.status || "已执行";
    form.content = row.content || "";
    form.remark = row.remark || "";
    form.costAmount = Number(row.costAmount) || 0;
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
  const payload = { ...form, careTime: form.careTime ? `${form.careTime}:00Z` : undefined };
  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/care/records/${form.id}`, payload);
    } else {
      await http.post("/care/records", payload);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await Promise.all([load(), loadOptions()]);
  } finally {
    saving.value = false;
  }
}

async function remove(row: RecordRow) {
  const confirmed = window.confirm("确定删除这条关怀记录吗？");
  if (!confirmed) return;
  await http.delete(`/care/records/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  await Promise.all([load(), loadOptions()]);
}

onMounted(async () => {
  await Promise.all([load(), loadOptions()]);
});
</script>

<style scoped>
.header-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.toolbar-input { width: 340px; }
.status-select { width: 140px; }
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
  .toolbar-input, .status-select { width: 100%; }
  .pager { flex-wrap: wrap; justify-content: flex-start; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
