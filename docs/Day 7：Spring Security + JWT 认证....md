# Day 7：Spring Security + JWT 认证授权实战

在前 6 天里，我们已经搭建好了全栈项目的底层数据库、缓存和前端管理界面。今天我们要给整个系统加上**安全防线**：只有登录并拿到 Token 的用户才能访问后端的受保护接口（比如用户的增删改查）。

​

### 今天 Day 07 的核心架构设计

1. **JWT (JSON Web Token)**：用户登录成功后，后端签发包含用户身份信息的加密字符串（Token）返回给前端；前端后续发请求时，在 Request Header（`Authorization`）中带上该 Token。
2. **Spring Security 过滤器链**：拦截所有 HTTP 请求，通过 JWT 过滤器解析并验证 Token，校验通过后将身份放置到 Spring Security 上下文。
3. **无状态放行机制**：放行 `/api/auth/login` 登录接口，关闭 Session 机制，保持无状态 API 服务。

​

### 第一步：在后端引入 JWT 依赖与配置

#### 1. 修改 `pom.xml`

在后端的 `pom.xml` 中引入 `spring-boot-starter-security` 和 `jjwt`（JWT 工具库）依赖：

```html
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT 依赖 (jjwt 0.11.5) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

_添加后点击 IDEA 右上角的 **Maven 刷新图标**。_

_​_

#### 2. 在 `application.properties` 中添加 JWT 密钥配置

打开 `application.properties`，在末尾追加 JWT 密钥和过期时间（单位：毫秒，这里配置为 24 小时）：

```html
# JWT 配置
jwt.secret=FullstackLearningDemoJwtSecretKey2026MustBeLongEnoughForHS256Algorithm
jwt.expiration=86400000
```

####

**第二步：编写 JWT 工具类与安全核心组件**

在 `src/main/java/com/example/fullstacklearningdemo/` 下新建 **`day07`** 包。

#### 1. 编写 `JwtUtils.java`（Token 生成与解析）

在 `day07` 包下新建 `JwtUtils.java`：

```java
package com.example.fullstacklearningdemo.day07;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成 Token
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 从 Token 解析用户名
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 验证 Token 是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 2. 编写 `JwtAuthenticationFilter.java`（请求拦截校验过滤器）

在 `day07` 包下新建 `JwtAuthenticationFilter.java`：

```java
package com.example.fullstacklearningdemo.day07;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 判断请求头是否包含 "Bearer " Token
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);

                // 构建安全上下文认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

#### 3. 编写 `SecurityConfig.java`（Spring Security 核心过滤链配置）

在 `day07` 包下新建 `SecurityConfig.java`：

```java
package com.example.fullstacklearningdemo.day07;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 禁用 CSRF（因为采用 JWT 无状态鉴权）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态 Session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll() // 开放登录接口
                // 2. 放行 Day 15 的 Nacos 测试接口
                .requestMatchers("/api/day15/**").permitAll()
                .anyRequest().authenticated() // 其他所有接口均需 Token 鉴权
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

​

### 第三步：编写 AuthController 登录与测试接口

在 `day07` 包下新建 `AuthController.java`：

```java
package com.example.fullstacklearningdemo.day07;

import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    // 1. 登录接口（演示硬编码密码 admin / 123456）
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        if ("admin".equals(username) && "123456".equals(password)) {
            String token = jwtUtils.generateToken(username);
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("username", username);
            return Result.success(data);
        }

        return Result.error(401, "用户名或密码错误");
    }
}
```

后端无缝跑通！接下来我们在前端加一个登录页面，并在 Axios 请求里统一配置 **Token 请求头拦截器**。

###

### 第四步：配置 Axios 请求拦截器（自动携带 Token）

在前端项目 `src` 目录下新建一个网络请求配置文件 `src/api.js`：

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: 'http://localhost:8080/api'
})

// 1. 请求拦截器：发请求前自动带上 Authorization Header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 2. 响应拦截器：若返回 401 说明 Token 失效或未登录
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      ElMessage.error('身份验证失效或未登录，请重新登录')
      localStorage.removeItem('token')
      window.location.reload()
    }
    return Promise.reject(error)
  }
)

export default api
```

​

### 第五步：在 `App.vue` 中加入登录卡片与退出登录

修改 `src/App.vue`，用 `token` 控制页面状态：未登录显示登录卡片，登录后展示 Day 06 的表格系统：

```vue
<template>
  <div class="container">
    <!-- 未登录状态：显示登录卡片 -->
    <div v-if="!isLoggedIn" class="login-wrapper">
      <el-card class="login-card">
        <template #header>
          <h2>系统登录 (Day 07)</h2>
        </template>
        <el-form :model="loginForm" label-width="70px">
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" placeholder="测试账号: admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" placeholder="测试密码: 123456" show-password />
          </el-form-item>
          <el-button type="primary" style="width: 100%; margin-top: 10px;" :loading="loginLoading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
      </el-card>
    </div>

    <!-- 已登录状态：显示用户管理界面 -->
    <el-card v-else class="box-card">
      <template #header>
        <div class="card-header">
          <h2>用户管理系统 (已登录: {{ currentUser }})</h2>
          <div>
            <el-button type="primary" icon="Plus" @click="handleCreate">新增用户</el-button>
            <el-button type="danger" plain icon="SwitchButton" @click="handleLogout">退出登录</el-button>
          </div>
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
import api from './api'
import { ElMessage } from 'element-plus'

const isLoggedIn = ref(false)
const currentUser = ref('')
const loginLoading = ref(false)
const loginForm = ref({ username: 'admin', password: '123456' })

const userList = ref([])
const loading = ref(false)
const searchName = ref('')
const dialogVisible = ref(false)
const submitLoading = ref(false)
const form = ref({ id: null, name: '', age: 18, email: '' })

// 登录方法
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loginLoading.value = true
  try {
    const res = await api.post('/auth/login', loginForm.value)
    if (res.data.code === 200) {
      ElMessage.success('登录成功！')
      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('username', res.data.data.username)
      currentUser.value = res.data.data.username
      isLoggedIn.value = true
      fetchUserList()
    } else {
      ElMessage.error(res.data.msg || '登录失败')
    }
  } catch (err) {
    ElMessage.error('登录请求异常')
  } finally {
    loginLoading.value = false
  }
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  isLoggedIn.value = false
  ElMessage.info('已退出登录')
}

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await api.get('/day04/users', { params: { name: searchName.value } })
    if (res.data.code === 200) {
      userList.value = res.data.data
    }
  } catch (err) {
    // 拦截器统一处理 401
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
  const token = localStorage.getItem('token')
  const username = localStorage.getItem('username')
  if (token && username) {
    isLoggedIn.value = true
    currentUser.value = username
    fetchUserList()
  }
})
</script>

<style scoped>
.container {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.login-card {
  width: 380px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h2 {
  margin: 0;
  font-size: 18px;
}
.search-bar {
  display: flex;
  align-items: center;
}
</style>
```

​

### 🧪 体验安全鉴权

1. 刷新前端页面，看是不是会自动展示 **登录窗口**。
2. 输入 `admin` / `123456` 点击登录：成功登录并拿到 JWT Token，界面无缝切入用户表格！
3. F12 打开浏览器控制台查看 `Application -> Local Storage`，会看到存入的 `token`。
4. 点击右上角的 **退出登录**，重新变为未登录状态。

​
