# Day 10：分布式文件上传与 MinIO 对象存储集成

在实际企业开发中，我们很少把用户上传的文件（如头像、合同、图片）直接存在应用服务器的本地磁盘上，因为这样无法应对多台服务器部署（集群/分布式）的环境。今天我们将引入支持 S3 协议的开源分布式对象存储 —— **MinIO**，完成从后端文件上传接口到前端 Element Plus 文件上传与预览的全流程。

### 今天 Day 10 的核心目标

1. 使用 **Docker 部署 MinIO 服务** 并创建存储桶（Bucket）。
2. 在 Spring Boot 中集成 MinIO Java SDK，实现文件上传与 URL 获取接口。
3. 前端使用 Element Plus 的 `<el-upload>` 组件完成文件/头像上传与实时预览。

​

**第一步：使用 Docker 部署 MinIO**

```bash
docker run -d \
  --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

浏览器访问 MinIO 控制台：`http://localhost:9001`

使用账号 `minioadmin`，密码 `minioadmin` 登录。

点击左侧 **Buckets** -> **Create Bucket**，新建一个名为 **`fullstack-bucket`** 的存储桶。

进入 `fullstack-bucket` 的设置，将 **Access Policy**（访问策略）改为 **`Public`**（公开），确保上传后的图片可以直接通过 URL 访问。

✅ M 系列 (M1/M2/M3) Mac 直接装 mc（ARM64）:

```bash
# 下载arm64版本mc二进制
curl -L https://dl.min.io/client/mc/release/darwin-arm64/mc -o ~/mc

# 添加执行权限
chmod +x ~/mc

# 移动到系统PATH目录，全局可用
sudo mv ~/mc /usr/local/bin/mc
```

验证是否装好：

```
mc --version
```

输出版本号代表成功。

## 配置连接你的 MinIO

```
# 别名：myminio，地址填你的minio API地址(9000端口)，ak sk替换你自己
mc alias set myminio http://127.0.0.1:9000 minioadmin minioadmin
```

设置桶**只读公开（推荐，只能下载，不能上传删除）**

```
mc anonymous set download myminio/fullstack-bucket
```

查看匿名策略：

```
mc anonymous list myminio/fullstack-bucket
```

关闭公开权限：

```
mc anonymous reset myminio/fullstack-bucket
```

> 如果你非要完全公开读写（仅本地测试，线上严禁）：

```
mc anonymous set public myminio/fullstack-bucket
```

###

### 第二步：后端集成 MinIO SDK

#### 1. 在 `pom.xml` 中添加 MinIO 依赖

打开 `pom.xml`，添加以下依赖并刷新 Maven：

```html
<!-- MinIO Java SDK -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

#### 2. 在 `application.properties` 中添加配置

打开 `application.properties`，添加 MinIO 相关配置：

```properties
# MinIO 配置
minio.endpoint=http://localhost:9000
minio.accessKey=minioadmin
minio.secretKey=minioadmin
minio.bucketName=fullstack-bucket
```

#### 3. 编写 MinIO 配置类 `MinioConfig.java`

在 `day10` 包下创建 `MinioConfig.java`：

```java
package com.example.fullstacklearningdemo.day10;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

#### 4. 编写文件上传 Controller `FileUploadController.java`

在 `day10` 包下创建 `FileUploadController.java`：

```java
package com.example.fullstacklearningdemo.day10;

import com.example.fullstacklearningdemo.day02.Result;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }

        try {
            // 获取原始文件名并生成唯一 UUID 文件名，避免冲突
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = UUID.randomUUID().toString() + extension;

            // 上传文件流到 MinIO
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 拼接文件访问外网 URL
            String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
            return Result.success(fileUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "文件上传失败：" + e.getMessage());
        }
    }
}
```

重新启动后端 `FullstackLearningDemoApplication`。

​

### 第三步：前端 Element Plus 文件上传实战

我们将创建一个包含头像上传的组件，直接测试文件上传与图片预览功能。

在 `src/views` 目录下新建 `src/views/FileUpload.vue`：

```vue
<template>
  <el-card>
    <template #header>
      <h3>📸 MinIO 分布式文件上传实战 (Day 10)</h3>
    </template>

    <div class="upload-container">
      <el-upload
        class="avatar-uploader"
        action="/api/file/upload"
        :headers="uploadHeaders"
        :show-file-list="false"
        :on-success="handleUploadSuccess"
        :before-upload="beforeAvatarUpload"
      >
        <img v-if="imageUrl" :src="imageUrl" class="avatar" />
        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
      </el-upload>
      <div style="margin-top: 10px; color: #909399; font-size: 13px;">
        支持 jpg/png 格式，单张图片不超过 2MB
      </div>
    </div>

    <!-- 显示上传成功后的文件链接 -->
    <div v-if="imageUrl" class="url-display">
      <p><b>文件线上访问地址：</b></p>
      <el-input :value="imageUrl" readonly>
        <template #append>
          <el-button @click="copyUrl">复制链接</el-button>
        </template>
      </el-input>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const imageUrl = ref('')

// 上传需要带上 JWT Token 身份认证
const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token')}`
})

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    imageUrl.value = response.data
    ElMessage.success('上传成功！')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const beforeAvatarUpload = (rawFile) => {
  const isImage = rawFile.type === 'image/jpeg' || rawFile.type === 'image/png'
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传头像图片只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
    return false
  }
  return true
}

const copyUrl = () => {
  navigator.clipboard.writeText(imageUrl.value)
  ElMessage.success('已复制到剪贴板！')
}
</script>

<style scoped>
.upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}
.avatar-uploader .avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
  border-radius: 6px;
}
</style>

<style>
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}
.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
}
</style>
```

#### 更新路由与菜单

1. 打开 `src/router/index.js`，在 Layout 子路由中添加 `FileUpload`：

```javascript
{
  path: 'file-upload',
  name: 'FileUpload',
  component: () => import('../views/FileUpload.vue'),
  meta: { title: '文件上传' }
}
```

1. 打开 `src/layout/Layout.vue`，在左侧侧边栏中添加菜单项：

```vue
<el-menu-item index="/file-upload">
  <el-icon><Upload /></el-icon>
  <span>文件上传</span>
</el-menu-item>
```

​

### 🧪 验证体验

1. 保证 MinIO Docker 容器与 Spring Boot 后端均已启动。
2. 前端登录系统，进入侧边栏的 **文件上传** 页面。
3. 点击上传框选择一张图片，观察图片是否正常实时预览，并生成可点击访问的 MinIO URL 链接。

​

### **BUG1: 请求发到了 `http://localhost:5173/api/file/upload` 返回了 404 Not Found。**

这是因为 `<el-upload>` 的 `action="/api/file/upload"` 走的是前端 Vite 开发服务器（`5173` 端口），而你的 **Vite 没有将 `/api` 请求代理转发到后端 Spring Boot（`8080` 端口）**，或者 **Spring Security 把该路径给拦截掉了**。

### 🛠️ 2步排查与修复

#### 第一步：检查并配置 Vite 跨域代理（`vite.config.js`）

打开前端根目录下的 `vite.config.js`，确保配置了将 `/api` 开头的请求代理到 `http://localhost:8080`：

JavaScript

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080", // 转发给后端 Spring Boot
        changeOrigin: true,
      },
    },
  },
});

```

_如果修改了 `vite.config.js`，需要按 `Ctrl + C` 停止前端服务并重新运行 `npm run dev` 才能生效！_
