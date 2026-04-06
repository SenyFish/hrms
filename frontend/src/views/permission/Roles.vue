<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>角色管理（菜单分配）</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增角色</UButton>
      </div>
    </template>

    <UTable :data="roles" :columns="columns" :loading="loading" class="table-wrap">
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
      </template>
    </UTable>

    <UModal v-model:open="visible" :title="form.id ? '编辑角色' : '新增角色'">
      <template #body>
        <div class="form-grid">
          <div class="field-block">
            <label class="field-label">名称</label>
            <UInput v-model="form.name" />
          </div>
          <div class="field-block">
            <label class="field-label">编码</label>
            <UInput v-model="form.code" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">说明</label>
            <UInput v-model="form.description" />
          </div>
          <div class="field-block field-block-full">
            <label class="field-label">菜单</label>
            <USelectMenu
              v-model="form.menuIds"
              multiple
              searchable
              :items="menuOptions"
              value-key="id"
              label-key="label"
              class="w-full"
            />
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
import { computed, onMounted, reactive, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";

type RoleRow = Record<string, any> & { id?: number };
type MenuRow = { id: number; title: string; path: string };

const toast = useToast();
const roles = ref<RoleRow[]>([]);
const allMenus = ref<MenuRow[]>([]);
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);

const columns: TableColumn<RoleRow>[] = [
  { accessorKey: "name", header: "名称" },
  { accessorKey: "code", header: "编码" },
  { accessorKey: "description", header: "说明" },
  { accessorKey: "actions", header: "操作" },
];

const menuOptions = computed(() =>
  allMenus.value.map((m) => ({
    id: m.id,
    label: `${m.title} ${m.path}`,
  }))
);

const form = reactive({
  id: undefined as number | undefined,
  name: "",
  code: "",
  description: "",
  menuIds: [] as number[],
});

async function load() {
  loading.value = true;
  try {
    roles.value = (await http.get("/roles")) as RoleRow[];
    allMenus.value = (await http.get("/menus")) as MenuRow[];
  } finally {
    loading.value = false;
  }
}

async function open(row?: RoleRow) {
  if (row) {
    form.id = row.id as number;
    form.name = row.name as string;
    form.code = row.code as string;
    form.description = (row.description as string) || "";
    const detail = (await http.get(`/roles/${row.id}`)) as { menuIds: number[] };
    form.menuIds = detail.menuIds || [];
  } else {
    form.id = undefined;
    form.name = "";
    form.code = "";
    form.description = "";
    form.menuIds = [];
  }
  visible.value = true;
}

async function save() {
  const body = {
    name: form.name,
    code: form.code,
    description: form.description,
    menuIds: form.menuIds,
  };
  saving.value = true;
  try {
    if (form.id) {
      await http.put(`/roles/${form.id}`, body);
    } else {
      await http.post("/roles", body);
    }
    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

async function remove(row: RoleRow) {
  const confirmed = window.confirm("确定删除该角色吗？");
  if (!confirmed) return;
  await http.delete(`/roles/${row.id}`);
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
