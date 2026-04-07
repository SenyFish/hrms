<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>五险一金与工资</span>
        <div v-if="canEdit" class="header-actions">
          <UInput v-model="month" class="month-input" placeholder="yyyy-MM" />
          <UButton color="neutral" variant="soft" @click="load">查询</UButton>
          <UButton color="success" icon="i-lucide-download" :loading="exporting" @click="exportXlsx">导出月报表</UButton>
          <UButton color="primary" icon="i-lucide-plus" @click="open()">新增</UButton>
        </div>
      </div>
    </template>

    <UTable :data="rows" :columns="columns" :loading="loading" class="table-wrap">
      <template #user-cell="{ row }">{{ row.original.user?.realName || "-" }}</template>
      <template #baseSalary-cell="{ row }">¥{{ amount(row.original.baseSalary) }}</template>
      <template #socialSecurityBase-cell="{ row }">¥{{ amount(row.original.socialSecurityBase) }}</template>
      <template #housingFundBase-cell="{ row }">¥{{ amount(row.original.housingFundBase) }}</template>
      <template #pensionPersonal-cell="{ row }">¥{{ amount(row.original.pensionPersonal) }}</template>
      <template #housingFundPersonal-cell="{ row }">¥{{ amount(row.original.housingFundPersonal) }}</template>
      <template #insuredCity-cell="{ row }">{{ row.original.insuredCity?.name || "-" }}</template>
      <template #netSalary-cell="{ row }">¥{{ amount(netSalary(row.original)) }}</template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="showDetail(row.original)">明细</UButton>
          <UButton color="success" variant="ghost" size="sm" :loading="downloadingId === row.original.id" @click="downloadPayslip(row.original)">下载工资条</UButton>
          <UButton v-if="canEdit" color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
        </div>
      </template>
    </UTable>

    <UModal v-model:open="visible" :title="form.id ? '编辑工资记录' : '新增工资记录'">
      <template #body>
        <div class="form-grid">
          <div v-if="canEdit" class="field-block field-block-full">
            <label class="field-label">员工</label>
            <USelectMenu v-model="form.userId" :items="userList" value-key="id" label-key="realName" searchable class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">月份</label>
            <UInput v-model="form.salaryMonth" placeholder="2026-04" />
          </div>
          <div class="field-block">
            <label class="field-label">基本工资</label>
            <UInputNumber v-model="form.baseSalary" :min="0" :step="100" />
          </div>
          <div class="field-block">
            <label class="field-label">参保城市</label>
            <USelectMenu v-model="form.insuredCityId" :items="cities" value-key="id" label-key="name" searchable class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">社保基数</label>
            <UInputNumber v-model="form.socialSecurityBase" :min="0" :step="100" />
          </div>
          <div class="field-block">
            <label class="field-label">公积金基数</label>
            <UInputNumber v-model="form.housingFundBase" :min="0" :step="100" />
          </div>
          <div v-if="selectedCity" class="field-block field-block-full">
            <label class="field-label">规则说明</label>
            <div class="stats-grid">
              <div class="stat-card"><span class="stat-label">养老 个人 / 公司</span><strong>{{ percent(selectedCity.pensionPersonalRate) }} / {{ percent(selectedCity.pensionCompanyRate) }}</strong></div>
              <div class="stat-card"><span class="stat-label">医疗 个人 / 公司</span><strong>{{ percent(selectedCity.medicalPersonalRate) }} / {{ percent(selectedCity.medicalCompanyRate) }}</strong></div>
              <div class="stat-card"><span class="stat-label">失业 个人 / 公司</span><strong>{{ percent(selectedCity.unemploymentPersonalRate) }} / {{ percent(selectedCity.unemploymentCompanyRate) }}</strong></div>
              <div class="stat-card"><span class="stat-label">工伤 / 生育 公司</span><strong>{{ percent(selectedCity.injuryCompanyRate) }} / {{ percent(selectedCity.maternityCompanyRate) }}</strong></div>
              <div class="stat-card"><span class="stat-label">公积金 个人 / 公司</span><strong>{{ percent(selectedCity.housingFundPersonalRate) }} / {{ percent(selectedCity.housingFundCompanyRate) }}</strong></div>
              <div class="stat-card"><span class="stat-label">社平工资</span><strong>¥{{ amount(selectedCity.socialAvgSalary) }}</strong></div>
            </div>
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">自动计算结果</label>
            <div class="stats-grid">
              <div class="stat-card"><span class="stat-label">养老保险（个人）</span><strong>¥{{ amount(preview.pensionPersonal) }}</strong></div>
              <div class="stat-card"><span class="stat-label">养老保险（公司）</span><strong>¥{{ amount(preview.pensionCompany) }}</strong></div>
              <div class="stat-card"><span class="stat-label">医疗保险（个人）</span><strong>¥{{ amount(preview.medicalPersonal) }}</strong></div>
              <div class="stat-card"><span class="stat-label">医疗保险（公司）</span><strong>¥{{ amount(preview.medicalCompany) }}</strong></div>
              <div class="stat-card"><span class="stat-label">失业保险（个人）</span><strong>¥{{ amount(preview.unemploymentPersonal) }}</strong></div>
              <div class="stat-card"><span class="stat-label">失业保险（公司）</span><strong>¥{{ amount(preview.unemploymentCompany) }}</strong></div>
              <div class="stat-card"><span class="stat-label">工伤保险（公司）</span><strong>¥{{ amount(preview.injuryCompany) }}</strong></div>
              <div class="stat-card"><span class="stat-label">生育保险（公司）</span><strong>¥{{ amount(preview.maternityCompany) }}</strong></div>
              <div class="stat-card"><span class="stat-label">公积金（个人）</span><strong>¥{{ amount(preview.housingFundPersonal) }}</strong></div>
              <div class="stat-card"><span class="stat-label">公积金（公司）</span><strong>¥{{ amount(preview.housingFundCompany) }}</strong></div>
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

    <UModal v-model:open="detailVisible" title="工资明细">
      <template #body>
        <div v-if="detailRow" class="detail-grid">
          <div class="detail-card"><span class="detail-label">员工</span><strong>{{ detailRow.user?.realName || "-" }}</strong></div>
          <div class="detail-card"><span class="detail-label">工号</span><strong>{{ detailRow.user?.employeeNo || "-" }}</strong></div>
          <div class="detail-card"><span class="detail-label">月份</span><strong>{{ detailRow.salaryMonth || "-" }}</strong></div>
          <div class="detail-card"><span class="detail-label">参保城市</span><strong>{{ detailRow.insuredCity?.name || "-" }}</strong></div>
          <div class="detail-card"><span class="detail-label">基本工资</span><strong>¥{{ amount(detailRow.baseSalary) }}</strong></div>
          <div class="detail-card"><span class="detail-label">扣除个缴后实发参考</span><strong>¥{{ amount(netSalary(detailRow)) }}</strong></div>
          <div class="detail-card"><span class="detail-label">社保基数</span><strong>¥{{ amount(detailRow.socialSecurityBase) }}</strong></div>
          <div class="detail-card"><span class="detail-label">公积金基数</span><strong>¥{{ amount(detailRow.housingFundBase) }}</strong></div>
          <div class="detail-card"><span class="detail-label">养老保险（个人）</span><strong>¥{{ amount(detailRow.pensionPersonal) }}</strong></div>
          <div class="detail-card"><span class="detail-label">养老保险（公司）</span><strong>¥{{ amount(detailRow.pensionCompany) }}</strong></div>
          <div class="detail-card"><span class="detail-label">医疗保险（个人）</span><strong>¥{{ amount(detailRow.medicalPersonal) }}</strong></div>
          <div class="detail-card"><span class="detail-label">医疗保险（公司）</span><strong>¥{{ amount(detailRow.medicalCompany) }}</strong></div>
          <div class="detail-card"><span class="detail-label">失业保险（个人）</span><strong>¥{{ amount(detailRow.unemploymentPersonal) }}</strong></div>
          <div class="detail-card"><span class="detail-label">失业保险（公司）</span><strong>¥{{ amount(detailRow.unemploymentCompany) }}</strong></div>
          <div class="detail-card"><span class="detail-label">工伤保险（公司）</span><strong>¥{{ amount(detailRow.injuryCompany) }}</strong></div>
          <div class="detail-card"><span class="detail-label">生育保险（公司）</span><strong>¥{{ amount(detailRow.maternityCompany) }}</strong></div>
          <div class="detail-card"><span class="detail-label">公积金（个人）</span><strong>¥{{ amount(detailRow.housingFundPersonal) }}</strong></div>
          <div class="detail-card"><span class="detail-label">公积金（公司）</span><strong>¥{{ amount(detailRow.housingFundCompany) }}</strong></div>
          <div class="detail-card"><span class="detail-label">个人扣缴合计</span><strong>¥{{ amount(personalDeduction(detailRow)) }}</strong></div>
          <div class="detail-card"><span class="detail-label">公司承担合计</span><strong>¥{{ amount(companyContribution(detailRow)) }}</strong></div>
          <div class="detail-card detail-card-full"><span class="detail-label">备注</span><strong>{{ detailRow.remark || "-" }}</strong></div>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="detailVisible = false">关闭</UButton>
          <UButton v-if="detailRow" color="success" :loading="downloadingId === detailRow.id" @click="downloadPayslip(detailRow)">下载工资条</UButton>
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
import { downloadByFetch } from "@/utils/download";
import { useUserStore } from "@/stores/user";

type SalaryRow = {
  id?: number;
  salaryMonth?: string;
  baseSalary?: number | string;
  pensionPersonal?: number | string;
  pensionCompany?: number | string;
  medicalPersonal?: number | string;
  medicalCompany?: number | string;
  unemploymentPersonal?: number | string;
  unemploymentCompany?: number | string;
  injuryCompany?: number | string;
  maternityCompany?: number | string;
  housingFundPersonal?: number | string;
  housingFundCompany?: number | string;
  socialSecurityBase?: number | string;
  housingFundBase?: number | string;
  remark?: string;
  user?: { id?: number; realName?: string; employeeNo?: string };
  insuredCity?: CityRule;
};

type SimpleUser = { id: number; realName: string; employeeNo: string };
type CityRule = {
  id: number;
  name: string;
  socialAvgSalary?: number | string;
  pensionPersonalRate?: number | string;
  pensionCompanyRate?: number | string;
  medicalPersonalRate?: number | string;
  medicalCompanyRate?: number | string;
  unemploymentPersonalRate?: number | string;
  unemploymentCompanyRate?: number | string;
  injuryCompanyRate?: number | string;
  maternityCompanyRate?: number | string;
  housingFundPersonalRate?: number | string;
  housingFundCompanyRate?: number | string;
};

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

function getCurrentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function isValidMonth(value: string): boolean {
  return MONTH_PATTERN.test(value);
}

function calc(base: number, rate?: number | string) {
  return Number((base * Number(rate || 0)).toFixed(2));
}

function numeric(value?: number | string) {
  return Number(value || 0);
}

const toast = useToast();
const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const canEdit = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");

const month = ref(getCurrentMonth());
const rows = ref<SalaryRow[]>([]);
const userList = ref<SimpleUser[]>([]);
const cities = ref<CityRule[]>([]);
const loading = ref(false);
const saving = ref(false);
const exporting = ref(false);
const visible = ref(false);
const detailVisible = ref(false);
const detailRow = ref<SalaryRow | null>(null);
const downloadingId = ref<number | undefined>(undefined);

const columns: TableColumn<SalaryRow>[] = [
  { accessorKey: "user", header: "员工" },
  { accessorKey: "salaryMonth", header: "月份" },
  { accessorKey: "baseSalary", header: "基本工资" },
  { accessorKey: "socialSecurityBase", header: "社保基数" },
  { accessorKey: "housingFundBase", header: "公积金基数" },
  { accessorKey: "pensionPersonal", header: "养老个人" },
  { accessorKey: "housingFundPersonal", header: "公积金个人" },
  { accessorKey: "netSalary", header: "扣除个缴后实发参考" },
  { accessorKey: "insuredCity", header: "参保城市" },
  { accessorKey: "actions", header: "操作" },
];

const form = reactive({
  id: undefined as number | undefined,
  userId: undefined as number | undefined,
  salaryMonth: "",
  baseSalary: 0,
  socialSecurityBase: 0,
  housingFundBase: 0,
  insuredCityId: undefined as number | undefined,
  remark: "",
});

const selectedCity = computed(() => cities.value.find((item) => item.id === form.insuredCityId));

const preview = computed(() => {
  const city = selectedCity.value;
  const socialBase = numeric(form.socialSecurityBase);
  const fundBase = numeric(form.housingFundBase);
  if (!city) {
    return {
      pensionPersonal: 0, pensionCompany: 0, medicalPersonal: 0, medicalCompany: 0,
      unemploymentPersonal: 0, unemploymentCompany: 0, injuryCompany: 0, maternityCompany: 0,
      housingFundPersonal: 0, housingFundCompany: 0,
    };
  }
  return {
    pensionPersonal: calc(socialBase, city.pensionPersonalRate),
    pensionCompany: calc(socialBase, city.pensionCompanyRate),
    medicalPersonal: calc(socialBase, city.medicalPersonalRate),
    medicalCompany: calc(socialBase, city.medicalCompanyRate),
    unemploymentPersonal: calc(socialBase, city.unemploymentPersonalRate),
    unemploymentCompany: calc(socialBase, city.unemploymentCompanyRate),
    injuryCompany: calc(socialBase, city.injuryCompanyRate),
    maternityCompany: calc(socialBase, city.maternityCompanyRate),
    housingFundPersonal: calc(fundBase, city.housingFundPersonalRate),
    housingFundCompany: calc(fundBase, city.housingFundCompanyRate),
  };
});

function resetForm() {
  form.id = undefined;
  form.userId = undefined;
  form.salaryMonth = month.value;
  form.baseSalary = 0;
  form.socialSecurityBase = 0;
  form.housingFundBase = 0;
  form.insuredCityId = undefined;
  form.remark = "";
}

function amount(value?: number | string) {
  return numeric(value).toFixed(2);
}

function percent(value?: number | string) {
  return `${(numeric(value) * 100).toFixed(2)}%`;
}

function personalDeduction(row: SalaryRow) {
  return numeric(row.pensionPersonal) + numeric(row.medicalPersonal) + numeric(row.unemploymentPersonal) + numeric(row.housingFundPersonal);
}

function companyContribution(row: SalaryRow) {
  return numeric(row.pensionCompany) + numeric(row.medicalCompany) + numeric(row.unemploymentCompany) + numeric(row.injuryCompany) + numeric(row.maternityCompany) + numeric(row.housingFundCompany);
}

function netSalary(row: SalaryRow) {
  return numeric(row.baseSalary) - personalDeduction(row);
}

async function load() {
  if (canEdit.value && !isValidMonth(month.value)) {
    toast.add({ title: "月份格式必须是 yyyy-MM", color: "warning" });
    return;
  }

  loading.value = true;
  try {
    if (canEdit.value) {
      const [salaryRows, users, cityList] = await Promise.all([
        http.get("/salary/records", { params: { month: month.value } }),
        http.get("/users"),
        http.get("/insured-cities"),
      ]);
      rows.value = salaryRows as SalaryRow[];
      userList.value = users as SimpleUser[];
      cities.value = cityList as CityRule[];
    } else {
      rows.value = (await http.get("/salary/records/my")) as SalaryRow[];
    }
  } catch (error: unknown) {
    rows.value = [];
    toast.add({ title: error instanceof Error ? error.message : "工资数据加载失败", color: "error" });
  } finally {
    loading.value = false;
  }
}

function open(row?: SalaryRow) {
  if (row) {
    form.id = row.id;
    form.userId = row.user?.id;
    form.salaryMonth = row.salaryMonth || month.value;
    form.baseSalary = numeric(row.baseSalary);
    form.socialSecurityBase = numeric(row.socialSecurityBase);
    form.housingFundBase = numeric(row.housingFundBase);
    form.insuredCityId = row.insuredCity?.id;
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

function showDetail(row: SalaryRow) {
  detailRow.value = row;
  detailVisible.value = true;
}

async function save() {
  if (!form.userId && canEdit.value) {
    toast.add({ title: "请选择员工", color: "warning" });
    return;
  }
  if (!isValidMonth(form.salaryMonth)) {
    toast.add({ title: "月份格式必须是 yyyy-MM", color: "warning" });
    return;
  }
  if (!form.insuredCityId) {
    toast.add({ title: "请选择参保城市", color: "warning" });
    return;
  }

  saving.value = true;
  try {
    await http.post("/salary/records", { ...form });
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "保存失败", color: "error" });
  } finally {
    saving.value = false;
  }
}

async function exportXlsx() {
  if (!isValidMonth(month.value)) {
    toast.add({ title: "月份格式必须是 yyyy-MM", color: "warning" });
    return;
  }
  if (!store.token) {
    toast.add({ title: "登录状态已失效，请重新登录", color: "error" });
    return;
  }

  exporting.value = true;
  try {
    const url = new URL("/api/salary/export", window.location.origin);
    url.searchParams.set("month", month.value);
    await downloadByFetch(url, { method: "GET", headers: { Authorization: `Bearer ${store.token}` } }, `salary-${month.value}.xlsx`);
    toast.add({ title: "月报表已开始下载", color: "success" });
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "导出失败", color: "error" });
  } finally {
    exporting.value = false;
  }
}

async function downloadPayslip(row: SalaryRow) {
  if (!row.id) return;
  if (!store.token) {
    toast.add({ title: "登录状态已失效，请重新登录", color: "error" });
    return;
  }

  downloadingId.value = row.id;
  try {
    await downloadByFetch(`/api/salary/records/${row.id}/payslip`, { method: "GET", headers: { Authorization: `Bearer ${store.token}` } }, `payslip-${row.salaryMonth || "salary"}.xlsx`);
    toast.add({ title: "工资条已开始下载", color: "success" });
  } catch (error: unknown) {
    toast.add({ title: error instanceof Error ? error.message : "下载工资条失败", color: "error" });
  } finally {
    downloadingId.value = undefined;
  }
}

resetForm();
onMounted(load);
</script>

<style scoped>
.header-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.header-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.month-input { width: 120px; }
.table-wrap { overflow: hidden; }
.action-group { display: flex; gap: 6px; flex-wrap: wrap; }
.form-grid, .detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.field-block { display: grid; gap: 8px; }
.field-block-full, .detail-card-full { grid-column: 1 / -1; }
.field-label { font-size: 14px; font-weight: 600; color: #264334; }
.stats-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.stat-card, .detail-card { display: grid; gap: 6px; padding: 12px 14px; border-radius: 14px; background: rgba(255, 255, 255, 0.72); border: 1px solid rgba(38, 67, 52, 0.08); }
.stat-label, .detail-label { color: #6b7280; font-size: 13px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }

@media (max-width: 900px) {
  .header-bar { align-items: flex-start; flex-direction: column; }
  .header-actions, .month-input { width: 100%; }
  .form-grid, .stats-grid, .detail-grid { grid-template-columns: 1fr; }
}
</style>
