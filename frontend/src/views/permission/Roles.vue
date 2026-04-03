<template>
  <div class="app-page">
    <PageSection
      eyebrow="Security"
      title="角色管理"
      description="维护角色定义，并通过菜单列表分配访问范围。接口仍然沿用现有角色与菜单端点。"
      flush
    >
      <template #actions>
        <button class="frappe-button" data-variant="solid" type="button" @click="open()">新增角色</button>
      </template>

      <div class="app-table">
        <el-table :data="roles" border>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="code" label="编码" width="140" />
          <el-table-column prop="description" label="说明" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" link @click="open(row)">编辑</el-button>
              <el-button type="danger" link @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </PageSection>

    <el-dialog v-model="visible" title="角色" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="菜单">
          <el-select v-model="form.menuIds" multiple filterable style="width: 100%" placeholder="选择菜单">
            <el-option v-for="m in allMenus" :key="m.id" :label="`${m.title} ${m.path}`" :value="m.id" />
          </el-select>
        </el-form-item>
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

const roles = ref<Array<Record<string, unknown>>>([]);
const allMenus = ref<Array<{ id: number; title: string; path: string }>>([]);
const visible = ref(false);
const form = reactive({
  id: undefined as number | undefined,
  name: "",
  code: "",
  description: "",
  menuIds: [] as number[],
});

async function load() {
  roles.value = (await http.get("/roles")) as typeof roles.value;
  allMenus.value = (await http.get("/menus")) as typeof allMenus.value;
}

async function open(row?: Record<string, unknown>) {
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
  if (form.id) {
    await http.put(`/roles/${form.id}`, body);
  } else {
    await http.post("/roles", body);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  load();
}

async function remove(row: Record<string, unknown>) {
  await ElMessageBox.confirm("确定删除该角色吗？", "提示");
  await http.delete(`/roles/${row.id}`);
  ElMessage.success("已删除");
  load();
}

onMounted(load);
</script>
