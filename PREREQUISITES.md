# 📚 概念手册

> 这份文档回答**「为什么」**：为什么需要网关、为什么要有注册中心、为什么锁个座位要动用 Redis 和数据库两层。
>
> 具体的配置怎么写、接口长什么样、表结构是什么 —— 那些在 `README.md` 里，本文不重复。
>
> 阅读建议：按顺序读。第三节的九个概念是有依赖关系的，跳着读容易断片。

---

## 目录

- [一、先看全景：这个项目在做什么](#一先看全景这个项目在做什么)
- [二、为什么要拆成微服务](#二为什么要拆成微服务)
- [三、九个核心概念](#三九个核心概念)
  - [3.1 服务注册与发现](#31-服务注册与发现--nacos-的第一重身份)
  - [3.2 配置中心](#32-配置中心--nacos-的第二重身份)
  - [3.3 API 网关](#33-api-网关--spring-cloud-gateway)
  - [3.4 服务间调用](#34-服务间调用--openfeign)
  - [3.5 服务容错](#35-服务容错--sentinel-与降级)
  - [3.6 分布式锁](#36-分布式锁--redis-与乐观锁的双层防御)
  - [3.7 分布式定时任务](#37-分布式定时任务--xxl-job)
  - [3.8 跨服务数据一致性](#38-跨服务数据一致性)
  - [3.9 统一契约](#39-统一契约--common-模块)
- [四、它们之间怎么配合](#四它们之间怎么配合)
- [五、请求穿行实录](#五请求穿行实录)
- [六、概念速查表](#六概念速查表)
- [七、容易混淆的几组概念](#七容易混淆的几组概念)

---

## 一、先看全景：这个项目在做什么

### 1.1 业务

一个电影票务系统。用户能做的事：

```
注册 → 登录 → 浏览电影 → 选场次 → 看座位图 → 锁座 → 下单 → 支付 → 查订单
```

看起来是个普通 CRUD 系统。**但这里面藏着一个真正的技术问题**：

> 两个用户同时点了同一个座位，怎么保证不会都锁成功？

这一个问题，就撑起了整个项目的技术骨架 —— 因为要回答它，你必须同时处理并发（锁）、跨进程状态（Redis）、数据一致性（事务 + 乐观锁）。

### 1.2 服务清单

```
┌──────────────────────────────────────────────────────┐
│                    前端 / curl                        │
└──────────────────────┬───────────────────────────────┘
                       │ 只认识这一个地址
                       ▼
              ┌─────────────────┐
              │  gateway :8080  │   ← 唯一入口
              └────────┬────────┘
                       │
     ┌─────────────┬───┴────────┬──────────────┐
     ▼             ▼            ▼              ▼
┌─────────┐  ┌──────────┐  ┌───────────┐  ┌───────────┐
│  user   │  │  movie   │  │  session  │  │   order   │
│  :8081  │  │  :8082   │  │   :8083   │  │   :8084   │
│ 用户    │  │ 电影     │  │ 场次/座位  │  │  订单     │
└────┬────┘  └────┬─────┘  └─────┬─────┘  └─────┬─────┘
     │            │              │              │
     │            │              │ Feign ───────┘
     │            │              ▼
     │            │         ┌─────────┐
     │            │         │  Redis  │  分布式锁
     │            │         └─────────┘
     ▼            ▼              ▼              ▼
  db_user     db_movie      db_session     db_order
  └──────────────── MySQL :13306 ──────────────┘
                     ▲
         ┌───────────┴────────────┐
         │                        │
   ┌───────────┐          ┌──────────────┐
   │  Nacos    │          │  XXL-Job     │
   │  :8848    │          │   Admin      │
   │ 注册+配置 │          │    :8088     │
   └───────────┘          └──────────────┘
         ▲
   ┌───────────┐
   │ Sentinel  │
   │   :8089   │  熔断限流
   └───────────┘
```

### 1.3 技术栈

| 层 | 选型 | 为什么是它 |
|---|---|---|
| 语言 | Java 21 | 虚拟线程、Record 等新特性 |
| 框架 | Spring Boot 3.x | 自动配置，起步快 |
| 微服务套件 | Spring Cloud Alibaba 2023.x | 阿里系，Nacos/Sentinel 生态完整 |
| 注册+配置 | Nacos 2.3.x | 一个组件干两件事，少维护一个中间件 |
| 数据访问 | MyBatis-Plus | `BaseMapper` 免写基础 SQL |
| 缓存/锁 | Redis 7.x | 分布式锁的事实标准 |
| 定时任务 | XXL-Job 2.4.x | 有控制台，多实例不重复跑 |
| 部署 | Docker Compose | 一键起 10 个容器 |

---

## 二、为什么要拆成微服务

### 2.1 先看单体的问题

假设所有功能写在一个 Spring Boot 应用里：

```
movie-ticketing-monolith.jar
├── UserController / UserService
├── MovieController / MovieService
├── SessionController / SessionService
├── OrderController / OrderService
└── 连一个库 movie_ticketing
```

**能跑，而且更简单。** 那为什么要拆？单体真正的问题是三个：

**① 无法独立扩容**

`查电影列表` 是读多写少，需要 10 个实例；`下单` 是写重操作，2 个实例就够。单体的话，你只能整体扩容 —— 为了扛住电影列表的流量，被迫把下单也复制 10 份，浪费资源。

**② 改一处，全量重发**

改个电影列表的排序规则，整个应用重新打包、重新部署。期间**所有功能都停服**。改一行代码要让下单功能陪葬。

**③ 边界会烂掉**

单体里，`OrderService` 直接 `new UserMapper()` 就能拿到用户数据。今天查个用户名，明天直接 JOIN 用户表拿手机号，后天在订单表里加个 `user_nickname` 冗余字段。**半年后没人说得清哪个模块依赖哪个模块。**

### 2.2 微服务付了什么代价

**这是本文最重要的一段。** 微服务不是免费的，你为上面三个好处付出的代价是：

| 单体里本来很简单的事 | 微服务里变成了 | 本项目用什么解决 |
|---|---|---|
| 方法调用 `userService.getById(1)` | 网络调用，会超时、会失败 | **Feign** + 降级 |
| 配置文件在 `application.yml` | 改配置要重启 4 个服务 | **Nacos 配置中心** |
| 服务地址写 `localhost:8081` | 容器 IP 会变，写死了就挂 | **Nacos 注册中心** |
| 前端调 4 个地址 | 跨域、鉴权重复 4 遍 | **Gateway** |
| `synchronized` 锁住座位 | 锁只在单个 JVM 内有效 | **Redis 分布式锁** |
| `@Scheduled` 定时扫订单 | 4 个实例各跑一遍，重复执行 | **XXL-Job** |
| 一个 `@Transactional` 搞定 | 跨服务没有事务了 | **状态机 + 补偿** |

**所以：中间件不是"高级"，是"补丁"。** 每一个中间件都在补一个「因为拆开了所以坏掉」的东西。

理解了这点，你就能理解为什么本项目有 5 个中间件 —— 它们不是炫技，是拆分的账单。

### 2.3 拆分的依据

本项目的切法 —— **按业务能力切，不是按技术层切**：

```
✅ 按业务能力（本项目）
   user-service    —— 用户相关的一切
   movie-service   —— 电影相关的一切
   session-service —— 场次和座位
   order-service   —— 订单

❌ 按技术层（错误示范）
   controller-service —— 所有 Controller
   service-service    —— 所有 Service
   dao-service        —— 所有 Mapper
```

按技术层切的话，改一个功能要同时动 3 个服务、发 3 次版 —— 比单体还惨。

### 2.4 顺带一个产物：数据库也分了

```
db_user / db_movie / db_session / db_order
```

**一个服务一个库**。这不是为了性能，是为了**边界固化**。

如果 order-service 能直接 `JOIN db_movie.movie`，那它俩就在数据库层面耦死了 —— movie 表改个列名，order 就编译不过。分库之后，order 想拿电影数据**只能走 movie-service 的 HTTP 接口**，边界是物理强制的。

> 这也是为什么 `order_item` 表要冗余存 `seatCode` —— 宁可存一份，也不跨库查 session。

---

## 三、九个核心概念

下面每个概念按同一个套路讲：**没有它会怎样 → 它怎么解决 → 在本项目里怎么体现**。

---

### 3.1 服务注册与发现 —— Nacos 的第一重身份

#### 没有它会怎样

微服务之间要互相调用，就得知道对方的地址。最直接的做法是写死：

```yaml
# order-service 的配置
user-service:
  url: http://192.168.1.100:8081
```

**单体时代这没问题。但在 Docker 里，这个 IP 是会变的。**

容器每次重建，Docker 分配的 IP 都可能不同。而且如果 user-service 扩到 3 个实例，你写死一个 IP 就等于只用了一个实例。

#### 它怎么解决

引入一个「通讯录」：

```
user-service 启动
   └─► 向 Nacos 报到：「我是 user-service，我在 172.18.0.5:8081」
   └─► 之后每 ~10 秒发一次心跳：「我还活着」

order-service 要调 user-service
   └─► 问 Nacos：「user-service 有哪些健康实例？」
   └─► Nacos 答：「172.18.0.5:8081、172.18.0.6:8081」（3 秒没心跳的已被剔除）
   └─► order-service 挑一个发请求
```

**核心价值：地址从「写死在代码里」变成「运行时动态查询」。**

带来的三个直接好处：

1. **容器重建 IP 变了？** 无所谓，重新注册一下，调用方自动拿到新地址
2. **扩容到 3 个实例？** 调用方自动看到 3 个，轮询分发 —— 这就是**负载均衡**
3. **某个实例挂了？** 心跳超时被剔除，流量自动避开 —— 这就是**故障转移**

#### 在本项目里

`bootstrap.yml` 里声明「我要注册到哪」：

```yaml
spring:
  application:
    name: user-service        # ← 这就是「通讯录」里的名字
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_ADDR:nacos}:8848
        namespace: movie-ticketing
```

调用的地方用 `lb://`（LoadBalancer）而不是 `http://`：

```java
// ❌ 写死地址：服务挂了不知道，也无法负载均衡
.uri("http://localhost:8081")

// ✅ 从 Nacos 发现所有健康实例，自动负载均衡
.uri("lb://user-service")
```

> **`lb://` 就是「去 Nacos 查这个名字，然后帮我挑一个实例」的意思。**

#### 概念延伸：服务发现有两种做法

| | 客户端发现（Nacos 模式） | 服务端发现（K8s 模式） |
|---|---|---|
| 谁负责挑实例 | 调用方（Spring Cloud LoadBalancer） | 基础设施（K8s Service + DNS） |
| 应用需要感知注册中心吗 | 需要，要引依赖、要配地址 | 不需要，解析域名就行 |
| 语言绑定 | 通常绑定 Spring Cloud 生态 | 与语言无关 |

本项目的 Phase 9（K8s）会用后者 —— 那时候 Nacos 的注册发现角色可以被 K8s DNS 替代，但**配置中心的角色替代不了**。

---

### 3.2 配置中心 —— Nacos 的第二重身份

#### 没有它会怎样

假设 user-service 要连数据库，配置写在 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/db_user
```

现在要改连接池大小。你需要：

1. 改文件
2. 重新打包
3. 重新构建镜像
4. 重启容器

改一个数字，走一遍完整发布流程。

更麻烦的是**共享配置**。`jwt.secret` 这个密钥，user-service 用它签发 token，gateway 用它验签 —— 两边必须一致。如果各自写在本地文件里，改密钥时你要记住改几个地方，漏一个就是线上事故。

#### 它怎么解决

把配置从「代码的一部分」变成「运行时可读的外部数据」：

```
改配置
  → 在 Nacos 控制台改一下，点发布
  → 服务收到推送，@RefreshScope 的 Bean 自动重新加载
  → 无需重启
```

#### 在本项目里

**两层配置结构**：

| 类型 | data-id | 放什么 | 为什么 |
|---|---|---|---|
| 共享配置 | `common.yml` | `jwt.secret` 这种所有服务都要的 | **改一次，全生效**，不会漏改 |
| 专属配置 | `user-service.yml` 等 | 该服务自己的 datasource | 各服务数据库不同 |

通过 `shared-configs` 引入共享配置：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_ADDR:nacos}:8848
        namespace: movie-ticketing
        file-extension: yml
      shared-configs:
        - data-id: common.yml       # ← 引共享配置
          refresh: true             # ← 支持热更新
```

#### 必须理解的一点：`bootstrap.yml` 和 `application.yml` 的区别

这是初学者最容易卡住的地方。

```
应用启动
   │
   ├─ ① 加载 bootstrap.yml  ← 由「父上下文」加载，优先执行
   │      内容：Nacos 地址、命名空间、要拉哪些 data-id
   │      作用：先去 Nacos 把远程配置拉下来
   │
   ├─ ② 远程配置生效（common.yml + user-service.yml）
   │
   └─ ③ 加载 application.yml ← 本地配置，可覆盖远程
          │
          └─ ④ 创建 Bean，此时 @Value("${jwt.secret}") 才能取到值
```

**为什么不能都用 `application.yml`**：如果 Nacos 地址写在 `application.yml` 里，那加载 `application.yml` 时 Bean 已经开始创建了，而配置还没从 Nacos 拉下来 —— `@Value("${jwt.secret}")` 会注入失败。

`bootstrap.yml` 由 Spring Cloud 的**父上下文**先加载，就是为了插队到 Bean 创建之前。

#### 一个容易踩的坑：配置内容不在 Git 里

```
仓库里有的：  bootstrap.yml（只有「去哪儿拿」）
仓库里没有的：Nacos 上的 common.yml / user-service.yml 的实际内容（「拿到了什么」）
```

所以排查问题时**光看代码不够**，得开 `http://localhost:8848/nacos` 看配置到底在不在。

> 类似这样的运行期事实，本项目已经攒了一些（比如 Sentinel 镜像内部端口是 8858 而不是 8089，Nacos 命名空间是 `movie-ticketing` 而不是默认的 `public`）。这类"代码里看不出来"的知识最容易反复踩，建议单独记一份。

---

### 3.3 API 网关 —— Spring Cloud Gateway

#### 没有它会怎样

前端直接调 4 个服务：

```
前端 → user-service:8081     （注册/登录）
前端 → movie-service:8082    （查电影）
前端 → session-service:8083  （查场次/锁座）
前端 → order-service:8084    （下单/支付）
```

四个问题：

1. **前端要知道所有服务的地址** —— 后端加个服务，前端要跟着改
2. **跨域** —— 4 个不同端口 = 4 个不同源，CORS 白名单要配 4 份
3. **鉴权重复** —— 每个服务都要写一套 JWT 解析代码，容易写得不一致
4. **无法统一治理** —— 限流、日志、灰度都得在每个服务里各实现一遍

#### 它怎么解决

加一个「总机」：

```
前端 → Gateway:8080 ──┬──► user-service:8081
                      ├──► movie-service:8082
                      ├──► session-service:8083
                      └──► order-service:8084
```

前端只认识 8080。网关按**路径前缀**分发：

| 路径 | 转发到 |
|---|---|
| `/api/user/**` | user-service |
| `/api/movie/**` | movie-service |
| `/api/session/**` | session-service |
| `/api/order/**` | order-service |

#### 在本项目里：鉴权是网关最重要的职责

**核心设计决策：JWT 只在网关解析一次。**

```
请求带 Authorization: Bearer eyJhbGci...
        │
        ▼
Gateway AuthFilter（全局过滤器）
        │
        ├── 路径在白名单？ ──► 直接放行
        │   （/api/user/register、/api/user/login、
        │     /api/movie/list、/api/movie/category/list）
        │
        └── 不在白名单
              ├── 验签 JWT
              │     ├── 有效 ──► 注入 X-User-Id / X-Username ──► 放行
              │     └── 无效 ──► 直接返回 401（请求根本到不了下游）
              │
              ▼
        下游服务 Controller
        @RequestHeader("X-User-Id") Long userId    ← 只管取值
```

**为什么这么设计**：

| 方案 | 优点 | 缺点 |
|---|---|---|
| 每个服务自己解析 JWT | 网关挂了不影响 | 解析代码重复 4 遍；改密钥要改 4 处；容易漏配 |
| **网关统一解析** ✅ | 只写一次；下游完全解耦 | 网关是单点（用多实例 + Nginx 解决） |

下游服务拿到的是 `X-User-Id` 这个请求头，**完全不知道 JWT 长什么样、密钥是什么**。

#### 三个必须知道的实现细节

**① `GlobalFilter` vs `GatewayFilter`**

- `GatewayFilter`：要在每条路由里手动声明 `filters: - AuthFilter`，加新路由容易漏配
- `GlobalFilter`：对所有路由**自动生效**，不用逐条配 ✅

**② 网关是 WebFlux，不是 Servlet**

Spring Cloud Gateway 基于 **WebFlux 响应式栈**（非阻塞 IO），整个请求链是 `Mono`/`Flux`。

**后果**：`@RestControllerAdvice` 写的 `GlobalExceptionHandler` 在网关里**不生效**（那是 Servlet 体系的）。所以网关里的错误响应要**手动构造字节流**返回，不能靠注解兜底。

**这也意味着网关的 `pom.xml` 里不能引 `spring-boot-starter-web`** —— 会和 WebFlux 冲突。

**③ 白名单用 `startsWith` 而不是 `equals`**

```java
// ❌ equals：/api/movie/list?page=1 匹配不上
path.equals("/api/movie/list")

// ✅ startsWith：忽略查询参数
path.startsWith("/api/movie/list")
```

前端请求普遍带分页/筛选参数，用 `equals` 会因为 `?` 后面那截匹配失败。

---

### 3.4 服务间调用 —— OpenFeign

#### 没有它会怎样

order-service 要查用户是否存在。用最原始的 `RestTemplate`：

```java
String url = "http://user-service/api/user/" + userId;   // 地址哪来的？
Result<Map> result = restTemplate.getForObject(url, Result.class);
// 还要考虑：超时怎么办？序列化？连接池？服务发现？
```

每个调用点都要写这一套。而且 URL 拼错是运行时才发现。

#### 它怎么解决

**声明式**：写一个接口 + 注解，像调本地方法一样调远程服务。

```java
@FeignClient(name = "user-service")     // ← 服务名，不是 URL
public interface UserFeignClient {
    @GetMapping("/api/user/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);
}
```

调用时：

```java
Result<User> r = userFeignClient.getUserById(1L);   // 看起来就是普通方法调用
```

#### 背后发生了什么

Feign 的核心是**动态代理**：

```
启动时
  Spring 扫描 @FeignClient 接口
  → 为每个接口生成一个代理实现类，注册成 Bean

调用 userFeignClient.getUserById(1L) 时
  → 代理拦截
  → 从 Nacos 查 user-service 的实例列表
  → 用 LoadBalancer 挑一个：172.18.0.5:8081
  → 拼 URL：http://172.18.0.5:8081/api/user/1
  → 发 HTTP 请求
  → 把 JSON 响应反序列化成 Result<User>
  → 返回
```

**这一切对调用方透明。** 所以你看到 `@FeignClient(name = "user-service")` 里的 `name` 是**服务名**而不是 URL —— 因为地址是运行时从 Nacos 查的。

#### 在本项目里的调用链

`order-service` 是最复杂的，它要编排另外两个服务：

```
下单：
  ① Feign → user-service     验证用户存在
  ② Feign → session-service  查场次票价
  ③ 本地   INSERT orders + order_item
  ④ Feign → session-service  确认座位 LOCKED → SOLD
```

```
                    order-service
                         │
        ┌────────────────┼────────────────┐
        │ Feign          │                │ Feign
        ▼                ▼                ▼
   user-service     本地数据库      session-service
```

#### 一个细节：`contextId`

如果一个服务要调同一个目标服务的多个接口组，会有多个 `@FeignClient(name = "user-service")` —— 这时 Feign 会报 Bean 名冲突。解法是加 `contextId` 区分：

```java
@FeignClient(name = "user-service", contextId = "userBasicClient")
```

本项目目前每个目标服务只有一个客户端，不会冲突，但**显式写 `contextId` 是好习惯**。

---

### 3.5 服务容错 —— Sentinel 与降级

#### 没有它会怎样：雪崩

```
session-service 挂了 / 变慢
  → order-service 调它超时，处理线程被卡住等超时
    → order-service 的线程池被占满
      → order-service 自己也变慢 / 不可用
        → Gateway 调 order-service 超时
          → Gateway 线程被占满
            → 整个系统不可用
```

**一个服务挂了，拖死整条链。** 这叫**雪崩**。

#### 它怎么解决：快速失败

核心思路：**与其等着超时，不如立刻放弃。**

```
session-service 连续失败超过阈值
  → Sentinel 打开断路器
    → 后续请求直接走降级逻辑，不再真的去调 session-service
      → 省掉每次等待超时的时间，order-service 自己活得好好的
        → 15 秒后进入「半开」，放一个请求试探
          ├── 成功 → 关闭断路器，恢复正常
          └── 失败 → 继续打开
```

#### 熔断器的三种状态

```
        CLOSED（正常通行）
           │
           │ 失败率达到阈值
           ▼
        OPEN（熔断，全部走降级）
           │
           │ 等待 N 秒
           ▼
      HALF-OPEN（半开试探）
        ├── 试探成功 ──► CLOSED
        └── 试探失败 ──► OPEN
```

#### 两种规则

| 规则 | 触发条件 | 适用场景 |
|---|---|---|
| **熔断（Circuit Breaker）** | 慢调用比例 / 异常比例 / 异常数超阈值 | 下游**已经坏了**，别再打它 |
| **限流（Flow Control）** | QPS 或并发线程数超阈值 | 保护**自己**不被流量打垮 |

本项目里 `session-service` 的锁座接口是瓶颈（Redis + DB 双层操作），适合限 QPS；`order-service` 的 Feign 调用适合异常比例熔断。

#### 在本项目里：降级用 Fallback 类实现

```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
```

`fallback` 类实现同一个接口，返回一个「友好的错误」：

```java
@Component
public class UserClientFallback implements UserFeignClient {
    @Override
    public Result<User> getUserById(Long id) {
        log.warn("UserFeignClient 降级: getUserById({})", id);
        return Result.error(500, "用户服务暂不可用，请稍后重试");
    }
}
```

**为什么需要降级**：没有它，Feign 异常会冒泡到 Controller → 全局异常处理器 → 前端收到 500「服务器内部错误」。前端只知道"炸了"，**不知道是哪个服务炸的**，也没法给用户一个有意义的提示。

有了降级，前端收到「用户服务暂不可用」—— 用户知道该等一会儿再试。

> ⚠️ **一个容易漏的点**：fallback 类必须是个被 Spring 管理的 Bean（标 `@Component`）。因为 Feign 构造客户端时要**从容器里取**这个 fallback 实例，取不到会直接启动失败（`NoSuchBeanDefinitionException`）。

#### 规则存哪：为什么还要配 Nacos 数据源

Sentinel 默认把规则存在**内存**里 —— 服务一重启，规则全丢，得重新在控制台配一遍。

解法是把规则持久化到 Nacos：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        ds1:
          nacos:
            server-addr: ${NACOS_ADDR:nacos}:8848
            data-id: order-service-sentinel
            group-id: DEFAULT_GROUP
            data-type: json
            rule-type: flow
```

这样服务启动时自动从 Nacos 读规则，重启不丢。**这就是为什么 pom 里除了 `sentinel` starter 还要额外引 `sentinel-datasource-nacos`。**

---

### 3.6 分布式锁 —— Redis 与乐观锁的双层防御

**这是全项目技术含量最高的一节。**

#### 没有它会怎样：超卖

用户 A 和用户 B 同时看到「3 排 5 座」可选，同时点击锁定：

```
时间  用户A                          用户B
────────────────────────────────────────────────────
T1    读到 seat.status = 0（可用）
T2                                  读到 seat.status = 0（可用）   ← 脏读！
T3    UPDATE seat SET status=1
      WHERE id = 5
T4                                  UPDATE seat SET status=1
                                      WHERE id = 5    ← 也成功了！
────────────────────────────────────────────────────
结果：两个人同时锁到了同一个座位
```

**根因**：「先查后改」这个操作**不是原子的**。T1 和 T2 之间，A 的判断依据已经过期了。

#### 单机时代怎么解决

```java
synchronized (this) {
    // 查座位 → 判断 → 改状态
}
```

**但微服务里这招完全失效** —— 因为：

```
session-service 实例1（JVM A）  ← synchronized 锁的是 JVM A 的对象锁
session-service 实例2（JVM B）  ← 完全不受 JVM A 的锁影响
```

`synchronized` 只在**单个 JVM 内**有效。多实例部署时，两个请求可能落在不同实例上，各自的锁互不相干。

**这就是「分布式锁」要解决的问题：把锁的作用域从「一个 JVM」提升到「整个集群」。**

#### 第一层：Redis 分布式锁

利用 Redis 的 `SETNX`（SET if Not eXists）命令 —— **设置成功说明没人占，设置失败说明被占了**：

```
请求 A: SET lock:seat:5 userId=A NX EX 30   → 成功（拿到锁）
请求 B: SET lock:seat:5 userId=B NX EX 30   → 失败（座位正被处理）
```

`NX` = 不存在才设置（原子操作），`EX 30` = 30 秒后自动过期。

**`EX` 为什么必须有**：如果 A 拿到锁之后进程崩了，没设过期时间的话，这把锁**永远不会释放** —— 座位 5 就永久锁死了。过期时间是兜底。

**为什么不用 Redisson**：

| | SETNX | Redisson |
|---|---|---|
| 复杂度 | 一行命令 | 引依赖、配置 |
| 自动续期 | ❌ 要手动设过期 | ✅ WatchDog |
| 可重入 | ❌ | ✅ |
| 适用 | 锁持有时间**毫秒级** | 锁持有时间**秒级以上** |

锁座操作是毫秒级的（几条 SQL + 一次 Redis 命令），30 秒过期绰绰有余，**不需要 Redisson 那套复杂度**。

#### 第二层：数据库乐观锁

只用 Redis 够吗？**不够** —— Redis 挂了怎么办？锁全丢，又回到超卖。

所以加第二层兜底，用**数据库的乐观锁**：

```sql
UPDATE seat
SET status = 1, version = version + 1
WHERE id = 5
  AND status = 0        -- 还没被锁
  AND version = 3       -- 版本号必须匹配（我刚读到的版本）
```

**原理**：`version` 字段每次成功更新都 +1。如果在我「读」和「写」之间有人改过，`version` 就变了，`WHERE` 不成立，`affected rows = 0` → 说明冲突了。

#### 为什么需要两层

| 方案 | 问题 |
|---|---|
| 只用 Redis | Redis 挂了 → 锁全丢 → 超卖 |
| 只用 DB 乐观锁 | 高并发下大量 UPDATE 冲突 + 重试，数据库压力大、响应慢 |
| **两层结合** ✅ | Redis 挡掉 99% 的并发（快速失败，不碰数据库），DB 保证数据**绝对正确**（兜底） |

**一句话**：Redis 是**性能层**（快），DB 是**正确性层**（准）。性能层可以失效，正确性层不能。

#### 一个非常容易忽略的坑：Redis 不在事务里

```
@Transactional 开始
  │
  ├─ Redis SETNX            ← 不在事务范围内！
  ├─ DB UPDATE（失败）
  │    └─ 事务回滚 ──► DB 回滚了
  │
  └─ 但 Redis 的锁还在！  ← 要手动删
```

**Spring 事务只管数据库连接，管不了 Redis。** 事务回滚不会把 Redis 的锁也回滚掉。

如果不手动 `redisTemplate.delete(lockKey)`，那个座位的锁会**一直持有到 30 秒过期** —— 这 30 秒内其他人都无法操作那个座位。

这就是为什么锁座的代码里要有 `try-catch` + 手动清理 Redis 的逻辑。

#### 还有第三个问题：防超卖

座位锁住了，但场次的「剩余座位数」也要同步扣减。这个**不能先查后改**：

```sql
-- ❌ 先查后改（并发下会算错）
SELECT available_seats FROM session WHERE id = 1;   -- A 读到 10
SELECT available_seats FROM session WHERE id = 1;   -- B 也读到 10
UPDATE session SET available_seats = 9 WHERE id = 1;  -- A 写 9
UPDATE session SET available_seats = 9 WHERE id = 1;  -- B 也写 9
-- 实际应该减 2，结果只减了 1
```

```sql
-- ✅ 原子更新（MySQL 行锁保证）
UPDATE session
SET available_seats = available_seats - 1
WHERE id = 1
  AND available_seats - 1 >= 0      -- 防超卖：扣到负数就失败
```

**关键在 `available_seats = available_seats - 1`**：把「读」和「写」合并成一条 SQL，MySQL 的行锁保证它原子执行。

`AND available_seats - 1 >= 0` 是第二道保险 —— 确保不会扣成负数。

---

### 3.7 分布式定时任务 —— XXL-Job

#### 没有它会怎样

业务需求：用户下单后 15 分钟不支付，座位要自动释放。

用 Spring 自带的 `@Scheduled`：

```java
@Scheduled(cron = "0 * * * * ?")   // 每分钟
public void releaseExpiredOrders() { ... }
```

单实例没问题。**但 order-service 部署 3 个实例呢？**

```
实例1  ─┐
实例2  ─┼─► 三个实例各跑一遍 → 同一订单被取消 3 次、座位被释放 3 次
实例3  ─┘
```

**任务是重复执行的。** 而且 Cron 表达式写在代码里，改频率要重新部署。

#### 它怎么解决

把「什么时候跑」和「谁来跑」分离到独立的调度中心：

```
┌─────────────────────┐
│  XXL-Job Admin      │  调度中心：管 Cron、管路由、存执行日志
│  :8088              │
└──────────┬──────────┘
           │ 每分钟触发一次
           ▼
┌─────────────────────┐
│  order-service      │  执行器：注册自己，接收调度，执行任务
│  @XxlJob 方法       │
└─────────────────────┘
```

对比：

| | `@Scheduled` | XXL-Job |
|---|---|---|
| Cron 配置 | 写在代码里，改要重新部署 | **控制台动态改，实时生效** |
| 多实例 | **每个实例都跑一遍** | 路由策略保证**只跑一次** |
| 执行日志 | 混在应用日志里 | 独立的任务历史，可追溯 |
| 失败重试 | 自己实现 | 内置 |
| 分片 | 不支持 | 支持（大数据量并发处理） |

#### 在本项目里

任务流程：

```
XXL-Job Admin 每分钟触发
  │
  ▼
OrderTimeoutJob.handle()
  │ ① SELECT * FROM orders WHERE status = 0 AND expire_time < NOW()
  │
  │ ② 逐条处理：
  │      ├─ 查 order_item 拿到 seatIds
  │      ├─ Feign → session-service 释放座位
  │      └─ UPDATE orders SET status = 2（已取消）
  │
  └─ ③ 打日志：共 N 条，成功 M 条
```

**为什么必须逐条处理，不能批量 UPDATE**：

```sql
-- 批量 UPDATE 只能改本地库
UPDATE orders SET status = 2 WHERE status = 0 AND expire_time < NOW();
-- 问题：座位的释放要调 session-service，这一步没法批量化！
```

订单状态在 `db_order`（本地），座位状态在 `db_session`（远程）。**批量 SQL 跨不了服务**，只能一条一条来。

**每条订单独立 try-catch** —— 一条失败不影响其他订单，避免"一条坏数据卡死整个任务"。

#### 执行器需要一个 Bean

XXL-Job 的执行器需要注册一个 `XxlJobSpringExecutor` Bean，它负责：

1. 启动一个内嵌 HTTP 服务（默认 9999 端口），接收调度中心的触发请求
2. 向 Admin 注册自己（appname = 服务名）
3. 把 `@XxlJob("xxx")` 注解的方法注册成可调度的任务

**没有这个 Bean，`@XxlJob` 注解就是纯装饰** —— 注解在，但没人认识它，任务永远不会被触发。这是一个"不报错但完全不工作"的坑。

---

### 3.8 跨服务数据一致性

#### 问题：下单要写两个库

下单这个动作，实际涉及：

```
① INSERT orders + order_item      → db_order（本地）
② Feign 调 session-service 确认售出 → db_session（远程）
```

**本地事务管不了远程调用。**

```java
@Transactional              // ← 只管 db_order
public Orders create(...) {
    orderMapper.insert(order);              // 本地，能回滚
    sessionFeignClient.confirmSeats(...);   // 远程，回滚不了！
}
```

如果第 ② 步失败了，第 ① 步的订单**已经提交了** —— 但因为第 ② 步抛异常，本地事务会回滚…… 然后座位那边**可能已经确认售出了**，就出现了「订单没了，座位却是 SOLD」的脏状态。

#### 本项目的做法：状态机 + 补偿

**不追求强一致，而是让状态可追踪、可修复。**

订单状态流转：

```
PENDING(0) ──支付──► PAID(1)
    │                   │
    └──超时/取消──► CANCELLED(2)
                        │
                 PAID(1) ──退款──► REFUNDED(3)
```

配合一个**定时补偿**：XXL-Job 每隔一段时间扫「停留太久的状态」，发现异常就修正。

这是微服务里很常见的取舍 —— **最终一致性**而不是强一致性。

#### 配套概念：幂等

因为有了重试和补偿，**同一个操作可能被执行多次**（比如定时任务重跑、Feign 自动重试）。所以每个操作都要**幂等** —— 执行一次和执行多次，结果一样。

本项目里的幂等靠 **SQL 的条件更新**实现：

```sql
-- 只有 status=0 的订单才能被取消
UPDATE orders SET status = 2 WHERE id = ? AND status = 0
```

第一次执行：`status` 是 0 → 更新成功，`affected rows = 1`
第二次执行：`status` 已经是 2 → `WHERE` 不成立，`affected rows = 0` → **什么都不做**

这就是为什么这些 UPDATE 语句里都要带 `AND status = 0` —— **不只是防重复，更是防脏写**。

---

### 3.9 统一契约 —— common 模块

#### 没有它会怎样

每个服务各写各的响应格式：

```json
// user-service 返回
{ "success": true, "result": {...} }

// movie-service 返回
{ "code": 0, "data": {...} }

// session-service 返回
{ "errno": 200, "payload": {...} }
```

Feign 调用方**每个接口都要写一套解析逻辑**，前端也要判断三种格式。而且 user-service 抛异常时返回 Spring 默认的错误页，movie-service 返回自定义格式 —— **调用方根本没法统一处理**。

#### 它怎么解决

`common` 模块定义全项目共享的契约：

```
common/
├── result/Result<T>              # 统一响应体 { code, message, data }
├── exception/BizCodeEnum         # 错误码枚举（按模块分段）
├── exception/BizException        # 业务异常
├── exception/GlobalExceptionHandler  # 全局异常兜底
├── auth/JwtUtil                  # JWT 签发/解析
└── config/JacksonConfig          # 日期序列化格式
```

**注意 `common` 不是一个服务**，它是一个被所有服务依赖的 **jar 包**，不对应任何容器。

#### `Result<T>`：统一响应体

```java
public class Result<T> {
    private int code;        // 200=成功，其他=异常
    private String message;
    private T data;
}
```

`<T>` 泛型让每种接口返回自己的类型（`Result<User>`、`Result<List<Movie>>`）。

#### `BizException` + `GlobalExceptionHandler`：关注点分离

Service 层遇到业务规则不满足，**只管抛**：

```java
if (seat.getStatus() != 0) {
    throw new BizException(BizCodeEnum.SEAT_LOCKED);
}
```

**不用管怎么变成 HTTP 响应**。`GlobalExceptionHandler` 统一拦截：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());   // 3002 "座位已被锁定"
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("系统异常", e);                            // 打印完整堆栈
        return Result.error(500, "服务器内部错误");           // 但不对前端暴露
    }
}
```

**两个处理器的分工很关键**：

| | 业务异常 `BizException` | 未知异常 `Exception` |
|---|---|---|
| 日志级别 | `warn`（预期内的） | `error` + 完整堆栈 |
| 返回给前端 | **真实的 code 和 message** | 统一的「服务器内部错误」 |
| 为什么 | 前端需要知道"座位被锁了"才能提示用户 | **不暴露内部细节**（堆栈、SQL 都可能泄露） |

**`@RestControllerAdvice` 的原理**是 Spring MVC 的 AOP —— 它给所有 Controller 织入异常拦截。所以**只有在 Servlet 体系的服务里生效**，网关（WebFlux）用不了。

#### 一个容易忽略的配置

`common` 的包是 `com.movieticket.common`，各服务的包是 `com.movieticket.user` 之类 —— **不在同一个包路径下**。

所以**每个启动类都必须写**：

```java
@SpringBootApplication(scanBasePackages = "com.movieticket")
```

不写这行，Spring 只扫 `com.movieticket.user`，扫不到 `com.movieticket.common` 下的 `GlobalExceptionHandler`，异常就不会被兜底。

---

## 四、它们之间怎么配合

### 4.1 依赖关系

```
                     ┌──────────────┐
                     │   Gateway    │
                     │ 路由 + 鉴权   │
                     └──────┬───────┘
                            │ 依赖 Nacos 做服务发现
                            │ 依赖 JwtUtil 做验签
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                 ▼
    ┌──────────┐      ┌──────────┐      ┌──────────┐
    │  user    │      │  movie   │      │ session  │◄──── Feign ────┐
    └────┬─────┘      └────┬─────┘      └────┬─────┘                │
         │                 │                 │                      │
         │  全部依赖 Nacos 做注册 + 配置      │                      │
         │                 │                 │                ┌─────┴────┐
         ▼                 ▼                 ▼                │  order   │
      db_user          db_movie         db_session            └────┬─────┘
                                            ▲                     │
                                       ┌────┴────┐                │
                                       │  Redis  │ 分布式锁       │
                                       └─────────┘                │
                                                                  ▼
                                                            db_order
                                                            + XXL-Job
                                                            + Sentinel
```

### 4.2 谁依赖谁：一张表说清

| 组件 | 被谁依赖 | 依赖什么 | 挂了会怎样 |
|---|---|---|---|
| **Nacos** | 所有服务 | — | 全站瘫痪（服务找不到彼此，配置拉不到） |
| **MySQL** | 4 个业务服务 | — | 对应服务不可用 |
| **Redis** | session-service | — | 锁座降级为只靠 DB 乐观锁（仍能正确，但性能下降） |
| **Gateway** | 前端 | Nacos、JwtUtil | 外部无法访问（内部服务间 Feign 调用不受影响） |
| **XXL-Job Admin** | order-service | MySQL | 超时订单不会被自动释放 |
| **Sentinel** | session/order | — | 熔断规则不再动态生效（已有规则仍在内存里跑） |
| **common** | 所有服务 | — | 编译期就挂了（是 jar 不是服务） |

### 4.3 三个「跨」

理解这个项目，本质是理解三个跨越：

| 跨什么 | 为什么需要跨 | 用什么跨 |
|---|---|---|
| **跨容器** | 10 个进程在不同容器里 | Docker 网络 + **服务名**（`mysql:3306`） |
| **跨服务** | 业务被切成了 4 份 | **Gateway**（外部）/ **Feign**（内部） |
| **跨进程存状态** | 用户的登录态、座位的锁状态，不能存在单个 JVM 内存里 | **JWT**（无状态身份）、**Redis**（分布式锁） |

**第三点最容易被忽视。** 单机时代，用户登录后把 session 存在 `HttpSession` 里就行。多实例之后，用户第一次请求落在实例 A（session 存在 A 的内存），第二次落在实例 B（B 内存里没有）→ **要求用户重新登录**。

两个解法：

- **JWT**（本项目用）：身份信息**编码在 token 里**，服务端不存 —— 谁拿到都能验签。代价是**无法主动让 token 失效**
- **Redis 共享 session**：所有实例共读一份 Redis。代价是多一次网络调用

---

## 五、请求穿行实录

理论讲完，看四个真实请求怎么穿过这些层。**建议对照 `README.md` 的接口定义一起看。**

### 5.1 场景一：注册（免鉴权 + 写数据库）

```
① curl -X POST "http://localhost:8080/api/user/register?username=t1&password=123456"
        │
        ▼
② Docker 网络：宿主 8080 → mt-gateway:8080
        │
        ▼
③ Gateway AuthFilter
   ├── path = /api/user/register
   ├── 在白名单里？→ 是 → 直接放行，不检查 JWT
   │
   ▼
④ Gateway 路由：/api/user/** → lb://user-service
   └── 问 Nacos：user-service 在哪？→ 172.18.0.5:8081
        │
        ▼
⑤ user-service :: UserController.register()
        │
        ▼
⑥ UserServiceImpl.register()
   ├── 查用户名是否已存在 → 存在则 throw BizException(USERNAME_EXISTS)
   ├── MD5 加密密码
   └── INSERT INTO user ...
        │
        ▼
⑦ MySQL :13306（db_user 库）
        │
        ▼
⑧ Result.success(user)
   └── 原路返回
```

**这条链路用到了**：Docker 网络、Gateway（白名单放行）、Nacos（服务发现）、Controller→Service→Mapper 分层、`Result` 统一响应。

### 5.2 场景二：登录（签发 JWT）

和注册几乎一样，区别在第 ⑥ 步：

```
⑥ UserServiceImpl.login()
   ├── 查用户 → 不存在则 throw BizException(PASSWORD_ERROR)
   ├── MD5(输入的密码) 和库里的密文比对 → 不匹配则抛异常
   ├── 检查 status 是否被禁用
   └── JwtUtil.generateToken(userId, username, secret)
         │
         ├── subject = userId         ← 注意：userId 放在 sub 字段
         ├── claim("username", ...)
         ├── expiration = now + 7天
         └── signWith(密钥)           ← 用 HMAC-SHA 签名
        │
        ▼
返回 Result.success(token)
```

**前端拿到 token 后存起来**，后续请求带上：

```
Authorization: Bearer eyJhbGciOi...
```

> ⚠️ **这里的「签发」和网关的「解析」必须严格对齐** —— userId 存在哪个字段（`sub` 还是自定义 claim）、用哪个密钥签名，两边必须一致。这是最容易出错、且**报错信息很误导**的地方：字段对不上会表现为「所有接口都 401」，而不是"字段错"。

### 5.3 场景三：锁座（鉴权 + Redis + 双层锁）

**这是最复杂的一条链路。**

```
① curl -X POST -H "Authorization: Bearer eyJ..." \
     "http://localhost:8080/api/session/lock?seatIds=5&sessionId=1"
        │
        ▼
② Gateway AuthFilter
   ├── /api/session/lock 不在白名单
   ├── 取 Authorization 头 → 剥掉 "Bearer "
   ├── JwtUtil.parseToken(token, secret) 验签
   │     ├── 成功 → 拿到 userId
   │     └── 失败 → 返回 401，请求到此为止
   └── 注入 X-User-Id: 1 → 放行
        │
        ▼
③ Gateway 路由：/api/session/** → lb://session-service
        │
        ▼
④ session-service :: SessionController.lock()
   @RequestHeader("X-User-Id") Long userId    ← 只管取值，不碰 JWT
        │
        ▼
⑤ SessionServiceImpl.lockSeats()   @Transactional
   │
   ├── 第 1 层：Redis SETNX
   │     SET lock:seat:5 userId=1 NX EX 30
   │     ├── 失败 → throw BizException(SEAT_LOCKED)「座位已被锁定」
   │     └── 成功 → 继续
   │
   ├── 第 2 层：DB 乐观锁
   │     UPDATE seat SET status=1, version=version+1
   │     WHERE id=5 AND status=0 AND version=3
   │     ├── affected rows = 0 → 冲突 → 回滚 + **手动删 Redis 锁**
   │     └── affected rows = 1 → 继续
   │
   └── 第 3 步：扣减剩余座位（原子更新，防超卖）
         UPDATE session SET available_seats = available_seats - 1
         WHERE id=1 AND available_seats - 1 >= 0
        │
        ▼
⑥ Result.success(null)
```

**三个关键点**：

1. **JWT 解析只在 ② 做了一次** —— ⑤ 只管从 `X-User-Id` 取值
2. **Redis 锁不在事务范围里** —— DB 回滚时 Redis 锁要**手动删**，否则会锁定 30 秒
3. **扣减座位用原子 SQL** —— 不是「查出来减 1 再写回」

### 5.4 场景四：下单（跨服务编排）

order-service 要调另外两个服务 —— **这是 Feign 的实战**。

```
① POST /api/order/create  {sessionId:1, movieId:1, seatIds:[5]}
        │
        ▼
② Gateway（同场景三，解析 JWT → X-User-Id:1）
        │
        ▼
③ order-service :: OrderController.create()
        │
        ▼
④ OrderServiceImpl.create()   ← 跨服务编排
   │
   ├── ① Feign → user-service     验证用户存在
   │      userFeignClient.getUserById(1)
   │      └── 内部：查 Nacos 拿地址 → 发 HTTP → 反序列化
   │
   ├── ② Feign → session-service  查场次（拿票价）
   │      sessionFeignClient.getSessionById(1)
   │
   ├── ③ 本地：算总价 = 票价 × 座位数
   │      INSERT orders      （orderNo、status=0 PENDING、
   │                           expireTime = now + 15min）
   │      INSERT order_item  （每个座位一条）
   │      -- ①②③ 在一个本地事务里
   │
   └── ④ Feign → session-service  确认座位 LOCKED → SOLD
        │
        ▼
⑤ Result.success(order)
```

**注意 ③ 和 ④ 之间的那道鸿沟**：

```
@Transactional 的范围  ├────────────────┤
                      ①  ②  ③           ④
                                         ↑
                              这一步在事务之外
                              （远程调用回滚不了）
```

如果 ④ 失败了，③ 的订单会被回滚 —— 但 session 那边可能已经改了状态。**这就是 3.8 讲的「跨服务没有事务」**，靠状态机 + 定时补偿来兜。

---

## 六、概念速查表

| 概念 | 解决什么问题 | 本项目用什么 | 关键配置/代码位置 |
|---|---|---|---|
| 服务注册发现 | 容器 IP 会变，写死地址就挂 | Nacos Discovery | `bootstrap.yml` + `lb://` |
| 配置中心 | 改配置要重新部署；共享配置易漏改 | Nacos Config | `bootstrap.yml` + `shared-configs` |
| API 网关 | 前端要知道 N 个地址；鉴权重复 | Spring Cloud Gateway | `GatewayRouteConfig` / `AuthFilter` |
| 服务调用 | 手写 HTTP 调用繁琐 | OpenFeign + LoadBalancer | `@FeignClient(name="...")` |
| 熔断降级 | 一个服务挂了拖死整条链 | Sentinel + fallback 类 | `@FeignClient(fallback=...)` |
| 限流 | 流量尖峰打垮服务 | Sentinel | Nacos 里的规则配置 |
| 分布式锁 | synchronized 只在单 JVM 有效 | Redis SETNX | `SET lock:seat:{id} NX EX 30` |
| 乐观锁 | 并发下「先查后改」会脏写 | DB `version` 字段 | `AND version = ?` |
| 防超卖 | 剩余座位数被并发写错 | 原子 UPDATE | `available_seats = available_seats - 1` |
| 分布式定时任务 | `@Scheduled` 多实例重复执行 | XXL-Job | `@XxlJob("...")` + `XxlJobSpringExecutor` |
| 幂等 | 重试/补偿导致重复执行 | SQL 条件更新 | `AND status = 0` |
| 统一契约 | 各服务响应格式不一 | `common` 模块 | `Result<T>` / `BizException` |
| 无状态身份 | HttpSession 在多实例间不共享 | JWT | `JwtUtil` |

### 数字速查

| 端口 | 服务 | 备注 |
|---|---|---|
| 8080 | Gateway | 外部唯一入口 |
| 8081-8084 | user / movie / session / order | 直连调试用 |
| 13306 | MySQL | 宿主端口，容器内是 3306 |
| 16379 | Redis | 宿主端口，容器内是 6379 |
| 8848 / 9848 | Nacos | 控制台 / gRPC |
| 8088 | XXL-Job Admin | 调度中心 |
| 8089 | Sentinel Dashboard | ⚠️ 镜像内部端口是 8858 |

---

## 七、容易混淆的几组概念

### 7.1 Nacos 的注册中心 vs 配置中心

| | 注册中心 | 配置中心 |
|---|---|---|
| 存在的数据 | 「谁在哪」—— 服务实例列表 | 「配置是什么」—— YAML 文本 |
| 变化频率 | 高（实例重启、扩缩容） | 低（改参数时） |
| 消费方式 | 每次调用前查询（本地有缓存） | 启动时拉取 + 变更时推送 |
| 对应的 starter | `nacos-discovery` | `nacos-config` |

**同一个 Nacos 服务，两套独立的数据。** 所以「Nacos 挂了」要分情况：配置拉不到 → 服务起不来；注册查不到 → 服务起得来但互相调不通。

### 7.2 Feign vs RestTemplate

| | RestTemplate | Feign |
|---|---|---|
| 写法 | 手写 URL 拼接 + 序列化 | 声明接口 + 注解 |
| 服务发现 | 自己集成 | 内置（`name` 就是服务名） |
| 错误处理 | 每个调用点自己写 | 统一 fallback |
| 本质 | HTTP 客户端 | **HTTP 客户端的声明式封装**（底层也可以是 RestTemplate） |

### 7.3 熔断（Sentinel） vs 降级（Fallback）

容易当成一回事，其实是**触发条件**和**触发后做什么**的关系：

```
Sentinel 判断「下游是不是坏了」
    └── 坏了 → 打开断路器
              └── 后续请求不再调下游，改走 → Fallback 逻辑
```

- **熔断**是**决策**：判定服务不可用
- **降级**是**动作**：不可用时返回什么

配套的还有**限流**（保护自己不被打垮）和**超时**（别无限等）。

### 7.4 Nacos 服务发现 vs K8s 服务发现

| | Nacos | K8s |
|---|---|---|
| 层面 | 应用层（Spring Cloud 体系内） | 基础设施层（所有语言通用） |
| 发现方式 | SDK 查注册表 | DNS 解析 |
| 配置管理 | ✅ 有配置中心，支持热更新 | ⚠️ ConfigMap 改完通常要重启 Pod |
| 适用 | Spring Cloud 应用 | 任何容器化应用 |

**结论**：迁到 K8s 后，服务发现的角色可以被 K8s Service 替代，但**配置中心的角色替代不了** —— 这就是本项目 Phase 9 的架构决策里「保留 Nacos」的原因。

### 7.5 本地事务 vs 分布式事务

| | 本地事务 | 分布式事务 |
|---|---|---|
| 范围 | 单个数据库连接 | 跨多个服务/库 |
| 实现 | `@Transactional` | 2PC / TCC / Saga / 消息最终一致 |
| 本项目 | order + order_item 的写入 | 订单状态 + 座位状态的最终一致（**状态机 + 定时补偿**） |

**注意**：`@Transactional` 只保护**同一个数据源**的操作。注解包住 Feign 调用是**没有意义的** —— 远程服务不会被回滚。

### 7.6 日志里的 `warn` vs `error`

不是随便选的，本项目有明确约定：

- **`warn`**：预期内的业务异常（座位被抢、密码错误）—— 不是 bug，是正常业务流的一部分
- **`error`**：系统异常（空指针、数据库连不上）—— 需要排查，要打完整堆栈

`GlobalExceptionHandler` 就是按这个分的：

```java
@ExceptionHandler(BizException.class)
public Result<?> handleBiz(BizException e) {
    log.warn(...);      // ← 业务异常，warn
}

@ExceptionHandler(Exception.class)
public Result<?> handleOther(Exception e) {
    log.error("系统异常", e);   // ← 未知异常，error + 堆栈
}
```

---

## 附：与其它文档的分工

| 文档 | 回答什么 |
|---|---|
| `README.md` | **是什么** —— 技术选型、接口定义、表结构、配置项、路线图 |
| `PREREQUISITES.md`（本文） | **为什么** —— 每个概念为什么存在、解决什么问题、彼此怎么配合 |
| `PROGRESS.md` | **做到哪了** —— 各 Phase 进度、实现细节、待修复清单 |

读到具体配置项看不懂时，回 `README.md` 查；读实现代码卡住时，回 `PROGRESS.md` 查。
