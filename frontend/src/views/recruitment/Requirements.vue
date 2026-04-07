<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>招聘需求</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增需求</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        placeholder="请输入需求编号、岗位、申请人关键字"
        @keyup.enter="search"
      />
      <USelectMenu
        v-model="query.status"
        :items="statusOptions"
        value-key="value"
        label-key="label"
        class="status-select"
        @update:model-value="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #departmentId-cell="{ row }">{{ departmentName(row.original.departmentId) }}</template>
      <template #salary-cell="{ row }">{{ salaryRange(row.original) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton
            v-if="row.original.status === '待审批'"
            color="success"
            variant="ghost"
            size="sm"
            @click="approve(row.original, '已通过')"
          >
            通过
          </UButton>
          <UButton
            v-if="row.original.status === '待审批'"
            color="error"
            variant="ghost"
            size="sm"
            @click="approve(row.original, '已驳回')"
          >
            驳回
          </UButton>
          <UButton
            v-if="canGenerate(row.original.status)"
            color="warning"
            variant="ghost"
            size="sm"
            @click="generatePosition(row.original)"
          >
            生成职位
          </UButton>
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

    <UModal v-model:open="visible" :title="form.id ? '编辑招聘需求' : '新增招聘需求'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">岗位名称</label>
            <UInput v-model="form.jobTitle" />
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
            <label class="field-label">招聘人数</label>
            <UInputNumber v-model="form.headcount" :min="1" />
          </div>
          <div class="field-block">
            <label class="field-label">预算薪资</label>
            <div class="pair-row">
              <UInputNumber v-model="form.budgetSalaryMin" :min="0" :step="1000" />
              <UInputNumber v-model="form.budgetSalaryMax" :min="0" :step="1000" />
            </div>
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">期望到岗</label>
            <UInput v-model="form.expectedOnboardDate" type="date" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">岗位说明</label>
            <UTextarea v-model="form.jobDescription" :rows="3" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">申请原因</label>
            <UTextarea v-model="form.reason" :rows="3" />
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
import { onMounted, reactive, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";

type Department = { id: number; name: string };
type RequirementRow = Record<string, unknown> & { id?: number; status?: string };
type PageData = { list: RequirementRow[]; total: number };

const toast = useToast();
const rows = ref<RequirementRow[]>([]);
const total = ref(0);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const departments = ref<Department[]>([]);

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const statusOptions = [
  { label: "全部状态", value: "" },
  { label: "待审批", value: "待审批" },
  { label: "已通过", value: "已通过" },
  { label: "已驳回", value: "已驳回" },
  { label: "招聘中", value: "招聘中" },
  { label: "已完成", value: "已完成" },
];

const columns: TableColumn<RequirementRow>[] = [
  { accessorKey: "requirementCode", header: "需求编号" },
  { accessorKey: "jobTitle", header: "岗位名称" },
  { accessorKey: "departmentId", header: "所属部门" },
  { accessorKey: "headcount", header: "招聘人数" },
  { accessorKey: "salary", header: "预算薪资" },
  { accessorKey: "expectedOnboardDate", header: "期望到岗" },
  { accessorKey: "applicantName", header: "申请人" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "actions", header: "操作" },
];

const query = reactive({
  keyword: "",
  status: "",
  page: 1,
  size: 10,
});

const form = reactive({
  id: undefined as number | undefined,
  jobTitle: "",
  departmentId: undefined as number | undefined,
  headcount: 1,
  budgetSalaryMin: 0,
  budgetSalaryMax: 0,
  expectedOnboardDate: "",
  jobDescription: "",
  reason: "",
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.jobTitle = "";
  form.departmentId = undefined;
  form.headcount = 1;
  form.budgetSalaryMin = 0;
  form.budgetSalaryMax = 0;
  form.expectedOnboardDate = "";
  form.jobDescription = "";
  form.reason = "";
  form.remark = "";
}

function departmentName(id?: number) {
  return departments.value.find((item) => item.id === id)?.name || "-";
}

function salaryRange(row: RequirementRow) {
  return `¥${Number(row.budgetSalaryMin || 0)} - ¥${Number(row.budgetSalaryMax || 0)}`;
}

function canGenerate(status?: string) {
  return status === "已通过" || status === "招聘中" || status === "已完成";
}

function statusColor(status?: string) {
  if (status === "已通过" || status === "招聘中") return "success";
  if (status === "已驳回") return "error";
  if (status === "已完成") return "primary";
  return "warning";
}

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/recruitment/requirements", {
      params: {
        keyword: query.keyword || undefined,
        status: query.status || undefined,
        page: query.page,
        size: query.size,
      },
    })) as PageData;
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadDepartments() {
  departments.value = (await http.get("/departments")) as Department[];
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

function open(row?: RequirementRow) {
  if (row) {
    form.id = row.id as number;
    form.jobTitle = (row.jobTitle as string) || "";
    form.departmentId = row.departmentId as number;
    form.headcount = Number(row.headcount) || 1;
    form.budgetSalaryMin = Number(row.budgetSalaryMin) || 0;
    form.budgetSalaryMax = Number(row.budgetSalaryMax) || 0;
    form.expectedOnboardDate = (row.expectedOnboardDate as string) || "";
    form.jobDescription = (row.jobDescription as string) || "";
    form.reason = (row.reason as string) || "";
    form.remark = (row.remark as string) || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.jobTitle.trim()) {
    toast.add({ title: "请输入岗位名称", color: "warning" });
    return;
  }

  saving.value = true;
  try {
    const payload = { ...form };
    if (form.id) {
      await http.put(`/recruitment/requirements/${form.id}`, payload);
    } else {
      await http.post("/recruitment/requirements", payload);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function approve(row: RequirementRow, status: string) {
  await http.post(`/recruitment/requirements/${row.id}/approve`, null, { params: { status } });
  toast.add({ title: "处理成功", color: "success" });
  await load();
}

async function generatePosition(row: RequirementRow) {
  await http.post(`/recruitment/requirements/${row.id}/generate-position`);
  toast.add({ title: "已生成招聘职位", color: "success" });
  await load();
}

async function remove(row: RequirementRow) {
  const confirmed = window.confirm("确定删除该招聘需求吗？删除后，对应的招聘职位和候选人记录也会一并删除。");
  if (!confirmed) return;

  try {
    await http.delete(`/recruitment/requirements/${row.id}`);
    toast.add({ title: "已删除", color: "success" });
    if (rows.value.length === 1 && query.page > 1) {
      query.page -= 1;
    }
    await load();
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "删除失败",
      color: "error",
    });
  }
}

onMounted(async () => {
  await Promise.all([load(), loadDepartments()]);
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

.pair-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
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

  .form-grid,
  .pair-row {
    grid-template-columns: 1fr;
  }
}
</style>
