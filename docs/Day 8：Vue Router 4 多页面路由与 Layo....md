# Day 8：Vue Router 4 多页面路由与 Layout 布局搭建

在 Day 07 中，我们所有的内容（登录界面 + 用户管理表格）都挤在 `App.vue` 一个文件里，靠 `v-if/v-else` 来切换。真正的后台管理系统通常是由 **菜单栏、顶部导航、主内容区域** 组成的通用布局（Layout），并且通过 **Vue Router** 实现页面之间的跳转和权限拦截。

###

### 今天 Day 08 的核心目标

1. 安装并配置 **Vue Router 4**。
2. 搭建标准的后台管理系统 **Layout 布局**（左侧菜单栏 + 顶部 Header + 主内容区）。
3. 配置 **路由守卫（`beforeEach`）**：未登录时强制跳转到 `/login`，已登录时访问 `/login` 自动重定向到首页 `/users`。
4. 将“用户管理”抽离为独立的 View 组件，体验多页面无刷新切换。

​

### 第一步：安装 Vue Router 4

打开前端项目 `fullstack-learning-web` 的终端，按 `Ctrl + C` 暂停服务，运行以下命令安装：

```bash
npm install vue-router@4
```

### 第二步：创建页面组件（Views & Layout）

我们在 `src` 目录下新建两个文件夹：`src/views`（存放具体页面）和 `src/layout`（存放系统整体布局）。

#### 1. 创建登录页 `src/views/Login.vue`

```vue
<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <template #header>
        <h2>系统登录 (Day 08)</h2>
      </template>
      <el-form :model="loginForm" label-width="70px">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="测试账号: admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="测试密码: 123456" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%; margin-top: 10px;" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const loginForm = ref({ username: 'admin', password: '123456' })

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', loginForm.value)
    if (res.data.code === 200) {
      ElMessage.success('登录成功！')
      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('username', res.data.data.username)
      // 登录成功，跳转到系统主页
      router.push('/users')
    } else {
      ElMessage.error(res.data.msg || '登录失败')
    }
  } catch (err) {
    ElMessage.error('登录请求异常')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.login-card {
  width: 380px;
}
</style>
```

#### 2. 创建用户管理页 `src/views/UserManagement.vue`

（将原 `App.vue` 中的用户表格逻辑抽离到这里）：

```vue
<template>
  <el-card class="box-card">
    <template #header>
      <div class="card-header">
        <span>用户管理列表</span>
        <el-button type="primary" icon="Plus" @click="handleCreate">新增用户</el-button>
      </div>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchName"
        placeholder="请输入姓名搜索"
        clearable
        style="width: 240px; margin-right: 12px;"
        @clear="fetchUserList"
        @keyup.enter="fetchUserList"
      />
      <el-button type="primary" icon="Search" @click="fetchUserList">搜索</el-button>
    </div>

    <!-- 用户表格 -->
    <el-table :data="userList" v-loading="loading" border stripe style="width: 100%; margin-top: 16px;">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="age" label="年龄" width="100" align="center" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="操作" width="180" align="center">
        <template #default="scope">
          <el-button size="small" type="warning" @click="handleEdit(scope.row)">编辑</el-button>
          <el-popconfirm title="确定要删除该用户吗？" @confirm="handleDelete(scope.row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="1" :max="120" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const userList = ref([])
const loading = ref(false)
const searchName = ref('')
const dialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({ id: null, name: '', age: 18, email: '' })

const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await api.get('/day04/users', { params: { name: searchName.value } })
    if (res.data.code === 200) {
      userList.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.value = { id: null, name: '', age: 18, email: '' }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入姓名')
    return
  }
  submitLoading.value = true
  try {
    const res = await api.post('/day04/user', form.value)
    if (res.data.code === 200) {
      ElMessage.success(form.value.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      fetchUserList()
    }
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (id) => {
  const res = await api.delete(`/day04/user/${id}`)
  if (res.data.code === 200) {
    ElMessage.success('删除成功')
    fetchUserList()
  }
}

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-bar {
  display: flex;
  align-items: center;
}
</style>
```

#### 3. 创建 Dashboard 占位页 `src/views/Dashboard.vue`

新建一个简单首页，方便验证菜单切换：

```vue
<template>
  <el-card>
    <h2>🎉 欢迎来到全栈后台管理系统！</h2>
    <p>当前登录账号：<b>{{ username }}</b></p>
    <el-text type="info">已成功接入 Vue Router 4 路由导航管理与动态菜单系统。</el-text>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const username = ref('')

onMounted(() => {
  username.value = localStorage.getItem('username') || '未知用户'
})
</script>
```

#### 4. 创建通用布局组件 `src/layout/Layout.vue`

后台系统的经典左侧侧边栏 + 顶部 Header 结构：

```vue
<template>
  <el-container class="layout-container">
    <!-- 左侧菜单栏 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <h3>Fullstack Demo</h3>
      </div>
      <el-menu
        :default-active="route.path"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#fff"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体内容 -->
    <el-container>
      <!-- 顶部 Header -->
      <el-header class="header">
        <div class="header-left">
          <span>当前页面：<b>{{ route.meta.title || '系统' }}</b></span>
        </div>
        <div class="header-right">
          <span class="user-info">欢迎，{{ currentUser }}</span>
          <el-button type="danger" plain size="small" icon="SwitchButton" @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </el-header>

      <!-- 中间内容区域（路由出口） -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const currentUser = ref(localStorage.getItem('username') || 'Admin')

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  ElMessage.info('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
  color: #fff;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  background-color: #2b2f3a;
}
.logo h3 {
  margin: 0;
  color: #fff;
  font-size: 18px;
}
.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
```

### 第三步：编写 `src/router/index.js` 路由表与路由守卫

新建文件夹 `src/router` 并创建 `src/router/index.js`：

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '控制台首页' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('../views/UserManagement.vue'),
        meta: { title: '用户管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 🔐 全局前置路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path === '/login') {
    if (token) {
      // 已登录用户访问登录页，直接重定向到主页
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      // 未登录用户访问受保护路由，重定向到登录页
      next('/login')
    } else {
      next()
    }
  }
})

export default router
```

​

### 第四步：修改 `src/main.js` 和 `src/App.vue`

#### 1. 修改 `src/main.js` 挂载路由

打开 `src/main.js`，引入路由：

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 引入 Element Plus 及样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus)
app.use(router) // 👈 挂载 Vue Router
app.mount('#app')
```

#### 2. 简化 `src/App.vue`

打开 `src/App.vue`，将其清空，只保留路由出口组件：

```vue
<template>
  <router-view />
</template>

<script setup>
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}
</style>
```

​

### 🧪 验证体验

1. 在前端终端启动 `npm run dev`。
2. 浏览器打开 `http://localhost:5173/`，观察是否会自动触发路由守卫重定向到 `/login`。
3. 输入 `admin` / `123456` 登录，页面将无缝切入 `/dashboard` 控制台！
4. 点击左侧侧边栏的“**用户管理**”，观察页面 URL 切换至 `/users`，并且页面无刷新载入表格。
5. 手动在浏览器地址栏输入 `http://localhost:5173/login`，测试是否会被拦截并拉回 `/dashboard`。
