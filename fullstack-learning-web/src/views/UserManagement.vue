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
      <el-input v-model="searchName" placeholder="请输入姓名搜索" clearable style="width: 240px; margin-right: 12px;"
        @clear="fetchUserList" @keyup.enter="fetchUserList" />
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