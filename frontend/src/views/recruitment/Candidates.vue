<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>{{ pageLabel }}</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">{{ addButtonText }}</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        :placeholder="isEmployee ? '请输入姓名、手机号、邮箱或职位关键字' : '请输入姓名、手机号、邮箱、职位或内推人关键字'"
        @keyup.enter="search"
      />
      <USelectMenu v-model="query.status" :items="statusOptions" class="status-select" @update:model-value="search" />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #position-cell="{ row }">{{ row.original.position?.positionName || "-" }}</template>
      <template v-if="!isEmployee" #referrerName-cell="{ row }">{{ row.original.referrerName || "-" }}</template>
      <template #referralTime-cell="{ row }">{{ formatDateTime(row.original.referralTime || row.original.createdAt) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton
            color="error"
            variant="ghost"
            size="sm"
            :disabled="isEmployee && row.original.status === '已入职'"
            @click="remove(row.original)"
          >
            删除
          </UButton>
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

    <UModal v-model:open="visible" :title="dialogTitle">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">姓名</label>
            <UInput v-model="form.name" />
          </div>
          <div class="field-block">
            <label class="field-label">手机号</label>
            <UInput v-model="form.phone" />
          </div>
          <div class="field-block">
            <label class="field-label">邮箱</label>
            <UInput v-model="form.email" />
          </div>
          <div class="field-block">
            <label class="field-label">学历</label>
            <UInput v-model="form.education" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">应聘职位</label>
            <USelectMenu
              v-model="form.positionId"
              :items="positions"
              value-key="id"
              label-key="positionName"
              searchable
              class="w-full"
            />
          </div>
          <div class="field-block">
            <label class="field-label">期望薪资</label>
            <UInputNumber v-model="form.expectedSalary" :min="0" :step="1000" />
          </div>
          <div v-if="!isEmployee" class="field-block">
            <label class="field-label">来源渠道</label>
            <USelectMenu v-model="form.sourceChannel" :items="sourceOptions" class="w-full" />
          </div>
          <div v-if="!isEmployee" class="field-block">
            <label class="field-label">面试时间</label>
            <UInput v-model="form.interviewTime" type="datetime-local" size="lg" variant="subtle" icon="i-lucide-calendar-search" class="time-input" />
            <span class="field-hint">安排初试或复试时间。</span>
          </div>
          <div v-if="!isEmployee" class="field-block">
            <label class="field-label">面试官</label>
            <UInput v-model="form.interviewerName" />
          </div>
          <div v-if="!isEmployee" class="field-block">
            <label class="field-label">当前状态</label>
            <USelectMenu v-model="form.status" :items="statusOptions.filter((item) => item)" class="w-full" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">简历附件</label>
            <div class="upload-row">
              <UButton color="neutral" variant="soft" :loading="uploading" @click="triggerFilePick">上传附件</UButton>
              <UButton
                v-if="form.resumeFileId"
                color="primary"
                variant="ghost"
                :loading="downloadingResume"
                @click="downloadResume"
              >
                {{ form.resumeFileName || "下载附件" }}
              </UButton>
              <span v-else class="upload-name">未上传</span>
            </div>
            <span class="field-hint">支持上传候选人简历文件。</span>
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">备注</label>
            <UTextarea v-model="form.remark" :rows="2" />
          </div>
        </div>
        <input ref="fileInput" type="file" class="hidden-input" @change="handleFileChange" />
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
import { useRoute } from "vue-router";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { downloadByFetch } from "@/utils/download";

type Position = { id: number; positionCode: string; positionName: string; status?: string };
type CandidateRow = Record<string, any> & { id?: number };
type PageData = { list: CandidateRow[]; total: number };
type UploadedFile = { id: number; originalName: string };

const route = useRoute();
const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const isEmployee = computed(() => roleCode.value === "EMP");

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const statusOptions = ["", "待筛选", "待初试", "待复试", "待发Offer", "已发Offer", "已入职", "已淘汰"];
const sourceOptions = ["社会招聘", "校园招聘", "员工内推", "猎头推荐"];

const rows = ref<CandidateRow[]>([]);
const total = ref(0);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const downloadingResume = ref(false);
const positions = ref<Position[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const query = reactive({ keyword: "", status: "", page: 1, size: 10 });
const form = reactive({
  id: undefined as number | undefined,
  name: "",
  phone: "",
  email: "",
  education: "",
  sourceChannel: "社会招聘",
  positionId: undefined as number | undefined,
  expectedSalary: 0,
  interviewTime: "",
  interviewerName: "",
  status: "待筛选",
  result: "",
  resumeRemark: "",
  resumeFileId: undefined as number | undefined,
  resumeFileName: "",
  remark: "",
});

const pageLabel = computed(() => (isEmployee.value || route.path === "/recruitment/referrals" ? "内推记录" : "候选人管理"));
const addButtonText = computed(() => (isEmployee.value ? "新增内推" : "新增候选人"));
const dialogTitle = computed(() => {
  if (isEmployee.value) {
    return form.id ? "编辑内推记录" : "新增内推";
  }
  return form.id ? "编辑候选人" : "新增候选人";
});

const columns = computed<TableColumn<CandidateRow>[]>(() => {
  const base: TableColumn<CandidateRow>[] = [
    { accessorKey: "name", header: "姓名" },
    { accessorKey: "phone", header: "手机号" },
    { accessorKey: "email", header: "邮箱" },
    { accessorKey: "education", header: "学历" },
    { accessorKey: "position", header: "应聘职位" },
  ];
  if (!isEmployee.value) {
    base.push(
      { accessorKey: "sourceChannel", header: "来源渠道" },
      { accessorKey: "referrerName", header: "内推人" }
    );
  }
  base.push(
    { accessorKey: "referralTime", header: "内推时间" },
    { accessorKey: "status", header: "状态" },
    { accessorKey: "actions", header: "操作" }
  );
  return base;
});

function resetForm() {
  form.id = undefined;
  form.name = "";
  form.phone = "";
  form.email = "";
  form.education = "";
  form.sourceChannel = "社会招聘";
  form.positionId = undefined;
  form.expectedSalary = 0;
  form.interviewTime = "";
  form.interviewerName = "";
  form.status = "待筛选";
  form.result = "";
  form.resumeRemark = "";
  form.resumeFileId = undefined;
  form.resumeFileName = "";
  form.remark = "";
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function statusColor(status?: string) {
  if (status === "已入职" || status === "已发Offer") return "success";
  if (status === "已淘汰") return "error";
  if (status === "待复试" || status === "待发Offer") return "primary";
  return "warning";
}

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/recruitment/candidates", {
      params: {
        ...query,
        keyword: query.keyword || undefined,
        status: query.status || undefined,
      },
    })) as PageData;
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadPositions() {
  const data = (await http.get("/recruitment/positions", { params: { page: 1, size: 200 } })) as { list: Position[] };
  positions.value = data.list || [];
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

function open(row?: CandidateRow) {
  if (row) {
    form.id = row.id as number;
    form.name = row.name || "";
    form.phone = row.phone || "";
    form.email = row.email || "";
    form.education = row.education || "";
    form.sourceChannel = row.sourceChannel || "社会招聘";
    form.positionId = row.position?.id;
    form.expectedSalary = Number(row.expectedSalary) || 0;
    form.interviewTime = row.interviewTime ? String(row.interviewTime).slice(0, 16) : "";
    form.interviewerName = row.interviewerName || "";
    form.status = row.status || "待筛选";
    form.result = row.result || "";
    form.resumeRemark = row.resumeRemark || "";
    form.resumeFileId = row.resumeFileId || undefined;
    form.resumeFileName = row.resumeFileName || "";
    form.remark = row.remark || "";
  } else {
    resetForm();
    if (isEmployee.value) {
      form.sourceChannel = "员工内推";
    }
  }
  visible.value = true;
}

function openReferralFromRoute() {
  if (!isEmployee.value) return;
  if (route.query.action !== "create") return;
  resetForm();
  form.sourceChannel = "员工内推";
  const positionId = Number(route.query.positionId || 0);
  if (positionId > 0) {
    form.positionId = positionId;
  }
  visible.value = true;
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
      throw new Error((await response.text()) || "简历附件上传失败");
    }
    const result = await response.json();
    const fileData = result.data as UploadedFile;
    form.resumeFileId = fileData.id;
    form.resumeFileName = fileData.originalName;
    toast.add({ title: "简历附件上传成功", color: "success" });
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "简历附件上传失败", color: "error" });
  } finally {
    uploading.value = false;
    target.value = "";
  }
}

async function downloadResume() {
  if (!form.resumeFileId) return;
  downloadingResume.value = true;
  try {
    await downloadByFetch(
      `/api/files/${form.resumeFileId}/download`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      form.resumeFileName || "resume"
    );
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "简历附件下载失败", color: "error" });
  } finally {
    downloadingResume.value = false;
  }
}

async function save() {
  if (!form.name.trim()) {
    toast.add({ title: isEmployee.value ? "请输入内推人选姓名" : "请输入候选人姓名", color: "warning" });
    return;
  }
  if (!form.positionId) {
    toast.add({ title: "请选择应聘职位", color: "warning" });
    return;
  }
  const payload = {
    ...form,
    interviewTime: form.interviewTime ? `${form.interviewTime}:00Z` : undefined,
    sourceChannel: isEmployee.value ? undefined : form.sourceChannel,
    status: isEmployee.value ? undefined : form.status,
    result: isEmployee.value ? undefined : form.result,
    interviewerName: isEmployee.value ? undefined : form.interviewerName,
  };

  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/recruitment/candidates/${form.id}`, payload);
    } else {
      await http.post("/recruitment/candidates", payload);
    }
    toast.add({ title: isEmployee.value ? "内推提交成功" : "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: CandidateRow) {
  const confirmed = window.confirm(isEmployee.value ? "确定删除这条内推记录吗？" : "确定删除该候选人吗？");
  if (!confirmed) return;
  await http.delete(`/recruitment/candidates/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  await load();
}

onMounted(async () => {
  await Promise.all([load(), loadPositions()]);
  openReferralFromRoute();
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
  width: 340px;
}

.status-select {
  width: 160px;
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

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-name {
  color: #6b7280;
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

  .status-select {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
