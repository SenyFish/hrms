<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>绩效考核</span>
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
        placeholder="请输入员工姓名、考核周期、等级、状态或考核人"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #grade-cell="{ row }">
        <UBadge :color="gradeColor(row.original.grade)" variant="soft">{{ row.original.grade || "-" }}</UBadge>
      </template>
      <template #status-cell="{ row }">
        <UBadge :color="statusColor(row.original.status)" variant="soft">{{ row.original.status || "-" }}</UBadge>
      </template>
      <template #annualBonusLevel-cell="{ row }">
        <UBadge :color="bonusColor(row.original.annualBonusLevel)" variant="soft">
          {{ row.original.annualBonusLevel || "-" }}
        </UBadge>
      </template>
      <template #annualBonusAmountDisplay-cell="{ row }">{{ row.original.annualBonusAmountDisplay || "-" }}</template>
      <template #scoreTrend-cell="{ row }">{{ row.original.scoreTrend || "-" }}</template>
      <template #developmentSuggestion-cell="{ row }">
        <div class="suggestion-cell">
          <UButton
            v-if="row.original.developmentSuggestion"
            color="primary"
            variant="ghost"
            size="xs"
            @click="openSuggestion(row.original)"
          >
            展开查看
          </UButton>
          <span v-else class="suggestion-empty">-</span>
        </div>
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

    <UModal v-model:open="visible" :title="form.id ? '编辑绩效考核' : '新增绩效考核'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">员工</label>
            <USelectMenu
              v-model="form.employeeId"
              :items="employees"
              value-key="id"
              label-key="realName"
              searchable
              class="w-full"
              @update:model-value="syncEmployee"
            />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">考核周期</label>
            <div class="period-grid">
              <USelectMenu v-model="periodForm.type" :items="periodTypeOptions" value-key="value" label-key="label" class="w-full" />
              <USelectMenu v-model="periodForm.year" :items="yearOptions" class="w-full" />
              <USelectMenu
                v-if="periodSlotOptions.length"
                v-model="periodForm.slot"
                :items="periodSlotOptions"
                value-key="value"
                label-key="label"
                class="w-full"
              />
            </div>
            <UInput :model-value="form.assessmentPeriod" disabled />
          </div>
          <div class="field-block">
            <label class="field-label">分数</label>
            <UInputNumber v-model="form.score" :min="0" :max="100" :step="1" />
          </div>
          <div class="field-block">
            <label class="field-label">等级</label>
            <UInput :model-value="preview.grade" disabled />
          </div>
          <div class="field-block">
            <label class="field-label">状态</label>
            <USelectMenu v-model="form.status" :items="statusOptions" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">考核人</label>
            <USelectMenu v-model="form.evaluatorName" :items="evaluatorOptions" class="w-full" />
          </div>
          <div class="field-block field-block-full">
            <div class="field-label-row">
              <label class="field-label">自动判断结果</label>
              <UBadge v-if="previewLoading" color="neutral" variant="soft">计算中</UBadge>
            </div>
            <div class="stats-grid">
              <div class="stat-card">
                <span class="stat-label">年终奖档位</span>
                <strong>{{ preview.annualBonusLevel }}</strong>
              </div>
              <div class="stat-card">
                <span class="stat-label">预计年终奖</span>
                <strong>¥{{ formatAmount(preview.annualBonusAmount) }}</strong>
              </div>
              <div class="stat-card">
                <span class="stat-label">分数趋势</span>
                <strong>{{ preview.scoreTrend }}</strong>
              </div>
              <div class="stat-card stat-card-full">
                <span class="stat-label">发展建议</span>
                <strong>{{ preview.developmentSuggestion }}</strong>
              </div>
            </div>
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

    <UModal v-model:open="suggestionVisible" title="发展建议">
      <template #body>
        <div class="suggestion-detail">
          <div class="suggestion-meta">
            <span>{{ suggestionRow?.employeeName || "-" }}</span>
            <UBadge color="primary" variant="soft">{{ suggestionRow?.assessmentPeriod || "-" }}</UBadge>
          </div>
          <p class="suggestion-detail-text">{{ suggestionRow?.developmentSuggestion || "-" }}</p>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="primary" @click="suggestionVisible = false">关闭</UButton>
        </div>
      </template>
    </UModal>
  </UCard>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";

type Employee = { id: number; realName: string };
type Row = {
  id?: number;
  employeeId?: number;
  employeeName?: string;
  assessmentPeriod?: string;
  score?: number;
  grade?: string;
  status?: string;
  evaluatorName?: string;
  remark?: string;
  annualBonusAmount?: number;
  annualBonusAmountDisplay?: string;
  annualBonusLevel?: string;
  scoreTrend?: string;
  developmentSuggestion?: string;
};
type PageData = { list: Row[]; total: number };
type PreviewData = {
  grade: string;
  annualBonusAmount: number;
  annualBonusLevel: string;
  scoreTrend: string;
  developmentSuggestion: string;
};
type PeriodType = "MONTHLY" | "QUARTERLY" | "HALF_YEAR" | "YEARLY";
type OptionItem = { label: string; value: string };

const toast = useToast();
const list = ref<Row[]>([]);
const total = ref(0);
const visible = ref(false);
const suggestionVisible = ref(false);
const loading = ref(false);
const saving = ref(false);
const employees = ref<Employee[]>([]);
const suggestionRow = ref<Row | null>(null);
const query = reactive({ keyword: "", page: 1, size: 10 });
const previewLoading = ref(false);
const currentYear = new Date().getFullYear();
const form = reactive<Row>({
  id: undefined,
  employeeId: undefined,
  employeeName: "",
  assessmentPeriod: "",
  score: 0,
  grade: "B",
  status: "待确认",
  evaluatorName: "",
  remark: "",
});
const periodForm = reactive({
  type: "QUARTERLY" as PeriodType,
  year: String(currentYear),
  slot: "Q1",
});

const previewFallback: PreviewData = {
  grade: "D",
  annualBonusAmount: 0,
  annualBonusLevel: "无年终奖",
  scoreTrend: "单次考核",
  developmentSuggestion: "当前绩效偏低，建议先辅导改进并评估调岗风险",
};

const previewData = ref<PreviewData>({ ...previewFallback });

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const statusOptions = ["待确认", "已确认", "已归档"];
const evaluatorOptions = ["王人事"];
const periodTypeOptions = [
  { label: "月度", value: "MONTHLY" },
  { label: "季度", value: "QUARTERLY" },
  { label: "半年度", value: "HALF_YEAR" },
  { label: "年度", value: "YEARLY" },
];
const yearOptions = Array.from({ length: 7 }, (_, index) => String(currentYear - 3 + index));

const periodSlotOptions = computed<OptionItem[]>(() => {
  if (periodForm.type === "MONTHLY") {
    return Array.from({ length: 12 }, (_, index) => {
      const month = String(index + 1).padStart(2, "0");
      return { label: `${index + 1} 月`, value: month };
    });
  }
  if (periodForm.type === "QUARTERLY") {
    return [
      { label: "第一季度", value: "Q1" },
      { label: "第二季度", value: "Q2" },
      { label: "第三季度", value: "Q3" },
      { label: "第四季度", value: "Q4" },
    ];
  }
  if (periodForm.type === "HALF_YEAR") {
    return [
      { label: "上半年", value: "H1" },
      { label: "下半年", value: "H2" },
    ];
  }
  return [];
});

const columns: TableColumn<Row>[] = [
  { accessorKey: "employeeName", header: "员工" },
  { accessorKey: "assessmentPeriod", header: "考核周期" },
  { accessorKey: "score", header: "分数" },
  { accessorKey: "grade", header: "等级" },
  { accessorKey: "annualBonusLevel", header: "年终奖档位" },
  { accessorKey: "annualBonusAmountDisplay", header: "年终奖金额" },
  { accessorKey: "scoreTrend", header: "分数趋势" },
  { accessorKey: "developmentSuggestion", header: "发展建议" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "evaluatorName", header: "考核人" },
  { accessorKey: "actions", header: "操作" },
];

const localPreview = computed<PreviewData>(() => {
  const score = Number(form.score || 0);
  const employeeHistory = list.value
    .filter((item) => item.employeeId === form.employeeId && item.id !== form.id)
    .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
    .slice(0, 2);
  const recentScores = [score, ...employeeHistory.map((item) => Number(item.score || 0))];

  const grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : "D";
  const annualBonusLevel =
    score >= 95 ? "特优年终奖" :
    score >= 90 ? "优秀年终奖" :
    score >= 80 ? "良好年终奖" :
    score >= 70 ? "达标年终奖" :
    score >= 60 ? "基础年终奖" : "无年终奖";

  const multiplier =
    score >= 95 ? 3 :
    score >= 90 ? 2.5 :
    score >= 85 ? 2 :
    score >= 80 ? 1.5 :
    score >= 70 ? 1 :
    score >= 60 ? 0.5 : 0;

  const estimatedBaseSalary = 8000;
  const scoreTrend =
    employeeHistory.length === 0 ? "单次考核" :
    score > Number(employeeHistory[0]?.score || 0) ? "持续提升" :
    score < Number(employeeHistory[0]?.score || 0) ? "有所下滑" : "基本持平";

  const developmentSuggestion =
    recentScores.length >= 3 && recentScores.every((item) => item >= 90) ? "连续高分，建议优先升职、加薪" :
    recentScores.length >= 3 && recentScores.every((item) => item < 60) ? "长期低分，建议调岗、降级" :
    score >= 90 ? "本期表现突出，建议优先纳入调薪和晋升观察名单" :
    score >= 80 ? "整体表现良好，建议持续培养并结合岗位评估加薪" :
    score >= 60 ? "绩效达标但仍有提升空间，建议制定专项提升计划" :
    "当前绩效偏低，建议先辅导改进并评估调岗风险";

  return {
    grade,
    annualBonusLevel,
    annualBonusAmount: Number((estimatedBaseSalary * multiplier).toFixed(2)),
    scoreTrend,
    developmentSuggestion,
  };
});

const preview = computed<PreviewData>(() => {
  if (!form.employeeId) {
    return localPreview.value;
  }
  return previewData.value;
});

function buildRowPreview(row: Row, source: Row[]) {
  const score = Number(row.score || 0);
  const history = source
    .filter((item) => item.employeeId === row.employeeId && item.id !== row.id)
    .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
    .slice(0, 2);
  const recentScores = [score, ...history.map((item) => Number(item.score || 0))];

  const grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : "D";
  const annualBonusLevel =
    score >= 95 ? "特优年终奖" :
    score >= 90 ? "优秀年终奖" :
    score >= 80 ? "良好年终奖" :
    score >= 70 ? "达标年终奖" :
    score >= 60 ? "基础年终奖" : "无年终奖";

  const scoreTrend =
    history.length === 0 ? "单次考核" :
    score > Number(history[0]?.score || 0) ? "持续提升" :
    score < Number(history[0]?.score || 0) ? "有所下滑" : "基本持平";

  const developmentSuggestion =
    recentScores.length >= 3 && recentScores.every((item) => item >= 90) ? "连续高分，建议优先升职、加薪" :
    recentScores.length >= 3 && recentScores.every((item) => item < 60) ? "长期低分，建议调岗、降级" :
    score >= 90 ? "本期表现突出，建议优先纳入调薪和晋升观察名单" :
    score >= 80 ? "整体表现良好，建议持续培养并结合岗位评估加薪" :
    score >= 60 ? "绩效达标但仍有提升空间，建议制定专项提升计划" :
    "当前绩效偏低，建议先辅导改进并评估调岗风险";

  return {
    grade,
    annualBonusLevel,
    scoreTrend,
    developmentSuggestion,
  };
}

function normalizeRows(rows: Row[]) {
  return rows.map((row) => {
    const computedRow = buildRowPreview(row, rows);
    const annualBonusAmount = row.annualBonusAmount ?? 0;
    return {
      ...row,
      grade: row.grade || computedRow.grade,
      annualBonusLevel: row.annualBonusLevel || computedRow.annualBonusLevel,
      annualBonusAmount,
      annualBonusAmountDisplay: `¥${formatAmount(annualBonusAmount)}`,
      scoreTrend: row.scoreTrend || computedRow.scoreTrend,
      developmentSuggestion: row.developmentSuggestion || computedRow.developmentSuggestion,
    };
  });
}

async function load() {
  loading.value = true;
  try {
    const data = (await http.get("/salary/performance", { params: query })) as PageData;
    list.value = normalizeRows(data.list || []);
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadEmployees() {
  employees.value = (await http.get("/users")) as Employee[];
}

function syncEmployee() {
  const employee = employees.value.find((item) => item.id === form.employeeId);
  form.employeeName = employee?.realName || "";
}

function buildAssessmentPeriod() {
  if (periodForm.type === "MONTHLY") {
    form.assessmentPeriod = `${periodForm.year}-${periodForm.slot}`;
    return;
  }
  if (periodForm.type === "QUARTERLY") {
    form.assessmentPeriod = `${periodForm.year}-${periodForm.slot}`;
    return;
  }
  if (periodForm.type === "HALF_YEAR") {
    form.assessmentPeriod = `${periodForm.year}-${periodForm.slot}`;
    return;
  }
  form.assessmentPeriod = `${periodForm.year}-年度`;
}

function applyPeriodDefaults(type: PeriodType) {
  if (type === "MONTHLY") {
    periodForm.slot = "01";
    return;
  }
  if (type === "QUARTERLY") {
    periodForm.slot = "Q1";
    return;
  }
  if (type === "HALF_YEAR") {
    periodForm.slot = "H1";
    return;
  }
  periodForm.slot = "";
}

function syncPeriodForm(value?: string) {
  const period = String(value || "").trim();
  const yearlyMatch = period.match(/^(\d{4})-年度$/);
  if (yearlyMatch) {
    periodForm.type = "YEARLY";
    periodForm.year = yearlyMatch[1];
    periodForm.slot = "";
    buildAssessmentPeriod();
    return;
  }

  const halfYearMatch = period.match(/^(\d{4})-(H[12])$/i);
  if (halfYearMatch) {
    periodForm.type = "HALF_YEAR";
    periodForm.year = halfYearMatch[1];
    periodForm.slot = halfYearMatch[2].toUpperCase();
    buildAssessmentPeriod();
    return;
  }

  const quarterMatch = period.match(/^(\d{4})-(Q[1-4])$/i);
  if (quarterMatch) {
    periodForm.type = "QUARTERLY";
    periodForm.year = quarterMatch[1];
    periodForm.slot = quarterMatch[2].toUpperCase();
    buildAssessmentPeriod();
    return;
  }

  const monthMatch = period.match(/^(\d{4})-(0[1-9]|1[0-2])$/);
  if (monthMatch) {
    periodForm.type = "MONTHLY";
    periodForm.year = monthMatch[1];
    periodForm.slot = monthMatch[2];
    buildAssessmentPeriod();
    return;
  }

  periodForm.type = "QUARTERLY";
  periodForm.year = String(currentYear);
  periodForm.slot = "Q1";
  buildAssessmentPeriod();
}

function resetForm() {
  form.id = undefined;
  form.employeeId = undefined;
  form.employeeName = "";
  form.assessmentPeriod = "";
  form.score = 0;
  form.grade = "B";
  form.status = "待确认";
  form.evaluatorName = "王人事";
  form.remark = "";
  periodForm.type = "QUARTERLY";
  periodForm.year = String(currentYear);
  periodForm.slot = "Q1";
  buildAssessmentPeriod();
  previewData.value = { ...previewFallback };
}

function open(row?: Row) {
  if (row) {
    Object.assign(form, row);
    syncPeriodForm(row.assessmentPeriod);
  } else {
    resetForm();
  }
  visible.value = true;
}

function openSuggestion(row: Row) {
  suggestionRow.value = row;
  suggestionVisible.value = true;
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

function formatAmount(value?: number) {
  return Number(value || 0).toFixed(2);
}

async function refreshPreview() {
  if (!form.employeeId) {
    previewData.value = localPreview.value;
    return;
  }

  previewLoading.value = true;
  try {
    previewData.value = (await http.get("/salary/performance/preview", {
      params: {
        employeeId: form.employeeId,
        score: Number(form.score || 0),
        currentId: form.id,
      },
    })) as PreviewData;
  } catch {
    previewData.value = localPreview.value;
  } finally {
    previewLoading.value = false;
  }
}

function gradeColor(grade?: string) {
  if (grade === "A") return "success";
  if (grade === "B") return "primary";
  if (grade === "C") return "warning";
  return "error";
}

function statusColor(status?: string) {
  if (status === "已归档") return "neutral";
  if (status === "已确认") return "success";
  return "warning";
}

function bonusColor(level?: string) {
  if (level === "特优年终奖" || level === "优秀年终奖") return "success";
  if (level === "良好年终奖" || level === "达标年终奖") return "primary";
  if (level === "基础年终奖") return "warning";
  return "error";
}

watch(
  () => [periodForm.type, periodForm.year],
  (value, oldValue) => {
    const [type] = value;
    const previousType = oldValue?.[0];
    if (type !== previousType) {
      applyPeriodDefaults(type as PeriodType);
    }
    buildAssessmentPeriod();
  },
  { immediate: true }
);

watch(
  () => periodForm.slot,
  () => {
    buildAssessmentPeriod();
  }
);

watch(
  () => [form.employeeId, form.score, form.id],
  () => {
    refreshPreview();
  },
  { immediate: true }
);

async function save() {
  syncEmployee();
  if (!form.employeeId || !form.employeeName || !form.assessmentPeriod) {
    toast.add({ title: "请完善绩效考核信息", color: "warning" });
    return;
  }

  saving.value = true;
  try {
    const payload = {
      employeeId: form.employeeId,
      employeeName: form.employeeName,
      assessmentPeriod: form.assessmentPeriod,
      score: Number(form.score || 0),
      status: form.status,
      evaluatorName: form.evaluatorName || "王人事",
      remark: form.remark,
    };
    let savedRow: Row;
    if (form.id) {
      savedRow = (await http.put(`/salary/performance/${form.id}`, payload)) as Row;
      const nextRows = list.value.map((item) => (item.id === savedRow.id ? savedRow : item));
      list.value = normalizeRows(nextRows);
    } else {
      savedRow = (await http.post("/salary/performance", payload)) as Row;
      list.value = normalizeRows([savedRow, ...list.value]);
      total.value += 1;
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: Row) {
  const confirmed = window.confirm("确定删除这条绩效考核记录吗？");
  if (!confirmed) return;

  await http.delete(`/salary/performance/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await load();
}

onMounted(async () => {
  await Promise.all([load(), loadEmployees()]);
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
  width: 360px;
}

.table-wrap {
  overflow-x: auto;
}

.table-wrap :deep(table) {
  min-width: 1440px;
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

.period-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.field-block-full {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: #264334;
}

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(38, 67, 52, 0.08);
}

.stat-card-full {
  grid-column: 1 / -1;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
}

.suggestion-text {
  color: #264334;
}

.suggestion-cell {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 120px;
}

.suggestion-detail {
  display: grid;
  gap: 12px;
}

.suggestion-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.suggestion-detail-text {
  margin: 0;
  line-height: 1.8;
  color: #264334;
  white-space: pre-wrap;
  word-break: break-word;
}

.suggestion-empty {
  color: #9ca3af;
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

  .pager {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .form-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .period-grid {
    grid-template-columns: 1fr;
  }
}
</style>
