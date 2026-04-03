<template>
  <el-card>
    <template #header>
      <div class="header-bar">
        <span>资产信息</span>
        <el-button v-if="isAdminOrHr" type="primary" @click="open()">新增资产</el-button>
      </div>
    </template>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="请输入资产编码、名称、类别、状态等关键字"
        clearable
        style="width: 320px"
        @keyup.enter="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="assetCode" label="资产编码" width="160" />
      <el-table-column prop="assetName" label="资产名称" min-width="180" />
      <el-table-column prop="category" label="资产类别" width="120" />
      <el-table-column prop="quantity" label="剩余数量" width="100" />
      <el-table-column prop="unitPrice" label="单价" width="120" />
      <el-table-column label="购置日期" width="120">
        <template #default="{ row }">{{ row.purchaseDate || "-" }}</template>
      </el-table-column>
      <el-table-column label="所属部门" width="140">
        <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column :label="isAdminOrHr ? '操作' : '申请'" :width="isAdminOrHr ? 150 : 110">
        <template #default="{ row }">
          <template v-if="isAdminOrHr">
            <el-button type="primary" link @click="open(row)">编辑</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
          <template v-else>
            <el-button type="primary" link @click="openApply(row)" :disabled="(row.quantity || 0) <= 0">
              申请
            </el-button>
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

    <el-dialog v-model="visible" title="资产信息" width="620px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="资产名称"><el-input v-model="form.assetName" /></el-form-item>
        <el-form-item label="资产类别"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="剩余数量">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="form.departmentId" clearable style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="在库" value="在库" />
            <el-option label="闲置" value="闲置" />
            <el-option label="维修中" value="维修中" />
            <el-option label="已领完" value="已领完" />
            <el-option label="已报废" value="已报废" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" title="申请资产" width="560px">
      <el-form :model="applyForm" label-width="110px">
        <el-form-item label="资产名称">
          <el-input :model-value="applyForm.assetName" disabled />
        </el-form-item>
        <el-form-item label="申请人">
          <el-input :model-value="applyForm.applicantName" disabled />
        </el-form-item>
        <el-form-item label="申请数量">
          <el-input-number
            v-model="applyForm.requestedQuantity"
            :min="1"
            :max="applyForm.maxQuantity"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="applyForm.applyReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

type Department = { id: number; name: string };
type AssetRow = {
  id?: number;
  assetCode?: string;
  assetName?: string;
  category?: string;
  quantity?: number;
  unitPrice?: number | string;
  purchaseDate?: string;
  departmentId?: number;
  status?: string;
  remark?: string;
};
type AssetListResponse = {
  list: AssetRow[];
  total: number;
  page: number;
  size: number;
};

const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const isAdminOrHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");

const list = ref<AssetRow[]>([]);
const total = ref(0);
const departments = ref<Department[]>([]);
const visible = ref(false);
const applyVisible = ref(false);
const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});
const form = reactive({
  id: undefined as number | undefined,
  assetName: "",
  category: "",
  quantity: 1,
  unitPrice: 0,
  purchaseDate: "",
  departmentId: undefined as number | undefined,
  status: "在库",
  remark: "",
});
const applyForm = reactive({
  assetId: undefined as number | undefined,
  assetName: "",
  applicantName: "",
  requestedQuantity: 1,
  maxQuantity: 1,
  applyReason: "",
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.assetName = "";
  form.category = "";
  form.quantity = 1;
  form.unitPrice = 0;
  form.purchaseDate = new Date().toISOString().slice(0, 10);
  form.departmentId = undefined;
  form.status = "在库";
  form.remark = "";
}

function resetApplyForm() {
  applyForm.assetId = undefined;
  applyForm.assetName = "";
  applyForm.applicantName = (store.profile?.realName as string) || (store.profile?.username as string) || "";
  applyForm.requestedQuantity = 1;
  applyForm.maxQuantity = 1;
  applyForm.applyReason = "";
  applyForm.remark = "";
}

function departmentName(id?: number) {
  const department = departments.value.find((item) => item.id === id);
  return department?.name || (id ? `部门ID:${id}` : "-");
}

async function load() {
  try {
    const data = (await http.get("/finance/assets", {
      params: {
        keyword: query.keyword || undefined,
        page: query.page,
        size: query.size,
      },
    })) as AssetListResponse;
    list.value = data.list || [];
    total.value = data.total || 0;
  } catch (error: unknown) {
    list.value = [];
    total.value = 0;
    ElMessage.error(error instanceof Error ? error.message : "资产信息加载失败");
  }
}

async function loadDepartments() {
  try {
    departments.value = (await http.get("/departments")) as Department[];
  } catch {
    departments.value = [];
  }
}

function search() {
  query.page = 1;
  load();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

function open(row?: AssetRow) {
  if (row) {
    form.id = row.id;
    form.assetName = row.assetName || "";
    form.category = row.category || "";
    form.quantity = row.quantity || 1;
    form.unitPrice = Number(row.unitPrice) || 0;
    form.purchaseDate = row.purchaseDate || new Date().toISOString().slice(0, 10);
    form.departmentId = row.departmentId;
    form.status = row.status || "在库";
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

function openApply(row: AssetRow) {
  resetApplyForm();
  applyForm.assetId = row.id;
  applyForm.assetName = row.assetName || "";
  applyForm.maxQuantity = Math.max(1, row.quantity || 1);
  applyVisible.value = true;
}

async function save() {
  if (!form.assetName.trim()) {
    ElMessage.warning("请输入资产名称");
    return;
  }

  const payload = { ...form };
  if (form.id) {
    await http.put(`/finance/assets/${form.id}`, payload);
  } else {
    await http.post("/finance/assets", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await load();
}

async function submitApply() {
  if (!applyForm.assetId) {
    ElMessage.warning("请选择资产");
    return;
  }
  if (!applyForm.applyReason.trim()) {
    ElMessage.warning("请输入申请说明");
    return;
  }

  await http.post("/finance/approvals", {
    assetId: applyForm.assetId,
    applicantName: applyForm.applicantName,
    requestedQuantity: applyForm.requestedQuantity,
    applyReason: applyForm.applyReason,
    remark: applyForm.remark,
  });
  ElMessage.success("申请已提交");
  applyVisible.value = false;
}

async function remove(row: AssetRow) {
  await ElMessageBox.confirm("确定删除这条资产信息吗？", "提示");
  await http.delete(`/finance/assets/${row.id}`);
  ElMessage.success("已删除");
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await load();
}

resetForm();
resetApplyForm();
onMounted(async () => {
  await Promise.all([load(), loadDepartments()]);
});
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
</style>
