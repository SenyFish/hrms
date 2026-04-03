<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>{{ pageLabel }}</span>
        <el-button type="primary" @click="open()">{{ addButtonText }}</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        :placeholder="isEmployee ? '请输入姓名、手机号、邮箱或职位关键字' : '请输入姓名、手机号、邮箱、职位或内推人关键字'"
        clearable
        style="width: 340px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 160px" @change="search">
        <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="education" label="学历" width="100" />
      <el-table-column label="应聘职位" min-width="160">
        <template #default="{ row }">{{ row.position?.positionName || "-" }}</template>
      </el-table-column>
      <el-table-column v-if="!isEmployee" prop="sourceChannel" label="来源渠道" width="110" />
      <el-table-column v-if="!isEmployee" label="内推人" width="120">
        <template #default="{ row }">{{ row.referrerName || "-" }}</template>
      </el-table-column>
      <el-table-column label="内推时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.referralTime || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column :width="isEmployee ? 170 : 160" label="操作" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)" :disabled="isEmployee && row.status === '已入职'">删除</el-button>
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

    <el-dialog v-model="visible" :title="dialogTitle" width="720px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="学历"><el-input v-model="form.education" /></el-form-item>
        <el-form-item label="应聘职位">
          <el-select v-model="form.positionId" clearable filterable style="width: 100%">
            <el-option
              v-for="item in positions"
              :key="item.id"
              :label="`${item.positionCode} ${item.positionName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="期望薪资"><el-input-number v-model="form.expectedSalary" :min="0" :step="1000" style="width: 100%" /></el-form-item>
        <el-form-item v-if="!isEmployee" label="来源渠道">
          <el-select v-model="form.sourceChannel" style="width: 100%">
            <el-option label="社会招聘" value="社会招聘" />
            <el-option label="校园招聘" value="校园招聘" />
            <el-option label="员工内推" value="员工内推" />
            <el-option label="猎头推荐" value="猎头推荐" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEmployee" label="面试时间">
          <el-date-picker
            v-model="form.interviewTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!isEmployee" label="面试官"><el-input v-model="form.interviewerName" /></el-form-item>
        <el-form-item v-if="!isEmployee" label="当前状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="简历备注"><el-input v-model="form.resumeRemark" type="textarea" :rows="2" /></el-form-item>
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
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

type Position = { id: number; positionCode: string; positionName: string; status?: string };
type CandidateRow = Record<string, any> & { id?: number };
type PageData = { list: CandidateRow[]; total: number };

const route = useRoute();
const store = useUserStore();
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const isEmployee = computed(() => roleCode.value === "EMP");

const statusOptions = ["待筛选", "待初试", "待复试", "待发Offer", "已发Offer", "已入职", "已淘汰"];
const rows = ref<CandidateRow[]>([]);
const total = ref(0);
const visible = ref(false);
const positions = ref<Position[]>([]);
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
  form.remark = "";
}

function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

async function load() {
  const data = (await http.get("/recruitment/candidates", {
    params: {
      ...query,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    },
  })) as PageData;
  rows.value = data.list || [];
  total.value = data.total || 0;
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
    form.interviewTime = (row.interviewTime || "").replace(".000+00:00", "Z");
    form.interviewerName = row.interviewerName || "";
    form.status = row.status || "待筛选";
    form.result = row.result || "";
    form.resumeRemark = row.resumeRemark || "";
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

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning(isEmployee.value ? "请输入内推人选姓名" : "请输入候选人姓名");
    return;
  }
  if (!form.positionId) {
    ElMessage.warning("请选择应聘职位");
    return;
  }
  const payload = {
    ...form,
    interviewTime: form.interviewTime || undefined,
    sourceChannel: isEmployee.value ? undefined : form.sourceChannel,
    status: isEmployee.value ? undefined : form.status,
    result: isEmployee.value ? undefined : form.result,
    interviewerName: isEmployee.value ? undefined : form.interviewerName,
  };
  if (form.id) {
    await http.put(`/recruitment/candidates/${form.id}`, payload);
  } else {
    await http.post("/recruitment/candidates", payload);
  }
  ElMessage.success(isEmployee.value ? "内推提交成功" : "保存成功");
  visible.value = false;
  await load();
}

async function remove(row: CandidateRow) {
  await ElMessageBox.confirm(isEmployee.value ? "确定删除这条内推记录吗？" : "确定删除该候选人吗？", "提示");
  await http.delete(`/recruitment/candidates/${row.id}`);
  ElMessage.success("已删除");
  await load();
}

onMounted(async () => {
  await Promise.all([load(), loadPositions()]);
  openReferralFromRoute();
});
</script>

<style scoped>
.header-bar,
.toolbar,
.pager {
  display: flex;
}

.header-bar {
  align-items: center;
  justify-content: space-between;
}

.toolbar {
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
