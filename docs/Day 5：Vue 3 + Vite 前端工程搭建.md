# **Day 5：Vue 3 + Vite 前端工程搭建**

在前面 4 天里，我们一步步搭建好了 Spring Boot 3 + MySQL + MyBatis-Plus + Redis 的全套高性能后端架构。从今天开始，我们要为它配上一个现代化、高颜值的 **Vue 3 前端界面**，真正迈向全栈！

​

### 今天 Day 05 的核心目标

1. 用 **Vite** 脚手架快速初始化一个 **Vue 3** 项目。
2. 引入前端最流行的 UI 组件库 **Element Plus** 和 HTTP 请求库 **Axios**。
3. 调整后端跨域配置（CORS），让前端 Vue 能够顺利调用我们之前写的 Spring Boot 接口。

​

### 第一步：初始化 Vue 3 项目

确保你的电脑上已经安装了 Node.js（建议版本 ≥ 18）。

打开电脑终端（Mac 终端或 Windows CMD/PowerShell），切换到你的工作目录（建议和后端项目 `fullstack-learning-demo` 平级放置，例如放在 `Desktop` 下）：

```bash
cd ~/Desktop
npm create vite@latest fullstack-learning-web -- --template vue
```

终端会提示创建成功，接着依次运行以下 3 条命令进入项目目录、安装依赖并启动：

```bash
cd fullstack-learning-web
npm install
npm run dev
```

运行成功后，终端会输出一个本地地址（如 `http://localhost:5173`）。在浏览器打开它，看到 Vite + Vue 的欢迎页面就说明前端工程搭建好了！

​

### 第二步：安装 Element Plus 与 Axios

在前端项目目录 `fullstack-learning-web` 的终端中，按 `Ctrl + C` 暂停服务，运行以下命令安装依赖：

```bash
npm install element-plus @element-plus/icons-vue axios
```

安装完成后，打开 `src/main.js`，将其修改为全量引入 Element Plus 的配置：

```javascript
import { createApp } from 'vue'
import App from './App.vue'

// 引入 Element Plus 及样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.mount('#app')
```

###

### 第三步：解决后端跨域问题（CORS）

因为前端运行在 `http://localhost:5173`，后端运行在 `http://localhost:8080`，协议/域名/端口有任何不同都会被浏览器拦截。

回到你的 **Spring Boot 项目** `fullstack-learning-demo`，在 `src/main/java/com/example/fullstacklearningdemo/` 下新建一个包 `config`，并创建 `CorsConfig.java`：

```java
package com.example.fullstacklearningdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许跨域访问的路径
                .allowedOriginPatterns("*") // 允许跨域请求的域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方式
                .allowedHeaders("*") // 允许的响应头
                .allowCredentials(true) // 是否发送 Cookie
                .maxAge(3600); // 预检请求的有效期（秒）
    }
}
```

​

### 第四步：写一个前端测试页面调通后端

打开前端项目的 `src/App.vue`，清空原有内容，换成下面这段带 Element Plus 按钮与 Axios 请求的代码：

```vue
<template>
  <div style="padding: 40px; max-width: 600px; margin: 0 auto;">
    <el-card header="Day 05 - Vue 3 + Spring Boot 联调测试">
      <el-space direction="vertical" alignment="start" style="width: 100%;">
        <el-input v-model="userId" placeholder="输入用户 ID" style="width: 200px;" />
        <el-button type="primary" :loading="loading" @click="fetchUser">
          查询用户（走 Redis 缓存）
        </el-button>

        <el-alert v-if="userInfo" type="success" :closable="false" show-icon>
          <template #title>
            查询成功！响应数据：
          </template>
          <pre>{{ userInfo }}</pre>
        </el-alert>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const userId = ref('1')
const userInfo = ref(null)
const loading = ref(false)

const fetchUser = async () => {
  if (!userId.value) {
    ElMessage.warning('请输入用户 ID')
    return
  }
  loading.value = true
  try {
    const res = await axios.get(`http://localhost:8080/api/day04/user/${userId.value}`)
    if (res.data.code === 200) {
      userInfo.value = res.data.data
      ElMessage.success('数据获取成功！')
    } else {
      ElMessage.error(res.data.msg || '获取失败')
    }
  } catch (err) {
    ElMessage.error('网络请求异常，请检查后端是否启动')
  } finally {
    loading.value = false
  }
}
</script>
```

​

### 🧪 验证流程

1. 启动后端 Spring Boot 项目 `FullstackLearningDemoApplication`。
2. 在前端项目终端运行 `npm run dev` 启动前端。
3. 打开浏览器访问 `http://localhost:5173`，点击“**查询用户**”按钮。
4. 观察页面上是否成功展示出来自 Day 04 接口的数据，同时看一眼后端控制台，确认 Redis 命中/未命中日志正常打出。
