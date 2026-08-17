# Day 9：RBAC 权限模型与 Vue 3 自定义指令实战

在 Day 08 中，我们搭建好了 Vue Router 路由与通用 Layout。今天我们要实现真正的**细粒度权限控制**：根据用户的角色和权限标识，控制**菜单的可见性**以及**按钮级别的操作权限**（比如普通用户看得到表格但看不到“新增”、“删除”按钮）。

​

### 今天 Day 09 的核心目标

1. 建立 **RBAC（Role-Based Access Control，基于角色的访问控制）** 权限模型。
2. 改造后端的 JWT 签发与 Security 校验，在 Token 中载入用户的 **权限列表（Permissions）**。
3. 前端编写 **Vue 3 自定义指令 `v-permission`**，实现按钮级权限控制。

​

### 第一步：后端 RBAC 权限设计与 JWT 增强

我们需要在签发 Token 时把用户的权限标识（如 `user:list`, `user:add`, `user:delete`）写入 JWT 载荷（Claims）。

#### 1. 修改 `JwtUtils.java` 支持载入权限

打开 `day07/JwtUtils.java`，修改 `generateToken` 方法，允许传入权限列表：

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
import java.util.List;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 重载：生成带 permissions 权限集合的 Token
    public String generateToken(String username, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .claim("permissions", permissions) // 👈 将权限列表塞入 JWT Payload
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 从 Token 中解析出 permissions 权限列表
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (List<String>) claims.get("permissions");
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

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

#### 2. 修改 `AuthController.java` 模拟不同角色的权限

修改 `day07/AuthController.java` 中的登录逻辑，模拟分配权限：

* **`admin`**：拥有所有权限 `["user:list", "user:add", "user:edit", "user:delete"]`。
* **`test`**（密码 `123456`）：只有只读权限 `["user:list"]`。

```java
package com.example.fullstacklearningdemo.day07;

import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        List<String> permissions;

        if ("admin".equals(username) && "123456".equals(password)) {
            // 超级管理员：完整 CRUD 权限
            permissions = Arrays.asList("user:list", "user:add", "user:edit", "user:delete");
        } else if ("test".equals(username) && "123456".equals(password)) {
            // 测试普通用户：仅有列表查询权限
            permissions = Arrays.asList("user:list");
        } else {
            return Result.error(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(username, permissions);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("permissions", permissions); // 返回给前端持久化保存

        return Result.success(data);
    }
}
```

重新启动后端 `FullstackLearningDemoApplication`。

### 第二步：前端编写 Vue 3 自定义指令 `v-permission`

当用户登录后，我们将用户的权限数组存在 `localStorage` 中。接着编写一个自定义指令，若用户没有对应权限，直接将按钮节点从 DOM 中移除！

#### 1. 创建指令文件 `src/directives/permission.js`

在 `src` 目录下新建 `directives` 文件夹，并创建 `permission.js`：

```javascript
export const permission = {
  mounted(el, binding) {
    const { value } = binding // 获取指令传入的权限值，例如 'user:add'
    const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')

    if (value && typeof value === 'string') {
      const hasPermission = permissions.includes(value)

      // 如果没有对应权限，移除该 DOM 元素
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`请指定正确格式的权限标识，如 v-permission="'user:add'"`)
    }
  }
}
```

#### 2. 在 `src/main.js` 中全局注册指令

打开 `src/main.js`，引入并注册 `v-permission`：

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { permission } from './directives/permission' // 👈 引入权限指令

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.directive('permission', permission) // 👈 注册全局指令 v-permission

app.use(ElementPlus)
app.use(router)
app.mount('#app')
```

### 第三步：修改前端登录逻辑与页面按钮

#### 1. 修改 `src/views/Login.vue` 保存权限数组

打开 `src/views/Login.vue`，在登录成功时把返回的 `permissions` 存入 `localStorage`：

```vue
// handleLogin 方法中：
if (res.data.code === 200) {
  ElMessage.success('登录成功！')
  localStorage.setItem('token', res.data.data.token)
  localStorage.setItem('username', res.data.data.username)
  localStorage.setItem('permissions', JSON.stringify(res.data.data.permissions)) // 👈 保存权限标识列表
  router.push('/dashboard')
}
```

#### 2. 修改 `src/views/UserManagement.vue` 应用 `v-permission`

打开 `src/views/UserManagement.vue`，为操作按钮加上 `v-permission` 指令：

```vue
<!-- 新增用户按钮：需要 user:add 权限 -->
<el-button 
  v-permission="'user:add'" 
  type="primary" 
  icon="Plus" 
  @click="handleCreate"
>
  新增用户
</el-button>

<!-- 表格中的编辑按钮：需要 user:edit 权限 -->
<el-button 
  v-permission="'user:edit'" 
  size="small" 
  type="warning" 
  @click="handleEdit(scope.row)"
>
  编辑
</el-button>

<!-- 表格中的删除按钮：需要 user:delete 权限 -->
<el-popconfirm 
  title="确定要删除该用户吗？" 
  @confirm="handleDelete(scope.row.id)"
>
  <template #reference>
    <el-button v-permission="'user:delete'"  size="small" type="danger">删除</el-button>
  </template>
</el-popconfirm>
```

###

###

### 🧪 验证体验

1. 确保后端已重启，前端在终端正常运行。
2. 打开页面先退出登录，分别尝试两个账号：
   * **测试账号 1（管理员）**：账号 `admin` / 密码 `123456`
     * 登录后进入“用户管理”，可以看到【新增用户】按钮以及表格里的【编辑】和【删除】按钮。
   * **测试账号 2（普通只读用户）**：账号 `test` / 密码 `123456`
     * 登录后进入“用户管理”，你会发现【新增用户】按钮、以及表格右侧的所有【编辑】和【删除】按钮**全部被自动擦除移除**！页面只保留了数据查看和搜索功能。
