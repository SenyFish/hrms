<template>
  <el-card>
    <template #header>文件上传与下载</template>
    <el-upload :action="uploadUrl" :headers="headers" :on-success="load" :show-file-list="false">
      <el-button type="primary">上传文件</el-button>
    </el-upload>
    <el-table :data="list" style="margin-top: 16px" border>
      <el-table-column prop="originalName" label="文件名" />
      <el-table-column prop="sizeBytes" label="大小(字节)" width="120" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" link @click="download(row.id)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";
import { downloadByFetch } from "@/utils/download";

const store = useUserStore();
const list = ref<Array<{ id: number; originalName: string; sizeBytes: number }>>([]);

const uploadUrl = "/api/files/upload";
const headers = computed(() => ({
  Authorization: store.token ? `Bearer ${store.token}` : "",
}));

async function load() {
  list.value = (await http.get("/files")) as typeof list.value;
}

async function download(id: number) {
  try {
    await downloadByFetch(
      `/api/files/${id}/download`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      "download"
    );
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : "下载失败");
  }
}

onMounted(load);
</script>
