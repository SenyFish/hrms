<template>
  <div class="app-page">
    <PageSection
      eyebrow="Attendance"
      title="请假审批"
      :description="isHr ? '处理待审批请假并查看全量请假流转。' : '发起个人请假并跟踪当前审批状态。'"
      flush
    >
      <template #actions>
        <el-radio-group v-if="isHr" v-model="filter" @change="search">
          <el-radio-button label="待审批">待审批</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
        </el-radio-group>
        <button v-else class="frappe-button" data-variant="solid" type="button" @click="applyVisible = true">发起请假</button>
      </template>

      <FilterToolbar>
        <el-input v-model="query.keyword" class="page-field page-field--wide" placeholder="搜索员工、请假类型、原因或状态" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </FilterToolbar>

      <div class="app-table">
        <el-table :data="rows" border>
          <el-table-column prop="user.realName" label="员工" width="120" />
          <el-table-column prop="leaveType" label="类型" width="120" />
          <el-table-column label="开始时间" width="180">
            <template #default="{ row }">{{ fmt(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" width="180">
            <template #default="{ row }">{{ fmt(row.endTime) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <span class="pill-tag" :class="statusClass(String(row.status || ''))">{{ row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isHr" label="操作" width="180">
            <template #default="{ row }">
              <el-button v-if="row.status === '待审批'" type="success" link @click="approve(row, true)">通过</el-button>
              <el-button v-if="row.status === '待审批'" type="danger" link @click="approve(row, false)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pager">
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
    </PageSection>

    <el-dialog v-model="applyVisible" title="请假申请" width="520px">
      <el-form :model="apply" label-width="90px">
        <el-form-item label="类型"><el-input v-model="apply.leaveType" /></el-form-item>
        <el-form-item label="开始"><el-date-picker v-model="apply.start" type="datetime" style="width: 100%" /></el-form-item>
        <el-form-item label="结束"><el-date-picker v-model="apply.end" type="datetime" style="width: 100%" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="apply.reason" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import FilterToolbar from "@/components/ui/FilterToolbar.vue";
import PageSection from "@/components/ui/PageSection.vue";

type LeaveRow = Record<string, unknown> & { id?: number; status?: string };
type LeaveListResponse = {
  list: LeaveRow[];
  total: number;
};

const store = useUserStore();
const roleCode = computed(() => store.profile?.roleCode as string);
const isHr = computed(() => roleCode.value === "ADMIN" || roleCode.value === "HR");
const filter = ref("待审批");
const rows = ref<LeaveRow[]>([]);
const total = ref(0);
const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});
const applyVisible = ref(false);
const apply = reactive({
  leaveType: "事假",
  start: new Date(),
  end: new Date(),
  reason: "",
});

function fmt(v: unknown) {
  if (!v) return "";
  return new Date(v as string).toLocaleString("zh-CN");
}

function statusClass(status: string) {
  if (status === "待审批") return "pill-tag--warning";
  if (status === "已通过") return "pill-tag--success";
  if (status === "已驳回") return "pill-tag--danger";
  return "pill-tag--neutral";
}

async function load() {
  const params = {
    keyword: query.keyword || undefined,
    page: query.page,
    size: query.size,
  } as Record<string, string | number | undefined>;

  let data: LeaveListResponse;
  if (isHr.value) {
    params.status = filter.value || undefined;
    data = (await http.get("/leaves", { params })) as LeaveListResponse;
  } else {
    data = (await http.get("/leaves/my", { params })) as LeaveListResponse;
  }
  rows.value = data.list || [];
  total.value = data.total || 0;
}

function search() {
  query.page = 1;
  load();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  if (isHr.value) {
    filter.value = "待审批";
  }
  load();
}

function handleSizeChange() {
  query.page = 1;
  load();
}

async function approve(row: LeaveRow, ok: boolean) {
  await http.post(`/leaves/${row.id}/approve`, { approved: ok, remark: ok ? "同意" : "不同意" });
  ElMessage.success("已处理");
  await load();
}

async function submitApply() {
  await http.post("/leaves", {
    leaveType: apply.leaveType,
    startTime: (apply.start as Date).toISOString(),
    endTime: (apply.end as Date).toISOString(),
    reason: apply.reason,
  });
  ElMessage.success("已提交");
  applyVisible.value = false;
  await load();
}

onMounted(load);
</script>

<style scoped>
.page-field {
  min-width: 220px;
}

.page-field--wide {
  width: 320px;
}

.page-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
