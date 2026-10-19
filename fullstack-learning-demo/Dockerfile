FROM eclipse-temurin:17-jre

WORKDIR /app

# 将打包好的 Jar 复制到容器中
COPY target/*.jar app.jar

# 暴露后端端口
EXPOSE 8080

# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "app.jar"]