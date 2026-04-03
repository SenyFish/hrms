<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>关怀记录</span>
        <el-button type="primary" @click="open()">新增记录</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="请输入记录编号、员工姓名、类型或执行人" clearable style="width: 340px" @keyup.enter="search" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="search">
        <el-option label="已执行" value="已执行" />
        <el-option label="已跟进" value="已跟进" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="recordCode" label="记录编号" width="170" />
      <el-table-column label="关怀员工" width="120">
        <template #default="{ row }">{{ row.user?.realName || "-" }}</template>
      </el-table-column>
      <el-table-column label="关联计划" width="170">
        <template #default="{ row }">{{ row.plan?.planCode || "-" }}</template>
      </el-table-column>
      <el-table-column prop="careType" label="关怀类型" width="120" />
      <el-table-column label="执行时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.careTime) }}</template>
      </el-table-column>
      <el-table-column prop="handlerName" label="执行人" width="120" />
      <el-table-column prop="costAmount" label="实际花费" width="120" />
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
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10,20,50,100]" :total="total" @current-change="load" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑关怀记录' : '新增关怀记录'" width="660px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="关联计划">
          <el-select v-model="form.planId" clearable filterable style="width: 100%">
            <el-option v-for="item in plans" :key="item.id" :label="`${item.planCode} ${item.user?.realName || ''}`" :value="item.id" />
          </el-select>
        </el-form-item>
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
        <el-form-item label="执行时间">
          <el-date-picker v-model="form.careTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss[Z]" style="width: 100%" />
        </el-form-item>
        <el-form-item label="执行人"><el-input v-model="form.handlerName" /></el-form-item>
        <el-form-item label="实际花费"><el-input-number v-model="form.costAmount" :min="0" :precision="2" :step="100" style="width: 100%" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已执行" value="已执行" />
            <el-option label="已跟进" value="已跟进" />
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

type UserOption = { id: number; realName: string; employeeNo: string };
type PlanOption = Record<string, any> & { id: number; planCode: string };
type RecordRow = Record<string, any> & { id?: number };
type PageData = { list: RecordRow[]; total: number };

const rows = ref<RecordRow[]>([]);
const total = ref(0);
const users = ref<UserOption[]>([]);
const plans = ref<PlanOption[]>([]);
const visible = ref(false);
const query = reactive({ keyword: "", status: "", page: 1, size: 10 });
const form = reactive({
  id: undefined as number | undefined,
  planId: undefined as number | undefined,
  userId: undefined as number | undefined,
  careType: "日常关怀",
  careTime: "",
  handlerName: "",
  status: "已执行",
  content: "",
  remark: "",
  costAmount: 0,
});

function resetForm() {
  form.id = undefined;
  form.planId = undefined;
  form.userId = undefined;
  form.careType = "日常关怀";
  form.careTime = "";
  form.handlerName = "";
  form.status = "已执行";
  form.content = "";
  form.remark = "";
  form.costAmount = 0;
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  return value.replace("T", " ").replace(".000+00:00", "");
}

async function load() {
  const data = (await http.get("/care/records", { params: { ...query, keyword: query.keyword || undefined, status: query.status || undefined } })) as PageData;
  rows.value = data.list || [];
  total.value = data.total || 0;
}

async function loadOptions() {
  const [userList, planPage] = await Promise.all([
    http.get("/users"),
    http.get("/care/plans", { params: { page: 1, size: 200 } }),
  ]);
  users.value = userList as UserOption[];
  plans.value = ((planPage as { list: PlanOption[] }).list || []);
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

function open(row?: RecordRow) {
  if (row) {
    form.id = row.id as number;
    form.planId = row.plan?.id;
    form.userId = row.user?.id;
    form.careType = row.careType || "日常关怀";
    form.careTime = (row.careTime || "").replace(".000+00:00", "Z");
    form.handlerName = row.handlerName || "";
    form.status = row.status || "已执行";
    form.content = row.content || "";
    form.remark = row.remark || "";
    form.costAmount = Number(row.costAmount) || 0;
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
  const payload = { ...form, careTime: form.careTime || undefined };
  if (form.id) {
    await http.put(`/care/records/${form.id}`, payload);
  } else {
    await http.post("/care/records", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await Promise.all([load(), loadOptions()]);
}

async function remove(row: RecordRow) {
  await ElMessageBox.confirm("确定删除这条关怀记录吗？", "提示");
  await http.delete(`/care/records/${row.id}`);
  ElMessage.success("已删除");
  await Promise.all([load(), loadOptions()]);
}

onMounted(async () => {
  await Promise.all([load(), loadOptions()]);
});
</script>

<style scoped>
.header-bar,.toolbar,.pager{display:flex}
.header-bar{align-items:center;justify-content:space-between}
.toolbar{gap:12px;margin-bottom:16px;flex-wrap:wrap}
.pager{justify-content:flex-end;margin-top:16px}
</style>
