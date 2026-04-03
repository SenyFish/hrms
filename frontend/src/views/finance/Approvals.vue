<template>
  <el-card>
    <template #header>
      <span>资产审批</span>
      <el-button type="primary" style="float: right" @click="open()">新增审批</el-button>
    </template>

    <el-table :data="list" border>
      <el-table-column label="资产名称" min-width="160">
        <template #default="{ row }">{{ row.asset?.assetName || "-" }}</template>
      </el-table-column>
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="requestedQuantity" label="申请数量" width="100" />
      <el-table-column prop="applyReason" label="申请说明" min-width="220" />
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="approverName" label="审批人" width="120" />
      <el-table-column label="操作" :width="canApprove ? 220 : 150">
        <template #default="{ row }">
          <template v-if="canApprove">
            <el-button type="success" link @click="changeStatus(row, '已通过')" :disabled="row.status === '已通过'">通过</el-button>
            <el-button type="warning" link @click="changeStatus(row, '已驳回')" :disabled="row.status === '已驳回'">驳回</el-button>
          </template>
          <el-button type="primary" link @click="open(row)" :disabled="!canEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)" :disabled="!canDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="资产审批" width="620px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="资产">
          <el-select v-model="form.assetId" filterable style="width: 100%">
            <el-option v-for="asset in assets" :key="asset.id" :label="asset.assetName" :value="asset.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="form.applicantName" :disabled="!isAdmin" />
        </el-form-item>
        <el-form-item label="申请数量">
          <el-input-number v-model="form.requestedQuantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.applyReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="isAdmin" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待审批" value="待审批" />
            <el-option label="已通过" value="已通过" />
            <el-option label="已驳回" value="已驳回" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";

type AssetOption = { id: number; assetName: string };
type AssetListResponse = {
  list: AssetOption[];
  total: number;
  page: number;
  size: number;
};
type ApprovalRow = {
  id?: number;
  applicantName?: string;
  applicantUserId?: number;
  requestedQuantity?: number;
  applyReason?: string;
  applyTime?: string;
  status?: string;
  approverName?: string;
  remark?: string;
  asset?: { id?: number; assetName?: string };
};

const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const currentUserId = computed(() => Number(store.profile?.id) || 0);
const isAdmin = computed(() => roleCode.value === "ADMIN");
const canApprove = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");

const list = ref<ApprovalRow[]>([]);
const assets = ref<AssetOption[]>([]);
const visible = ref(false);
const form = reactive({
  id: undefined as number | undefined,
  assetId: undefined as number | undefined,
  applicantName: "",
  requestedQuantity: 1,
  applyReason: "",
  status: "待审批",
  remark: "",
});

function resetForm() {
  form.id = undefined;
  form.assetId = undefined;
  form.applicantName = (store.profile?.realName as string) || (store.profile?.username as string) || "";
  form.requestedQuantity = 1;
  form.applyReason = "";
  form.status = "待审批";
  form.remark = "";
}

function formatTime(value?: string) {
  if (!value) return "";
  return value.replace("T", " ").replace("Z", "");
}

function canEdit(row: ApprovalRow) {
  if (canApprove.value) return true;
  return row.applicantUserId === currentUserId.value && row.status === "待审批";
}

function canDelete(row: ApprovalRow) {
  if (canApprove.value) return true;
  return row.applicantUserId === currentUserId.value;
}

async function load() {
  const [approvals, assetPage] = await Promise.all([
    http.get("/finance/approvals"),
    http.get("/finance/assets", { params: { page: 1, size: 200 } }),
  ]);
  list.value = approvals as ApprovalRow[];
  assets.value = ((assetPage as AssetListResponse).list || []).map((item) => ({ id: item.id, assetName: item.assetName }));
}

function open(row?: ApprovalRow) {
  if (row) {
    form.id = row.id;
    form.assetId = row.asset?.id;
    form.applicantName = row.applicantName || "";
    form.requestedQuantity = row.requestedQuantity || 1;
    form.applyReason = row.applyReason || "";
    form.status = row.status || "待审批";
    form.remark = row.remark || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.assetId) {
    ElMessage.warning("请选择资产");
    return;
  }
  if (!form.requestedQuantity || form.requestedQuantity < 1) {
    ElMessage.warning("请输入申请数量");
    return;
  }
  if (!form.applyReason.trim()) {
    ElMessage.warning("请输入申请说明");
    return;
  }

  const payload = { ...form };
  if (!canApprove.value) {
    payload.status = "待审批";
    payload.applicantName = (store.profile?.realName as string) || (store.profile?.username as string) || "";
  }

  if (form.id) {
    await http.put(`/finance/approvals/${form.id}`, payload);
  } else {
    await http.post("/finance/approvals", payload);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  await load();
}

async function changeStatus(row: ApprovalRow, status: string) {
  await http.post(`/finance/approvals/${row.id}/approve`, null, { params: { status } });
  ElMessage.success(`审批状态已更新为${status}`);
  await load();
}

async function remove(row: ApprovalRow) {
  await ElMessageBox.confirm("确定删除这条资产审批记录？", "提示");
  await http.delete(`/finance/approvals/${row.id}`);
  ElMessage.success("已删除");
  await load();
}

resetForm();
onMounted(load);
</script>
