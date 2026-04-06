<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>部门管理（含考勤时间、请假、罚款/加班设置）</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增部门</UButton>
      </div>
    </template>

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
      </template>
    </UTable>

    <UModal v-model:open="visible" :title="form.id ? '编辑部门' : '新增部门'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">名称</label>
            <UInput v-model="form.name" />
          </div>
          <div class="field-block">
            <label class="field-label">上班时间</label>
            <UInput v-model="form.workStartTime" type="time" size="lg" variant="subtle" icon="i-lucide-sunrise" class="time-input" />
            <span class="field-hint">统一选择上班打卡基准时间。</span>
          </div>
          <div class="field-block">
            <label class="field-label">下班时间</label>
            <UInput v-model="form.workEndTime" type="time" size="lg" variant="subtle" icon="i-lucide-sunset" class="time-input" />
            <span class="field-hint">统一选择下班打卡基准时间。</span>
          </div>
          <div class="field-block">
            <label class="field-label">迟到宽限（分钟）</label>
            <UInputNumber v-model="form.lateGraceMinutes" :min="0" />
          </div>
          <div class="field-block">
            <label class="field-label">迟到罚款（元）</label>
            <UInputNumber v-model="form.finePerLate" :min="0" :step="10" />
          </div>
          <div class="field-block">
            <label class="field-label">加班计薪倍数</label>
            <UInputNumber v-model="form.overtimeRate" :min="1" :step="0.1" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">请假规则说明</label>
            <UTextarea v-model="form.leaveSettingsNote" :rows="2" />
          </div>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="visible = false">取消</UButton>
          <UButton color="primary" :loading="saving" @click="save">保存</UButton>
        </div>
      </template>
    </UModal>
  </UCard>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";

type DepartmentRow = Record<string, any> & { id?: number };

const toast = useToast();
const list = ref<DepartmentRow[]>([]);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);

const columns: TableColumn<DepartmentRow>[] = [
  { accessorKey: "name", header: "部门名称" },
  { accessorKey: "workStartTime", header: "上班时间" },
  { accessorKey: "workEndTime", header: "下班时间" },
  { accessorKey: "lateGraceMinutes", header: "迟到宽限（分）" },
  { accessorKey: "finePerLate", header: "迟到罚款" },
  { accessorKey: "overtimeRate", header: "加班倍数" },
  { accessorKey: "actions", header: "操作" },
];

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

function resetForm() {
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

async function load() {
  loading.value = true;
  try {
    list.value = (await http.get("/departments")) as DepartmentRow[];
  } finally {
    loading.value = false;
  }
}

function open(row?: DepartmentRow) {
  if (row) {
    Object.assign(form, row);
    form.id = row.id as number;
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.name.trim()) {
    toast.add({ title: "请输入部门名称", color: "warning" });
    return;
  }

  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/departments/${form.id}`, form);
    } else {
      await http.post("/departments", form);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "保存失败",
      color: "error",
    });
  } finally {
    saving.value = false;
  }
}

async function remove(row: DepartmentRow) {
  const confirmed = window.confirm("确定删除该部门吗？");
  if (!confirmed) return;
  try {
    await http.delete(`/departments/${row.id}`);
    toast.add({ title: "已删除", color: "success" });
    await load();
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "删除失败",
      color: "error",
    });
  }
}

onMounted(load);
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-wrap {
  overflow: hidden;
}

.action-group {
  display: flex;
  gap: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block-full {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: #264334;
}

.field-hint {
  font-size: 12px;
  color: #7b8a83;
}

.time-input :deep(input) {
  min-height: 44px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
