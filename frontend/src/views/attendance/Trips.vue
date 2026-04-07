<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>{{ isAdminOrHr ? "出差申请审批" : "出差申请" }}</span>
        <div class="header-actions">
          <div v-if="isAdminOrHr" class="status-switch">
            <UButton
              v-for="item in filterOptions"
              :key="item.value || 'all'"
              :color="filter === item.value ? 'primary' : 'neutral'"
              :variant="filter === item.value ? 'solid' : 'soft'"
              size="sm"
              @click="switchFilter(item.value)"
            >
              {{ item.label }}
            </UButton>
          </div>
          <UButton v-if="!isAdminOrHr" color="primary" icon="i-lucide-plus" @click="openApply">新建申请</UButton>
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
        :placeholder="isAdminOrHr ? '请输入申请单号、员工、部门、出差类型、目的地或状态' : '请输入申请单号、部门、出差类型、目的地或状态'"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #estimatedDays-cell="{ row }">{{ amount(row.original.estimatedDays, 1) }} 天</template>
      <template #estimatedExpense-cell="{ row }">¥{{ amount(row.original.estimatedExpense) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #attachmentName-cell="{ row }">
        <UButton
          v-if="row.original.attachmentFileId"
          color="primary"
          variant="ghost"
          size="sm"
          :loading="downloadingId === row.original.id"
          @click="downloadAttachment(row.original)"
        >
          {{ row.original.attachmentName || "下载附件" }}
        </UButton>
        <span v-else>-</span>
      </template>
      <template #actions-cell="{ row }">
        <div v-if="isAdminOrHr" class="action-group">
          <UButton
            v-if="row.original.status === '待审批'"
            color="success"
            variant="ghost"
            size="sm"
            @click="approve(row.original, true)"
          >
            通过
          </UButton>
          <UButton
            v-if="row.original.status === '待审批'"
            color="error"
            variant="ghost"
            size="sm"
            @click="approve(row.original, false)"
          >
            驳回
          </UButton>
        </div>
        <span v-else class="readonly-text">{{ row.original.status || "-" }}</span>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu v-model="query.size" :items="pageSizeOptions" value-key="value" label-key="label" class="size-select" @update:model-value="handleSizeChange" />
    </div>

    <UModal v-model:open="applyVisible" title="新建出差申请">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">员工姓名</label>
            <UInput :model-value="selfInfo.realName" readonly />
          </div>
          <div class="field-block">
            <label class="field-label">工号</label>
            <UInput :model-value="selfInfo.employeeNo" readonly />
          </div>
          <div class="field-block">
            <label class="field-label">所属部门</label>
            <UInput :model-value="selfDepartmentName" readonly />
          </div>
          <div class="field-block">
            <label class="field-label">岗位</label>
            <UInput :model-value="selfInfo.positionName" readonly />
          </div>
          <div class="field-block">
            <label class="field-label">出差类型</label>
            <USelectMenu v-model="apply.tripType" :items="tripTypeOptions" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">预计出差天数</label>
            <UInputNumber v-model="apply.estimatedDays" :min="0.5" :step="0.5" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">目的地</label>
            <UInput v-model="apply.destination" placeholder="例如：上海市浦东新区张江高科 ×× 客户现场" />
          </div>
          <div class="field-block">
            <label class="field-label">预估费用</label>
            <UInputNumber v-model="apply.estimatedExpense" :min="0" :step="100" />
          </div>
          <div class="field-block">
            <label class="field-label">附件证明</label>
            <div class="upload-row">
              <UButton color="neutral" variant="soft" :loading="uploading" @click="triggerFilePick">上传附件</UButton>
              <span class="upload-name">{{ apply.attachmentName || "未上传" }}</span>
            </div>
            <span class="field-hint">支持上传邀请函、项目文件等证明材料。</span>
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">附件说明</label>
            <UTextarea v-model="apply.attachmentRemark" :rows="2" />
          </div>
        </div>
        <input ref="fileInput" type="file" class="hidden-input" @change="handleFileChange" />
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="applyVisible = false">取消</UButton>
          <UButton color="primary" :loading="submitting" @click="submitApply">提交申请</UButton>
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
import { downloadByFetch } from "@/utils/download";

type Department = { id: number; name: string };
type TripRow = {
  id?: number;
  serialNo?: string;
  employeeName?: string;
  employeeNo?: string;
  departmentName?: string;
  positionName?: string;
  tripType?: string;
  destination?: string;
  estimatedDays?: number | string;
  estimatedExpense?: number | string;
  attachmentFileId?: number;
  attachmentName?: string;
  attachmentRemark?: string;
  status?: string;
};
type PageData = { list: TripRow[]; total: number; page: number; size: number };
type UploadedFile = { id: number; originalName: string };

const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const isAdminOrHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const selfInfo = computed(() => ({
  id: Number(store.profile?.id || 0),
  realName: String(store.profile?.realName || store.profile?.username || ""),
  employeeNo: String(store.profile?.employeeNo || ""),
  positionName: String(store.profile?.positionName || ""),
  departmentId: Number(store.profile?.departmentId || 0),
}));
const selfDepartmentName = computed(() => departments.value.find((item) => item.id === selfInfo.value.departmentId)?.name || "");

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];
const filterOptions = [
  { label: "待审批", value: "待审批" },
  { label: "全部", value: "" },
];
const tripTypeOptions = ["客户拜访", "项目驻场", "外出培训", "会议", "其他"];

const columns: TableColumn<TripRow>[] = computed(() => {
  const base: TableColumn<TripRow>[] = [
    { accessorKey: "serialNo", header: "申请单号" },
    { accessorKey: "tripType", header: "出差类型" },
    { accessorKey: "destination", header: "目的地" },
    { accessorKey: "estimatedDays", header: "预计天数" },
    { accessorKey: "estimatedExpense", header: "预估费用" },
    { accessorKey: "attachmentName", header: "附件证明" },
    { accessorKey: "status", header: "状态" },
  ];
  if (isAdminOrHr.value) {
    base.splice(1, 0,
      { accessorKey: "employeeName", header: "员工" },
      { accessorKey: "employeeNo", header: "工号" },
      { accessorKey: "departmentName", header: "部门" }
    );
    base.push({ accessorKey: "actions", header: "操作" });
  } else {
    base.splice(1, 0, { accessorKey: "departmentName", header: "部门" });
  }
  return base;
}) as unknown as TableColumn<TripRow>[];

const rows = ref<TripRow[]>([]);
const departments = ref<Department[]>([]);
const total = ref(0);
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);
const downloadingId = ref<number | undefined>(undefined);
const applyVisible = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const filter = ref("待审批");
const query = reactive({ keyword: "", page: 1, size: 10 });
const apply = reactive({
  tripType: "客户拜访",
  destination: "",
  estimatedDays: 1,
  estimatedExpense: 0,
  attachmentFileId: undefined as number | undefined,
  attachmentName: "",
  attachmentRemark: "",
});

function amount(value?: number | string, digits = 2) {
  return Number(value || 0).toFixed(digits);
}

function statusColor(status?: string) {
  if (status === "已通过") return "success";
  if (status === "已驳回") return "error";
  return "warning";
}

async function load() {
  loading.value = true;
  try {
    const params = {
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
      status: isAdminOrHr.value ? filter.value || undefined : undefined,
    };
    const data = isAdminOrHr.value
      ? ((await http.get("/attendance/trips", { params })) as PageData)
      : ((await http.get("/attendance/trips/my", { params })) as PageData);
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadDepartments() {
  departments.value = (await http.get("/departments")) as Department[];
}

function switchFilter(value: string) {
  filter.value = value;
  search();
}

function search() {
  query.page = 1;
  load();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  if (isAdminOrHr.value) {
    filter.value = "待审批";
  }
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

function resetApply() {
  apply.tripType = "客户拜访";
  apply.destination = "";
  apply.estimatedDays = 1;
  apply.estimatedExpense = 0;
  apply.attachmentFileId = undefined;
  apply.attachmentName = "";
  apply.attachmentRemark = "";
}

function openApply() {
  resetApply();
  applyVisible.value = true;
}

function triggerFilePick() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);

  uploading.value = true;
  try {
    const response = await fetch("/api/files/upload", {
      method: "POST",
      headers: {
        Authorization: store.token ? `Bearer ${store.token}` : "",
      },
      body: formData,
    });
    if (!response.ok) {
      throw new Error((await response.text()) || "附件上传失败");
    }
    const result = await response.json();
    const fileData = result.data as UploadedFile;
    apply.attachmentFileId = fileData.id;
    apply.attachmentName = fileData.originalName;
    toast.add({ title: "附件上传成功", color: "success" });
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "附件上传失败", color: "error" });
  } finally {
    uploading.value = false;
    target.value = "";
  }
}

async function submitApply() {
  if (!apply.destination.trim()) {
    toast.add({ title: "请输入目的地", color: "warning" });
    return;
  }
  if (Number(apply.estimatedDays || 0) <= 0) {
    toast.add({ title: "预计出差天数必须大于 0", color: "warning" });
    return;
  }
  submitting.value = true;
  try {
    await http.post("/attendance/trips", {
      tripType: apply.tripType,
      destination: apply.destination,
      estimatedDays: apply.estimatedDays,
      estimatedExpense: apply.estimatedExpense,
      attachmentFileId: apply.attachmentFileId,
      attachmentName: apply.attachmentName,
      attachmentRemark: apply.attachmentRemark,
    });
    toast.add({ title: "出差申请已提交", color: "success" });
    applyVisible.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

async function approve(row: TripRow, ok: boolean) {
  await http.post(`/attendance/trips/${row.id}/approve`, { approved: ok, remark: ok ? "同意出差申请" : "不同意出差申请" });
  toast.add({ title: "已处理", color: "success" });
  await load();
}

async function downloadAttachment(row: TripRow) {
  if (!row.attachmentFileId) return;
  downloadingId.value = row.id;
  try {
    await downloadByFetch(`/api/files/${row.attachmentFileId}/download`, { headers: { Authorization: `Bearer ${store.token}` } }, row.attachmentName || "attachment");
  } finally {
    downloadingId.value = undefined;
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-switch {
  display: flex;
  gap: 8px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-input {
  width: 360px;
}

.table-wrap {
  overflow: hidden;
}

.action-group {
  display: flex;
  gap: 6px;
}

.readonly-text {
  color: #7b8a83;
  font-size: 13px;
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

.upload-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.upload-name {
  color: #426454;
  font-size: 13px;
}

.hidden-input {
  display: none;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .toolbar-input {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .pager {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
