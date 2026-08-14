import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";

// 引入 Element Plus 及样式
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";

const app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(ElementPlus);
app.use(router); // 👈 挂载 Vue Router
app.mount("#app");
