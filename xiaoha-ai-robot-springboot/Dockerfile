# ========== 第一阶段：Maven 构建 ==========
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存——依赖没变就不会重新下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# ========== 第二阶段：JRE 运行 ==========
FROM eclipse-temurin:21-jre

WORKDIR /app

# 从构建阶段复制 jar 包
COPY --from=builder /build/target/ai-robot-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
