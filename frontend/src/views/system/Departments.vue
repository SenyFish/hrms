<template>
  <div class="app-page">
    <PageSection
      eyebrow="System"
      title="部门管理"
      description="维护部门上下班时间、请假说明、迟到罚款与加班倍率等基础规则。"
      flush
    >
      <template #actions>
        <button class="frappe-button" data-variant="solid" type="button" @click="open()">新增部门</button>
      </template>

      <div class="app-table">
        <el-table :data="list" border>
          <el-table-column prop="name" label="部门名称" />
          <el-table-column prop="workStartTime" label="上班时间" width="120" />
          <el-table-column prop="workEndTime" label="下班时间" width="120" />
          <el-table-column prop="lateGraceMinutes" label="迟到宽限(分)" width="130" />
          <el-table-column prop="finePerLate" label="迟到罚款" width="110" />
          <el-table-column prop="overtimeRate" label="加班倍率" width="110" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" link @click="open(row)">编辑</el-button>
              <el-button type="danger" link @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </PageSection>

    <el-dialog v-model="visible" title="部门" width="560px">
      <el-form :model="form" label-width="126px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="上班时间"><el-input v-model="form.workStartTime" placeholder="09:00" /></el-form-item>
        <el-form-item label="下班时间"><el-input v-model="form.workEndTime" placeholder="18:00" /></el-form-item>
        <el-form-item label="迟到宽限(分钟)"><el-input-number v-model="form.lateGraceMinutes" :min="0" /></el-form-item>
        <el-form-item label="请假规则说明"><el-input v-model="form.leaveSettingsNote" type="textarea" rows="2" /></el-form-item>
        <el-form-item label="迟到罚款(元)"><el-input-number v-model="form.finePerLate" :min="0" :step="10" /></el-form-item>
        <el-form-item label="加班倍率"><el-input-number v-model="form.overtimeRate" :min="1" :step="0.1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "@/api/http";
import PageSection from "@/components/ui/PageSection.vue";

const list = ref<Array<Record<string, unknown>>>([]);
const visible = ref(false);
const form = reactive({
  id: undefined as number | undefined,
  name: "",
  parentId: 0,
  sortOrder: 1,
  workStartTime: "09:00",
  workEndTime: "18:00",
  lateGraceMinutes: 10,
  leaveSettingsNote: "",
  finePerLate: 50,
  overtimeRate: 1.5,
});

async function load() {
  list.value = (await http.get("/departments")) as typeof list.value;
}

function open(row?: Record<string, unknown>) {
  if (row) {
    Object.assign(form, row);
    form.id = row.id as number;
  } else {
    form.id = undefined;
    form.name = "";
    form.parentId = 0;
    form.sortOrder = 1;
    form.workStartTime = "09:00";
    form.workEndTime = "18:00";
    form.lateGraceMinutes = 10;
    form.leaveSettingsNote = "";
    form.finePerLate = 50;
    form.overtimeRate = 1.5;
  }
  visible.value = true;
}

async function save() {
  if (form.id) {
    await http.put(`/departments/${form.id}`, form);
  } else {
    await http.post("/departments", form);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  load();
}

async function remove(row: Record<string, unknown>) {
  await ElMessageBox.confirm("确定删除该部门吗？", "提示");
  await http.delete(`/departments/${row.id}`);
  ElMessage.success("已删除");
  load();
}

onMounted(load);
</script>
