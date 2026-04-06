<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>参保城市</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">添加参保城市</UButton>
      </div>
    </template>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #socialAvgSalary-cell="{ row }">¥{{ formatAmount(row.original.socialAvgSalary) }}</template>
      <template #pensionRate-cell="{ row }">
        {{ formatPercent(row.original.pensionPersonalRate) }} / {{ formatPercent(row.original.pensionCompanyRate) }}
      </template>
      <template #medicalRate-cell="{ row }">
        {{ formatPercent(row.original.medicalPersonalRate) }} / {{ formatPercent(row.original.medicalCompanyRate) }}
      </template>
      <template #unemploymentRate-cell="{ row }">
        {{ formatPercent(row.original.unemploymentPersonalRate) }} / {{ formatPercent(row.original.unemploymentCompanyRate) }}
      </template>
      <template #injuryRate-cell="{ row }">
        {{ formatPercent(row.original.injuryCompanyRate) }} / {{ formatPercent(row.original.maternityCompanyRate) }}
      </template>
      <template #housingFundRate-cell="{ row }">
        {{ formatPercent(row.original.housingFundPersonalRate) }} / {{ formatPercent(row.original.housingFundCompanyRate) }}
      </template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
      </template>
    </UTable>

    <UModal v-model:open="visible" :title="form.id ? '编辑参保城市' : '新增参保城市'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">城市名称</label>
            <UInput v-model="form.name" />
          </div>
          <div class="field-block">
            <label class="field-label">区划编码</label>
            <UInput v-model="form.regionCode" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">社平工资参考</label>
            <UInputNumber v-model="form.socialAvgSalary" :min="0" :step="100" />
          </div>

          <div class="field-block field-block-full">
            <label class="field-label">养老保险比例</label>
            <div class="pair-row">
              <UInputNumber v-model="form.pensionPersonalRate" :min="0" :max="1" :step="0.001" />
              <UInputNumber v-model="form.pensionCompanyRate" :min="0" :max="1" :step="0.001" />
            </div>
            <div class="pair-tip">个人 / 公司，输入 0.08 表示 8%</div>
          </div>

          <div class="field-block field-block-full">
            <label class="field-label">医疗保险比例</label>
            <div class="pair-row">
              <UInputNumber v-model="form.medicalPersonalRate" :min="0" :max="1" :step="0.001" />
              <UInputNumber v-model="form.medicalCompanyRate" :min="0" :max="1" :step="0.001" />
            </div>
          </div>

          <div class="field-block field-block-full">
            <label class="field-label">失业保险比例</label>
            <div class="pair-row">
              <UInputNumber v-model="form.unemploymentPersonalRate" :min="0" :max="1" :step="0.001" />
              <UInputNumber v-model="form.unemploymentCompanyRate" :min="0" :max="1" :step="0.001" />
            </div>
          </div>

          <div class="field-block field-block-full">
            <label class="field-label">工伤 / 生育比例</label>
            <div class="pair-row">
              <UInputNumber v-model="form.injuryCompanyRate" :min="0" :max="1" :step="0.001" />
              <UInputNumber v-model="form.maternityCompanyRate" :min="0" :max="1" :step="0.001" />
            </div>
            <div class="pair-tip">公司承担</div>
          </div>

          <div class="field-block field-block-full">
            <label class="field-label">公积金比例</label>
            <div class="pair-row">
              <UInputNumber v-model="form.housingFundPersonalRate" :min="0" :max="1" :step="0.001" />
              <UInputNumber v-model="form.housingFundCompanyRate" :min="0" :max="1" :step="0.001" />
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
  </UCard>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";

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

const toast = useToast();
const list = ref<CityRow[]>([]);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);

const columns: TableColumn<CityRow>[] = [
  { accessorKey: "name", header: "城市" },
  { accessorKey: "regionCode", header: "区划编码" },
  { accessorKey: "socialAvgSalary", header: "社平工资参考" },
  { accessorKey: "pensionRate", header: "养老(个人/公司)" },
  { accessorKey: "medicalRate", header: "医疗(个人/公司)" },
  { accessorKey: "unemploymentRate", header: "失业(个人/公司)" },
  { accessorKey: "injuryRate", header: "工伤/生育(公司)" },
  { accessorKey: "housingFundRate", header: "公积金(个人/公司)" },
  { accessorKey: "remark", header: "备注" },
  { accessorKey: "actions", header: "操作" },
];

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

function formatAmount(value?: number | string) {
  return Number(value || 0).toFixed(2);
}

function formatPercent(value?: number | string) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`;
}

async function load() {
  loading.value = true;
  try {
    list.value = (await http.get("/insured-cities")) as CityRow[];
  } finally {
    loading.value = false;
  }
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

async function save() {
  if (!form.name.trim()) {
    toast.add({ title: "请输入城市名称", color: "warning" });
    return;
  }

  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/insured-cities/${form.id}`, { ...form });
    } else {
      await http.post("/insured-cities", { ...form });
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: CityRow) {
  const confirmed = window.confirm("确定删除该参保城市吗？");
  if (!confirmed) return;
  await http.delete(`/insured-cities/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
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
  gap: 12px;
}

.table-wrap {
  overflow: hidden;
}

.action-group {
  display: flex;
  gap: 6px;
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
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.pair-tip {
  color: #6b7280;
  font-size: 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .header-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-grid,
  .pair-row {
    grid-template-columns: 1fr;
  }
}
</style>
