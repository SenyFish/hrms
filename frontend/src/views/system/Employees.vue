<template>
  <div class="app-page">
    <PageSection
      eyebrow="System"
      title="员工管理"
      description="管理员工账号、角色、部门以及基础档案。首轮重构只替换界面层，保留现有用户接口和字段。"
      flush
    >
      <template #actions>
        <button class="frappe-button" data-variant="solid" type="button" @click="open()">新增员工</button>
      </template>

      <FilterToolbar>
        <el-input v-model="query.keyword" class="page-field page-field--wide" placeholder="搜索账号、姓名、工号或手机号" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </FilterToolbar>

      <div class="app-table">
        <el-table :data="users" border>
          <el-table-column prop="username" label="账号" width="140" />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column prop="employeeNo" label="工号" width="130" />
          <el-table-column label="角色" width="140">
            <template #default="{ row }">{{ row.role?.name || "-" }}</template>
          </el-table-column>
          <el-table-column prop="departmentId" label="部门 ID" width="100" />
          <el-table-column prop="phone" label="手机号" width="150" />
          <el-table-column label="生日" width="130">
            <template #default="{ row }">{{ row.birthday || "-" }}</template>
          </el-table-column>
          <el-table-column label="入职日期" width="130">
            <template #default="{ row }">{{ row.hireDate || "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="open(row)">编辑</el-button>
              <el-button type="danger" link @click="remove(row)">删除</el-button>
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
          @current-change="loadUsers"
          @size-change="handleSizeChange"
        />
      </div>
    </PageSection>

    <el-dialog v-model="visible" :title="form.id ? '编辑员工' : '新增员工'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item v-if="!form.id" label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="form.id ? '密码(可选)' : '密码'" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空表示不修改' : '请输入初始密码'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="form.employeeNo" />
        </el-form-item>
        <el-form-item label="部门" prop="departmentId">
          <el-select v-model="form.departmentId" placeholder="选择部门" style="width: 100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import http from "@/api/http";
import FilterToolbar from "@/components/ui/FilterToolbar.vue";
import PageSection from "@/components/ui/PageSection.vue";

type UserRow = {
  id?: number;
  username?: string;
  realName?: string;
  employeeNo?: string;
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

const users = ref<UserRow[]>([]);
const total = ref(0);
const depts = ref<Array<{ id: number; name: string }>>([]);
const roles = ref<Array<{ id: number; name: string }>>([]);
const visible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
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
  departmentId: undefined as number | undefined,
  roleId: undefined as number | undefined,
  phone: "",
  email: "",
  birthday: "",
  hireDate: "",
});

const rules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  realName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (!form.id && (!value || !value.trim())) {
          callback(new Error("新增员工必须设置密码"));
          return;
        }
        callback();
      },
      trigger: "blur",
    },
  ],
  departmentId: [{ required: true, message: "请选择部门", trigger: "change" }],
  roleId: [{ required: true, message: "请选择角色", trigger: "change" }],
};

async function loadUsers() {
  const data = (await http.get("/users", {
    params: {
      keyword: query.keyword || undefined,
      page: query.page,
      size: query.size,
    },
  })) as UserListResponse;
  users.value = data.list || [];
  total.value = data.total || 0;
}

async function loadOptions() {
  depts.value = (await http.get("/departments")) as typeof depts.value;
  roles.value = (await http.get("/roles")) as typeof roles.value;
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
  form.departmentId = undefined;
  form.roleId = undefined;
  form.phone = "";
  form.email = "";
  form.birthday = "";
  form.hireDate = "";
}

function open(row?: UserRow) {
  if (row) {
    form.id = row.id;
    form.username = row.username || "";
    form.password = "";
    form.realName = row.realName || "";
    form.employeeNo = row.employeeNo || "";
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
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    saving.value = true;
    const payload = { ...form };
    const savedUser = (form.id ? await http.put(`/users/${form.id}`, payload) : await http.post("/users", payload)) as UserRow;

    if ((savedUser.birthday || "") !== (form.birthday || "") || (savedUser.hireDate || "") !== (form.hireDate || "")) {
      throw new Error("员工日期信息未成功更新，请重试");
    }

    ElMessage.success("保存成功");
    visible.value = false;
    await loadUsers();
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : "保存失败");
  } finally {
    saving.value = false;
  }
}

async function remove(row: UserRow) {
  await ElMessageBox.confirm("确定删除该员工吗？", "提示");
  await http.delete(`/users/${row.id}`);
  ElMessage.success("已删除");
  if (users.value.length === 1 && query.page > 1) {
    query.page -= 1;
  }
  await loadUsers();
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadOptions()]);
});
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
