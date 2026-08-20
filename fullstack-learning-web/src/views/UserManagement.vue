<template>
  <el-card class="box-card">
    <template #header>
      <div class="card-header">
        <span>用户管理列表 (Day 11 全功能版)</span>
        <el-button v-permission="'user:add'" type="primary" icon="Plus" @click="handleCreate">新增用户</el-button>
      </div>
    </template>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="请输入姓名搜索" clearable style="width: 240px; margin-right: 12px;"
        @clear="handleSearch" @keyup.enter="handleSearch" />
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
    </div>

    <!-- 用户表格 -->
    <el-table :data="userList" v-loading="loading" border stripe style="width: 100%; margin-top: 16px;">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="roleName" label="角色" width="130" align="center">
        <template #default="scope">
          <el-tag type="success">{{ scope.row.roleName || '普通用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="90" align="center" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="操作" width="180" align="center">
        <template #default="scope">
          <el-button v-permission="'user:edit'" size="small" type="warning"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-popconfirm title="确定要删除该用户吗？" @confirm="handleDelete(scope.row.id)">
            <template #reference>
              <el-button v-permission="'user:delete'" size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部分页组件 -->
    <div class="pagination-container">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[3, 5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="fetchUserPage"
        @current-change="fetchUserPage" />
    </div>

    <!-- 补全：新增/编辑弹窗 -->
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

// 分页状态
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)

// 补全：弹窗与表单状态
const dialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({ id: null, name: '', age: 18, email: '' })

// 获取分页列表
const fetchUserPage = async () => {
  loading.value = true
  try {
    const res = await api.get('/day11/users/page', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        name: searchName.value
      }
    })
    if (res.data.code === 200) {
      userList.value = res.data.data.records
      total.value = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchUserPage()
}

// 补全：新增按钮点击处理
const handleCreate = () => {
  form.value = { id: null, name: '', age: 18, email: '' }
  dialogVisible.value = true
}

// 补全：编辑按钮点击处理
const handleEdit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

// 补全：提交表单（新增/修改）
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
      fetchUserPage() // 重新刷新当前页数据
    }
  } finally {
    submitLoading.value = false
  }
}

// 补全：删除操作
const handleDelete = async (id) => {
  const res = await api.delete(`/day04/user/${id}`)
  if (res.data.code === 200) {
    ElMessage.success('删除成功')
    fetchUserPage() // 重新刷新当前页数据
  }
}

onMounted(() => {
  fetchUserPage()
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

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>