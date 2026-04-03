<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>招聘需求</span>
        <el-button type="primary" @click="open()">新增需求</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="请输入需求编号、岗位、申请人关键字"
        clearable
        style="width: 320px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="search">
        <el-option label="待审批" value="待审批" />
        <el-option label="已通过" value="已通过" />
        <el-option label="已驳回" value="已驳回" />
        <el-option label="招聘中" value="招聘中" />
        <el-option label="已完成" value="已完成" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="requirementCode" label="需求编号" width="150" />
      <el-table-column prop="jobTitle" label="岗位名称" min-width="150" />
      <el-table-column label="所属部门" width="140">
        <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
      </el-table-column>
      <el-table-column prop="headcount" label="招聘人数" width="90" />
      <el-table-column label="预算薪资" width="180">
        <template #default="{ row }">{{ salaryRange(row) }}</template>
      </el-table-column>
      <el-table-column prop="expectedOnboardDate" label="期望到岗" width="120" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">编辑</el-button>
          <el-button v-if="row.status === '待审批'" type="success" link @click="approve(row, '已通过')">通过</el-button>
          <el-button v-if="row.status === '待审批'" type="danger" link @click="approve(row, '已驳回')">驳回</el-button>
          <el-button v-if="canGenerate(row.status)" type="warning" link @click="generatePosition(row)">生成职位</el-button>
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

    <el-dialog v-model="visible" :title="form.id ? '编辑招聘需求' : '新增招聘需求'" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="岗位名称">
          <el-input v-model="form.jobTitle" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.departmentId" clearable style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="招聘人数">
          <el-input-number v-model="form.headcount" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预算薪资">
          <div class="pair-row">
            <el-input-number v-model="form.budgetSalaryMin" :min="0" :step="1000" style="width: 48%" />
            <el-input-number v-model="form.budgetSalaryMax" :min="0" :step="1000" style="width: 48%" />
          </div>
        </el-form-item>
        <el-form-item label="期望到岗">
          <el-date-picker v-model="form.expectedOnboardDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="岗位说明">
          <el-input v-model="form.jobDescription" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/api/http";

type Department = { id: number; name: string };
type RequirementRow = Record<string, unknown> & { id?: number; status?: string };
type PageData = { list: RequirementRow[]; total: number };

const rows = ref<RequirementRow[]>([]);
const total = ref(0);
const visible = ref(false);
const departments = ref<Department[]>([]);

const query = reactive({
  keyword: "",
  status: "",
  page: 1,
  size: 10,
});

const form = reactive({
  id: undefined as number | undefined,
  jobTitle: "",
  departmentId: undefined as number | undefined,
  headcount: 1,
  budgetSalaryMin: 0,
  budgetSalaryMax: 0,
  expectedOnboardDate: "",
  jobDescription: "",
  reason: "",
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.jobTitle = "";
  form.departmentId = undefined;
  form.headcount = 1;
  form.budgetSalaryMin = 0;
  form.budgetSalaryMax = 0;
  form.expectedOnboardDate = "";
  form.jobDescription = "";
  form.reason = "";
  form.remark = "";
}

function departmentName(id?: number) {
  return departments.value.find((item) => item.id === id)?.name || "-";
}

function salaryRange(row: RequirementRow) {
  return `￥${Number(row.budgetSalaryMin || 0)} - ￥${Number(row.budgetSalaryMax || 0)}`;
}

function canGenerate(status?: string) {
  return status === "已通过" || status === "招聘中" || status === "已完成";
}

async function load() {
  const data = (await http.get("/recruitment/requirements", {
    params: {
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    },
  })) as PageData;
  rows.value = data.list || [];
  total.value = data.total || 0;
}

async function loadDepartments() {
  departments.value = (await http.get("/departments")) as Department[];
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

function open(row?: RequirementRow) {
  if (row) {
    form.id = row.id as number;
    form.jobTitle = (row.jobTitle as string) || "";
    form.departmentId = row.departmentId as number;
    form.headcount = Number(row.headcount) || 1;
    form.budgetSalaryMin = Number(row.budgetSalaryMin) || 0;
    form.budgetSalaryMax = Number(row.budgetSalaryMax) || 0;
    form.expectedOnboardDate = (row.expectedOnboardDate as string) || "";
    form.jobDescription = (row.jobDescription as string) || "";
    form.reason = (row.reason as string) || "";
    form.remark = (row.remark as string) || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.jobTitle.trim()) {
    ElMessage.warning("请输入岗位名称");
    return;
  }
  const payload = { ...form };
  if (form.id) {
    await http.put(`/recruitment/requirements/${form.id}`, payload);
  } else {
    await http.post("/recruitment/requirements", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await load();
}

async function approve(row: RequirementRow, status: string) {
  await http.post(`/recruitment/requirements/${row.id}/approve`, null, { params: { status } });
  ElMessage.success("处理成功");
  await load();
}

async function generatePosition(row: RequirementRow) {
  await http.post(`/recruitment/requirements/${row.id}/generate-position`);
  ElMessage.success("已生成招聘职位");
  await load();
}

async function remove(row: RequirementRow) {
  try {
    await ElMessageBox.confirm(
      "确定删除该招聘需求吗？删除后，对应的招聘职位和候选人记录也会一并删除。",
      "提示",
      { type: "warning" }
    );
  } catch {
    return;
  }

  try {
    await http.delete(`/recruitment/requirements/${row.id}`);
    ElMessage.success("已删除");
    if (rows.value.length === 1 && query.page > 1) {
      query.page -= 1;
    }
    await load();
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : "删除失败");
  }
}

onMounted(async () => {
  await Promise.all([load(), loadDepartments()]);
});
</script>

<style scoped>
.header-bar,
.toolbar,
.pair-row,
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
}

.pair-row {
  width: 100%;
  justify-content: space-between;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
