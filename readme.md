# Fullstack 全栈学习项目

一个前后端分离的全栈学习项目,按「天」(day) 推进知识点,配套 Vue 3 + Element Plus 后台管理界面。后端是 Spring Boot 3 + MyBatis-Plus + Redis + JWT 的 REST API,前端是 Vue 3 + Vite + Element Plus。

> 本项目由 `fullstack-learning-demo`(后端)与 `fullstack-learning-web`(前端)两个子项目组成。

## 技术栈

| 端 | 技术 | 版本 |
| --- | --- | --- |
| 后端 | Java / Spring Boot | 17 / 3.3.0 |
| 后端 | MyBatis-Plus | 3.5.7 |
| 后端 | Spring Security + JWT (jjwt) | 0.11.5 |
| 后端 | MySQL / Redis | 8.4 / 最新 |
| 后端 | MinIO (对象存储) | minio-8.5.7 |
| 后端 | RabbitMQ (AMQP) | 3-management |
| 部署 | Docker Compose | 一键编排 6 个服务 |
| 部署 | Nginx (前端静态托管 + `/api` 反向代理) | alpine |
| 前端 | Vue | 3.5 |
| 前端 | Vue Router | 4.6 |
| 前端 | Element Plus | 2.14 |
| 前端 | Vite | 8.2 |
| 前端 | Axios | 1.x |

## 项目结构

```
fullstack
├── docker-compose.yml                # 全栈一键编排(MySQL/Redis/MinIO/RabbitMQ/后端/前端)
├── fullstack-learning-demo/          # 后端 (Spring Boot)
│   ├── Dockerfile                    # 后端镜像(eclipse-temurin:17-jre,复制 target/*.jar)
│   ├── docker-compose.yml            # 仅 MySQL + Redis(本地开发用)
│   ├── pom.xml
│   └── src/main/java/com/example/fullstacklearningdemo/
│       ├── FullstackLearningDemoApplication.java   # 启动类
│       ├── config/                   # CorsConfig / RedisConfig
│       ├── day01/                    # IoC / DI 入门
│       ├── day02/                    # 统一返回体 + 全局异常
│       ├── day03/                    # MyBatis-Plus CRUD
│       ├── day04/                    # Redis 缓存
│       ├── day07/                    # JWT + Spring Security + RBAC 权限载荷
│       ├── day10/                    # MinIO 分布式文件上传
│       ├── day11/                    # MyBatis-Plus 分页插件 + 多表查询
│       ├── day12/                    # Spring AOP 业务操作日志
│       └── day13/                    # RabbitMQ 异步消息队列(日志投递与消费)
│
├── fullstack-learning-web/           # 前端 (Vue 3 + Vite)
│   ├── Dockerfile                    # 前端镜像(nginx:alpine,复制 dist/)
│   ├── nginx.conf                    # history 模式回退 + /api 反向代理到 backend:8080
│   ├── index.html
│   └── src/
│       ├── api.js                    # axios 封装(请求/响应拦截器,自动带 Token)
│       ├── router/index.js           # 路由表 + 登录守卫
│       ├── layout/Layout.vue         # 后台布局(侧边菜单 + 顶栏)
│       ├── directives/permission.js  # v-permission 自定义指令(按钮级权限)
│       └── views/                    # Login / Dashboard / UserManagement / FileUpload
│
└── docs/                             # 逐日学习笔记(Day 1-14 + 7 天总结)
```

## 功能与学习路线

| 模块 | 知识点 | 对应接口 |
| --- | --- | --- |
| Day 01 | Spring IoC / DI、`@RestController`、分层设计 | `/api/day01/*` |
| Day 02 | `Result<T>` 统一响应、全局异常拦截、业务 code 规范 | `/api/day02/*` |
| Day 03 | MyBatis-Plus ORM CRUD、`LambdaQueryWrapper` 条件查询 | `/api/day03/*` |
| Day 04 | Redis 缓存(Cache-Aside 旁路缓存、TTL、主动清理) | `/api/day04/*` |
| Day 05 | Vue 3 + Vite 工程搭建、Element Plus、Axios、CORS | — |
| Day 06 | 前端 CRUD 表格(`el-table` / `el-dialog`)、自增主键 | — |
| Day 07 | JWT + Spring Security 无状态认证、Axios 拦截器 | `/api/auth/*` |
| Day 08 | Vue Router 4 多页路由、Layout 布局、路由守卫 | — |
| Day 09 | RBAC 权限模型、JWT 权限载荷、Vue 3 自定义指令 `v-permission` | `/api/auth/login` |
| Day 10 | MinIO 分布式对象存储、`el-upload` 图片上传 | `/api/file/upload` |
| Day 11 | MyBatis-Plus 分页插件、`UserVO` 多表关联查询 | `/api/day11/users/page` |
| Day 12 | Spring AOP 自定义 `@Log` 注解、切面统一记录操作日志 | `/api/day11/users/page`(已挂 `@Log`) |
| Day 13 | RabbitMQ 消息队列：Direct 交换机/队列/绑定、生产者投递、`@RabbitListener` 异步消费 | `/api/day11/users/page`(日志经 MQ 投递) |
| Day 14 | 前端 `npm run build` 打包、Nginx 托管与反向代理、后端 Jar + Dockerfile、Docker Compose 全栈一键编排 | `http://localhost`(Nginx 80 端口) |

前端已实现一套完整的后台管理界面:登录页 → 侧边栏 Layout 布局 → 控制台首页 / 用户管理(列表搜索、新增、编辑、删除)。通过 Vue Router 路由守卫控制访问权限,axios 拦截器自动携带 Token 并在 401 时清理登录态。Day 09 起引入 RBAC 按钮级权限:登录返回权限列表存入 `localStorage`,页面按钮通过 `v-permission` 指令按权限标识控制显隐。Day 10 新增「文件上传」页面,基于 `el-upload` 上传图片到 MinIO,成功后回显线上访问地址。Day 11 用户管理列表改为真后端分页,搭配 `<el-pagination>` 分页组件,并按名称搜索、以 Tag 展示关联的角色名称。

## 学习笔记

项目附带的逐日学习笔记存放在 [`docs/`](docs/) 目录,与代码一一对应:

- **Day 1** — Spring Boot 3 核心思想:IoC 与 DI(`@RestController` vs `@Controller`)
- **Day 2** — RESTful API 与统一响应封装(HTTP 状态码 vs 业务 code)
- **Day 3** — MySQL 与 ORM 映射:MyBatis-Plus 实战(逻辑删除、连接池)
- **Day 4** — Redis 缓存集成与分布式 key-value 设计(Cache-Aside 模式)
- **Day 5** — Vue 3 + Vite 前端工程搭建(Element Plus、CORS)
- **Day 6** — Vue 3 增删改查表格与状态管理(`el-table` / `el-dialog`)
- **Day 7** — Spring Security + JWT 认证授权实战
- **Day 8** — Vue Router 4 多页面路由与 Layout 布局搭建
- **Day 9** — RBAC 权限模型与 Vue 3 自定义指令实战(`v-permission` 按钮级权限)
- **Day 10** — MinIO 分布式文件上传实战(`MinioClient` 上传对象存储、`el-upload` 图片上传组件)
- **Day 11** — MyBatis-Plus 分页插件与复杂多表查询(`MybatisPlusInterceptor` 分页拦截器、`UserVO` 多表 Join 查询、`<el-pagination>` 组件)
- **Day 12** — Spring Boot AOP 业务操作日志(自定义 `@Log` 注解、`@Aspect` 环绕通知)
- **Day 13** — RabbitMQ 异步消息队列与削峰填谷(`RabbitTemplate` 生产者投递、`@RabbitListener` 消费者监听、Direct 交换机/队列/绑定)
- **Day 14** — 全栈项目打包、Docker 容器化与生产环境部署(Nginx 配置、前后端 Dockerfile、Docker Compose 编排)
- **总结** — 7 天全栈学习总结(知识点速览表)

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+ 与 npm
- Docker(用于一键启动 MySQL / Redis)
- Maven(项目自带 `mvnw` wrapper,可省略本地安装)

### 方式一:Docker Compose 一键部署(Day 14)

在项目根目录先打包前后端(两个 Dockerfile 只复制构建产物,不在镜像内编译):

```bash
cd fullstack-learning-demo && ./mvnw clean package -DskipTests && cd ..
cd fullstack-learning-web && npm install && npm run build && cd ..
```

再启动全部服务:

```bash
docker compose up -d --build
```

| 服务 | 容器名 | 端口 | 说明 |
| --- | --- | --- | --- |
| mysql | `fullstack-mysql` | 3306 | root / `root`,初始库 `fullstack_db`(后端实际使用 `mp_demo`,需手动建库建表,见下文) |
| redis | `fullstack-redis` | 6379 | — |
| minio | `fullstack-minio` | 9000 / 9001 | `minioadmin` / `minioadmin`,9001 为控制台 |
| minio-init | — | — | 一次性任务:创建 `fullstack-bucket` 并开放匿名读 |
| rabbitmq | `fullstack-rabbitmq` | 5672 / 15672 | `guest` / `guest` |
| backend | `fullstack-backend` | 8080 | 通过环境变量把数据库、Redis、RabbitMQ、MinIO 地址指向容器服务名 |
| frontend | `fullstack-frontend` | 80 | Nginx 托管前端,`/api/` 反向代理到 `backend:8080` |

启动后访问 `http://localhost` 即可。上传文件返回的 URL 由 `minio.public-endpoint`(compose 中为 `http://localhost:9000`)决定,未配置时退回 `minio.endpoint`,避免返回浏览器无法解析的容器内地址。

> 注意:`depends_on` 只保证启动顺序,不等待 MySQL 就绪。若 backend 首次启动连不上数据库,待 MySQL 初始化完成后执行 `docker compose restart backend`。

### 方式二:本地开发

#### 1. 启动依赖服务

只启动中间件,后端和前端在本机运行:

```bash
docker compose up -d mysql redis minio minio-init rabbitmq
```

服务端口与账号见上表。本地后端 `application.properties` 默认连接 `localhost:3306`,账号 `root`,密码 `root`,与根目录 compose 中的 MySQL 一致。

> `fullstack-learning-demo/docker-compose.yml` 是早期只含 MySQL + Redis 的版本(MySQL 映射 `3307`、密码 `Root@123456`),如果用它启动数据库,需要同步修改 `application.properties` 中的连接地址和密码。

启动后可访问 MinIO 控制台 `http://localhost:9001` 与 RabbitMQ 管理界面 `http://localhost:15672`。后端首次启动时会自动声明交换机 `system.log.exchange` 与队列 `system.log.queue`。

#### 2. 初始化数据表

连接 MySQL(`localhost:3306`,账号 `root` / `root`)执行:

```sql
CREATE DATABASE IF NOT EXISTS mp_demo DEFAULT CHARACTER SET utf8mb4;
USE mp_demo;

CREATE TABLE IF NOT EXISTS sys_user (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(50),
    age   INT,
    email VARCHAR(100)
);
```

Docker Compose 部署时,同样需要先进入 MySQL 容器(`docker exec -it fullstack-mysql mysql -uroot -proot`)执行上面的 SQL,再重启 backend。

#### 3. 启动后端

```bash
cd fullstack-learning-demo
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
```

启动后 API 地址:`http://localhost:8080/api`

#### 4. 启动前端

```bash
cd fullstack-learning-web
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。

> 前端 `api.js` 的 `baseURL` 为 `http://localhost:8080/api`,Compose 部署时由宿主机映射的 8080 端口转发到后端容器。

### 默认账号

```
用户名: admin   密码: 123456   权限: user:list / user:add / user:edit / user:delete(全部)
用户名: test    密码: 123456   权限: user:list(仅只读)
```

> 登录接口硬编码了这两个账号(见 `AuthController`),登录成功后返回 JWT **及权限列表** `permissions`,前端存入 `localStorage` 并在后续请求自动携带 Token、用 `v-permission` 指令控制按钮显隐。

## API 一览

除登录接口外,**所有接口都需要在请求头携带** `Authorization: Bearer <token>`(由 `/api/auth/login` 返回)。所有响应统一为:

```json
{ "code": 200, "message": "操作成功", "data": { }, "timestamp": 1720000000000 }
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录,返回 token + 权限列表(admin / test,密码均 123456) |
| GET | `/api/day01/role?id=1` | Day01:按 id 返回角色字符串 |
| GET | `/api/day02/user/{id}` | Day02:返回统一 Result 结构 |
| GET | `/api/day02/test-error` | Day02:演示全局异常处理(主动抛异常) |
| POST | `/api/day03/user` | Day03:新增用户 |
| GET | `/api/day03/users?minAge=18` | Day03:按年龄条件查询 |
| DELETE | `/api/day03/user/{id}` | Day03:删除用户 |
| GET | `/api/day04/users?name=` | Day04:用户列表(按姓名模糊搜索) |
| POST | `/api/day04/user` | Day04:新增或更新用户(更新时清理缓存) |
| DELETE | `/api/day04/user/{id}` | Day04:删除用户(清理缓存) |
| GET | `/api/day04/user/{id}` | Day04:用户详情(优先查 Redis 缓存,10 分钟过期) |
| POST | `/api/file/upload` | Day10:上传文件到 MinIO,返回可访问的文件 URL |
| GET | `/api/day11/users/page?page=1&size=5&name=` | Day11:分页查询用户列表(含角色名称,支持按姓名模糊搜索;Day12 起挂 `@Log`,Day13 起日志经 RabbitMQ 解耦投递消费) |

> 前端「用户管理」页面实际调用的是 Day04 的一组接口。

## 目录与包结构说明

- **config/**:`CorsConfig` 全局跨域放行(开发期);`RedisConfig` 配置 RedisTemplate 的 JSON 序列化。
- **day01/**:演示 IoC 容器与 `@Autowired` 依赖注入。
- **day02/**:`Result<T>` 统一响应体 + `@RestControllerAdvice` 全局异常兜底。
- **day03/**:`User` 实体映射 `sys_user` 表,`UserMapper extends BaseMapper` 免写 SQL;`LambdaQueryWrapper` 实现条件查询。
- **day04/**:Cache-Aside 旁路缓存——查询「先查 Redis、未命中查 MySQL 再写回缓存(TTL 10 分钟)」,增删改时主动删除缓存保持一致性。
- **day07/**:`JwtUtils` 签发/校验 token(载荷中可携带 `permissions` 权限列表);`JwtAuthenticationFilter` 解析请求头;`SecurityConfig` 放行登录接口、其余接口全部鉴权。`AuthController` 按角色模拟分配权限:`admin` 全量 CRUD、`test` 只读。
- **day10/**:`MinioConfig` 注入 `MinioClient` Bean;`FileUploadController` 接收 `MultipartFile`,以 UUID 生成对象名后上传至 MinIO 并返回可访问 URL。
- **day11/**:`MybatisPlusConfig` 注册 MySQL 分页拦截器;`UserPageMapper` 通过 `@Select` 自定义多表 SQL 返回 `UserVO`;`UserPageController` 接收 `page`/`size`/`name` 参数返回分页结果(IPage)。
- **day12/**:`Log` 自定义注解标记需记录日志的方法;`LogAspect` 环绕切面拦截 `@Log` 方法,采集请求 URL/IP/参数/耗时/当前登录人后封装为 `SysLog`;`LogService` 作为生产者将日志序列化为 JSON 投递到 RabbitMQ,不再走单机线程池。
- **day13/**:`RabbitConfig` 声明 Direct 交换机 `system.log.exchange`、持久化队列 `system.log.queue` 及其绑定;`LogMessageConsumer` 通过 `@RabbitListener` 监听队列,反序列化后异步消费日志,实现业务解耦与削峰填谷。

## 常见问题

- **接口返回 401**:先调用 `/api/auth/login` 拿到 token,再在请求头携带 `Authorization: Bearer <token>`。
- **后端启动失败连不上数据库**:确认 MySQL 已在 `3306` 端口运行且已建库 `mp_demo` 建表(Compose 部署时 backend 先于 MySQL 就绪会连接失败,执行 `docker compose restart backend`)。
- **前端登录后接口报错**:后端需先启动,且 `api.js` 中 `baseURL` 指向 `http://localhost:8080/api`。
- **文件上传报错/超时**:确认 MinIO 已启动且 `fullstack-bucket` 已创建(Compose 的 `minio-init` 会自动创建),`application.properties` 中 `minio.endpoint` 与实际地址一致。
- **上传后图片打不开**:检查 `minio.public-endpoint` 是否为浏览器可访问的地址(Compose 中为 `http://localhost:9000`),且 bucket 已开放匿名读。
- **操作页面后控制台无日志输出**:确认 RabbitMQ 容器已启动(`http://localhost:15672` 可登录),后端启动日志中已声明 `system.log.exchange` / `system.log.queue`;投递失败时 `LogService` 会打印异常堆栈。
