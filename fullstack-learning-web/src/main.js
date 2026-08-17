import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { permission } from './directives/permission' // 👈 引入权限指令

// 引入 Element Plus 及样式
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";

const app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.directive('permission', permission) // 👈 注册全局指令 v-permission

app.use(ElementPlus);
app.use(router); // 👈 挂载 Vue Router
app.mount("#app");
