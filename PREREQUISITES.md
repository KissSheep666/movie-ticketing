# 前置概念手册

本文档覆盖从单体开发过渡到微服务所需要的所有新概念。
每个概念从你已有的 Spring Boot 认知出发，说明**它解决什么问题**、**在项目中怎么用**。

---

## 目录

1. [Nacos — 服务注册与发现](#1-nacos--服务注册与发现)
2. [Nacos — 配置中心](#2-nacos--配置中心)
3. [OpenFeign — 服务间调用](#3-openfeign--服务间调用)
4. [Spring Cloud Gateway — API 网关](#4-spring-cloud-gateway--api-网关)
5. [JWT 与微服务认证透传](#5-jwt-与微服务认证透传)
6. [Sentinel — 熔断降级与限流](#6-sentinel--熔断降级与限流)
7. [XXL-Job — 分布式定时任务](#7-xxl-job--分布式定时任务)
8. [数据库隔离与跨服务数据拼接](#8-数据库隔离与跨服务数据拼接)
9. [Redis 分布式锁](#9-redis-分布式锁)
10. [乐观锁 — 数据库层面的并发控制](#10-乐观锁--数据库层面的并发控制)
11. [Docker Compose — 多容器编排](#11-docker-compose--多容器编排)
12. [Maven 多模块项目](#12-maven-多模块项目)

---

## 1. Nacos — 服务注册与发现

### 单体怎么做的

你之前的若依/秒杀项目，所有模块在一个 JVM 里。A 调 B 直接 `@Autowired` 注入，不需要知道对方在哪。

### 微服务的问题

4 个服务 → 4 个独立进程 → 4 个不同的容器。`order-service` 要调 `session-service`，怎么知道对方地址？

### Nacos 是电话本

每个服务启动时去 Nacos **报到**（注册），调用方去 Nacos **查询**（发现）。

```
启动时:
  session-service → Nacos: "我上线了, IP=192.168.1.10, 端口=8083"

调用时:
  order-service → Nacos: "session-service 在哪?"
  Nacos → order-service: "192.168.1.10:8083, 还有一个副本在 192.168.1.11:8083"

挂了时:
  Nacos 收不到 session-service 心跳 → 从列表移除
  order-service 不再尝试调死掉的实例
```

### 术语

| 概念 | 说明 |
|------|------|
| 注册中心 | Nacos Server，独立进程 |
| 服务注册 | 服务启动时把自己 IP+端口+服务名告诉 Nacos |
| 服务发现 | 调用方按服务名去 Nacos 拿可用实例列表 |
| 健康检查 | Nacos 定期向服务发心跳包，没响应就标记下线 |

### 在项目里

每个服务的 `application.yml` 配：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848   # Nacos 地址
        namespace: movie-ticketing # 命名空间，隔离不同项目
```

然后代码里 Feign 写服务名就行，不用写 URL（见第 3 节）。

---

## 2. Nacos — 配置中心

### 单体怎么做的

所有配置放 `application.yml`，改配置要改文件、重新打包、重启。

### 微服务的问题

4 个服务，各自有 `application.yml`：
- 数据库密码改了？改 4 个文件
- Sentinel 规则调整？改 `order-service` 的配置文件，重启
- 如果每个服务有多个实例，问题翻倍

### 配置中心做什么

所有配置统一存在 Nacos 里，服务启动时从 Nacos 拉取配置。修改配置后，一行命令热刷新，不用重启。

```
之前: 每个服务自己管配置
  user-service/application.yml
  movie-service/application.yml
  session-service/application.yml
  order-service/application.yml

之后: 所有配置在 Nacos 里统一管理
  Nacos 配置中心:
    ├── user-service-dev.yml
    ├── movie-service-dev.yml
    ├── session-service-dev.yml
    ├── order-service-dev.yml
    └── common.yml  (公共配置，所有服务共享)
```

### 核心配置项（项目里放 Nacos 的）

| 配置 | 举例 | 为什么放 Nacos |
|------|------|---------------|
| 数据库连接 | `spring.datasource.url` | 密码可能轮换 |
| Redis 连接 | `spring.data.redis.host` | 环境切换 |
| Sentinel 规则 | 限流阈值 | 运行时动态调整 |
| Feign 超时 | `spring.cloud.openfeign.client.config.*.read-timeout` | 根据网络状况调 |
| JWT 密钥 | `jwt.secret` | 敏感信息，统一管理 |
| XXL-Job 地址 | `xxl.job.admin.addresses` | 环境不同 |

### 在项目里

每个服务的 `bootstrap.yml`（比 `application.yml` 更早加载）只写一行：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: nacos:8848
        namespace: movie-ticketing
        file-extension: yml
```

然后 Nacos 控制台里有该服务的配置文件，启动时自动拉取。

---

## 3. OpenFeign — 服务间调用

### 单体怎么做的

```java
@Autowired
private UserService userService;  // 同进程内调用，直接注入
```

### 微服务怎么做

`order-service` 要调 `user-service`，它们是两个进程，只能通过 **HTTP/RPC** 通信。

最原始的做法：自己用 `RestTemplate` 发 HTTP 请求：

```java
// 又长又丑，还得手动拼 URL、处理异常
String url = "http://192.168.1.10:8081/api/user/" + userId;
UserDTO user = restTemplate.getForObject(url, UserDTO.class);
```

### OpenFeign 做什么

**声明式的 HTTP 客户端**——你只写接口，自动发 HTTP 请求。配合 Nacos，连 URL 都不用写。

```java
@FeignClient(name = "user-service")  // ← 只写服务名，Nacos 解析地址
public interface UserClient {

    @GetMapping("/api/user/{id}")
    Result<UserDTO> getUserById(@PathVariable Long id);
}
```

`order-service` 使用：

```java
@Autowired
private UserClient userClient;  // 像注入本地 Service 一样

public OrderDetailDTO getOrderDetail(Long orderId) {
    Order order = orderMapper.selectById(orderId);
    UserDTO user = userClient.getUserById(order.getUserId()); // 实际发了 HTTP 请求
    // 拼数据...
}
```

### Feign 帮你做了什么

```
order-service 代码: userClient.getUserById(1L)
         │
         ▼
Feign 代理对象:
  1. 去 Nacos 查 "user-service" 的地址 → 192.168.1.10:8081
  2. 拼接 URL: http://192.168.1.10:8081/api/user/1
  3. 发 GET 请求
  4. 接收 JSON 响应
  5. 反序列化成 Result<UserDTO>
  6. 返回给你
```

### 和 Nacos 的关系

没有 Nacos 时写 Feign：

```java
@FeignClient(name = "user-service", url = "http://192.168.1.10:8081") // 硬编码
```

有了 Nacos 之后，只写 `name`，地址由 Nacos 自动解析——负载均衡、故障转移全自动。

### 在项目里

4 个服务之间所有调用都用 Feign：

| 调用方 | 被调方 | 场景 |
|--------|--------|------|
| order-service | user-service | 下单时验用户、查详情时拼用户名 |
| order-service | movie-service | 下单时查电影价格、查详情时拼电影名 |
| order-service | session-service | 下单时锁座位、取消时释放座位、定时任务释放座位 |
| order-service | session-service | 支付后确认出票 |

---

## 4. Spring Cloud Gateway — API 网关

### 单体没有这个概念

单体项目里没有“网关”，因为只有一个入口。所有请求直接打在 Controller 上。

### 微服务的问题

4 个服务 → 4 个端口 → 4 个地址：
- 前端要调 `http://A:8081/user/login`
- 前端要调 `http://B:8082/movie/list`
- 前端要调 `http://C:8083/session/list`

前端疯了吗？而且跨域问题一堆。

### Gateway 做什么

**统一的入口**，一个门卫 + 路由员。所有请求都走 `localhost:8080`，Gateway 根据路径转发到对应的服务。

```
              来自前端的请求

                    │
                    ▼
         ┌──────────────────┐
         │  Gateway :8080   │  ← 唯一入口
         └──────────────────┘
                    │
      ┌─────────────┼─────────────┐
      │  /api/user/**  → user-service:8081
      │  /api/movie/** → movie-service:8082
      │  /api/session/** → session-service:8083
      │  /api/order/**  → order-service:8084
      └─────────────────────────────────────────┘
```

### 它还做什么

| 功能 | 说明 | 你的项目 |
|------|------|---------|
| **路由转发** | 按路径规则转发到不同服务 | 上述 4 条规则 |
| **鉴权验证** | 入口处检查 JWT，不合法直接拦 | Gateway 全局过滤器 |
| **跨域处理** | 统一配置 CORS，不用每个服务各写一遍 | `application.yml` 配 |
| **限流** | 入口层限制请求频率 | 可选，你已经有 Sentinel |
| **请求改写** | 添加/删除/修改请求头 | 把 JWT 解析出的 userId 塞进 Header |

### Gateway ≠ Controller

Gateway 是**基于 WebFlux（响应式）**的，不是传统的 Spring MVC。它不做业务逻辑，只做转发。你不在 Gateway 里写业务代码。

### 在项目里

路由配置（`application.yml`）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service        # lb = load balance，走 Nacos
          predicates:
            - Path=/api/user/**
        - id: movie-service
          uri: lb://movie-service
          predicates:
            - Path=/api/movie/**
```

JWT 鉴权在 Gateway 层写成 `GlobalFilter`，每个请求进来先验 token。

---

## 5. JWT 与微服务认证透传

### 鉴权面临的挑战

单体项目：用户登录 → 存 Session → 所有请求自动带上 Cookie → 后端从 Session 拿用户信息。

微服务：Session 不行了——4 个服务，Session 存在哪？user-service 的 Session，movie-service 怎么拿到？

### JWT 无状态认证

JWT（JSON Web Token）是个包含用户信息的签名字符串，**服务端不存 Session，全靠 token 自己**。

**结构**（Base64 编码的三段字符串）：

```
eyJhbGciOi...    .eyJ1c2VySW...     .SflKxwRJ...
    │                   │                  │
   Header              Payload           Signature
  (算法类型)          (用户信息)         (签名防篡改)
```

Payload 里面的东西：

```json
{
  "userId": 1,
  "username": "zhangsan",
  "iat": 1737414000,
  "exp": 1737500400
}
```

### JWT 的生命周期

```
登录                                      请求业务
  │                                          │
  │  POST /api/user/login                    │  GET /api/order/my
  │  Body: {username, password}              │  Header: Authorization: Bearer <jwt>
  │                                          │
  ▼                                          ▼
user-service                      Gateway 全局过滤器:
  ① 验密码                                   ① 从 Header 取 token
  ② 生成 JWT                                ② 用密钥验签（任何服务都能验，因为有密钥）
  ③ 返回 token                              ③ 解析出 userId=1, username=zhangsan
                                            ④ Header 加 X-User-Id: 1, X-Username: zhangsan
                                            ⑤ 转发到 order-service
```

### 微服务认证透传——这个项目最精妙的地方

问题来了：`order-service` 内部又要 Feign 调 `session-service`，`session-service` 怎么知道是谁在请求？

答案：**Feign 拦截器自动传 token**。

```
外部请求 Header:
  Authorization: Bearer <jwt>

Gateway → 解析 token → Header 加 X-User-Id → 转发

order-service 收到请求:
  │
  │  Feign 调 session-service 之前
  │  FeignRequestInterceptor 自动:
  │    从当前请求上下文取出 Authorization 头
  │    塞到 Feign 请求里
  │
  ▼
session-service 收到 Feign 请求:
  │  Authorization: Bearer <jwt>   ← 传过来了
  │  解析 token → 知道是谁在操作
```

这样整个调用链上，每个服务都能知道“当前请求是谁”，不需要反复传递 userId。

### 在项目里

```java
// Feign 拦截器（写在 common 模块或 order-service 里）
@Component
public class FeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 从当前 HTTP 请求里拿 token
        HttpServletRequest request = ((ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        if (token != null) {
            // 注入到 Feign 请求头
            template.header("Authorization", token);
        }
    }
}
```

---

## 6. Sentinel — 熔断降级与限流

### 问题场景

你在秒杀项目里应该遇到过：瞬时流量打进来，数据库扛不住，整个服务卡死。单体项目里是一个整体崩溃，微服务里问题更隐蔽——**级联故障**。

```
用户狂刷座位锁请求
    │
session-service CPU 飙到 100% → 响应变慢
    │
order-service 在等 session-service 响应 → 线程池耗尽
    │
Gateway 连接 order-service 超时 → 整个站点不可用
```

一个服务拖死了全家。

### Sentinel 三大核心能力

#### ① 限流（Flow Control）

限制单位时间内通过的请求数。

```
Sentinel 规则: QPS > 100 时拒绝新请求

来访 200 QPS → 100 个正常处理，100 个直接返回"系统繁忙"
```

你的项目：session-service 的锁座接口配 QPS 100。

#### ② 熔断（Circuit Breaker）

当被调方连续失败，调用方直接**熔断**，不再尝试调它。

```
正常:
  order ──调用──► session  ✅

session 开始失败:
  order ──调用──► session  ❌
  order ──调用──► session  ❌
  order ──调用──► session  ❌  (连续失败 5 次)

Sentinel 熔断器打开:
  order ──不调了,直接走 fallback──► 返回"座位服务异常,请稍后"
         │
         │  等 10 秒，发一个探测请求
         │
         ├── 成功 → 熔断器关闭，恢复正常调用
         └── 失败 → 继续熔断
```

#### ③ 降级（Fallback）

熔断后的备选方案：

```java
@FeignClient(name = "session-service", fallback = SessionClientFallback.class)
public interface SessionClient { ... }

// 降级实现
@Component
public class SessionClientFallback implements SessionClient {
    @Override
    public Result lock(Long id) {
        return Result.fail("座位服务繁忙，请稍后再试");
    }
}
```

### 三个能力的关系

```
限流 → "你来得太快了，等一下"
熔断 → "那边服务好像坏了，别去了"
降级 → "原路走不通，我走个备用方案"
```

### 在项目里

| 场景 | 规则 |
|------|------|
| 锁座接口 QPS 保护 | 限流 100 QPS |
| session-service 不可用 | order-service 对 session 调用的熔断 |
| movie-service 慢响应 | 慢调用比例熔断（>1s 且 >50%比例） |
| 所有 Feign 调用 | 异常比例熔断（>50% 开熔断器） |

---

## 7. XXL-Job — 分布式定时任务

### 单体怎么做的

```java
@Scheduled(cron = "0/30 * * * * ?")  // Spring 内置，简单好用
public void cancelExpiredOrders() { ... }
```

这在一个服务里没问题。微服务里如果你的 order-service 有 3 个实例，三个实例都会执行这个定时任务——每个过期订单被取消了 3 次。

### XXL-Job 做什么

让定时任务**只在一个地方执行一次**。

```
之前(@Scheduled):
  order-service-1 每30s扫过期订单 → 取消
  order-service-2 每30s扫过期订单 → 取消（重复！）
  order-service-3 每30s扫过期订单 → 取消（重复！）

之后(XXL-Job):
  XXL-Job Admin 调度中心:
    "该执行了" ──调度──► order-service-1 执行（路由策略: 轮询）
                         order-service-2 空闲
                         order-service-3 空闲
```

### 架构

```
┌─────────────────────┐
│   XXL-Job Admin     │  调度中心（Web 管理台，独立进程）
│   任务管理           │  - 管理哪些任务
│   调度触发           │  - 什么时候触发
│   日志查看           │  - 执行记录
└────────┬────────────┘
         │ 调度
  ┌──────┴──────┐
  │             │
  ▼             ▼
┌─────────┐  ┌─────────┐
│ order-1 │  │ order-2 │  执行器（你的业务服务）
│ (执行器)│  │ (执行器)│  - 注入 XXL-Job 客户端依赖
└─────────┘  └─────────┘  - 写 @XxlJob 注解的方法
```

### 在项目里

```java
@XxlJob("cancelExpiredOrderJob")  // ← XXL-Job 注解，不是 @Scheduled
public void cancelExpiredOrders() {
    // 查过期未支付订单
    List<Order> expiredOrders = orderMapper.selectExpiredPending();
    for (Order order : expiredOrders) {
        // 释放座位（Feign 调 session-service）
        sessionClient.unlock(order.getSessionId(), order.getSeatIds());
        // 标记订单取消
        orderMapper.updateStatus(order.getId(), "CANCELLED");
    }
}
```

在 XXL-Job Admin 控制台配置任务：cron `0/30 * * * * ?`，路由策略"轮询"。

---

## 8. 数据库隔离与跨服务数据拼接

### 这是微服务最痛的点

单体项目：一条 SQL 搞定所有。

```sql
SELECT o.*, u.username, m.name AS movie_name, s.start_time
FROM orders o
JOIN user u ON o.user_id = u.id
JOIN movie m ON o.movie_id = m.id
JOIN session s ON o.session_id = s.id
```

微服务：user 数据在 `db_user`，movie 数据在 `db_movie`，order 数据在 `db_order`。**你无法跨库 JOIN。**

### 怎么拼数据

只能通过 Feign 调用逐个收集数据，在代码里拼装：

```
查订单详情:
  ① orderMapper.selectById(orderId)            → {userId:1, movieId:5, sessionId:3, ...}
  ② userClient.getById(1)                       → {username:"张三", phone:"138..."}
  ③ movieClient.getById(5)                      → {name:"流浪地球", director:"郭帆"}
  ④ sessionClient.getById(3)                    → {startTime:"19:30", hall:"IMAX厅"}
  ⑤ 在代码里拼装 OrderDetailDTO
```

### 代价 & 收益

| 代价 | 收益 |
|------|------|
| 一次 JOIN 变成 N 次 HTTP 调用 | user-service 挂了不影响 movie 列表查询 |
| 网络延迟增加 | 每个服务的数据库独立扩容 |
| 代码变复杂 | 每个团队独立开发，不会踩到对方的表 |

### 性能优化（后续扩展）

- 并行调用：3 个 Feign 请求可以同时发出，总耗时 = 最慢的那个
- 缓存：用户名、电影名这种改得不频繁的，缓存到 Redis

---

## 9. Redis 分布式锁

### 问题

你的项目里，用户 A 和用户 B 同时锁座位 "IMAX-1排-3座"：

```
用户A: SELECT status FROM seat WHERE id=100  → status=AVAILABLE ✓
用户B: SELECT status FROM seat WHERE id=100  → status=AVAILABLE ✓  (同时读到)
用户A: UPDATE seat SET status='LOCKED' WHERE id=100  → 锁成功
用户B: UPDATE seat SET status='LOCKED' WHERE id=100  → 也锁成功了???
```

两个人买到了同一个座位。

### Redis 分布式锁原理

Redis 的 `SET NX`（SET if Not eXists）是原子操作——只有一个线程能成功：

```
用户A: SET lock:seat:3:A-03 LOCKED NX EX 5   → OK     (抢先拿到锁)
用户B: SET lock:seat:3:A-03 LOCKED NX EX 5   → NULL   (键已存在，获取失败)
         │
         ▼
     用户B 返回"座位已被锁定"

用户A 操作完成后:
    DEL lock:seat:3:A-03  (释放锁)
```

| 参数 | 含义 |
|------|------|
| `lock:seat:3:A-03` | key：sessionId + seatCode 组合 |
| `NX` | Only set if Not eXists——原子性保证 |
| `EX 5` | 5 秒后自动过期，防止死锁（服务崩溃没来得及释放） |

### 和数据库乐观锁的关系

Redis 锁 + 数据库乐观锁，**双重保障**：

```
请求进来:
  ① Redis SET NX ← 挡掉 99% 并发
     │ 抢到了
     ▼
  ② UPDATE seat SET status='LOCKED', version=version+1
     WHERE id=100 AND version=0 AND status='AVAILABLE'
     │ affected rows=1 → 成功
     │ affected rows=0 → 被别人抢了（乐观锁兜底）
```

---

## 10. 乐观锁 — 数据库层面的并发控制

### 悲观锁 vs 乐观锁

你会用 `SELECT ... FOR UPDATE`（悲观锁）吗？它的思路是**先锁住，再操作**。

乐观锁反过来：**先操作，提交时检查有没有被别人改过**。

### 实现方式（version 字段）

表里加一个 `version` 字段：

```sql
UPDATE seat
SET status = 1, version = version + 1
WHERE id = 100 AND version = 0 AND status = 0;
```

| 场景 | affected rows | 结果 |
|------|---------------|------|
| 没人改过 | 1 | 锁座成功 |
| 别人先改了（version 从 0 变成 1） | 0 | 锁座失败 |

代码判断：

```java
int rows = seatMapper.lockSeat(seatId, currentVersion);
if (rows == 0) {
    throw new BizException("座位已被锁定");
}
```

### 为什么 Redis 锁之后还要乐观锁

Redis 锁保护应用层并发，乐观锁保护数据库层并发（比如 Redis 锁失效了、代码 bug、手动操作数据库等边界情况）。

---

## 11. Docker Compose — 多容器编排

### 你已有的认知

你会 Docker，可能跑过 `docker run mysql` 这种单容器命令。

### 微服务的问题

9 个容器要一起跑：Nacos、MySQL、Redis、XXL-Job、Gateway、4 个业务服务。手动逐个启动：

```bash
docker run nacos ...
docker run mysql ...
docker run redis ...
docker run xxl-job-admin ...
docker build -t user-service . && docker run user-service ...
docker build -t movie-service . && docker run movie-service ...
...
# 每次重启都来一遍，疯了吧
```

而且有启动顺序要求：必须先启动 Nacos 和 MySQL，业务服务启动时要去连 Nacos。

### Docker Compose 做什么

一个 YAML 文件描述所有容器，一条命令启动全部：

```bash
docker-compose up -d
# 9 个容器全部启动
```

### 关键概念

| 概念 | 说明 |
|------|------|
| 服务 (services) | 你要跑的每个容器 |
| 网络 (networks) | 所有容器在同一个虚拟网络内，可以通过容器名互相访问（`nacos:8848` 而不是 `localhost:8848`） |
| 卷挂载 (volumes) | MySQL 数据、Nacos 日志等需要持久化的目录 |
| 构建 (build) | 4 个业务服务和 Gateway 是本地代码，需要 build 成镜像 |
| 依赖 (depends_on) | 声明启动顺序 |
| 健康检查 (healthcheck) | 检查MySQL 是否 ready，再启动依赖它的服务 |

### 典型结构

```yaml
version: '3.8'
services:
  nacos:
    image: nacos/nacos-server:v2.3.2
    ports:
      - "8848:8848"
    networks:
      - movie-net

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
    volumes:
      - ./sql:/docker-entrypoint-initdb.d  # 启动时自动执行 SQL
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    networks:
      - movie-net

  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
    depends_on:
      mysql:
        condition: service_healthy   # 等 MySQL healthy 再启动
      nacos:
        condition: service_healthy
    networks:
      - movie-net

  # ... 其他服务类似

networks:
  movie-net:
    driver: bridge
```

> **重要**：容器之间用 `nacos:8848`、`mysql:3306` 这种容器名来访问，而不是 `localhost`。每个容器有自己的 localhost，`localhost` 指向自己。

---

## 12. Maven 多模块项目

### 单体怎么做的

一个 Spring Boot 项目 = 一个 `pom.xml` + 一个 `Application.java`。

### 微服务怎么做

4 个独立的 Spring Boot 项目，但共享公共代码（Result、JWT 工具、异常处理）。如果把公共代码复制粘贴到每个服务，改一处要改 4 个。

### Maven 多模块

一个父 POM 管理所有子模块：

```
movie-ticketing/              ← 父模块，只放 pom.xml
├── pom.xml                   ← <packaging>pom</packaging>
├── common/                   ← 普通 jar，被其他模块依赖
│   └── pom.xml
├── gateway/                  ← Spring Boot 可执行 jar
│   └── pom.xml
├── user-service/             ← Spring Boot 可执行 jar
│   └── pom.xml
├── movie-service/
│   └── pom.xml
├── session-service/
│   └── pom.xml
└── order-service/
    └── pom.xml
```

### 层级关系

```
父 POM (movie-ticketing)
  │  <dependencyManagement> 统一版本号
  │
  ├── common (jar)
  │     │  被下面所有模块依赖
  │
  ├── gateway ────────────── 依赖 common
  ├── user-service ───────── 依赖 common
  ├── movie-service ──────── 依赖 common
  ├── session-service ────── 依赖 common
  └── order-service ──────── 依赖 common
```

### 父 POM 的作用

```xml
<!-- 父 POM 的 <dependencyManagement> 统一管理版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2023.0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

这样每个子模块引入依赖时**不用写版本号**，版本号在父 POM 里统一控制。保证了 Gateway 用的 Nacos 版本和 order-service 用的完全一致。

### 构建

```bash
# 在根目录
mvn clean package -DskipTests
# 自动按依赖顺序构建：common → 4个业务模块 → gateway
```

---

## 概念关系全景图

```
                         Docker Compose (11)
                         ┌──────────────────────────────────────────────┐
                         │  把所有东西编排成一个整体                      │
                         │                                              │
          ┌──────────────┼──────────────────────────────┐               │
          │              │                              │               │
     Nacos (1,2)    Gateway (4)                    MySQL (8) + Redis (9,10)
    注册中心+配置中心  API网关                         数据层              │
          │              │                              │               │
          │              │                              │               │
    ┌─────┴──────┬───────┴────────┬─────────────────────┤               │
    │            │                │                     │               │
  user-svc  movie-svc  session-svc  ───Feign(3)──► order-svc            │
   8081       8082        8083                        8084              │
                                          JWT 透传(5)                   │
                                          Sentinel 熔断(6)              │
                                          XXL-Job 定时任务(7)           │
    │            │                │                     │               │
    └────────────┴────────────────┴─────────────────────┘               │
                     Maven 多模块 (12)                                   │
                     └── 统一管理依赖、公共代码                            │
                         └──────────────────────────────────────────────┘
```

---

## 建议学习顺序

不要并行消化所有概念，按项目 Phase 来：

| Phase | 要先理解的概念 |
|-------|---------------|
| 1. 基础设施 | Maven 多模块(12)、Docker Compose(11) |
| 2. user-service | Nacos 注册中心(1)、配置中心(2) |
| 3. movie-service | OpenFeign(3) |
| 4. Gateway | Gateway(4)、JWT 认证透传(5) |
| 5. session-service | Redis 分布式锁(9)、乐观锁(10)、数据库隔离(8) |
| 6. order-service | 跨服务数据拼接(8)、XXL-Job(7)、Sentinel(6) |

---

当你开始写某个 Phase 的代码时，回来读对应的概念章节，立即就能落地。
