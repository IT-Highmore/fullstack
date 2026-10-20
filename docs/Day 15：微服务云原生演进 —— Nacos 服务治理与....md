# Day 15：微服务云原生演进 —— Nacos 服务治理与 Kubernetes (K8s) 部署实战（Mac 适配版）

Day 14 中，我们使用的是 **Docker Compose** 进行单机容器编排。而在大型互联网企业生产环境中，微服务架构通常会配合 **Nacos**（服务注册/发现与动态配置中心）以及 **Kubernetes / K8s**（生产级容器集群自动化运维平台）来构建高可用、弹性伸缩的云原生架构。

### 今天 Day 15 的核心目标

1. **解决 Mac 环境 Nacos 启动**：使用原生支持 ARM64 的容器镜像启动 Nacos 控制台。
2. **解决国内 Maven 依赖下载**：配置阿里云镜像源，解决 `spring-cloud-dependencies` 拉取超时与失败缓存问题。
3. **服务注册与发现**：将后端 Spring Boot 应用成功注册至 Nacos 注册中心。
4. **动态配置中心**：利用 Nacos Config 结合 `@RefreshScope`，实现配置无缝修改、无需重启服务即刻生效。
5. **K8s 云原生部署编排**：编写符合 K8s 规范的 `Deployment` 与 `Service` 声明文件。

​

### 第一步：启动 Mac 适配版 Nacos 服务

由于官方 `nacos/nacos-server` 镜像缺失 `arm64` 架构清单，在终端运行以下命令，使用强兼容的 `linux/amd64` 平台指令启动单机 Nacos 容器：

```bash
docker run -d \
  --name nacos-standalone \
  --platform linux/amd64 \
  -e MODE=standalone \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v2.2.3
```

* **验证方式**：

1. 在终端运行 `docker ps`，确认 `nacos-standalone` 状态为 `Up`。
2. 浏览器打开 `http://localhost:8848/nacos` （默认账号/密码：`nacos` / `nacos`）。

`​`

### 第二步：配置国内 Maven 镜像与依赖

针对此前出现的 `No route to host` 与下载缓存问题，请按照以下结构完整更新后端的 `pom.xml`：

```html
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>fullstack-learning-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <!-- 1. 配置阿里云 Maven 镜像源（解决下载超时与网络阻塞） -->
    <repositories>
        <repository>
            <id>aliyun-maven</id>
            <name>Aliyun Maven</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <!-- 2. Spring Cloud 与 Spring Cloud Alibaba 依赖版本统一管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- 修改：适配 Spring Boot 3.2.x / 3.3.x 的 Spring Cloud 版本 -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>2023.0.3</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- 修改：适配 Spring Boot 3.2.x / 3.3.x 的 Spring Cloud Alibaba 版本 -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>2023.0.1.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- 原有的 Spring Boot Starter 等依赖保留... -->

        <!-- 3. Nacos 服务注册与发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- 4. Nacos 动态配置中心 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- 5. Bootstrap 启动引导依赖 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bootstrap</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### 💡 清除坏缓存并刷新 Maven：

保存 `pom.xml` 后，在终端输入以下命令强制更新依赖（清除之前的失败缓存）：

```bash
# Mac / Linux 清理失败标记文件
find ~/.m2/repository -name "*.lastUpdated" -exec rm -f {} \;

# 强制更新 Maven 依赖
mvn clean compile -U
```

​

**第三步：配置 Nacos 客户端与动态配置代码**

#### 1. 新增 `src/main/resources/bootstrap.properties`

Spring Cloud 会优先加载 `bootstrap.properties` 以连接远程 Nacos。新建该文件并写入：

```properties
# 应用服务名称（与 Nacos 配置 Data ID 对应）
spring.application.name=fullstack-backend

# Nacos 注册中心地址
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848

# Nacos 配置中心地址与格式
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.file-extension=yaml
```

#### 2. 创建动态配置测试接口

在后端新建 **`day15`** 包，并创建 `NacosConfigController.java`：

```java
package com.example.fullstacklearningdemo.day15;

import com.example.fullstacklearningdemo.day02.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/day15")
@RefreshScope // 👈 关键注解：当 Nacos 配置发生变更时，自动刷新变量
public class NacosConfigController {

    @Value("${app.info:默认本地配置}")
    private String appInfo;

    @GetMapping("/config")
    public Result<String> getDynamicConfig() {
        return Result.success("当前 Nacos 实时动态配置内容为：" + appInfo);
    }
}
```

​

### 第四步：在 Nacos 控制台发布配置

1. 打开 Nacos 控制台 [`http://localhost:8848/nacos`。](http://localhost:8848/nacos。)
2. 进入 **配置管理** -> **配置列表** -> 点击右上角 **`+`** 新建配置：
   * **Data ID**: `fullstack-backend.yaml`
   * **Group**: `DEFAULT_GROUP`
   * **配置格式**: `YAML`
   * **配置内容**:

```yaml
app:
  info: "🚀 欢迎使用 Nacos 动态配置中心，已成功为 Mac ARM64 环境完成适配！"
```

3. 点击 **发布**。

​

### 第五步：Kubernetes (K8s) 部署清单准备

在项目根目录下新建 `k8s/` 文件夹，创建 `k8s/backend-deployment.yaml`：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fullstack-backend-deployment
  labels:
    app: fullstack-backend
spec:
  replicas: 2 # 2 个 Pod 副本实现高可用
  selector:
    matchLabels:
      app: fullstack-backend
  template:
    metadata:
      labels:
        app: fullstack-backend
    spec:
      containers:
        - name: fullstack-backend
          image: fullstack-backend:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          # K8s 健康检查与存活探针
          readinessProbe:
            httpGet:
              path: /api/day15/config
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /api/day15/config
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: fullstack-backend-service
spec:
  type: NodePort
  selector:
    app: fullstack-backend
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
      nodePort: 30080
```

​

### 🧪 最终验证步骤

1. 执行 `docker run --platform linux/amd64 ...` 启动 Nacos，确保能正常登录 Web 控制台。
2. 刷新 Maven 并启动 `FullstackLearningDemoApplication`。
3. 访问接口 `http://localhost:8848/nacos` 查看 **服务列表** 中是否出现了 `fullstack-backend`。
4. 浏览器访问 `http://localhost:8080/api/day15/config`，观察是否正确读取到 Nacos 配置；在网页端修改该配置并发布，**无需重启项目**，重新刷新接口查看配置是否实时更新！
