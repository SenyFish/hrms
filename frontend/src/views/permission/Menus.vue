<template>
  <el-card>
    <template #header>
      <span>菜单管理</span>
      <el-button type="primary" style="float: right" @click="open()">新增菜单</el-button>
    </template>
    <el-table :data="list" border row-key="id" default-expand-all>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="path" label="路由路径" />
      <el-table-column prop="icon" label="图标" width="100" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="parentId" label="父级ID" width="90" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button type="primary" link @click="open(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="菜单" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="路径"><el-input v-model="form.path" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Element 图标名" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="父级ID"><el-input-number v-model="form.parentId" :min="0" /></el-form-item>
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
import http from "@/api/http";
import { ElMessage, ElMessageBox } from "element-plus";

const list = ref<Array<Record<string, unknown>>>([]);
const visible = ref(false);
const form = reactive({
  id: undefined as number | undefined,
  title: "",
  path: "",
  icon: "",
  sortOrder: 1,
  parentId: 0,
});

async function load() {
  list.value = (await http.get("/menus")) as typeof list.value;
}

function open(row?: Record<string, unknown>) {
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
  if (form.id) {
    await http.put(`/menus/${form.id}`, form);
  } else {
    await http.post("/menus", form);
  }
  ElMessage.success("保存成功");
  visible.value = false;
  load();
}

async function remove(row: Record<string, unknown>) {
  await ElMessageBox.confirm("确定删除？", "提示");
  await http.delete(`/menus/${row.id}`);
  ElMessage.success("已删除");
  load();
}

onMounted(load);
</script>
