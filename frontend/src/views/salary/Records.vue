<template>
  <el-card>
    <template #header>
      <span>五险一金与工资</span>
      <div v-if="canEdit" class="header-actions">
        <el-input v-model="month" placeholder="yyyy-MM" style="width: 120px" />
        <el-button @click="load">查询</el-button>
        <el-button type="success" @click="exportXlsx">导出月报表</el-button>
        <el-button type="primary" @click="open()">新增</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="user.realName" label="员工" />
      <el-table-column prop="salaryMonth" label="月份" width="100" />
      <el-table-column prop="baseSalary" label="基本工资" width="110" />
      <el-table-column prop="socialSecurityBase" label="社保基数" width="110" />
      <el-table-column prop="housingFundBase" label="公积金基数" width="120" />
      <el-table-column label="养老个人" width="100">
        <template #default="{ row }">{{ amount(row.pensionPersonal) }}</template>
      </el-table-column>
      <el-table-column label="公积金个人" width="110">
        <template #default="{ row }">{{ amount(row.housingFundPersonal) }}</template>
      </el-table-column>
      <el-table-column label="参保城市" width="140">
        <template #default="{ row }">{{ row.insuredCity?.name || "-" }}</template>
      </el-table-column>
      <el-table-column v-if="canEdit" label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="薪资与五险一金" width="720px">
      <el-form :model="form" label-width="120px">
        <el-form-item v-if="canEdit" label="员工">
          <el-select v-model="form.userId" filterable style="width: 100%">
            <el-option
              v-for="u in userList"
              :key="u.id"
              :label="`${u.realName} ${u.employeeNo}`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-input v-model="form.salaryMonth" placeholder="2026-04" />
        </el-form-item>
        <el-form-item label="基本工资">
          <el-input-number v-model="form.baseSalary" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="参保城市">
          <el-select v-model="form.insuredCityId" clearable filterable style="width: 100%">
            <el-option v-for="c in cities" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="社保基数">
          <el-input-number v-model="form.socialSecurityBase" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="公积金基数">
          <el-input-number v-model="form.housingFundBase" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="规则说明" v-if="selectedCity">
          <el-descriptions :column="2" border size="small" style="width: 100%">
            <el-descriptions-item label="养老(个/企)">{{ percent(selectedCity.pensionPersonalRate) }} / {{ percent(selectedCity.pensionCompanyRate) }}</el-descriptions-item>
            <el-descriptions-item label="医疗(个/企)">{{ percent(selectedCity.medicalPersonalRate) }} / {{ percent(selectedCity.medicalCompanyRate) }}</el-descriptions-item>
            <el-descriptions-item label="失业(个/企)">{{ percent(selectedCity.unemploymentPersonalRate) }} / {{ percent(selectedCity.unemploymentCompanyRate) }}</el-descriptions-item>
            <el-descriptions-item label="工伤/生育(企)">{{ percent(selectedCity.injuryCompanyRate) }} / {{ percent(selectedCity.maternityCompanyRate) }}</el-descriptions-item>
            <el-descriptions-item label="公积金(个/企)">{{ percent(selectedCity.housingFundPersonalRate) }} / {{ percent(selectedCity.housingFundCompanyRate) }}</el-descriptions-item>
            <el-descriptions-item label="社平工资">{{ amount(selectedCity.socialAvgSalary) }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-form-item label="自动计算结果">
          <el-descriptions :column="2" border size="small" style="width: 100%">
            <el-descriptions-item label="养老保险(个人)">{{ amount(preview.pensionPersonal) }}</el-descriptions-item>
            <el-descriptions-item label="养老保险(公司)">{{ amount(preview.pensionCompany) }}</el-descriptions-item>
            <el-descriptions-item label="医疗保险(个人)">{{ amount(preview.medicalPersonal) }}</el-descriptions-item>
            <el-descriptions-item label="医疗保险(公司)">{{ amount(preview.medicalCompany) }}</el-descriptions-item>
            <el-descriptions-item label="失业保险(个人)">{{ amount(preview.unemploymentPersonal) }}</el-descriptions-item>
            <el-descriptions-item label="失业保险(公司)">{{ amount(preview.unemploymentCompany) }}</el-descriptions-item>
            <el-descriptions-item label="工伤保险(公司)">{{ amount(preview.injuryCompany) }}</el-descriptions-item>
            <el-descriptions-item label="生育保险(公司)">{{ amount(preview.maternityCompany) }}</el-descriptions-item>
            <el-descriptions-item label="公积金(个人)">{{ amount(preview.housingFundPersonal) }}</el-descriptions-item>
            <el-descriptions-item label="公积金(公司)">{{ amount(preview.housingFundCompany) }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import http from "@/api/http";
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
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  return `${year}-${month}`;
}

function isValidMonth(value: string): boolean {
  return MONTH_PATTERN.test(value);
}

function calc(base: number, rate?: number | string) {
  return Number((base * Number(rate || 0)).toFixed(2));
}

const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const canEdit = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");

const month = ref(getCurrentMonth());
const rows = ref<SalaryRow[]>([]);
const userList = ref<SimpleUser[]>([]);
const cities = ref<CityRule[]>([]);
const loading = ref(false);
const visible = ref(false);
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
  const socialBase = Number(form.socialSecurityBase) || 0;
  const fundBase = Number(form.housingFundBase) || 0;
  if (!city) {
    return {
      pensionPersonal: 0,
      pensionCompany: 0,
      medicalPersonal: 0,
      medicalCompany: 0,
      unemploymentPersonal: 0,
      unemploymentCompany: 0,
      injuryCompany: 0,
      maternityCompany: 0,
      housingFundPersonal: 0,
      housingFundCompany: 0,
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
  return Number(value || 0).toFixed(2);
}

function percent(value?: number | string) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`;
}

async function load() {
  if (canEdit.value && !isValidMonth(month.value)) {
    ElMessage.warning("月份格式必须是 yyyy-MM");
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
      return;
    }

    rows.value = (await http.get("/salary/records/my")) as SalaryRow[];
  } catch (error: unknown) {
    rows.value = [];
    ElMessage.error(error instanceof Error ? error.message : "薪资数据加载失败");
  } finally {
    loading.value = false;
  }
}

function open(row?: SalaryRow) {
  if (row) {
    form.id = row.id;
    form.userId = row.user?.id;
    form.salaryMonth = row.salaryMonth || month.value;
    form.baseSalary = Number(row.baseSalary) || 0;
    form.socialSecurityBase = Number(row.socialSecurityBase) || 0;
    form.housingFundBase = Number(row.housingFundBase) || 0;
    form.insuredCityId = row.insuredCity?.id;
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.userId && canEdit.value) {
    ElMessage.warning("请选择员工");
    return;
  }
  if (!isValidMonth(form.salaryMonth)) {
    ElMessage.warning("月份格式必须是 yyyy-MM");
    return;
  }
  if (!form.insuredCityId) {
    ElMessage.warning("请选择参保城市");
    return;
  }

  try {
    await http.post("/salary/records", { ...form });
    ElMessage.success("保存成功");
    visible.value = false;
    await load();
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : "保存失败");
  }
}

async function exportXlsx() {
  if (!isValidMonth(month.value)) {
    ElMessage.warning("月份格式必须是 yyyy-MM");
    return;
  }
  if (!store.token) {
    ElMessage.error("登录状态已失效，请重新登录");
    return;
  }

  try {
    const url = new URL("/api/salary/export", window.location.origin);
    url.searchParams.set("month", month.value);
    url.searchParams.set("access_token", store.token);

    const anchor = document.createElement("a");
    anchor.href = url.toString();
    anchor.style.display = "none";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : "导出失败");
  }
}

resetForm();
onMounted(load);
</script>

<style scoped>
.header-actions {
  float: right;
  display: flex;
  gap: 8px;
}
</style>
