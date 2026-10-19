# Day 14：全栈项目打包、Docker 容器化与生产环境部署

在前 13 天里，我们从最基础的 CRUD、JWT 认证、RBAC 权限、MinIO 文件存储、MyBatis-Plus 分页，一直做到 AOP 审计日志与 RabbitMQ 异步消息队列。今天我们要把前端（Vue 3）和后端（Spring Boot）打造成标准的生产级镜像，并用 **Docker Compose** 实现一键全栈编排部署。

​

### 今天 Day 14 的核心目标

1. **前端打包与 Nginx 部署**：生产环境构建 Vite 项目，配置 Nginx 解决前端路由 `history` 模式刷新 404 及 `/api` 反向代理。
2. **后端打包与 Multi-stage Dockerfile**：使用 Maven 进行 Jar 包构建，编写 Java 运行环境 Dockerfile。
3. **Docker Compose 全栈一键编排**：编写 `docker-compose.yml` 统一管理 MySQL、Redis、MinIO、RabbitMQ、Spring Boot 后端和 Nginx 前端。

​

## 第一步：前端生产环境打包与 Nginx 配置

#### 1. 前端打包

在前端项目 `fullstack-learning-web` 目录下运行：

```bash
npm run build
```

构建完成后，根目录下会生成一个 `dist` 静态资源文件夹。

#### 2. 创建 Nginx 配置文件 `nginx.conf`

在前端项目根目录下新建 `nginx.conf`：

```text
server {
    listen       80;
    server_name  localhost;

    # 前端静态资源
    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        # 👈 解决 Vue Router history 模式下刷新页面 404 问题
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 3. 创建前端 Dockerfile

在前端项目根目录下新建 `Dockerfile`：

```docker
# 使用轻量级 Nginx 镜像
FROM nginx:alpine

# 复制打包后的静态文件到 Nginx 目录
COPY dist/ /usr/share/nginx/html/

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

​

## 第二步：后端打包与 Dockerfile 配置

#### 1. 后端打 Jar 包

在后端根目录下（`fullstack-learning-demo`）运行：

```bash
mvn clean package -DskipTests
```

构建成功后会在 `target/` 目录下生成 `fullstack-learning-demo-0.0.1-SNAPSHOT.jar。`

#### 2. 创建后端 Dockerfile

在后端项目根目录下新建 `Dockerfile`：

```docker
FROM openjdk:17-jre

WORKDIR /app

# 将打包好的 Jar 复制到容器中
COPY target/*.jar app.jar

# 暴露后端端口
EXPOSE 8080

# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

​

## 第三步：编写 Docker Compose 全栈编排文件

在你的工作区根目录（或新建一个部署目录）下，创建 `docker-compose.yml`，将整个全栈系统的中间件与服务整合起来：

```yaml
version: '3.8'

services:
  # 1. MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: fullstack-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: fullstack_db
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  # 2. Redis 缓存
  redis:
    image: redis:alpine
    container_name: fullstack-redis
    ports:
      - "6379:6379"

  # 3. MinIO 对象存储
  minio:
    image: minio/minio
    container_name: fullstack-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"

  # 3.1 一次性任务：创建 bucket 并开放匿名读（上传后返回的 URL 才能直接访问）
  minio-init:
    image: minio/minio
    depends_on:
      - minio
    restart: "no"
    entrypoint: >
      sh -c "until mc alias set local http://minio:9000 minioadmin minioadmin; do sleep 1; done;
      mc mb --ignore-existing local/fullstack-bucket;
      mc anonymous set download local/fullstack-bucket"

  # 4. RabbitMQ 消息队列
  rabbitmq:
    image: rabbitmq:3-management
    container_name: fullstack-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  # 5. Spring Boot 后端服务
  backend:
    build:
      context: ./fullstack-learning-demo
      dockerfile: Dockerfile
    container_name: fullstack-backend
    ports:
      - "8080:8080"
    # 容器内 localhost 指向自身，用服务名覆盖 application.properties 里的连接地址
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mp_demo?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_PASSWORD: root
      SPRING_DATA_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
      MINIO_ENDPOINT: http://minio:9000
      MINIO_PUBLIC_ENDPOINT: http://localhost:9000 # 返回给浏览器的文件访问地址
    depends_on:
      - mysql
      - redis
      - minio
      - rabbitmq

  # 6. Vue 3 + Nginx 前端服务
  frontend:
    build:
      context: ./fullstack-learning-web
      dockerfile: Dockerfile
    container_name: fullstack-frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

​

### 🧪 一键启动与上线验证

在 `docker-compose.yml` 所在的目录下运行启动命令：

```bash
docker compose up -d --build
```

#### 检查与验证：

1. **容器状态**：运行 `docker-compose ps`，确认所有 6 个服务容器均为 `Up` 状态。
2. **访问系统**：打开浏览器访问 `http://localhost`（默认 80 端口），进入生产环境前台。
3. **全流程测试**：
   * 尝试登录（`admin` / `123456`）。
   * 测试用户管理列表的分页、搜索、增删改。
   * 测试图片上传到 MinIO。
   * 观察后端日志，验证切面日志与 RabbitMQ 消费正常。
