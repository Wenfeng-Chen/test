# 本地先打包: mvn package -DskipTests
# 再构建: docker-compose up --build
# 只需拉取 eclipse-temurin:17-jre，避免 maven 镜像下载失败
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/interview-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
