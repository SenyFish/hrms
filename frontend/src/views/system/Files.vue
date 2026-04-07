<template>
  <UCard variant="soft">
    <template #header>
      <div class="header-bar">
        <span>文件上传与下载</span>
        <UButton color="primary" icon="i-lucide-upload" :loading="uploading" @click="triggerFilePick">上传文件</UButton>
      </div>
    </template>

    <input ref="fileInput" type="file" class="hidden-input" @change="handleFileChange" />

    <UTable :data="list" :columns="columns" :loading="loading" class="table-wrap">
      <template #actions-cell="{ row }">
        <UButton color="primary" variant="ghost" size="sm" @click="download(row.original.id)">下载</UButton>
      </template>
    </UTable>
  </UCard>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import type { TableColumn } from "@nuxt/ui";
import { useToast } from "@nuxt/ui/composables";
import http from "@/api/http";
import { useUserStore } from "@/stores/user";
import { downloadByFetch } from "@/utils/download";

type FileRow = { id: number; originalName: string; sizeBytes: number };

const toast = useToast();
const store = useUserStore();
const list = ref<FileRow[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const loading = ref(false);
const uploading = ref(false);

const columns: TableColumn<FileRow>[] = [
  { accessorKey: "originalName", header: "文件名" },
  { accessorKey: "sizeBytes", header: "大小(字节)" },
  { accessorKey: "actions", header: "操作" },
];

const headers = computed(() => ({
  Authorization: store.token ? `Bearer ${store.token}` : "",
}));

async function load() {
  loading.value = true;
  try {
    list.value = (await http.get("/files")) as FileRow[];
  } finally {
    loading.value = false;
  }
}

function triggerFilePick() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);

  uploading.value = true;
  try {
    await fetch("/api/files/upload", {
      method: "POST",
      headers: headers.value,
      body: formData,
    }).then(async (res) => {
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "上传失败");
      }
    });
    toast.add({ title: "上传成功", color: "success" });
    await load();
  } catch (e: unknown) {
    toast.add({ title: e instanceof Error ? e.message : "上传失败", color: "error" });
  } finally {
    uploading.value = false;
    target.value = "";
  }
}

async function download(id: number) {
  try {
    await downloadByFetch(
      `/api/files/${id}/download`,
      { headers: { Authorization: `Bearer ${store.token}` } },
      "download"
    );
  } catch (e: unknown) {
    toast.add({ title: e instanceof Error ? e.message : "下载失败", color: "error" });
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

.hidden-input {
  display: none;
}

.table-wrap {
  overflow: hidden;
}
</style>
