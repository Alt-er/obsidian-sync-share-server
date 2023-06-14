
# obsidian-sync-share-server

# 使用jre 17 作为基础镜像
FROM azul/zulu-openjdk-alpine:17-latest

# 设置工作目录
WORKDIR /app

# 复制项目构建结果到镜像中
COPY target/obsidian-sync-share-server-*.jar obsidian-sync-share-server.jar
# COPY obsidian-sync-share-server-*.jar obsidian-sync-share-server.jar

# 暴露服务的端口号
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS=""

# 执行启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar obsidian-sync-share-server.jar"]






