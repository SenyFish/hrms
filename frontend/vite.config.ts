import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import Icons from "unplugin-icons/vite";
import { resolve } from "path";

export default defineConfig({
  plugins: [vue(), Icons({ compiler: "vue3" })],
  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
      "~icons/lucide/info": resolve(__dirname, "src/components/icons/LucideInfo.vue"),
      "frappe-ui-button": resolve(__dirname, "node_modules/frappe-ui/src/components/Button/index.ts"),
      "frappe-ui-form-control": resolve(__dirname, "node_modules/frappe-ui/src/components/FormControl/index.ts"),
      "frappe-ui-input": resolve(__dirname, "node_modules/frappe-ui/src/components/Input.vue"),
      "frappe-ui-resources-plugin": resolve(__dirname, "node_modules/frappe-ui/src/resources/plugin.js"),
      "feather-icons": resolve(__dirname, "src/shims/feather-icons.ts"),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) {
            return;
          }
          if (id.includes("echarts")) {
            return "echarts";
          }
          if (id.includes("element-plus") || id.includes("@element-plus")) {
            return "element-plus";
          }
          if (id.includes("vue") || id.includes("pinia") || id.includes("vue-router")) {
            return "vue-vendor";
          }
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
