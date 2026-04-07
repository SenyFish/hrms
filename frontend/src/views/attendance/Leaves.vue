<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>请假审批</span>
        <div v-if="isHr" class="status-switch">
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
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        placeholder="请输入员工、请假类型、原因、状态关键字"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
      <UButton v-if="!isHr" color="primary" icon="i-lucide-plus" @click="applyVisible = true">发起请假</UButton>
    </div>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #user-cell="{ row }">{{ row.original.user?.realName || "-" }}</template>
      <template #startTime-cell="{ row }">{{ fmt(row.original.startTime) }}</template>
      <template #endTime-cell="{ row }">{{ fmt(row.original.endTime) }}</template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #actions-cell="{ row }">
        <div v-if="isHr" class="action-group">
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
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="load" />
      <USelectMenu
        v-model="query.size"
        :items="pageSizeOptions"
        value-key="value"
        label-key="label"
        class="size-select"
        @update:model-value="handleSizeChange"
      />
    </div>

    <UModal v-model:open="applyVisible" title="请假申请">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">类型</label>
            <UInput v-model="apply.leaveType" />
          </div>
          <div class="field-block">
            <label class="field-label">开始时间</label>
            <UInput v-model="apply.start" type="datetime-local" size="lg" variant="subtle" icon="i-lucide-clock-3" class="time-input" />
            <span class="field-hint">按实际请假开始时间选择。</span>
          </div>
          <div class="field-block">
            <label class="field-label">结束时间</label>
            <UInput v-model="apply.end" type="datetime-local" size="lg" variant="subtle" icon="i-lucide-clock-4" class="time-input" />
            <span class="field-hint">结束时间需晚于开始时间。</span>
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">原因</label>
            <UTextarea v-model="apply.reason" :rows="3" />
          </div>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="applyVisible = false">取消</UButton>
          <UButton color="primary" :loading="submitting" @click="submitApply">提交</UButton>
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

type LeaveRow = Record<string, unknown> & {
  id?: number;
  status?: string;
  user?: { realName?: string };
  leaveType?: string;
  startTime?: string;
  endTime?: string;
  reason?: string;
};
type LeaveListResponse = {
  list: LeaveRow[];
  total: number;
  page: number;
  size: number;
};

const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const isHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");

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

const columns: TableColumn<LeaveRow>[] = [
  { accessorKey: "user", header: "员工" },
  { accessorKey: "leaveType", header: "类型" },
  { accessorKey: "startTime", header: "开始时间" },
  { accessorKey: "endTime", header: "结束时间" },
  { accessorKey: "reason", header: "原因" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "actions", header: "操作" },
];

const filter = ref("待审批");
const rows = ref<LeaveRow[]>([]);
const total = ref(0);
const loading = ref(false);
const applyVisible = ref(false);
const submitting = ref(false);
const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});
const apply = reactive({
  leaveType: "事假",
  start: new Date().toISOString().slice(0, 16),
  end: new Date().toISOString().slice(0, 16),
  reason: "",
});

function fmt(v: unknown) {
  if (!v) return "";
  return new Date(v as string).toLocaleString("zh-CN");
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
    } as Record<string, string | number | undefined>;

    let data: LeaveListResponse;
    if (isHr.value) {
      params.status = filter.value || undefined;
      data = (await http.get("/leaves", { params })) as LeaveListResponse;
    } else {
      data = (await http.get("/leaves/my", { params })) as LeaveListResponse;
    }
    rows.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
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
  if (isHr.value) {
    filter.value = "待审批";
  }
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

async function approve(row: LeaveRow, ok: boolean) {
  await http.post(`/leaves/${row.id}/approve`, { approved: ok, remark: ok ? "同意" : "不同意" });
  toast.add({ title: "已处理", color: "success" });
  await load();
}

async function submitApply() {
  if (!apply.reason.trim()) {
    toast.add({ title: "请输入请假原因", color: "warning" });
    return;
  }
  submitting.value = true;
  try {
    await http.post("/leaves", {
      leaveType: apply.leaveType,
      startTime: new Date(apply.start).toISOString(),
      endTime: new Date(apply.end).toISOString(),
      reason: apply.reason,
    });
    toast.add({ title: "已提交", color: "success" });
    applyVisible.value = false;
    apply.reason = "";
    await load();
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
}

.toolbar-input {
  width: 320px;
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
  .toolbar {
    flex-wrap: wrap;
  }

  .toolbar-input {
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
