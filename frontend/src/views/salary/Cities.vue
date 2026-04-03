<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>参保城市</span>
        <el-button type="primary" @click="open()">添加参保城市</el-button>
      </div>
    </template>

    <el-table :data="list" border>
      <el-table-column prop="name" label="城市" width="120" />
      <el-table-column prop="regionCode" label="区划编码" width="120" />
      <el-table-column prop="socialAvgSalary" label="社平工资参考" width="140" />
      <el-table-column label="养老(个/企)" width="130">
        <template #default="{ row }">{{ formatPercent(row.pensionPersonalRate) }} / {{ formatPercent(row.pensionCompanyRate) }}</template>
      </el-table-column>
      <el-table-column label="医疗(个/企)" width="130">
        <template #default="{ row }">{{ formatPercent(row.medicalPersonalRate) }} / {{ formatPercent(row.medicalCompanyRate) }}</template>
      </el-table-column>
      <el-table-column label="失业(个/企)" width="130">
        <template #default="{ row }">{{ formatPercent(row.unemploymentPersonalRate) }} / {{ formatPercent(row.unemploymentCompanyRate) }}</template>
      </el-table-column>
      <el-table-column label="工伤/生育(企)" width="130">
        <template #default="{ row }">{{ formatPercent(row.injuryCompanyRate) }} / {{ formatPercent(row.maternityCompanyRate) }}</template>
      </el-table-column>
      <el-table-column label="公积金(个/企)" width="140">
        <template #default="{ row }">{{ formatPercent(row.housingFundPersonalRate) }} / {{ formatPercent(row.housingFundCompanyRate) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="参保城市" width="680px">
      <el-form :model="form" label-width="130px">
        <el-form-item label="城市名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="区划编码"><el-input v-model="form.regionCode" /></el-form-item>
        <el-form-item label="社平工资参考">
          <el-input-number v-model="form.socialAvgSalary" :min="0" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="养老保险比例">
          <div class="pair-row">
            <el-input-number v-model="form.pensionPersonalRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
            <el-input-number v-model="form.pensionCompanyRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
          </div>
          <div class="pair-tip">个人 / 公司，输入 0.08 表示 8%</div>
        </el-form-item>
        <el-form-item label="医疗保险比例">
          <div class="pair-row">
            <el-input-number v-model="form.medicalPersonalRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
            <el-input-number v-model="form.medicalCompanyRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
          </div>
        </el-form-item>
        <el-form-item label="失业保险比例">
          <div class="pair-row">
            <el-input-number v-model="form.unemploymentPersonalRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
            <el-input-number v-model="form.unemploymentCompanyRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
          </div>
        </el-form-item>
        <el-form-item label="工伤/生育比例">
          <div class="pair-row">
            <el-input-number v-model="form.injuryCompanyRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
            <el-input-number v-model="form.maternityCompanyRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
          </div>
          <div class="pair-tip">公司承担</div>
        </el-form-item>
        <el-form-item label="公积金比例">
          <div class="pair-row">
            <el-input-number v-model="form.housingFundPersonalRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
            <el-input-number v-model="form.housingFundCompanyRate" :min="0" :max="1" :step="0.001" :precision="4" style="width: 48%" />
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import http from "@/api/http";
import { ElMessage, ElMessageBox } from "element-plus";

type CityRow = {
  id?: number;
  name?: string;
  regionCode?: string;
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
  remark?: string;
};

const defaultRates = {
  pensionPersonalRate: 0.08,
  pensionCompanyRate: 0.16,
  medicalPersonalRate: 0.02,
  medicalCompanyRate: 0.095,
  unemploymentPersonalRate: 0.005,
  unemploymentCompanyRate: 0.005,
  injuryCompanyRate: 0.002,
  maternityCompanyRate: 0.01,
  housingFundPersonalRate: 0.07,
  housingFundCompanyRate: 0.07,
};

const list = ref<CityRow[]>([]);
const visible = ref(false);
const form = reactive({
  id: undefined as number | undefined,
  name: "",
  regionCode: "",
  socialAvgSalary: 0,
  pensionPersonalRate: defaultRates.pensionPersonalRate,
  pensionCompanyRate: defaultRates.pensionCompanyRate,
  medicalPersonalRate: defaultRates.medicalPersonalRate,
  medicalCompanyRate: defaultRates.medicalCompanyRate,
  unemploymentPersonalRate: defaultRates.unemploymentPersonalRate,
  unemploymentCompanyRate: defaultRates.unemploymentCompanyRate,
  injuryCompanyRate: defaultRates.injuryCompanyRate,
  maternityCompanyRate: defaultRates.maternityCompanyRate,
  housingFundPersonalRate: defaultRates.housingFundPersonalRate,
  housingFundCompanyRate: defaultRates.housingFundCompanyRate,
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.name = "";
  form.regionCode = "";
  form.socialAvgSalary = 0;
  form.pensionPersonalRate = defaultRates.pensionPersonalRate;
  form.pensionCompanyRate = defaultRates.pensionCompanyRate;
  form.medicalPersonalRate = defaultRates.medicalPersonalRate;
  form.medicalCompanyRate = defaultRates.medicalCompanyRate;
  form.unemploymentPersonalRate = defaultRates.unemploymentPersonalRate;
  form.unemploymentCompanyRate = defaultRates.unemploymentCompanyRate;
  form.injuryCompanyRate = defaultRates.injuryCompanyRate;
  form.maternityCompanyRate = defaultRates.maternityCompanyRate;
  form.housingFundPersonalRate = defaultRates.housingFundPersonalRate;
  form.housingFundCompanyRate = defaultRates.housingFundCompanyRate;
  form.remark = "";
}

async function load() {
  list.value = (await http.get("/insured-cities")) as CityRow[];
}

function open(row?: CityRow) {
  if (row) {
    form.id = row.id;
    form.name = row.name || "";
    form.regionCode = row.regionCode || "";
    form.socialAvgSalary = Number(row.socialAvgSalary) || 0;
    form.pensionPersonalRate = Number(row.pensionPersonalRate) || defaultRates.pensionPersonalRate;
    form.pensionCompanyRate = Number(row.pensionCompanyRate) || defaultRates.pensionCompanyRate;
    form.medicalPersonalRate = Number(row.medicalPersonalRate) || defaultRates.medicalPersonalRate;
    form.medicalCompanyRate = Number(row.medicalCompanyRate) || defaultRates.medicalCompanyRate;
    form.unemploymentPersonalRate = Number(row.unemploymentPersonalRate) || defaultRates.unemploymentPersonalRate;
    form.unemploymentCompanyRate = Number(row.unemploymentCompanyRate) || defaultRates.unemploymentCompanyRate;
    form.injuryCompanyRate = Number(row.injuryCompanyRate) || defaultRates.injuryCompanyRate;
    form.maternityCompanyRate = Number(row.maternityCompanyRate) || defaultRates.maternityCompanyRate;
    form.housingFundPersonalRate = Number(row.housingFundPersonalRate) || defaultRates.housingFundPersonalRate;
    form.housingFundCompanyRate = Number(row.housingFundCompanyRate) || defaultRates.housingFundCompanyRate;
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

function formatPercent(value?: number | string) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`;
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning("请输入城市名称");
    return;
  }
  if (form.id) {
    await http.put(`/insured-cities/${form.id}`, { ...form });
  } else {
    await http.post("/insured-cities", { ...form });
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await load();
}

async function remove(row: CityRow) {
  await ElMessageBox.confirm("确定删除该参保城市吗？", "提示");
  await http.delete(`/insured-cities/${row.id}`);
  ElMessage.success("已删除");
  await load();
}

resetForm();
onMounted(load);
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pair-row {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

.pair-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}
</style>
