# 🎬 电影票务微服务 Demo

## 项目目标

通过一个完整的电影票务系统，深入理解微服务核心概念：
**服务拆分与通信（A） + 服务治理（B） + 容器化部署（E）**。

---

## 一、技术选型

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.x |
| 微服务套件 | Spring Cloud Alibaba | 2023.x |
| 注册中心 & 配置中心 | Nacos | 2.3.x |
| 服务调用 | OpenFeign + LoadBalancer | - |
| API 网关 | Spring Cloud Gateway | - |
| 熔断降级 | Sentinel | 1.8.x |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 定时任务 | XXL-Job | 2.4.x |
| 容器化 | Docker Compose | - |
| API 文档 | Knife4j (Swagger) | 4.x |
| 认证 | JWT (jjwt) | - |
| 项目构建 | Maven 多模块 | - |

---

## 二、项目结构

```
movie-ticketing/
├── pom.xml                        # 父 POM，统一管理依赖版本
├── common/                        # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/movieticket/common/
│       ├── result/                # 统一响应体 Result<T>
│       ├── exception/             # 全局异常处理
│       ├── auth/                  # JWT 工具类
│       └── config/                # 公共配置（Jackson、WebMvc 等）
├── gateway/                       # API 网关 (8080)
│   ├── pom.xml
│   └── src/main/java/com/movieticket/gateway/
│       ├── filter/                # JWT 鉴权过滤器
│       └── config/                # 路由配置、跨域配置
├── user-service/                  # 用户服务 (8081)
│   ├── pom.xml
│   └── src/main/java/com/movieticket/user/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       └── entity/
├── movie-service/                 # 电影服务 (8082)
│   ├── pom.xml
│   └── src/main/java/com/movieticket/movie/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       ├── entity/
│       └── client/                # Feign 客户端定义（供其他服务调用）
├── session-service/               # 场次服务 (8083)
│   ├── pom.xml
│   └── src/main/java/com/movieticket/session/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       ├── entity/
│       └── client/
├── order-service/                 # 订单服务 (8084)
│   ├── pom.xml
│   └── src/main/java/com/movieticket/order/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       ├── entity/
│       ├── client/                # Feign 调用其他服务
│       └── job/                   # XXL-Job 定时任务
├── sql/                           # 各服务数据库初始化脚本
│   ├── db_user.sql
│   ├── db_movie.sql
│   ├── db_session.sql
│   └── db_order.sql
└── docker-compose.yml             # 9 个容器编排
```

---

## 三、服务职责 & 数据模型

### 3.1 拓扑

```
                     ┌─────────────────┐
                     │   Gateway:8080   │  JWT 验签，统一路由
                     └────────┬────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
  ┌──────▼──────┐    ┌───────▼───────┐    ┌───────▼──────┐
  │ user:8081   │    │  movie:8082   │    │session:8083  │
  │ db_user     │    │  db_movie     │    │ db_session   │
  └─────────────┘    └───────────────┘    └──────────────┘
                              │                    │
                              │  Feign 跨服务调用   │
                              └──────────┬─────────┘
                                         │
                               ┌─────────▼─────────┐
                               │   order:8084      │
                               │    db_order       │
                               │  XXL-Job 客户端    │
                               └───────────────────┘
```

### 3.2 各服务详细设计

#### 🧑 user-service (8081) — 用户服务

数据库：`db_user`

| 表 | 字段 | 说明 |
|----|------|------|
| `user` | id, username, password(加密), nickname, phone, email, create_time, update_time | 用户表 |
| `user` | status | 0-禁用, 1-正常 |

核心 API：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 登录，返回 JWT |
| GET | `/api/user/current` | 获取当前用户信息（需 token） |
| GET | `/api/user/{id}` | 根据 ID 获取用户信息（内部 Feign 调用） |

---

#### 🎥 movie-service (8082) — 电影服务

数据库：`db_movie`

| 表 | 字段 | 说明 |
|----|------|------|
| `category` | id, name, create_time | 电影分类（动作、喜剧、科幻...） |
| `movie` | id, name, category_id, director, cast, duration, poster_url, description, rating, status, create_time | 电影表 |
| `movie` | status | 1-即将上映, 2-热映中, 3-已下映 |

核心 API：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/movie/list` | 电影列表（分页+分类筛选） |
| GET | `/api/movie/{id}` | 电影详情 |
| GET | `/api/movie/{id}` | 根据 ID 获取电影信息（内部 Feign 调用） |
| GET | `/api/category/list` | 分类列表 |

---

#### 🕐 session-service (8083) — 场次服务

数据库：`db_session`

| 表 | 字段 | 说明 |
|----|------|------|
| `session` | id, movie_id, cinema_hall, start_time, end_time, price, total_seats, available_seats, status, create_time | 场次表 |
| `session` | status | 1-即将开始, 2-售票中, 3-已结束, 4-已取消 |
| `seat` | id, session_id, seat_row, seat_col, seat_code(如"A-03"), status, version, expire_time | 座位表 |
| `seat` | status | 0-AVAILABLE, 1-LOCKED, 2-SOLD |
| `seat` | version | 乐观锁版本号，用于并发控制 |
| `seat` | expire_time | 锁定过期时间（15 分钟），用于超时释放 |

核心 API：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/session/list?movieId={id}` | 某电影的场次列表 |
| GET | `/api/session/{id}/seats` | 某场次的座位图 |
| POST | `/api/session/{id}/lock` | 锁定座位（需 token），入参：座位 ID 列表 |
| POST | `/api/session/{id}/unlock` | 释放座位（内部调用/定时释放） |
| POST | `/api/session/{id}/confirm` | 确认出票（支付成功后调用），座位 LOCKED→SOLD |

座位状态机：

```
AVAILABLE ──[lock]──► LOCKED ──[confirm]──► SOLD
    ▲                    │
    │  [unlock/expire]   │
    └────────────────────┘
```

并发控制策略：
1. **Redis 分布式锁**：锁定座位时，以 `session_id:seat_code` 为 key 加锁，防止同一座位被多人同时锁定
2. **数据库乐观锁**：`UPDATE seat SET status=1, version=version+1 WHERE id=? AND version=? AND status=0`，作为二次保障
3. 两者结合：Redis 锁挡住 99% 的并发冲突，乐观锁兜底

---

#### 📋 order-service (8084) — 订单服务

数据库：`db_order`

| 表 | 字段 | 说明 |
|----|------|------|
| `orders` | id, order_no, user_id, movie_id, session_id, total_amount, status, create_time, update_time, expire_time | 订单表 |
| `orders` | status | 0-PENDING(待支付), 1-PAID(已支付), 2-CANCELLED(已取消), 3-REFUNDED(已退款) |
| `orders` | expire_time | 订单过期时间（创建时间+15分钟） |
| `order_item` | id, order_id, seat_id, seat_code, price | 订单明细（座位） |

核心 API：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order/create` | 创建订单（需 token） |
| GET | `/api/order/{id}` | 订单详情（拼用户、电影、场次信息） |
| GET | `/api/order/my` | 我的订单列表（需 token） |
| PUT | `/api/order/{id}/cancel` | 取消订单，释放座位 |
| PUT | `/api/order/{id}/pay` | 模拟支付 |

时间任务：

| Job | 说明 |
|-----|------|
| `CancelExpiredOrderJob` | 每 30 秒扫描 status=PENDING 且 expire_time < NOW() 的订单，调 session-service 释放座位，订单状态改为 CANCELLED |

---

### 3.3 下单全流程

```
Client                        Gateway     user-service   movie-service   session-service   order-service
  │                              │             │               │                │               │
  │  POST /api/order/create      │             │               │                │               │
  │  Header: Authorization: Bearer <jwt>      │               │                │               │
  ├─────────────────────────────►│             │               │                │               │
  │                              │ JWT 验签    │               │                │               │
  │                              │ 解析 userId │               │                │               │
  │                              │ Header + X-User-Id       │               │                │               │
  │                              ├───────────────────────────┴───────────────┴──────────────►│
  │                              │              │               │                │  ① 验用户  │
  │                              │              │◄──────────────┼────────────────┼───────────────│
  │                              │              │  Feign 调用    │                │               │
  │                              │              │               │  ② 查电影      │               │
  │                              │              │               │◄───────────────┼───────────────│
  │                              │              │               │  Feign 调用     │               │
  │                              │              │               │                │  ③ 锁座位     │
  │                              │              │               │                │◄──────────────│
  │                              │              │               │                │ Feign 调用    │
  │                              │              │               │                │ Redis 锁      │
  │                              │              │               │                │ DB 乐观锁     │
  │                              │              │               │                │ ④ 创建订单    │
  │                              │              │               │                │ INSERT orders │
  │                              │              │               │                │ INSERT items  │
  │                              │  ← 返回订单详情（拼了用户名+电影名+场次信息）────────────────────│
  │◄─────────────────────────────┤              │               │                │               │
```

**超时释放流程：**

```
XXL-Job Admin ──[每30s调度]──► order-service::CancelExpiredOrderJob
                                      │
                                      │ 查询 status=PENDING AND expire_time < NOW()
                                      │
                                      │ 逐个取消：
                                      ├── UPDATE orders SET status=2
                                      └── Feign → session-service::unlock(seatIds)
                                                    │
                                                    └── UPDATE seat SET status=0, expire_time=NULL
```

---

## 四、中间件配置

### 4.1 Nacos

| 配置项 | 值 |
|--------|-----|
| 地址 | `nacos:8848` |
| 命名空间 | `movie-ticketing` |
| 服务分组 | `DEFAULT_GROUP` |
| 各服务配置 | 数据库连接、Redis 连接、Feign 超时、Sentinel 规则 全部放在 Nacos 配置中心 |

### 4.2 MySQL

| 库名 | 归属服务 |
|------|---------|
| `db_user` | user-service |
| `db_movie` | movie-service |
| `db_session` | session-service |
| `db_order` | order-service |

每个服务只能连接自己的库，权限隔离。

### 4.3 Redis

| 用途 | key 格式 | 过期时间 |
|------|---------|---------|
| 座位分布式锁 | `lock:seat:{sessionId}:{seatCode}` | 5 秒（仅锁定瞬时） |
| JWT 黑名单（退出登录） | `blacklist:token:{tokenId}` | Token 剩余有效期 |
| 可选：热门电影缓存 | `cache:movie:hot` | 10 分钟 |

### 4.4 XXL-Job

| 配置项 | 值 |
|--------|-----|
| Admin 地址 | `http://xxl-job-admin:8088/xxl-job-admin` |
| 执行器 appName | `order-service-executor` |
| 任务 `CancelExpiredOrderJob` | Cron: `0/30 * * * * ?`（每30秒） |

---

## 五、认证 & 安全

### 5.1 JWT 签发

- 登录成功时由 `user-service` 签发 JWT
- Payload：`{ userId, username, iat, exp }`
- 有效期：24 小时
- 密钥：Nacos 配置中心统一管理（`jwt.secret`）

### 5.2 请求链路

```
Client 请求
  │  Header: Authorization: Bearer <jwt>
  │
  ▼
Gateway 全局过滤器
  │  ① 从 Authorization 提取 token
  │  ② 用 jjwt 库验签、解析 userId
  │  ③ 将 X-User-Id、X-Username 写入请求头
  │  ④ 路由到目标服务
  │
  ▼
业务服务 Controller
  │  通过 @RequestHeader("X-User-Id") 获取用户 ID
  │
  ▼
业务服务 Feign 调用其他服务
  │  FeignRequestInterceptor 从 RequestContextHolder 取当前请求 token
  │  自动注入 Authorization 头到 Feign 请求
  │
  ▼
被调用服务同样解析 token，完成身份传递
```

### 5.3 安全规则

- Gateway 层：未携带 token 的请求，白名单之外的路径返回 401
- 白名单：`/api/user/register`, `/api/user/login`, `/api/movie/list`, `/api/movie/{id}`, `/api/session/list`, `/api/session/{id}/seats`, `/v3/api-docs/**`（Knife4j 文档）
- 服务层：每个服务各自的 Filter/Interceptor 验证内部转发的 X-User-Id，防止绕过 Gateway 直接访问

---

## 六、熔断降级

### 6.1 Sentinel 规则

| 资源 | 规则类型 | 阈值 | 说明 |
|------|---------|------|------|
| 场次服务 lock 接口 | 限流 QPS | 100 | 防止瞬时锁座请求打垮数据库 |
| 订单服务 create 接口 | 熔断(慢调用比例) | 响应>1s 且比例>50% | 下单慢就熔断，快速返回"系统繁忙" |
| 所有 Feign 调用 | 熔断(异常比例) | 异常比例>50% | 下游挂了不拖死上游 |

### 6.2 Feign 降级

每个 `@FeignClient` 都配 `fallback` 实现类：
- 用户服务挂了 → 订单详情返回 `userName: "未知用户"`
- 电影服务挂了 → 订单详情返回 `movieName: "未知电影"`
- 场次服务挂了 → 下单直接拒绝，提示"系统繁忙"

---

## 七、Docker Compose 编排

### 7.1 容器列表（9 个）

| 容器名 | 镜像 | 端口 | 依赖 |
|--------|------|------|------|
| nacos | nacos/nacos-server:v2.3.2 | 8848, 9848 | 无 |
| mysql | mysql:8.0 | 3306 | 无 |
| redis | redis:7-alpine | 6379 | 无 |
| xxl-job-admin | xuxueli/xxl-job-admin:2.4.1 | 8088 | mysql |
| gateway | 本地构建 | 8080 | nacos |
| user-service | 本地构建 | 8081 | nacos, mysql, redis |
| movie-service | 本地构建 | 8082 | nacos, mysql |
| session-service | 本地构建 | 8083 | nacos, mysql, redis |
| order-service | 本地构建 | 8084 | nacos, mysql, xxl-job-admin |

### 7.2 启动顺序

```
1. nacos, mysql, redis          (基础设施)
2. xxl-job-admin                (依赖 mysql)
3. user-service                 (依赖 nacos, mysql)
4. movie-service                (依赖 nacos, mysql)
5. session-service              (依赖 nacos, mysql, redis)
6. order-service                (依赖 nacos, mysql, xxl-job-admin)
7. gateway                      (依赖 nacos)
```

使用 `depends_on` + `healthcheck` 确保依赖就绪后再启动。

### 7.3 网络

所有容器在同一 Docker 网络 `movie-ticketing-net` 内，通过容器名通信。

---

## 八、开发路线图

建议按以下顺序开发，每步完成后再进入下一步：

### Phase 1：基础设施 + 公共模块

- [ ] 创建父 POM，统一管理 Spring Cloud Alibaba / Spring Boot 版本
- [ ] 写 `common` 模块：`Result<T>`、全局异常、JWT 工具
- [ ] 写 `docker-compose.yml`（nacos + mysql + redis + xxl-job-admin）
- [ ] 写 `sql/` 下的 4 个数据库初始化脚本
- [ ] 启动基础设施，确认 Nacos 控制台可访问

### Phase 2：user-service

- [ ] 建项目、配 Nacos、连 `db_user`
- [ ] 注册 + 登录（返回 JWT）+ 获取用户信息
- [ ] 通过 Knife4j 验证接口
- [ ] Docker Compose 中加入 user-service

### Phase 3：movie-service

- [ ] 建项目、配 Nacos、连 `db_movie`
- [ ] 电影列表（分页+分类筛选）+ 详情
- [ ] 预置 20 条电影数据
- [ ] Docker Compose 中加入 movie-service

### Phase 4：Gateway

- [ ] 建项目、配路由规则
- [ ] JWT 鉴权过滤器
- [ ] 验证：不放 token → 401，放 token → 正常转发
- [ ] Docker Compose 中加入 Gateway

### Phase 5：session-service

- [ ] 建项目、配 Nacos、连 `db_session`
- [ ] 场次列表 + 座位图展示
- [ ] 锁定座位（Redis 分布式锁 + DB 乐观锁）
- [ ] 释放座位
- [ ] 确认出票（LOCKED→SOLD）
- [ ] Docker Compose 中加入 session-service

### Phase 6：order-service（核心）

- [ ] 建项目、配 Nacos、连 `db_order`
- [ ] 创建订单（Feign 调用 user + movie + session）
- [ ] 订单详情（Feign 拼数据：用户 + 电影 + 场次 + 座位信息）
- [ ] 取消订单（释放座位）
- [ ] 模拟支付
- [ ] 接入 XXL-Job：写 `CancelExpiredOrderJob`
- [ ] Docker Compose 中加入 order-service

### Phase 7：治理 & 收尾

- [ ] Sentinel 控制台接入，配熔断降级规则
- [ ] Knife4j 文档聚合到 Gateway
- [ ] 全链路联调：注册 → 登录 → 查电影 → 查场次 → 锁座 → 下单 → 支付 → 超时释放
- [ ] `docker-compose up -d` 一键启动所有 9 个容器
- [ ] 写 README 最终版的运行说明

---

## 九、验收标准

完成以下场景即为 demo 成功：

1. ✅ `docker-compose up -d` 一次启动，9 个容器全部 healthy
2. ✅ 注册用户 → 登录获得 JWT
3. ✅ 浏览电影列表 → 查看场次 → 查看座位图
4. ✅ 锁座成功，其他用户无法重复锁定同一座位
5. ✅ 下单成功，订单详情完整展示用户名 + 电影名 + 场次 + 座位号
6. ✅ 订单 15 分钟未支付，XXL-Job 自动取消并释放座位
7. ✅ 手动停掉 session-service → 下单请求被 Sentinel 熔断，返回"系统繁忙"
8. ✅ 重启 session-service → 自动恢复

---

## 十、扩展方向（未来可选）

- [ ] 支付服务（真实对接支付宝/微信支付沙箱）
- [ ] 通知服务（下单成功发邮件/短信，MQ 异步）
- [ ] 分布式事务 Seata（支付成功 → 订单 PAID → 座位 SOLD 的事务链）
- [ ] 链路追踪 SkyWalking
- [ ] K8s 部署（Helm Chart）
- [ ] 秒杀场次（借鉴你已有的秒杀项目经验）
