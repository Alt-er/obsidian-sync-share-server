
# obsidian-sync-share-server

# Chinese translation done by GPT, sorry for any mistakes
# 中文翻译由GPT完成，如有错误敬请谅解

# Stage 1: Build the application
# 第一步: 构建应用程序
FROM maven:3.8.4-openjdk-17-slim AS build

# Set the working directory
# 设置工作目录
WORKDIR /app

# Copy the pom.xml file and download dependencies
# 复制pom.xml文件并下载依赖项
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code
# 复制源代码
COPY src ./src

# Build the application
# 构建应用程序
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
# 使用jre 17 作为基础镜像
FROM azul/zulu-openjdk-alpine:17-latest

# 设置工作目录
WORKDIR /app

# 复制项目构建结果到镜像中
# COPY target/obsidian-sync-share-server-*.jar obsidian-sync-share-server.jar
# COPY obsidian-sync-share-server-*.jar obsidian-sync-share-server.jar
# Copy the JAR file from the build stage
COPY --from=build /app/target/obsidian-sync-share-server-*.jar obsidian-sync-share-server.jar

# 暴露服务的端口号
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS=""

# 执行启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar obsidian-sync-share-server.jar"]








