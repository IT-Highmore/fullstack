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