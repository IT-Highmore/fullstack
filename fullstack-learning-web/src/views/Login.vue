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