<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>关怀计划</span>
        <div class="actions">
          <el-button type="success" @click="exportPlans">导出</el-button>
          <el-button type="primary" @click="open()">新增计划</el-button>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="请输入计划编号、员工姓名或关怀类型"
        clearable
        style="width: 320px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="search">
        <el-option label="待执行" value="待执行" />
        <el-option label="已完成" value="已完成" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="planCode" label="计划编号" width="170" />
      <el-table-column label="关怀员工" width="120">
        <template #default="{ row }">{{ row.user?.realName || "-" }}</template>
      </el-table-column>
      <el-table-column label="工号" width="120">
        <template #default="{ row }">{{ row.user?.employeeNo || "-" }}</template>
      </el-table-column>
      <el-table-column prop="careType" label="关怀类型" width="120" />
      <el-table-column label="计划时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.plannedTime) }}</template>
      </el-table-column>
      <el-table-column prop="budgetAmount" label="预算金额" width="120" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="content" label="关怀内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
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
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑关怀计划' : '新增关怀计划'" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="关怀员工">
          <el-select v-model="form.userId" filterable style="width: 100%">
            <el-option v-for="item in users" :key="item.id" :label="`${item.realName}（${item.employeeNo}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关怀类型">
          <el-select v-model="form.careType" style="width: 100%">
            <el-option label="生日慰问" value="生日慰问" />
            <el-option label="节日关怀" value="节日关怀" />
            <el-option label="入职回访" value="入职回访" />
            <el-option label="困难帮扶" value="困难帮扶" />
            <el-option label="日常关怀" value="日常关怀" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker v-model="form.plannedTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss[Z]" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预算金额">
          <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待执行" value="待执行" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="关怀内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
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
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/api/http";
import { downloadByFetch } from "@/utils/download";
import { useUserStore } from "@/stores/user";

type UserOption = { id: number; realName: string; employeeNo: string };
type PlanRow = Record<string, any> & { id?: number };
type PageData = { list: PlanRow[]; total: number };

const rows = ref<PlanRow[]>([]);
const total = ref(0);
const users = ref<UserOption[]>([]);
const visible = ref(false);
const store = useUserStore();
const query = reactive({ keyword: "", status: "", page: 1, size: 10 });
const form = reactive({
  id: undefined as number | undefined,
  userId: undefined as number | undefined,
  careType: "日常关怀",
  plannedTime: "",
  status: "待执行",
  content: "",
  remark: "",
  budgetAmount: 0,
});

function resetForm() {
  form.id = undefined;
  form.userId = undefined;
  form.careType = "日常关怀";
  form.plannedTime = "";
  form.status = "待执行";
  form.content = "";
  form.remark = "";
  form.budgetAmount = 0;
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  return value.replace("T", " ").replace(".000+00:00", "");
}

async function load() {
  const data = (await http.get("/care/plans", { params: { ...query, keyword: query.keyword || undefined, status: query.status || undefined } })) as PageData;
  rows.value = data.list || [];
  total.value = data.total || 0;
}

async function loadUsers() {
  users.value = (await http.get("/users")) as UserOption[];
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

function open(row?: PlanRow) {
  if (row) {
    form.id = row.id as number;
    form.userId = row.user?.id;
    form.careType = row.careType || "日常关怀";
    form.plannedTime = (row.plannedTime || "").replace(".000+00:00", "Z");
    form.status = row.status || "待执行";
    form.content = row.content || "";
    form.remark = row.remark || "";
    form.budgetAmount = Number(row.budgetAmount) || 0;
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.userId) {
    ElMessage.warning("请选择关怀员工");
    return;
  }
  if (!form.content.trim()) {
    ElMessage.warning("请输入关怀内容");
    return;
  }
  const payload = { ...form, plannedTime: form.plannedTime || undefined };
  if (form.id) {
    await http.put(`/care/plans/${form.id}`, payload);
  } else {
    await http.post("/care/plans", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await load();
}

async function remove(row: PlanRow) {
  await ElMessageBox.confirm("确定删除这条关怀计划吗？", "提示");
  await http.delete(`/care/plans/${row.id}`);
  ElMessage.success("已删除");
  await load();
}

async function exportPlans() {
  try {
    const params = new URLSearchParams();
    if (query.keyword) params.set("keyword", query.keyword);
    if (query.status) params.set("status", query.status);
    await downloadByFetch(
      `/api/care/plans/export?${params.toString()}`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      "care-plans.xlsx"
    );
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "导出失败");
  }
}

onMounted(async () => {
  await Promise.all([load(), loadUsers()]);
});
</script>

<style scoped>
.header-bar,.toolbar,.pager,.actions{display:flex}
.header-bar{align-items:center;justify-content:space-between}
.toolbar{gap:12px;margin-bottom:16px;flex-wrap:wrap}
.pager{justify-content:flex-end;margin-top:16px}
.actions{gap:12px}
</style>
