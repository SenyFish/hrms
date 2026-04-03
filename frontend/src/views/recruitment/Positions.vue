<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>{{ pageTitle }}</span>
        <el-button v-if="!isEmployee" type="primary" @click="open()">新增职位</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        :placeholder="isEmployee ? '请输入职位编号、名称或要求关键字' : '请输入职位编号、名称、要求关键字'"
        clearable
        style="width: 320px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="search" :disabled="isEmployee">
        <el-option label="待发布" value="待发布" />
        <el-option label="招聘中" value="招聘中" />
        <el-option label="已暂停" value="已暂停" />
        <el-option label="已招满" value="已招满" />
        <el-option label="已关闭" value="已关闭" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="positionCode" label="职位编号" width="150" />
      <el-table-column prop="positionName" label="职位名称" min-width="150" />
      <el-table-column v-if="!isEmployee" label="关联需求" width="140">
        <template #default="{ row }">{{ row.requirement?.requirementCode || "-" }}</template>
      </el-table-column>
      <el-table-column label="所属部门" width="140">
        <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
      </el-table-column>
      <el-table-column label="招聘进度" width="100">
        <template #default="{ row }">{{ row.filledCount || 0 }}/{{ row.plannedCount || 0 }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="jobRequirements" label="岗位要求" min-width="200" show-overflow-tooltip />
      <el-table-column :width="isEmployee ? 120 : 260" label="操作" fixed="right">
        <template #default="{ row }">
          <template v-if="isEmployee">
            <el-button type="primary" link @click="goReferral(row)">发起内推</el-button>
          </template>
          <template v-else>
            <el-button type="primary" link @click="open(row)">编辑</el-button>
            <el-button type="success" link @click="changeStatus(row, '招聘中')">发布</el-button>
            <el-button type="warning" link @click="changeStatus(row, '已暂停')">暂停</el-button>
            <el-button type="danger" link @click="changeStatus(row, '已关闭')">关闭</el-button>
          </template>
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

    <el-dialog v-model="visible" :title="form.id ? '编辑招聘职位' : '新增招聘职位'" width="680px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="职位名称"><el-input v-model="form.positionName" /></el-form-item>
        <el-form-item label="关联需求">
          <el-select v-model="form.requirementId" clearable filterable style="width: 100%">
            <el-option v-for="item in requirements" :key="item.id" :label="`${item.requirementCode} ${item.jobTitle}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.departmentId" clearable style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划人数"><el-input-number v-model="form.plannedCount" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="已入职人数"><el-input-number v-model="form.filledCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="职位说明"><el-input v-model="form.jobDescription" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="任职要求"><el-input v-model="form.jobRequirements" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待发布" value="待发布" />
            <el-option label="招聘中" value="招聘中" />
            <el-option label="已暂停" value="已暂停" />
            <el-option label="已招满" value="已招满" />
            <el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
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
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

type Department = { id: number; name: string };
type Requirement = { id: number; requirementCode: string; jobTitle: string };
type PositionRow = Record<string, any> & { id?: number };
type PageData = { list: PositionRow[]; total: number };

const router = useRouter();
const store = useUserStore();
const roleCode = computed(() => String(store.profile?.roleCode || ""));
const isEmployee = computed(() => roleCode.value === "EMP");
const pageTitle = computed(() => (isEmployee.value ? "内推职位" : "招聘职位"));

const rows = ref<PositionRow[]>([]);
const total = ref(0);
const visible = ref(false);
const departments = ref<Department[]>([]);
const requirements = ref<Requirement[]>([]);
const query = reactive({ keyword: "", status: "", page: 1, size: 10 });
const form = reactive({
  id: undefined as number | undefined,
  requirementId: undefined as number | undefined,
  positionName: "",
  departmentId: undefined as number | undefined,
  plannedCount: 1,
  filledCount: 0,
  jobDescription: "",
  jobRequirements: "",
  status: "待发布",
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.requirementId = undefined;
  form.positionName = "";
  form.departmentId = undefined;
  form.plannedCount = 1;
  form.filledCount = 0;
  form.jobDescription = "";
  form.jobRequirements = "";
  form.status = "待发布";
  form.remark = "";
}

function departmentName(id?: number) {
  return departments.value.find((item) => item.id === id)?.name || "-";
}

async function load() {
  const data = (await http.get("/recruitment/positions", {
    params: {
      ...query,
      keyword: query.keyword || undefined,
      status: isEmployee.value ? undefined : query.status || undefined,
    },
  })) as PageData;
  rows.value = data.list || [];
  total.value = data.total || 0;
}

async function loadOptions() {
  const deptList = (await http.get("/departments")) as Department[];
  departments.value = deptList || [];
  if (!isEmployee.value) {
    const reqPage = (await http.get("/recruitment/requirements", { params: { page: 1, size: 200 } })) as { list: Requirement[] };
    requirements.value = (reqPage.list || []).map((item) => ({
      id: item.id,
      requirementCode: item.requirementCode,
      jobTitle: item.jobTitle,
    }));
  }
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

function open(row?: PositionRow) {
  if (row) {
    form.id = row.id as number;
    form.requirementId = row.requirement?.id;
    form.positionName = row.positionName || "";
    form.departmentId = row.departmentId;
    form.plannedCount = Number(row.plannedCount) || 1;
    form.filledCount = Number(row.filledCount) || 0;
    form.jobDescription = row.jobDescription || "";
    form.jobRequirements = row.jobRequirements || "";
    form.status = row.status || "待发布";
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.positionName.trim()) {
    ElMessage.warning("请输入职位名称");
    return;
  }
  const payload = { ...form };
  if (form.id) {
    await http.put(`/recruitment/positions/${form.id}`, payload);
  } else {
    await http.post("/recruitment/positions", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await load();
}

async function changeStatus(row: PositionRow, status: string) {
  await http.post(`/recruitment/positions/${row.id}/status`, null, { params: { status } });
  ElMessage.success("状态已更新");
  await load();
}

function goReferral(row: PositionRow) {
  router.push({ path: "/recruitment/referrals", query: { positionId: String(row.id || ""), action: "create" } });
}

onMounted(async () => {
  await Promise.all([load(), loadOptions()]);
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
