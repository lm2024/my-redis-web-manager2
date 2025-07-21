# 使用OpenJDK 8作为基础镜像
FROM openjdk:8-jdk-alpine

# 设置工作目录
WORKDIR /app

# 安装Maven
RUN apk add --no-cache maven

# 复制pom.xml
COPY pom.xml .

# 复制源代码
COPY src ./src

# 编译项目
RUN mvn clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8"

# 启动应用
CMD ["java", "-jar", "target/redis-web-gui-1.0.0.jar"] 