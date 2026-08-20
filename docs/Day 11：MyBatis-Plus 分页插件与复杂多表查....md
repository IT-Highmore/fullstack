# Day 11：MyBatis-Plus 分页插件与复杂多表查询

在前面处理列表时，我们是一次性把数据库里的数据全部查出来。在海量数据场景下，这会瞬间撑爆内存和网络带宽。今天我们将通过 **MyBatis-Plus 分页插件** 实现真后端分页，并结合 **VO（Value Object）** 完成多表关联查询。

### 今天 Day 11 的核心目标

1. 配置 MyBatis-Plus 的 `MybatisPlusInterceptor` 分页拦截器。
2. 实现后端 RESTful 分页接口（接受 `page` 和 `size` 参数，返回总条数与当前页数据）。
3. 设计多表查询：创建用户角色关联模型，使用自定义 XML / Mapper 实现多表 Join 查询并封装为 `UserVO`。
4. 前端 Element Plus 表格联动 `<el-pagination>` 分页组件。

​

### 第一步：后端配置分页插件与多表查询

#### 1. 配置 `MybatisPlusConfig.java` 分页拦截器

在后端新建 `day11` 包，并创建 `MybatisPlusConfig.java`：

```java
package com.example.fullstacklearningdemo.day11;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 添加 MySQL 数据库类型的分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

#### 2. 创建多表关联视图对象 `UserVO.java`

在 `day11` 包下创建 `UserVO.java`，用于接收关联查询后包含“角色名称”的结果：

```java
package com.example.fullstacklearningdemo.day11;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String name;
    private Integer age;
    private String email;
    private String roleName; // 多表 Join 查出来的角色名称（如：超级管理员/普通用户）
}
```

#### 3. 编写 Mapper 自定义 SQL（XML / 注解）

在 `day03/UserMapper.java`（或 `day11` 下新建 Mapper）添加分页关联查询方法：

```java
package com.example.fullstacklearningdemo.day11;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstacklearningdemo.day03.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPageMapper extends BaseMapper<User> {

    // 自定义多表分页查询：将 Page 对象作为第一个参数，MyBatis-Plus 会自动注入分页逻辑
    @Select("SELECT u.id, u.name, u.age, u.email, '超级管理员' AS role_name " +
            "FROM sys_user u " +
            "WHERE u.is_deleted = 0 AND (#{name} IS NULL OR u.name LIKE CONCAT('%', #{name}, '%'))")
    IPage<UserVO> selectUserPageVo(Page<UserVO> page, @Param("name") String name);
}
```

#### 4. 编写 `UserPageController.java`

在 `day11` 包下创建 `UserPageController.java`：

```java
package com.example.fullstacklearningdemo.day11;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/day11")
public class UserPageController {

    @Autowired
    private UserPageMapper userPageMapper;

    @GetMapping("/users/page")
    public Result<IPage<UserVO>> getUserPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(required = false) String name) {

        // 构建 MyBatis-Plus Page 对象
        Page<UserVO> pageParam = new Page<>(page, size);
        
        // 执行分页查询
        IPage<UserVO> userVoPage = userPageMapper.selectUserPageVo(pageParam, name);

        return Result.success(userVoPage);
    }
}
```

重新启动后端 `FullstackLearningDemoApplication`。

​

### 第二步：前端整合 `<el-pagination>` 组件

我们在前端“用户管理”页面引入标准的分页条。

打开 `src/views/UserManagement.vue`，替换或改造列表与分页逻辑：

```vue
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
      <el-input
        v-model="searchName"
        placeholder="请输入姓名搜索"
        clearable
        style="width: 240px; margin-right: 12px;"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
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
          <el-button v-permission="'user:edit'" size="small" type="warning" @click="handleEdit(scope.row)">编辑</el-button>
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
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[3, 5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="fetchUserPage"
        @current-change="fetchUserPage"
      />
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
```

​

### 🧪 验证体验

1. 确保后端服务启动。
2. 刷新前端页面进入“用户管理”，观察表格下方是否出现了标准的 **分页控件**（显示总条数、页码、每页显示数量）。
3. 切换每页条数（如改为 `3` 条/页）或点击翻页，观察控制台 Network 选项卡发出的 `/api/day11/users/page?page=2&size=3` 请求，数据是否准实时响应且角色列显示为带 Style 的 Tag！

​
