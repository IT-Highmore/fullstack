# Day 6：Vue 3 增删改查表格与状态管理

昨天我们把 Vue 3 + Element Plus 的基础设施和跨域请求彻底打通了。今天我们要把这些零散的 API 串联起来，用 Element Plus 的 `<el-table>`、`<el-pagination>` 和 `<el-dialog>` 搭建一个标准的**后台管理系统用户管理界面**，实现数据列表展示、条件查询、新增、编辑和删除全流程！

​

### 第一步：在后端补充分页/列表与增删改接口

为了配合前端表格展示，我们在后端的 `Day04Controller.java` 里补齐**列表查询、新增和删除**接口（复用 MyBatis-Plus 提供的标准 CRUD 能力）。

打开 `day04/Day04Controller.java`，补充如下接口：

```java
package com.example.fullstacklearningdemo.day04;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.fullstacklearningdemo.day02.Result;
import com.example.fullstacklearningdemo.day03.User;
import com.example.fullstacklearningdemo.day03.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day04")
public class Day04Controller {

    @Autowired
    @Qualifier("day04UserService")
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    // 1. 获取用户列表（支持按姓名模糊搜索）
    // GET http://localhost:8080/api/day04/users?name=张
    @GetMapping("/users")
    public Result<List<User>> getUserList(@RequestParam(required = false) String name) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            queryWrapper.like(User::getName, name);
        }
        List<User> list = userMapper.selectList(queryWrapper);
        return Result.success(list);
    }

    // 2. 新增或更新用户
    // POST http://localhost:8080/api/day04/user
    @PostMapping("/user")
    public Result<String> saveOrUpdateUser(@RequestBody User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
            userService.evictUserCache(user.getId()); // 更新时同步清除 Redis 缓存
        }
        return Result.success("保存成功");
    }

    // 3. 删除用户
    // DELETE http://localhost:8080/api/day04/user/1
    @DeleteMapping("/user/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        userService.evictUserCache(id); // 删除时清理缓存
        return Result.success("删除成功");
    }

    // 单条带缓存查询（保留 Day04 接口）
    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserByIdWithCache(id);
        return Result.success(user);
    }
}
```

后端改好后保存，重启 `FullstackLearningDemoApplication`。

​

### 第二步：编写前端 `App.vue` 完整表格界面

打开前端项目的 `src/App.vue`，清空内容，换成下面这段标准的 Element Plus CRUD 页面：

```vue
<template>
  <div class="container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h2>用户管理系统 (Day 06)</h2>
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
    </el-card>

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE = 'http://localhost:8080/api/day04'

const userList = ref([])
const loading = ref(false)
const searchName = ref('')

const dialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({
  id: null,
  name: '',
  age: 18,
  email: ''
})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE}/users`, {
      params: { name: searchName.value }
    })
    if (res.data.code === 200) {
      userList.value = res.data.data
    } else {
      ElMessage.error(res.data.msg || '获取用户列表失败')
    }
  } catch (err) {
    ElMessage.error('无法连接后端服务，请检查后端状态')
  } finally {
    loading.value = false
  }
}

// 打开新增弹窗
const handleCreate = () => {
  form.value = { id: null, name: '', age: 18, email: '' }
  dialogVisible.value = true
}

// 打开编辑弹窗
const handleEdit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

// 提交表单（新增/修改）
const submitForm = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入姓名')
    return
  }
  submitLoading.value = true
  try {
    const res = await axios.post(`${API_BASE}/user`, form.value)
    if (res.data.code === 200) {
      ElMessage.success(form.value.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      fetchUserList()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (err) {
    ElMessage.error('请求失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除用户
const handleDelete = async (id) => {
  try {
    const res = await axios.delete(`${API_BASE}/user/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchUserList()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (err) {
    ElMessage.error('删除请求失败')
  }
}

// 页面加载时自动获取列表
onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.container {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h2 {
  margin: 0;
  font-size: 20px;
}
.search-bar {
  display: flex;
  align-items: center;
}
</style>
```

​

### 🧪 验证体验

1. 确保后端 Spring Boot 服务已启动。
2. 前端项目运行 `npm run dev`，打开 `http://localhost:5173`。
3. 测试**列表刷新**、**模糊搜索**、**新增用户**、**修改用户**和**气泡确认框删除**。

​

今天 **Day 06** 我们顺利完成了全栈打通的核心一环：

1. **完整增删改查链路**：前端 Vue 3 + Element Plus 的表格、弹窗与后端 MyBatis-Plus CRUD 接口深度结合。
2. **列表与搜索交互**：实现了按姓名模糊搜索和数据实时刷新的全套逻辑。
3. **主键生成策略理解**：排查并厘清了 MyBatis-Plus 默认雪花算法与数据库自增主键（`@TableId(type = IdType.AUTO)`）的区别。
4. **全栈缓存一致性验证**：亲手验证了数据库与 Redis 缓存重置对列表数据的同步过程。

​
