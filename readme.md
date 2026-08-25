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
| 前端 | Vue | 3.5 |
| 前端 | Vue Router | 4.6 |
| 前端 | Element Plus | 2.14 |
| 前端 | Vite | 8.2 |
| 前端 | Axios | 1.x |

## 项目结构

```
fullstack
├── fullstack-learning-demo/          # 后端 (Spring Boot)
│   ├── docker-compose.yml            # MySQL + Redis 一键启动
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
│       └── day12/                    # Spring AOP 业务操作日志 + 异步记录
│
├── fullstack-learning-web/           # 前端 (Vue 3 + Vite)
│   ├── index.html
│   └── src/
│       ├── api.js                    # axios 封装(请求/响应拦截器,自动带 Token)
│       ├── router/index.js           # 路由表 + 登录守卫
│       ├── layout/Layout.vue         # 后台布局(侧边菜单 + 顶栏)
│       ├── directives/permission.js  # v-permission 自定义指令(按钮级权限)
│       └── views/                    # Login / Dashboard / UserManagement / FileUpload
│
└── docs/                             # 逐日学习笔记(Day 1-12 + 7 天总结)
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
| Day 12 | Spring AOP 自定义 `@Log` 注解、切面统一记录操作日志、`@Async` 异步落盘 | `/api/day11/users/page`(已挂 `@Log`) |

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
- **Day 12** — Spring Boot AOP 业务操作日志与异步记录(自定义 `@Log` 注解、`@Aspect` 环绕通知、`@Async` 线程池异步落盘)
- **总结** — 7 天全栈学习总结(知识点速览表)

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+ 与 npm
- Docker(用于一键启动 MySQL / Redis)
- Maven(项目自带 `mvnw` wrapper,可省略本地安装)

### 1. 启动数据库

在 `fullstack-learning-demo/` 目录下:

```bash
docker compose up -d
```

会启动:
- MySQL `mp-mysql` 映射到宿主机 `3307` 端口,初始库 `mp_demo`,密码 `Root@123456`
- Redis `mp-redis` 映射到宿主机 `6379` 端口

> 注意:docker-compose 声明了 `./init-sql` 目录用于自动初始化,但该目录目前为空/不存在,需手动建表(见下)。

Day 10 的文件上传功能依赖 MinIO,`docker-compose.yml` 未包含该服务,需自行启动(默认账号密码均为 `minioadmin`,对应 `application.properties` 中的 `minio.*` 配置):

```bash
docker run -p 9000:9000 -p 9001:9001 --name minio \
  -e "MINIO_ROOT_USER=minioadmin" -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

启动后访问 `http://localhost:9001` 进入控制台,手动创建 `application.properties` 中配置的 bucket(默认 `fullstack-bucket`)。

### 2. 初始化数据表

连接 MySQL(`localhost:3307`,账号 `root` / `Root@123456`)执行:

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

如需 docker 启动时自动初始化,可在 `fullstack-learning-demo/init-sql/` 下放入上面的 SQL 文件。

### 3. 启动后端

```bash
cd fullstack-learning-demo
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
```

启动后 API 地址:`http://localhost:8080/api`

### 4. 启动前端

```bash
cd fullstack-learning-web
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。

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
| GET | `/api/day11/users/page?page=1&size=5&name=` | Day11:分页查询用户列表(含角色名称,支持按姓名模糊搜索;Day12 起挂 `@Log` 自动记录操作日志) |

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
- **day12/**:`Log` 自定义注解标记需记录日志的方法;`LogAspect` 环绕切面拦截 `@Log` 方法,采集请求 URL/IP/参数/耗时/当前登录人后封装为 `SysLog`;`AsyncConfig` 配置 `logExecutor` 线程池并开启 `@EnableAsync`;`LogService` 通过 `@Async("logExecutor")` 异步落盘,不阻塞主业务线程。

## 常见问题

- **接口返回 401**:先调用 `/api/auth/login` 拿到 token,再在请求头携带 `Authorization: Bearer <token>`。
- **后端启动失败连不上数据库**:确认 MySQL 已在 `3307` 端口运行且已建库建表。
- **前端登录后接口报错**:后端需先启动,且 `api.js` 中 `baseURL` 指向 `http://localhost:8080/api`。
- **文件上传报错/超时**:确认 MinIO 已启动且 `fullstack-bucket` 已创建,`application.properties` 中 `minio.endpoint` 与实际地址一致。
