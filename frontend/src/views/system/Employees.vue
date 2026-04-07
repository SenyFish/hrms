<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>员工管理</span>
        <UButton color="primary" icon="i-lucide-plus" @click="open()">新增</UButton>
      </div>
    </template>

    <div class="toolbar">
      <UInput
        v-model="query.keyword"
        class="toolbar-input"
        size="lg"
        variant="subtle"
        icon="i-lucide-search"
        placeholder="请输入账号、姓名、工号、岗位、手机号等关键字"
        @keyup.enter="search"
      />
      <UButton color="primary" @click="search">查询</UButton>
      <UButton color="neutral" variant="soft" @click="resetSearch">重置</UButton>
    </div>

    <UTable :data="users" :columns="columns" :loading="loading" class="table-wrap">
      <template #role-cell="{ row }">{{ row.original.role?.name || "-" }}</template>
      <template #departmentId-cell="{ row }">{{ departmentName(row.original.departmentId) }}</template>
      <template #birthday-cell="{ row }">{{ row.original.birthday || "-" }}</template>
      <template #hireDate-cell="{ row }">{{ row.original.hireDate || "-" }}</template>
      <template #positionName-cell="{ row }">{{ row.original.positionName || "-" }}</template>
      <template #actions-cell="{ row }">
        <div class="action-group">
          <UButton color="primary" variant="ghost" size="sm" @click="open(row.original)">编辑</UButton>
          <UButton color="error" variant="ghost" size="sm" @click="remove(row.original)">删除</UButton>
        </div>
      </template>
    </UTable>

    <div class="pager">
      <span class="pager-total">共 {{ total }} 条</span>
      <UPagination v-model:page="query.page" :total="total" :items-per-page="query.size" show-edges @update:page="loadUsers" />
      <USelectMenu
        v-model="query.size"
        :items="pageSizeOptions"
        value-key="value"
        label-key="label"
        class="size-select"
        @update:model-value="handleSizeChange"
      />
    </div>

    <UModal v-model:open="visible" :title="form.id ? '编辑员工' : '新增员工'">
      <template #body>
        <div class="form-grid">
          <div v-if="!form.id" class="field-block">
            <label class="field-label">用户名</label>
            <UInput v-model="form.username" />
          </div>
          <div class="field-block">
            <label class="field-label">{{ form.id ? "密码（可选）" : "密码" }}</label>
            <UInput v-model="form.password" type="password" />
          </div>
          <div class="field-block">
            <label class="field-label">姓名</label>
            <UInput v-model="form.realName" />
          </div>
          <div class="field-block">
            <label class="field-label">工号</label>
            <UInput v-model="form.employeeNo" />
          </div>
          <div class="field-block">
            <label class="field-label">岗位</label>
            <UInput v-model="form.positionName" placeholder="例如：前端工程师、招聘专员" />
          </div>
          <div class="field-block">
            <label class="field-label">部门</label>
            <USelectMenu v-model="form.departmentId" :items="depts" value-key="id" label-key="name" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">角色</label>
            <USelectMenu v-model="form.roleId" :items="roles" value-key="id" label-key="name" class="w-full" />
          </div>
          <div class="field-block">
            <label class="field-label">手机</label>
            <UInput v-model="form.phone" />
          </div>
          <div class="field-block">
            <label class="field-label">邮箱</label>
            <UInput v-model="form.email" />
          </div>
          <div class="field-block">
            <label class="field-label">生日</label>
            <UInput v-model="form.birthday" type="date" size="lg" variant="subtle" icon="i-lucide-calendar-days" class="time-input" />
            <span class="field-hint">用于首页生日关怀提醒。</span>
          </div>
          <div class="field-block">
            <label class="field-label">入职日期</label>
            <UInput v-model="form.hireDate" type="date" size="lg" variant="subtle" icon="i-lucide-briefcase-business" class="time-input" />
            <span class="field-hint">用于首页入职周年提醒。</span>
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

    <UModal v-model:open="deleteVisible" title="确认删除员工">
      <template #body>
        <div class="delete-dialog">
          <div class="delete-icon-wrap">
            <UIcon name="i-lucide-triangle-alert" class="delete-icon" />
          </div>
          <div class="delete-copy">
            <div class="delete-title">删除后将同步清理该员工关联业务数据</div>
            <div class="delete-desc">
              确定删除
              <strong>{{ deleteTarget?.realName || deleteTarget?.username || "该员工" }}</strong>
              吗？此操作无法撤销。
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <div class="modal-actions">
          <UButton color="neutral" variant="soft" @click="deleteVisible = false">取消</UButton>
          <UButton color="error" :loading="deleting" @click="confirmRemove">确认删除</UButton>
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

type UserRow = {
  id?: number;
  username?: string;
  realName?: string;
  employeeNo?: string;
  positionName?: string;
  departmentId?: number;
  phone?: string;
  email?: string;
  birthday?: string;
  hireDate?: string;
  role?: { id?: number; name?: string };
};

type UserListResponse = {
  list: UserRow[];
  total: number;
};

const toast = useToast();
const users = ref<UserRow[]>([]);
const total = ref(0);
const depts = ref<Array<{ id: number; name: string }>>([]);
const roles = ref<Array<{ id: number; name: string }>>([]);
const visible = ref(false);
const deleteVisible = ref(false);
const saving = ref(false);
const deleting = ref(false);
const loading = ref(false);
const deleteTarget = ref<UserRow | null>(null);

const pageSizeOptions = [
  { label: "10 条/页", value: 10 },
  { label: "20 条/页", value: 20 },
  { label: "50 条/页", value: 50 },
  { label: "100 条/页", value: 100 },
];

const columns: TableColumn<UserRow>[] = [
  { accessorKey: "username", header: "账号" },
  { accessorKey: "realName", header: "姓名" },
  { accessorKey: "employeeNo", header: "工号" },
  { accessorKey: "positionName", header: "岗位" },
  { accessorKey: "role", header: "角色" },
  { accessorKey: "departmentId", header: "部门" },
  { accessorKey: "phone", header: "手机号" },
  { accessorKey: "birthday", header: "生日" },
  { accessorKey: "hireDate", header: "入职日期" },
  { accessorKey: "actions", header: "操作" },
];

const query = reactive({
  keyword: "",
  page: 1,
  size: 10,
});

const form = reactive({
  id: undefined as number | undefined,
  username: "",
  password: "",
  realName: "",
  employeeNo: "",
  positionName: "",
  departmentId: undefined as number | undefined,
  roleId: undefined as number | undefined,
  phone: "",
  email: "",
  birthday: "",
  hireDate: "",
});

async function loadUsers() {
  loading.value = true;
  try {
    const data = (await http.get("/users", {
      params: {
        keyword: query.keyword || undefined,
        page: query.page,
        size: query.size,
      },
    })) as UserListResponse;
    users.value = data.list || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  try {
    const [departmentList, roleList] = await Promise.all([http.get("/departments"), http.get("/roles")]);
    depts.value = departmentList as typeof depts.value;
    roles.value = roleList as typeof roles.value;
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "部门或角色选项加载失败",
      color: "error",
    });
  }
}

function search() {
  query.page = 1;
  loadUsers();
}

function resetSearch() {
  query.keyword = "";
  query.page = 1;
  query.size = 10;
  loadUsers();
}

function handleSizeChange() {
  query.page = 1;
  loadUsers();
}

function resetForm() {
  form.id = undefined;
  form.username = "";
  form.password = "";
  form.realName = "";
  form.employeeNo = "";
  form.positionName = "";
  form.departmentId = undefined;
  form.roleId = undefined;
  form.phone = "";
  form.email = "";
  form.birthday = "";
  form.hireDate = "";
}

function departmentName(id?: number) {
  return depts.value.find((item) => item.id === id)?.name || "-";
}

function open(row?: UserRow) {
  if (row) {
    form.id = row.id;
    form.username = row.username || "";
    form.password = "";
    form.realName = row.realName || "";
    form.employeeNo = row.employeeNo || "";
    form.positionName = row.positionName || "";
    form.departmentId = row.departmentId;
    form.roleId = row.role?.id;
    form.phone = row.phone || "";
    form.email = row.email || "";
    form.birthday = row.birthday || "";
    form.hireDate = row.hireDate || "";
  } else {
    resetForm();
  }
  visible.value = true;
}

async function save() {
  if (!form.realName.trim()) {
    toast.add({ title: "请输入姓名", color: "warning" });
    return;
  }
  if (!form.id && !form.username.trim()) {
    toast.add({ title: "请输入用户名", color: "warning" });
    return;
  }
  if (!form.id && !form.password.trim()) {
    toast.add({ title: "新增员工必须设置密码", color: "warning" });
    return;
  }
  if (!form.departmentId || !form.roleId) {
    toast.add({ title: "请选择部门和角色", color: "warning" });
    return;
  }

  saving.value = true;
  try {
    const payload = { ...form };
    let savedUser: UserRow;
    if (form.id) {
      savedUser = (await http.put(`/users/${form.id}`, payload)) as UserRow;
    } else {
      savedUser = (await http.post("/users", payload)) as UserRow;
    }

    if ((savedUser.birthday || "") !== (form.birthday || "") || (savedUser.hireDate || "") !== (form.hireDate || "")) {
      throw new Error("员工日期信息未成功更新，请重试");
    }

    toast.add({ title: "保存成功", color: "success" });
    visible.value = false;
    await loadUsers();
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "保存失败",
      color: "error",
    });
  } finally {
    saving.value = false;
  }
}

function remove(row: UserRow) {
  deleteTarget.value = row;
  deleteVisible.value = true;
}

async function confirmRemove() {
  if (!deleteTarget.value?.id) return;
  deleting.value = true;
  try {
    await http.delete(`/users/${deleteTarget.value.id}`);
    toast.add({ title: "已删除", color: "success" });
    deleteVisible.value = false;
    deleteTarget.value = null;
    if (users.value.length === 1 && query.page > 1) {
      query.page -= 1;
    }
    await loadUsers();
  } catch (error: unknown) {
    toast.add({
      title: error instanceof Error ? error.message : "删除失败",
      color: "error",
    });
  } finally {
    deleting.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadOptions()]);
});
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-input {
  width: 320px;
}

.table-wrap {
  overflow: hidden;
}

.action-group {
  display: flex;
  gap: 6px;
}

.pager {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.pager-total {
  color: #6b7280;
  font-size: 14px;
}

.size-select {
  width: 120px;
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

.delete-dialog {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 6px 2px;
}

.delete-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  flex-shrink: 0;
}

.delete-icon {
  width: 22px;
  height: 22px;
}

.delete-copy {
  display: grid;
  gap: 6px;
}

.delete-title {
  font-size: 15px;
  font-weight: 700;
  color: #173127;
}

.delete-desc {
  line-height: 1.7;
  color: #6b7280;
}

.delete-desc strong {
  color: #173127;
}

@media (max-width: 900px) {
  .toolbar {
    flex-wrap: wrap;
  }

  .toolbar-input {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
