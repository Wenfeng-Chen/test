# LoadPulse 分布式 RESTful API 压测框架 — 设计文档

## 1. 架构概览

```
测试脚本(Python/JS) ──POST /api/tasks──► Master(Spring Boot)
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    │                      │                      │
               Worker-1              Worker-2                 MySQL
                    │                      │                      │
                    └──────────► 被测 REST API ◄──────────────────┘
```

- **Master**：任务调度、Worker 管理、结果聚合、SSE 推送、Web 监控页
- **Worker**：同一代码库，`worker` Profile 启动，轮询子任务并执行 HTTP 请求
- **存储**：MyBatis-Plus + MySQL

## 2. Master-Worker 通信协议

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/workers/register` | POST | Worker 注册，返回 workerId |
| `/api/workers/heartbeat` | POST | 心跳保活（30s 超时标记 OFFLINE） |
| `/api/workers/tasks/poll` | GET | Worker 轮询领取 SubTask |
| `/api/workers/metrics/report` | POST | 批量上报请求结果 |
| `/api/workers/tasks/complete` | POST | Worker 完成子任务通知 |

通信格式：JSON，统一响应 `ApiResponse<T>`。

## 3. 任务调度策略

1. 用户提交 TestPlan（JSON）
2. Master 持久化 `test_task`，状态 `PENDING → RUNNING`
3. **单机模式**（STANDALONE 或无活跃 Worker）：Master 本地线程池发压
4. **分布式模式**（DISTRIBUTED 且有 Worker）：
   - 总并发 C 均分给 N 个 Worker：`base = C/N`，余数分配给前几个 Worker
   - 生成 N 个 SubTask 入队
   - Worker 轮询领取并执行
5. 所有 Worker 完成后 Master 聚合指标，状态 `COMPLETED`

## 4. 数据聚合算法

输入：某 taskId 下全部 `request_result.latency_ms`

| 指标 | 算法 |
|------|------|
| QPS | totalRequests / durationSeconds |
| 平均 RT | sum(latency) / count |
| Min/Max | 排序后首尾 |
| TP90/95/99 | 升序排列，index = ceil(p × n) - 1 |
| 错误率 | failedCount / totalCount × 100 |

## 5. HTTP 方法支持

- GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS：`java.net.http.HttpClient`
- CONNECT/TRACE：Apache HttpClient 5 兜底

## 6. 扩展设计（未实现）

- RPS 模式、阶梯加压
- CLI 工具
- 单元测试覆盖聚合逻辑

## 7. 部署

```bash
docker-compose up --build
# Master: http://localhost:8080
# Dashboard: http://localhost:8080/index.html
```
