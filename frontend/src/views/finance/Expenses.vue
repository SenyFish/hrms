<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>财务支出</span>
        <div class="header-actions">
          <el-button type="success" @click="openChart">支出饼图</el-button>
          <el-button type="primary" @click="open()">新增</el-button>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="请输入序号或支出说明关键字"
        clearable
        style="width: 320px"
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="serialNo" label="序号" width="160" />
      <el-table-column prop="description" label="支出说明" min-width="220" />
      <el-table-column label="支出金额" width="140">
        <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="支出时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.expenseTime) }}</template>
      </el-table-column>
      <el-table-column label="支出部门" width="160">
        <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="loadExpenses"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="visible" title="财务支出" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="支出说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="支出金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支出时间">
          <el-date-picker
            v-model="form.expenseTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="支出部门">
          <el-select v-model="form.departmentId" clearable style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="chartVisible" title="财务支出饼图" width="760px" @opened="renderChart">
      <div class="chart-summary">按当前财务支出记录汇总支出说明金额占比</div>
      <div ref="chartRef" class="chart-box"></div>
      <template #footer>
        <el-button @click="chartVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadChart">下载饼图</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { PieChart } from "echarts/charts";
import { LegendComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { init, use, type ECharts } from "echarts/core";
import http from "@/api/http";

use([PieChart, LegendComponent, TooltipComponent, CanvasRenderer]);

type Department = { id: number; name: string };
type ExpenseRow = {
  id?: number;
  serialNo?: string;
  description?: string;
  amount?: number | string;
  expenseTime?: string;
  departmentId?: number;
};
type ExpenseListResponse = {
  list: ExpenseRow[];
  total: number;
  page: number;
  size: number;
};

const list = ref<ExpenseRow[]>([]);
const total = ref(0);
const departments = ref<Department[]>([]);
const visible = ref(false);
const chartVisible = ref(false);
const chartRef = ref<HTMLElement | null>(null);
let chartInstance: ECharts | null = null;

const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});

const form = reactive({
  id: undefined as number | undefined,
  description: "",
  amount: 0,
  expenseTime: "",
  departmentId: undefined as number | undefined,
});

function resetForm() {
  form.id = undefined;
  form.description = "";
  form.amount = 0;
  form.expenseTime = new Date().toISOString().slice(0, 19);
  form.departmentId = undefined;
}

function formatDateTime(value?: string) {
  if (!value) return "";
  return value.replace("T", " ");
}

function formatAmount(value?: number | string) {
  const amount = Number(value || 0);
  return amount.toFixed(2);
}

function departmentName(id?: number) {
  const department = departments.value.find((item) => item.id === id);
  return department?.name || "-";
}

async function loadExpenses() {
  const data = (await http.get("/finance/expenses", {
    params: {
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    },
  })) as ExpenseListResponse;
  list.value = data.list || [];
  total.value = data.total || 0;
}

async function loadDepartments() {
  departments.value = (await http.get("/departments")) as Department[];
}

function search() {
  query.page = 1;
  loadExpenses();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  loadExpenses();
}

function handleSizeChange() {
  query.page = 1;
  loadExpenses();
}

function open(row?: ExpenseRow) {
  if (row) {
    form.id = row.id;
    form.description = row.description || "";
    form.amount = Number(row.amount) || 0;
    form.expenseTime = row.expenseTime || new Date().toISOString().slice(0, 19);
    form.departmentId = row.departmentId;
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.description.trim()) {
    ElMessage.warning("请输入支出说明");
    return;
  }

  const payload = { ...form };
  if (form.id) {
    await http.put(`/finance/expenses/${form.id}`, payload);
  } else {
    await http.post("/finance/expenses", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await loadExpenses();
}

async function remove(row: ExpenseRow) {
  await ElMessageBox.confirm("确定删除这条财务支出记录吗？", "提示");
  await http.delete(`/finance/expenses/${row.id}`);
  ElMessage.success("已删除");
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await loadExpenses();
}

function chartData() {
  return list.value.map((item) => ({
    name: item.description || "未命名支出",
    value: Number(item.amount || 0),
  }));
}

function renderChart() {
  if (!chartRef.value) return;
  if (!chartInstance) {
    chartInstance = init(chartRef.value);
  }
  chartInstance.setOption({
    backgroundColor: "transparent",
    tooltip: {
      trigger: "item",
      formatter: "{b}<br/>金额：￥{c}<br/>占比：{d}%",
    },
    legend: {
      bottom: 0,
      textStyle: { color: "#44525f" },
    },
    color: ["#1b4332", "#2d6a4f", "#40916c", "#74c69d", "#95d5b2", "#d7c28b", "#b08968", "#588157", "#52796f", "#84a98c"],
    series: [
      {
        name: "财务支出",
        type: "pie",
        radius: ["38%", "72%"],
        center: ["50%", "44%"],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: "#f8f4ec",
          borderWidth: 2,
        },
        label: {
          color: "#1f2937",
          formatter: "{b}\n￥{c}",
        },
        data: chartData(),
      },
    ],
  });
  chartInstance.resize();
}

async function openChart() {
  chartVisible.value = true;
  await nextTick();
  renderChart();
}

function downloadChart() {
  if (!chartInstance) {
    ElMessage.warning("饼图尚未生成");
    return;
  }
  const url = chartInstance.getDataURL({
    type: "png",
    pixelRatio: 2,
    backgroundColor: "#f8f4ec",
  });
  const link = document.createElement("a");
  link.href = url;
  link.download = `财务支出饼图-${new Date().toISOString().slice(0, 10)}.png`;
  link.click();
  ElMessage.success("饼图已开始下载");
}

function handleResize() {
  chartInstance?.resize();
}

resetForm();

onMounted(async () => {
  await Promise.all([loadExpenses(), loadDepartments()]);
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  chartInstance?.dispose();
  chartInstance = null;
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
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.chart-summary {
  margin-bottom: 14px;
  color: #5f6b76;
  font-size: 14px;
}

.chart-box {
  height: 460px;
}
</style>
