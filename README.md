# LoadPulse — 分布式 RESTful API 压测框架

## 快速启动

### Docker 一键部署

```bash
# 1. 本地先打包（避免 Docker 内拉 maven 镜像）
mvn package -DskipTests

# 2. 只需拉 mysql + jre 两个小镜像
docker pull mysql:8.0
docker pull eclipse-temurin:17-jre

# 3. 启动
docker-compose up --build
```

- Master: http://localhost:8080
- 监控页: http://localhost:8080/index.html
- MySQL: localhost:3306 (root/root)

### 本地开发

```bash
# 1. 启动 MySQL 并执行 src/main/resources/db/schema.sql
# 2. 启动 Master
mvn spring-boot:run -Dspring-boot.run.profiles=master

# 3. 启动 Worker（另开终端）
mvn spring-boot:run -Dspring-boot.run.profiles=worker -Dspring-boot.run.arguments=--server.port=8081
```

### 提交压测任务

```bash
python scripts/submit_test.py
# 或
node scripts/submit_test.js
```

## API

| 接口 | 说明 |
|------|------|
| POST /api/tasks | 提交 TestPlan |
| GET /api/tasks/{id} | 查询任务与聚合指标 |
| GET /api/sse/tasks/{id} | SSE 实时指标推送 |

详细设计见 [docs/DESIGN.md](docs/DESIGN.md)
