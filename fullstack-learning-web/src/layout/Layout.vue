<template>
  <el-container class="layout-container">
    <!-- 左侧菜单栏 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <h3>Fullstack Demo</h3>
      </div>
      <el-menu :default-active="route.path" class="el-menu-vertical" background-color="#304156" text-color="#fff"
        active-text-color="#409EFF" router>
        <el-menu-item index="/dashboard">
          <el-icon>
            <Odometer />
          </el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon>
            <User />
          </el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/file-upload">
          <el-icon>
            <Upload />
          </el-icon>
          <span>文件上传</span>
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