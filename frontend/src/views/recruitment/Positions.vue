<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>{{ pageTitle }}</span>
        <UButton v-if="!isEmployee" color="primary" icon="i-lucide-plus" @click="open()">新增职位</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        :placeholder="isEmployee ? '请输入职位编号、名称或要求关键字' : '请输入职位编号、名称、要求关键字'"
        @keyup.enter="search"
      />
      <USelectMenu
        v-model="query.status"
        :items="statusOptions"
        value-key="value"
        label-key="label"
        class="status-select"
        :disabled="isEmployee"
        @update:model-value="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template v-if="!isEmployee" #requirement-cell="{ row }">{{ row.original.requirement?.requirementCode || "-" }}</template>
      <template #departmentId-cell="{ row }">{{ departmentName(row.original.departmentId) }}</template>
      <template #progress-cell="{ row }">{{ row.original.filledCount || 0 }}/{{ row.original.plannedCount || 0 }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <template v-if="isEmployee">
            <UButton color="primary" variant="ghost" size="sm" @click="goReferral(row.original)">发起内推</UButton>
          </template>
          <template v-else>
            <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
            <UButton color="success" variant="ghost" size="sm" @click="changeStatus(row.original, '招聘中')">发布</UButton>
            <UButton color="warning" variant="ghost" size="sm" @click="changeStatus(row.original, '已暂停')">暂停</UButton>
            <UButton color="error" variant="ghost" size="sm" @click="changeStatus(row.original, '已关闭')">关闭</UButton>
          </template>
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

    <UModal v-model:open="visible" :title="form.id ? '编辑招聘职位' : '新增招聘职位'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">职位名称</label>
            <UInput v-model="form.positionName" />
          </div>
          <div class="field-block">
            <label class="field-label">关联需求</label>
            <USelectMenu
              v-model="form.requirementId"
              :items="requirements"
              value-key="id"
              label-key="jobTitle"
              searchable
              class="w-full"
            />
          </div>
          <div class="field-block">
            <label class="field-label">所属部门</label>
            <USelectMenu
              v-model="form.departmentId"
              :items="departments"
              value-key="id"
              label-key="name"
              class="w-full"
            />
          </div>
          <div class="field-block">
            <label class="field-label">计划人数</label>
            <UInputNumber v-model="form.plannedCount" :min="1" />
          </div>
          <div class="field-block">
            <label class="field-label">已入职人数</label>
            <UInputNumber v-model="form.filledCount" :min="0" />
          </div>
          <div class="field-block">
            <label class="field-label">状态</label>
            <USelectMenu
              v-model="form.status"
              :items="statusOptions.slice(1)"
              value-key="value"
              label-key="label"
              class="w-full"
            />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">职位说明</label>
            <UTextarea v-model="form.jobDescription" :rows="3" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">任职要求</label>
            <UTextarea v-model="form.jobRequirements" :rows="3" />
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
import { useRouter } from "vue-router";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

type Department = { id: number; name: string };
type Requirement = { id: number; requirementCode: string; jobTitle: string };
type PositionRow = Record<string, any> & { id?: number };
type PageData = { list: PositionRow[]; total: number };

const router = useRouter();
const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const isEmployee = computed(() => roleCode.value === "EMP");
const pageTitle = computed(() => (isEmployee.value ? "内推职位" : "招聘职位"));

const rows = ref<PositionRow[]>([]);
const total = ref(0);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const departments = ref<Department[]>([]);
const requirements = ref<Requirement[]>([]);

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const statusOptions = [
  { label: "全部状态", value: "" },
  { label: "待发布", value: "待发布" },
  { label: "招聘中", value: "招聘中" },
  { label: "已暂停", value: "已暂停" },
  { label: "已招满", value: "已招满" },
  { label: "已关闭", value: "已关闭" },
];

const columns = computed<TableColumn<PositionRow>[]>(() => {
  const base: TableColumn<PositionRow>[] = [
    { accessorKey: "positionCode", header: "职位编号" },
    { accessorKey: "positionName", header: "职位名称" },
  ];
  if (!isEmployee.value) {
    base.push({ accessorKey: "requirement", header: "关联需求" });
  }
  base.push(
    { accessorKey: "departmentId", header: "所属部门" },
    { accessorKey: "progress", header: "招聘进度" },
    { accessorKey: "status", header: "状态" },
    { accessorKey: "jobRequirements", header: "岗位要求" },
    { accessorKey: "actions", header: "操作" }
  );
  return base;
});

const query = reactive({ keyword: "", status: "", page: 1, size: 10 });

const form = reactive({
  id: undefined as number | undefined,
  requirementId: undefined as number | undefined,
  positionName: "",
  departmentId: undefined as number | undefined,
  plannedCount: 1,
  filledCount: 0,
  jobDescription: "",
  jobRequirements: "",
  status: "待发布",
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.requirementId = undefined;
  form.positionName = "";
  form.departmentId = undefined;
  form.plannedCount = 1;
  form.filledCount = 0;
  form.jobDescription = "";
  form.jobRequirements = "";
  form.status = "待发布";
  form.remark = "";
}

function departmentName(id?: number) {
  return departments.value.find((item) => item.id === id)?.name || "-";
}

function statusColor(status?: string) {
  if (status === "招聘中") return "success";
  if (status === "已暂停") return "warning";
  if (status === "已关闭") return "error";
  if (status === "已招满") return "primary";
  return "neutral";
}

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/recruitment/positions", {
      params: {
        ...query,
        keyword: query.keyword || undefined,
        status: isEmployee.value ? undefined : query.status || undefined,
      },
    })) as PageData;
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  departments.value = (await http.get("/departments")) as Department[];
  if (!isEmployee.value) {
    const reqPage = (await http.get("/recruitment/requirements", { params: { page: 1, size: 200 } })) as { list: Requirement[] };
    requirements.value = (reqPage.list || []).map((item) => ({
      id: item.id,
      requirementCode: item.requirementCode,
      jobTitle: `${item.requirementCode} ${item.jobTitle}`,
    }));
  }
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

function open(row?: PositionRow) {
  if (row) {
    form.id = row.id as number;
    form.requirementId = row.requirement?.id;
    form.positionName = row.positionName || "";
    form.departmentId = row.departmentId;
    form.plannedCount = Number(row.plannedCount) || 1;
    form.filledCount = Number(row.filledCount) || 0;
    form.jobDescription = row.jobDescription || "";
    form.jobRequirements = row.jobRequirements || "";
    form.status = row.status || "待发布";
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.positionName.trim()) {
    toast.add({ title: "请输入职位名称", color: "warning" });
    return;
  }
  const payload = { ...form };
  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/recruitment/positions/${form.id}`, payload);
    } else {
      await http.post("/recruitment/positions", payload);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function changeStatus(row: PositionRow, status: string) {
  await http.post(`/recruitment/positions/${row.id}/status`, null, { params: { status } });
  toast.add({ title: "状态已更新", color: "success" });
  await load();
}

function goReferral(row: PositionRow) {
  router.push({ path: "/recruitment/referrals", query: { positionId: String(row.id || ""), action: "create" } });
}

onMounted(async () => {
  await Promise.all([load(), loadOptions()]);
});
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  flex-wrap: wrap;
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
