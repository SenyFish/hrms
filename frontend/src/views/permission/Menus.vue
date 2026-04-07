<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>菜单管理</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增菜单</UButton>
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

    <UModal v-model:open="visible" :title="form.id ? '编辑菜单' : '新增菜单'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">标题</label>
            <UInput v-model="form.title" />
          </div>
          <div class="field-block">
            <label class="field-label">路径</label>
            <UInput v-model="form.path" />
          </div>
          <div class="field-block">
            <label class="field-label">图标</label>
            <UInput v-model="form.icon" placeholder="图标名称" />
          </div>
          <div class="field-block">
            <label class="field-label">排序</label>
            <UInputNumber v-model="form.sortOrder" :min="0" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">父级ID</label>
            <UInputNumber v-model="form.parentId" :min="0" />
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

type MenuRow = Record<string, any> & { id?: number };

const toast = useToast();
const list = ref<MenuRow[]>([]);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);

const columns: TableColumn<MenuRow>[] = [
  { accessorKey: "title", header: "标题" },
  { accessorKey: "path", header: "路由路径" },
  { accessorKey: "icon", header: "图标" },
  { accessorKey: "sortOrder", header: "排序" },
  { accessorKey: "parentId", header: "父级ID" },
  { accessorKey: "actions", header: "操作" },
];

const form = reactive({
  id: undefined as number | undefined,
  title: "",
  path: "",
  icon: "",
  sortOrder: 1,
  parentId: 0,
});

async function load() {
  loading.value = true;
  try {
    list.value = (await http.get("/menus")) as MenuRow[];
  } finally {
    loading.value = false;
  }
}

function open(row?: MenuRow) {
  if (row) {
    Object.assign(form, row);
    form.id = row.id as number;
  } else {
    form.id = undefined;
    form.title = "";
    form.path = "";
    form.icon = "";
    form.sortOrder = 1;
    form.parentId = 0;
  }
  visible.value = true;
}

async function save() {
  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/menus/${form.id}`, form);
    } else {
      await http.post("/menus", form);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: MenuRow) {
  const confirmed = window.confirm("确定删除该菜单吗？");
  if (!confirmed) return;
  await http.delete(`/menus/${row.id}`);
  toast.add({ title: "已删除", color: "success" });
  await load();
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
