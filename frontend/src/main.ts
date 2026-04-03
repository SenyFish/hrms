import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import { Lock, User } from "@element-plus/icons-vue";
import App from "./App.vue";
import router from "./router";
import "./styles/theme.css";

const app = createApp(App);
app.component("User", User);
app.component("Lock", Lock);
app.use(createPinia());
app.use(router);
app.use(ElementPlus, { locale: zhCn });
app.mount("#app");
