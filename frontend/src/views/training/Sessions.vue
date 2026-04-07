<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>组织培训</span>
        <UButton v-if="canManage" color="primary" icon="i-lucide-plus" @click="open()">新增</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput v-model="query.keyword" class="toolbar-input" size="lg" variant="subtle" icon="i-lucide-search" placeholder="请输入培训主题、讲师或状态" @keyup.enter="search" />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #departmentId-cell="{ row }">{{ row.original.departmentName || departmentName(row.original.departmentId) }}</template>
      <template #trainingTime-cell="{ row }">{{ formatDateTime(row.original.trainingTime) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div v-if="canManage" class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
        <span v-else>-</span>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu v-model="query.size" :items="pageSizeOptions" value-key="value" label-key="label" class="size-select" @update:model-value="handleSizeChange" />
    </div>

    <UModal v-model:open="visible" :title="form.id ? '编辑培训' : '新增培训'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">培训主题</label>
            <UInput v-model="form.title" />
          </div>
          <div class="field-block">
            <label class="field-label">讲师</label>
            <UInput v-model="form.trainerName" />
          </div>
          <div class="field-block">
            <label class="field-label">部门</label>
            <USelectMenu v-model="form.departmentId" :items="departments" value-key="id" label-key="name" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">培训时间</label>
            <UInput v-model="form.trainingTime" type="datetime-local" size="lg" variant="subtle" icon="i-lucide-calendar-clock" class="time-input" />
            <span class="field-hint">建议精确到分钟，便于培训排期。</span>
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="trainingStatusOptions" class="w-full" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">培训内容</label>
            <UTextarea v-model="form.content" :rows="3" />
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

type Department = { id: number; name: string };
type Row = { id?: number; title?: string; trainerName?: string; departmentId?: number; departmentName?: string; trainingTime?: string; status?: string; content?: string };
type PageData = { list: Row[]; total: number };

const toast = useToast();
const store = useUserStore();
const canManage = computed(() => {
  const roleCode = String(store.profile?.roleCode || "");
  return roleCode === "ADMIN" || roleCode === "HR";
});
const list = ref<Row[]>([]);
const total = ref(0);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const departments = ref<Department[]>([]);
const query = reactive({ keyword: "", page: 1, size: 10 });
const form = reactive<Row>({ id: undefined, title: "", trainerName: "", departmentId: undefined, trainingTime: "", status: "待开展", content: "" });

const pageSizeOptions = [{ label: "10 条/页", value: 10 }, { label: "20 条/页", value: 20 }, { label: "50 条/页", value: 50 }, { label: "100 条/页", value: 100 }];
const trainingStatusOptions = ["待开展", "进行中", "已完成", "已取消"];
const columns: TableColumn<Row>[] = [
  { accessorKey: "title", header: "培训主题" },
  { accessorKey: "trainerName", header: "讲师" },
  { accessorKey: "departmentId", header: "部门" },
  { accessorKey: "trainingTime", header: "培训时间" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "content", header: "培训内容" },
  { accessorKey: "actions", header: "操作" },
];

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/training/sessions", { params: query })) as PageData;
    list.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadDepartments() {
  if (!canManage.value) {
    departments.value = [];
    return;
  }
  departments.value = (await http.get("/departments")) as Department[];
}

function resetForm() {
  form.id = undefined;
  form.title = "";
  form.trainerName = "";
  form.departmentId = undefined;
  form.trainingTime = "";
  form.status = "待开展";
  form.content = "";
}

function open(row?: Row) {
  if (row) {
    Object.assign(form, row);
    form.trainingTime = row.trainingTime ? String(row.trainingTime).slice(0, 16) : "";
  } else resetForm();
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

function departmentName(id?: number) {
  return departments.value.find((item) => item.id === id)?.name || "-";
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  return value.replace("T", " ");
}

function statusColor(status?: string) {
  if (status === "已完成") return "success";
  if (status === "进行中") return "primary";
  if (status === "已取消") return "error";
  return "warning";
}

async function save() {
  if (!form.title || !form.trainingTime) {
    toast.add({ title: "请完善培训信息", color: "warning" });
    return;
  }
  const payload = { ...form, trainingTime: form.trainingTime ? `${form.trainingTime}:00` : "" };
  saving.value = true;
  try {
    if (form.id) await http.put(`/training/sessions/${form.id}`, payload);
    else await http.post("/training/sessions", payload);
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: Row) {
  const confirmed = window.confirm("确定删除这条培训记录吗？");
  if (!confirmed) return;
  await http.delete(`/training/sessions/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  if (list.value.length === 1 && query.page > 1) query.page -= 1;
  await load();
}

onMounted(async () => {
  await Promise.all([load(), loadDepartments()]);
});
</script>

<style scoped>
.header-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.toolbar-input { width: 320px; }
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
