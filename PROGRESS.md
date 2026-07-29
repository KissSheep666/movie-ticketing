# 项目进度

> 最后更新: 2026-07-29

---

## 一、当前进度

- [x] **Phase 1：基础设施 + 公共模块** ✅ 6 个文件，编译通过
- [x] **Phase 2：user-service 注册/登录** ✅ 6 个文件，编译通过
- [x] **Phase 3：movie-service 电影列表/详情** ✅ 8 个文件，编译通过
- [x] **Phase 4：Gateway 路由/JWT 鉴权** ✅ 3 个文件，编译通过
- [x] **Phase 5：session-service 场次/锁座** ✅ 9 个文件，编译通过
- [x] **Phase 6：order-service 下单/XXL-Job** ✅ 10 个文件，编译通过
- [x] **Phase 7：Sentinel 熔断降级/联调** ✅ 降级类 + 依赖配置，编译通过

---

## 二、已完成工作（环境 + 骨架）

### 2.1 环境验证（2026-07-22）

| 组件             | 版本             | 状态     |
| -------------- | -------------- | ------ |
| Java           | 21.0.11 LTS    | ✅      |
| Maven          | 3.9.15         | ✅      |
| Docker         | 29.5.2         | ✅      |
| Docker Compose | v5.1.4         | ✅      |
| 磁盘             | 711GB 可用       | ✅      |
| 内存             | 16 GB          | ✅      |
| Clash 代理       | 127.0.0.1:7890 | ✅ 外网可达 |

### 2.2 现有文件清单

```
movie-ticketing/
├── pom.xml ✅                              # 父 POM，统一版本管理
├── common/
│   ├── pom.xml ✅
│   └── src/                    ← 空，待写 Java 代码
├── gateway/
│   ├── pom.xml ✅
│   ├── Dockerfile ✅
│   └── src/main/resources/bootstrap.yml ✅
│       (Java 代码待写)
├── user-service/
│   ├── pom.xml ✅
│   ├── Dockerfile ✅
│   └── src/main/resources/bootstrap.yml ✅
├── movie-service/
│   ├── pom.xml ✅
│   ├── Dockerfile ✅
│   └── src/main/resources/bootstrap.yml ✅
├── session-service/
│   ├── pom.xml ✅
│   ├── Dockerfile ✅
│   └── src/main/resources/bootstrap.yml ✅
├── order-service/
│   ├── pom.xml ✅
│   ├── Dockerfile ✅
│   └── src/main/resources/bootstrap.yml ✅
├── sql/
│   ├── db_user.sql ✅         # 用户表 + 3 条预置数据
│   ├── db_movie.sql ✅         # 分类表 + 电影表 + 20 部电影
│   ├── db_session.sql ✅       # 场次表 + 座位表 + 5 场次 + 400 座位
│   └── db_order.sql ✅         # 订单表 + 订单明细表
└── docker-compose.yml ✅       # 9 容器编排
```

> ⚠️ **0 个 Java 文件，代码一行没写**。下面所有文件都是待创建的。

### 2.3 基础设施状态（已启动并验证）

```bash
docker compose up -d mysql redis nacos
```

| 容器       | 端口    | 状态        |
| -------- | ----- | --------- |
| mt-mysql | 13306 | ✅ healthy |
| mt-redis | 16379 | ✅ healthy |
| mt-nacos | 8848  | ✅ healthy |

- Nacos 控制台：`http://localhost:8848/nacos` ✅ 可访问
- 4 个数据库 `db_user` `db_movie` `db_session` `db_order` ✅ 已创建

### 2.4 已决定的事项

- 所有中间件走 Docker（MySQL、Redis、Nacos、XXL-Job）
- Java 服务 JVM 限制 `-Xms64m -Xmx128m`
  - 端口 3306/6379 被本机占用 → compose 改用 13306/16379
- 外网访问走 Clash 代理 `127.0.0.1:7890`

---

## 三、Phase 1：公共模块 common（6 个文件）

> **目标**：写出 `common` 模块的所有 Java 类，编译通过。
> 全部在 `common/src/main/java/com/movieticket/common/` 目录下。
> 
> Common模块
> 
> 定义统一响应体
> 
> 错误码枚举
> 
> 业务异常类
> 
> 全局错误捕获 实现错误 -> 业务类异常的转化
> 
> JWT工具类 后期登录的校验

---

### 3.1 Result.java — 统一响应体

> **路径**：`common/src/main/java/com/movieticket/common/result/Result.java`
> 
> **作用**：封装所有接口返回值，前端和 Feign 调用方拿到统一格式 `{ code, message, data }`。
> 提供两个静态工厂：`Result.ok(data)` 成功、`Result.fail(code, msg)` 失败。

```java
package com.movieticket.common.result;

import lombok.Data;

/**
 * 统一响应体，所有接口返回值都用这个包装。
 * 前端/Feign 调用方只认一种格式：{ code, message, data }
 *
 * @param <T> 实际数据类型（比如 User、List<Movie>、String token）
 */
@Data
public class Result<T> {
    private int code;       // 业务状态码，200=成功，其他=异常（400/401/500…）
    private String message; // 提示信息，成功时默认 "success"
    private T data;         // 实际返回数据，失败时为 null

    /**
     * 成功响应（不需要返回数据时传 null）
     * 用法：Result.ok(user) / Result.ok(null)
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    /**
     * 失败响应，code 和 message 由调用方指定
     * 用法：Result.fail(400, "用户名已存在")
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

> **关键点**：
> 
> - 泛型 `<T>` 让每种接口返回自己的数据类型
> - `ok()` 的 code 固定 200
> - 后续 GlobalExceptionHandler 会调用 `fail()` 来包装异常

---

### 3.2 BizCodeEnum.java — 错误码枚举

> **路径**：`common/src/main/java/com/movieticket/common/exception/BizCodeEnum.java`
> 
> **作用**：集中管理所有业务错误码和消息，按模块分段。
> 抛异常时传枚举项，不再手写魔法数字和硬编码字符串。

```java
package com.movieticket.common.exception;

import lombok.Getter;

/**
 * 业务错误码枚举，按模块号段分段：
 *   1xxx — 用户模块（注册/登录/信息）
 *   2xxx — 电影模块（列表/详情/分类）
 *   3xxx — 场次模块（场次/座位）
 *   4xxx — 订单模块（下单/支付/取消）
 *   9xxx — 通用错误（参数/权限）
 */
@Getter
public enum BizCodeEnum {

    // ==================== 通用（9xxx）====================
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ==================== 用户模块（1xxx）====================
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),

    // ==================== 电影模块（2xxx）====================
    MOVIE_NOT_FOUND(2001, "电影不存在"),
    CATEGORY_NOT_FOUND(2002, "分类不存在"),

    // ==================== 场次模块（3xxx）====================
    SESSION_NOT_FOUND(3001, "场次不存在"),
    SEAT_LOCKED(3002, "座位已被锁定"),
    SEAT_SOLD(3003, "座位已售出"),

    // ==================== 订单模块（4xxx）====================
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_TIMEOUT(4002, "订单已超时"),
    ORDER_CANNOT_CANCEL(4003, "订单不可取消"),

    ;

    private final int code;     // 错误码，透传给 Result.fail()
    private final String message; // 固定消息，日志用

    BizCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

> **关键点**：
> 
> - 枚举值 = 错误码 + 固定消息，所有错误一目了然
> - 按号段分区：`1xxx` 用户 / `2xxx` 电影 / `3xxx` 场次 / `4xxx` 订单 / `9xxx` 通用
> - 后续新增错误码只需加一行枚举，不需要改 BizException
> - 如果某个场景需要带动态信息（比如"座位 X 已锁定"），通过日志补充：`log.warn("锁座失败: seatId={}", seatId)`

---

### 3.3 BizException.java — 业务异常

> **路径**：`common/src/main/java/com/movieticket/common/exception/BizException.java`
> 
> **作用**：接收 BizCodeEnum 枚举项，也保留手动传 code+msg 的兜底构造。
> Service 层统一用法：`throw new BizException(BizCodeEnum.USERNAME_EXISTS)`。

```java
package com.movieticket.common.exception;

import lombok.Getter;

/**
 * 业务异常，Service 层遇到规则不满足直接 throw。
 * 由 GlobalExceptionHandler 统一拦截 → 转成 Result.fail() 返回前端。
 *
 * 推荐用法（枚举）：
 *   throw new BizException(BizCodeEnum.USERNAME_EXISTS);
 *   throw new BizException(BizCodeEnum.SEAT_LOCKED);
 *
 * 兜底用法（手动传参）：
 *   throw new BizException("用户名已存在");              // code 默认 400
 *   throw new BizException(401, "请先登录");             // 自定义 code
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;     // 错误码，GlobalExceptionHandler 通过 getter 读取

    /** 枚举构造：从 BizCodeEnum 里取 code 和 message（推荐） */
    public BizException(BizCodeEnum bizCode) {
        super(bizCode.getMessage());      // 枚举的 message 作为异常消息
        this.code = bizCode.getCode();    // 枚举的 code 作为错误码
    }

    /** 快捷构造：只传消息，code 默认 400（Bad Request），临时场景兜底 */
    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    /** 完整构造：传错误码 + 消息，特殊场景兜底 */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

> **关键点**：
> 
> - 枚举构造器是主入口，新增错误场景先考虑加枚举项
> - 保留 String/Int 构造器作为兜底，避免临时场景找不到合适枚举时被迫加枚举
> - `GlobalExceptionHandler` 不变，仍然通过 `e.getCode()` 和 `e.getMessage()` 取值

---

### 3.4 GlobalExceptionHandler.java — 全局异常处理

> **路径**：`common/src/main/java/com/movieticket/common/exception/GlobalExceptionHandler.java`
> 
> **作用**：Spring MVC 切面，拦截 Controller 抛出的异常，转为统一的 `Result.fail()` 返回。
> 拦截两类：`BizException`（业务异常）和 `Exception`（未知异常兜底）。
> 
> 项目需求
> 
> 1,app端 和 网页端 添加 功能 一个物料可以拍多个 照片
> 
> 2,优化下app端的ui 更加适配项目
> 
> 3,优化app端ui排布 增加 手动刷新 物料重拍 等更加符合逻辑的操作

```java
package com.movieticket.common.exception;

import com.movieticket.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，Spring MVC AOP 切面。
 * 拦截所有 Controller 抛出的异常 → 转成 Result.fail() 统一格式返回。
 *
 * 执行流程：
 *   Controller → Service（throw BizException）→ 本类 handleBiz() → 前端收到 { code:400, message:"..." }
 *   Controller → Service（throw NullPointerException）→ 本类 handleOther() → 前端收到 { code:500, message:"服务器内部错误" }
 */
@Slf4j
@RestControllerAdvice   // = @ControllerAdvice + @ResponseBody，对所有 @RestController 生效
public class GlobalExceptionHandler {

    /** 业务异常 → 直接取出 code 和 message 透传给前端 */
    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 未知异常兜底 → 打印完整堆栈便于排查，返回 500 不暴露内部错误 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("系统异常", e);  // 第二个参数是 Throwable，会打印完整堆栈
        return Result.fail(500, "服务器内部错误");
    }
}
```

> **关键点**：
> 
> - `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`
> - `BizException` 处理方法拿 e.code 和 e.message 透传
> - 未知异常返回 500，不暴露堆栈给前端
> - 注意：不要让 common 模块扫描到子模块的包，子模块通过 `@SpringBootApplication(scanBasePackages = "com.movieticket")` 扫描 common

---

### 3.5 JwtUtil.java — JWT 工具类

> **路径**：`common/src/main/java/com/movieticket/common/auth/JwtUtil.java`
> 
> **作用**：签发 JWT（登录时调用）、验证 JWT（Gateway 鉴权时调用）、解析 userId（下游服务用）。
> 依赖 `io.jsonwebtoken:jjwt-api`（common 的 pom.xml 已引入）。

```java
package com.movieticket.common.auth;

import io.jsonwebtoken.*;                    // JJWT 核心接口
import io.jsonwebtoken.security.Keys;        // 密钥工具

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类，纯静态方法，不需要 Spring 管理。
 * 调用链：login → JwtUtil.generate() → 返回 token 字符串
 *        Gateway filter → JwtUtil.parse() → 拿到 userId → 放到 X-User-Id 头
 */
public class JwtUtil {

    /**
     * 签发 Token，把 userId 和 username 写进 payload。
     * @param userId   用户 ID，存到 sub（subject）字段
     * @param username 用户名，存到自定义 claim
     * @param secret   签名密钥（从 Nacos 配置中心读取，不硬编码）
     * @return JWT 字符串，三段 Base64 用点拼接：header.payload.signature
     */
    public static String generate(Long userId, String username, String secret) {
        // 密钥 → HMAC-SHA 算法的 Key 对象
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))    // sub 字段 = userId（字符串）
                .claim("username", username)        // 自定义字段，后续 parse 可取出
                .issuedAt(new Date())               // iat 签发时间
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)) // exp 过期时间（7天）
                .signWith(key)                      // 用 HMAC-SHA 签名
                .compact();                         // 生成最终的 JWT 字符串
    }

    /**
     * 验证并解析 Token。
     * @return JWT 的 Claims（payload 的键值对）
     * @throws JwtException 签名不对/过期/格式错误都会抛这个异常（或其子类）
     */
    public static Claims parse(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)               // 设置验签密钥
                .build()                       // 构建解析器
                .parseSignedClaims(token)      // 解析并验签 → Jws<Claims>
                .getPayload();                 // 拿到 payload（Claims 即 Map<String, Object>）
    }

    /** 从 Claims 中取 userId（sub 字段转 Long） */
    public static Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** 从 Claims 中取 username（自定义 claim） */
    public static String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }
}
```

> **关键点**：
> 
> - `secret` 从 Nacos 配置中心读取，不硬编码
> - `generate()` 的三个调用方：userId、username 来自登录查库结果，secret 来自 `@Value`
> - `parse()` 校验失败会抛 `JwtException`，Gateway filter 里 catch 后返回 401
> - 过期时间 7 天，生产环境建议缩短到 2 小时 + refresh token

---

### 3.6 JacksonConfig.java — Jackson 序列化配置

> **路径**：`common/src/main/java/com/movieticket/common/config/JacksonConfig.java`
> 
> **作用**：统一 JSON 格式：日期返回 `yyyy-MM-dd HH:mm:ss`、时区 Asia/Shanghai、Long 不转 String。

```java
package com.movieticket.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;        // Java 8 时间类型支持
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化配置，统一 JSON 输出格式。
 * 不配这个的话 LocalDateTime 默认序列化成 [2026,7,22,10,30,0]，前端没法用。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        // 注册 LocalDateTime 的序列化器：按 "yyyy-MM-dd HH:mm:ss" 格式输出
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.registerModule(module);
        return mapper;
    }
}
```

> **关键点**：
> 
> - `JavaTimeModule` 处理 Java 8 时间类型（`LocalDateTime` 等）
> - 不加这个配置的话，`LocalDateTime` 默认序列化成数组 `[2026,7,22,10,30,0]`，前端没法用

---

### 3.7 Nacos 配置检查清单

在 Nacos 控制台 `http://localhost:8848/nacos` 确认：

| 配置 ID              | Group         | 内容                                                             | 状态  |
| ------------------ | ------------- | -------------------------------------------------------------- | --- |
| `common.yml`       | DEFAULT_GROUP | `jwt.secret: movie-ticketing-secret-must-be-256-bits-long-!!!` | ✅   |
| `user-service.yml` | DEFAULT_GROUP | datasource 连 `mysql:3306/db_user`                              | ✅   |

> Phase 1 只需读 `common.yml` 中的 `jwt.secret`，各服务的 datasource 配置到 Phase 2+ 才用。

---

### 3.8 Phase 1 验证步骤

```bash
# 编译 common 模块
cd D:/codeSpace/movie-ticketing
mvn clean compile -pl common

# 如果编译通过，继续编译全项目
mvn clean compile

# 预期结果：BUILD SUCCESS，0 个 Java 文件报错
```

---

## 四、Phase 2：user-service 详细代码

> 你要创建 5 个文件，目录已建好，直接在里面新建即可。

### 4.0 前置准备：Nacos 配置 ✅

Nacos 控制台 `http://localhost:8848/nacos`，已创建 2 个配置：

**common.yml**（GROUP: DEFAULT_GROUP）

```yaml
jwt:
  secret: movie-ticketing-secret-must-be-256-bits-long-!!!
```

**user-service.yml**（GROUP: DEFAULT_GROUP）

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/db_user?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

### 4.1 User.java — 实体类

> **路径**：`user-service/src/main/java/com/movieticket/user/entity/User.java`
> 
> **作用**：映射 `db_user` 库的 `user` 表，一个字段对应一个列。
> MyBatis-Plus 的 `@TableName` 指定表名，`@TableId` 标记主键自增。

```java
package com.movieticket.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体，映射 db_user 库的 user 表。
 * 一个字段对应一个列，MyBatis-Plus 自动做驼峰→下划线转换（createTime → create_time）。
 */
@Data                        // Lombok：生成 getter/setter/toString/equals/hashCode
@TableName("user")           // 指定数据库表名（不写则默认类名下划线 → user 表）
public class User {

    @TableId(type = IdType.AUTO)  // 主键，数据库自增（MyBatis-Plus 插入时不需要手动设 ID）
    private Long id;

    private String username;      // 用户名，唯一索引（SQL 里建了 UNIQUE KEY）
    private String password;      // 密码，存储 MD5 密文，永远不返回给前端
    private String nickname;      // 昵称，注册时不传则默认用 username
    private String phone;         // 手机号，选填
    private String email;         // 邮箱，选填
    private Integer status;       // 0=禁用，1=正常

    @TableField(fill = FieldFill.INSERT)        // 插入时自动填充（配合 MetaObjectHandler）
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时自动填充
    private LocalDateTime updateTime;
}
```

> **注意**：Lombok 依赖 `common` 模块已通过 `spring-boot-starter-web` 间接引入，不需要额外添加。
> 如果 IDEA 报 `@Data` 找不到，检查 Lombok 插件是否安装：`File → Settings → Plugins → 搜 Lombok`。

---

### 4.2 UserMapper.java — 数据访问层

> **路径**：`user-service/src/main/java/com/movieticket/user/mapper/UserMapper.java`
> 
> **作用**：继承 MyBatis-Plus 的 `BaseMapper<User>`，自动获得 CRUD 方法。
> 不需要写 SQL，`selectById`、`selectOne`、`insert`、`updateById` 等方法全有了。

```java
package com.movieticket.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper，继承 BaseMapper<User> 自动获得 CRUD：
 *   selectById(id)         → 按主键查
 *   selectOne(wrapper)     → 按条件查一条（LambdaQueryWrapper 做 WHERE）
 *   selectList(wrapper)    → 按条件查多条
 *   selectCount(wrapper)   → COUNT 查询
 *   insert(user)           → 插入
 *   updateById(user)       → 按主键更新
 *   deleteById(id)         → 按主键删除
 *
 * 不需要写 XML，复杂查询用 LambdaQueryWrapper 链式调用。
 */
@Mapper  // Spring 会扫描到并生成代理实现类，注入到 Service 里
public interface UserMapper extends BaseMapper<User> {
    // 如果有 BaseMapper 覆盖不了的需求，这里加方法 → 配 XML 或用 @Select 注解
}
```

---

### 4.3 UserService.java — 业务接口

> **路径**：`user-service/src/main/java/com/movieticket/user/service/UserService.java`
> 
> **作用**：定义用户相关的 3 个业务方法。

```java
package com.movieticket.user.service;

import com.movieticket.user.entity.User;

/**
 * 用户业务接口，定义方法契约，实现类在 impl 包下。
 * 先写接口再写实现，是 Spring 惯例——方便后续 Mock 和 AOP 代理。
 */
public interface UserService {

    /**
     * 用户注册
     * @param username 用户名（必填，不可重复）
     * @param password 明文密码（Service 内部 MD5 加密后存库）
     * @param nickname 昵称（选填，不传则默认用 username）
     * @return 注册成功的用户对象（含自增 ID，不含密码）
     */
    User register(String username, String password, String nickname);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 明文密码，Service 内部 MD5 后和库里的密文比对
     * @return JWT token 字符串，前端存 localStorage，后续请求放 Authorization 头
     */
    String login(String username, String password);

    /**
     * 根据 ID 查询用户，供内部 Feign 调用和 /current 接口使用
     * @param id 用户 ID
     * @return 用户对象
     * @throws BizException 用户不存在时抛
     */
    User getById(Long id);
}
```

---

### 4.4 UserServiceImpl.java — 业务实现（关键）

> **路径**：`user-service/src/main/java/com/movieticket/user/service/impl/UserServiceImpl.java`
> 
> **作用**：注册（验重+加密+入库）、登录（验密+生成JWT）。

```java
package com.movieticket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.auth.JwtUtil;          // common 模块的 JWT 工具
import com.movieticket.common.exception.BizCodeEnum;   // 错误码枚举
import com.movieticket.common.exception.BizException;  // 业务异常
import com.movieticket.user.entity.User;
import com.movieticket.user.mapper.UserMapper;
import com.movieticket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 用户业务实现，核心三件事：注册（验重→加密→入库）、登录（查库→验密→签JWT）、查用户。
 *
 * LambdaQueryWrapper 说明：
 *   代替手写 SQL 字符串，用 Java 方法引用写 WHERE 条件。
 *   比如 wrapper.eq(User::getUsername, "admin") → WHERE username = 'admin'
 *   好处：列名改了一键重构，不会出现"手写了 username 但数据库列名已改"的 bug。
 */
@Slf4j                       // 日志门面，可以 log.info() / log.warn() / log.error()
@Service                     // 标记为 Spring Service Bean，启动时扫描并放入容器
@RequiredArgsConstructor     // Lombok：为所有 final 字段生成构造函数，Spring 自动注入
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;  // final + RequiredArgsConstructor = 构造器注入（优于 @Autowired）

    @Value("${jwt.secret}")  // 从 Nacos 配置中心读取 jwt.secret，启动时找不到会报错
    private String jwtSecret;

    /**
     * 注册逻辑：
     * ① LambdaQueryWrapper 查用户名是否已存在 → 存在则抛 BizException
     * ② MD5 加密密码（不可逆，数据库不存明文）
     * ③ 组装 User 对象并 insert → MyBatis-Plus 自动生成 INSERT SQL
     */
    @Override
    public User register(String username, String password, String nickname) {
        // 1. 查重：SELECT COUNT(*) FROM user WHERE username = ?
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);   // 方法引用代替字符串列名
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCodeEnum.USERNAME_EXISTS);  // 枚举：code=1002, msg="用户名已存在"
        }

        // 2. 加密：MD5(明文) → 32 位十六进制字符串
        //    注意：MD5 不可逆但不防彩虹表，生产环境建议 BCrypt
        String encryptedPwd = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8));

        // 3. 入库：INSERT INTO user (username, password, ...) VALUES (...)
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPwd);
        user.setNickname(nickname != null ? nickname : username); // 没传昵称默认用用户名
        user.setStatus(1);  // 1=正常，0=禁用
        userMapper.insert(user);  // MyBatis-Plus 自动回填自增 ID 到 user.id

        log.info("用户注册成功: username={}, id={}", username, user.getId());
        return user;
    }

    /**
     * 登录逻辑：
     * ① LambdaQueryWrapper 按 username 查用户 → 不存在抛异常（统一"用户名或密码错误"防撞库）
     * ② 输入密码 MD5 后和库里密文比对 → 不匹配抛异常
     * ③ 检查 status 是否为禁用
     * ④ JwtUtil.generate() 签发 token → 返回给前端
     */
    @Override
    public String login(String username, String password) {
        // 1. 查用户：SELECT * FROM user WHERE username = ?
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);  // selectOne 查询单条，多条抛异常
        if (user == null) {
            throw new BizException(BizCodeEnum.PASSWORD_ERROR);  // 枚举：code=1003, 统一模糊提示防撞库
        }

        // 2. 验密：输入明文 → MD5 → 和库里密文比对
        String encrypted = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8));
        if (!encrypted.equals(user.getPassword())) {
            throw new BizException(BizCodeEnum.PASSWORD_ERROR);
        }

        // 3. 检查账号状态
        if (user.getStatus() == 0) {
            throw new BizException(BizCodeEnum.USER_DISABLED);  // 枚举：code=1004
        }

        // 4. 签发 JWT，把 userId 和 username 写进 token payload
        String token = JwtUtil.generate(user.getId(), user.getUsername(), jwtSecret);
        log.info("用户登录成功: username={}, id={}", username, user.getId());
        return token;
    }

    /**
     * 根据 ID 查用户，查不到抛 BizException。
     */
    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);  // SELECT * FROM user WHERE id = ?
        if (user == null) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);  // 枚举：code=1001
        }
        return user;
    }
}
```

---

### 4.5 UserController.java — 控制器

> **路径**：`user-service/src/main/java/com/movieticket/user/controller/UserController.java`
> 
> **作用**：暴露 HTTP 接口，处理请求参数，调用 Service，返回统一格式 Result。

```java
package com.movieticket.user.controller;

import com.movieticket.common.result.Result;
import com.movieticket.user.entity.User;
import com.movieticket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器，暴露 RESTful 接口。
 * 职责：接收请求参数 → 调用 Service → 返回统一格式 Result。
 *
 * 接口/路径设计说明：
 *   POST   /api/user/register  注册（无状态，公开）
 *   POST   /api/user/login     登录（无状态，公开）
 *   GET    /api/user/current   当前用户（需 JWT，Gateway 透传 X-User-Id）
 *   GET    /api/user/{id}      按 ID 查询（内部 Feign 调用用）
 */
@RestController               // = @Controller + 每个方法自动转 JSON
@RequestMapping("/api/user")   // 统一前缀，所有接口路径以 /api/user 开头
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     * 请求：POST /api/user/register?username=zhangsan&password=123456&nickname=张三
     * 返回：{ code:200, message:"success", data:{ id:1, username:"zhangsan", nickname:"张三", ... } }
     *
     * 注意：返回前必须把 password 置 null，绝不在任何响应里暴露密码密文！
     */
    @PostMapping("/register")
    public Result<User> register(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam(required = false) String nickname) {
        User user = userService.register(username, password, nickname);
        user.setPassword(null);  // 安全红线：密码密文也不能返回
        return Result.ok(user);
    }

    /**
     * 用户登录
     * 请求：POST /api/user/login?username=zhangsan&password=123456
     * 返回：{ code:200, message:"success", data:"eyJhbGciOi..." }
     *
     * 前端拿到 token → localStorage.setItem("token", data)
     * 后续请求统一带请求头：Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username,
                                 @RequestParam String password) {
        String token = userService.login(username, password);
        return Result.ok(token);
    }

    /**
     * 获取当前登录用户信息
     * 请求：GET /api/user/current
     * 请求头：Authorization: Bearer <JWT>
     *
     * 重要：此接口不自己解析 JWT！
     * Gateway 已通过 AuthFilter 解析 JWT → 取出 userId → 放到 X-User-Id 请求头
     * 这里只管从请求头取值，完全信任 Gateway。
     * 这样做的好处：JWT 解析逻辑只在 Gateway 做一次，下游服务不重复解析。
     */
    @GetMapping("/current")
    public Result<User> current(@RequestHeader("X-User-Id") Long userId) {
        User user = userService.getById(userId);
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 根据 ID 查询用户（供其他微服务 Feign 内部调用）
     * 请求：GET /api/user/1
     * 调用方：order-service（下单时验用户是否存在）
     *
     * 注意：这接口不校验 JWT！认为调用方是内部服务（通过 Gateway 仍可过 AuthFilter）
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        user.setPassword(null);
        return Result.ok(user);
    }
}
```

---

### 4.6 写完后的验证步骤

```bash
# 1. 先编译整个项目
cd D:/codeSpace/movie-ticketing
mvn clean package -DskipTests

# 2. 构建 user-service 镜像
docker compose build user-service

# 3. 启动 user-service（基础设施已经在跑了）
docker compose up -d user-service

# 4. 看日志确认启动成功
docker logs -f mt-user-service

# 5. 测试注册（直接调 user-service 8081 端口，绕过 Gateway）
curl -X POST "http://localhost:8081/api/user/register?username=test&password=123456&nickname=测试用户"

# 6. 测试登录
curl -X POST "http://localhost:8081/api/user/login?username=test&password=123456"
# 返回的 data 就是 JWT token，记下来

# 7. 测试获取当前用户（手动模拟 Gateway 加的 X-User-Id 头）
curl -H "X-User-Id:1" "http://localhost:8081/api/user/current"
```

---

### 4.7 可能遇到的问题

| 问题                  | 原因                                       | 解决                                                              |
| ------------------- | ---------------------------------------- | --------------------------------------------------------------- |
| `@Data` 报红          | IDEA 没装 Lombok 插件                        | File → Settings → Plugins → 搜 Lombok → Install                  |
| 启动报"jwt.secret 找不到" | Nacos 配置没被读到                             | 检查 `bootstrap.yml` 里 Nacos 地址对不对，容器内部用 `nacos:8848`             |
| 数据库连不上              | user-service 容器内访问 MySQL 要用 `mysql:3306` | 确认 Nacos 中 `user-service.yml` 的 url 用的是 `mysql` 而不是 `localhost` |
| 注册返回"用户名已存在"        | sql 脚本预置了 3 个用户 admin/zhangsan/lisi      | 换一个没被占用的用户名                                                     |

---

### Phase 2+ 待处理

- [ ] 补充 `xxl_job` 数据库 → 启动 XXL-Job Admin
- [ ] 修复 `docker-compose.yml` 的 `version` 警告

---

## 五、Phase 3：movie-service（8 个文件已完成）

> **状态**：✅ 已编写并编译通过（2026-07-24）

### 5.1 架构层级

```
前端/Feign 调用方
  ↓ HTTP
MovieController —— 接收请求、参数校验、返回 Result
  ↓ 调用
MovieService（接口）/ MovieServiceImpl（实现）—— 业务逻辑
  ↓ 调用
MovieMapper / CategoryMapper —— MyBatis-Plus BaseMapper
  ↓ SQL
MySQL db_movie（movie 表 + category 表）
```

### 5.2 为什么这样设计

| 决策                                | 理由                                           |
| --------------------------------- | -------------------------------------------- |
| 分页用 MyBatis-Plus `Page`           | 自动做 COUNT+分页，不用手写两条 SQL，返回 total/pages 等分页信息 |
| 筛选用 `LambdaQueryWrapper`          | 方法引用写 WHERE，列名改了一键重构，不会出现"列名写错但编译通过"         |
| 分类列表不分页                           | 分类数量少（<20），一次返回即可，减少请求次数                     |
| 按评分降序 `orderByDesc(rating)`       | 用户默认看高分电影，符合浏览预期                             |
| Controller 接收 `required=false` 参数 | 首页不传筛选 → 查全部，点分类/状态后才传参数                     |
| Category 和 Movie 两个 Entity        | 一个表一个实体类，职责单一，后续扩展（比如分类加图标字段）互不影响            |

### 5.3 文件清单

```
movie-service/src/main/java/com/movieticket/movie/
├── MovieApplication.java      # Spring Boot 启动类
├── entity/
│   ├── Movie.java             # 电影实体（映射 movie 表）
│   └── Category.java          # 分类实体（映射 category 表）
├── mapper/
│   ├── MovieMapper.java       # extends BaseMapper<Movie>
│   └── CategoryMapper.java    # extends BaseMapper<Category>
├── service/
│   ├── MovieService.java      # 业务接口
│   └── impl/
│       └── MovieServiceImpl.java  # 业务实现（分页+筛选+详情）
└── controller/
    └── MovieController.java   # REST 接口（/api/movie/**）
```

### 5.4 接口说明

| 接口                                                     | 方法  | 说明                        |
| ------------------------------------------------------ | --- | ------------------------- |
| `/api/movie/list?page=1&size=10&categoryId=3&status=2` | GET | 分页列表，categoryId/status 可选 |
| `/api/movie/{id}`                                      | GET | 电影详情                      |
| `/api/movie/category/list`                             | GET | 分类列表（前端筛选栏）               |

### 5.5 详细代码

#### MovieApplication.java — 启动类

> **路径**：`movie-service/src/main/java/com/movieticket/movie/MovieApplication.java`
> 
> **作用**：Spring Boot 启动入口，`scanBasePackages = "com.movieticket"` 确保能加载 common 模块的 Bean（Result、GlobalExceptionHandler、JwtUtil 等）。

```java
package com.movieticket.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * movie-service 启动类。
 * scanBasePackages 扫到 com.movieticket 以加载 common 模块的 Bean。
 */
@SpringBootApplication(scanBasePackages = "com.movieticket")
public class MovieApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieApplication.class, args);
    }
}
```

> **关键点**：
> 
> - `scanBasePackages = "com.movieticket"` 是必须的，因为 common 模块的包是 `com.movieticket.common`，和 movie-service 的 `com.movieticket.movie` 不在同一个包路径下
> - 不配这个的话，`Result`、`GlobalExceptionHandler` 等类不会被 Spring 扫描到，接口返回会出问题

---

#### Movie.java — 电影实体

> **路径**：`movie-service/src/main/java/com/movieticket/movie/entity/Movie.java`
> 
> **作用**：映射 `db_movie` 库的 `movie` 表，一个字段对应一个列。MyBatis-Plus 自动做驼峰→下划线转换。

```java
package com.movieticket.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电影实体，映射 db_movie 库的 movie 表。
 */
@Data
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;            // 电影名称
    private Long categoryId;        // 分类ID
    private String director;        // 导演
    private String cast;            // 主演（逗号分隔）
    private Integer duration;       // 时长（分钟）
    private String posterUrl;       // 海报URL
    private String description;     // 简介
    private BigDecimal rating;      // 评分（0.0~10.0）
    private Integer status;         // 1=即将上映，2=热映中，3=已下映

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

> **关键点**：
> 
> - `@TableField(fill = FieldFill.INSERT)` 让 MyBatis-Plus 在插入时自动填充 `createTime`，配合 MetaObjectHandler 使用
> - `rating` 用 `BigDecimal` 而不是 `double`，避免浮点数精度问题
> - `status` 用 Integer 而不是 Enum，和数据库 int 值直接映射，简单直观

---

#### Category.java — 分类实体

> **路径**：`movie-service/src/main/java/com/movieticket/movie/entity/Category.java`
> 
> **作用**：映射 `db_movie` 库的 `category` 表（动作片/喜剧片/科幻片等）。

```java
package com.movieticket.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 电影分类实体，映射 db_movie 库的 category 表。
 */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;            // 分类名称（动作/喜剧/科幻...）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

> **关键点**：
> 
> - Category 和 Movie 是两个独立的 Entity，各自映射一张表，职责单一
> - 虽然 Category 字段很少，但独立出来便于后续扩展（比如加图标、排序权重等）

---

#### MovieMapper.java — 电影数据访问

> **路径**：`movie-service/src/main/java/com/movieticket/movie/mapper/MovieMapper.java`
> 
> **作用**：继承 `BaseMapper<Movie>`，自动获得 CRUD 方法。BaseMapper 提供的方法包括 `selectById`、`selectList`、`selectPage`、`insert`、`updateById`、`deleteById` 等。

```java
package com.movieticket.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.movie.entity.Movie;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MovieMapper extends BaseMapper<Movie> {
    // 复杂查询后续在此加方法
}
```

> **关键点**：
> 
> - `@Mapper` 注解让 MyBatis 扫描到并生成代理实现类
> - 不需要写任何 SQL，BaseMapper 已经覆盖了增删改查
> - 如果需要复杂查询（多表 JOIN 等），在这里加方法 + `@Select` 或 XML

---

#### CategoryMapper.java — 分类数据访问

> **路径**：`movie-service/src/main/java/com/movieticket/movie/mapper/CategoryMapper.java`

```java
package com.movieticket.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.movie.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
```

---

#### MovieService.java — 业务接口

> **路径**：`movie-service/src/main/java/com/movieticket/movie/service/MovieService.java`
> 
> **作用**：定义电影业务的 3 个方法，Controller 面向接口编程而非具体实现。

```java
package com.movieticket.movie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movieticket.movie.entity.Category;
import com.movieticket.movie.entity.Movie;

import java.util.List;

/**
 * 电影业务接口。
 */
public interface MovieService {

    /**
     * 分页查询电影列表，支持按分类筛选。
     * @param page       页码（从1开始）
     * @param size       每页条数
     * @param categoryId 分类ID（可选，null 则查全部）
     * @param status     状态筛选（可选，null 则查全部）
     * @return MyBatis-Plus Page 对象（含 total、pages、records）
     */
    Page<Movie> listByPage(int page, int size, Long categoryId, Integer status);

    /** 查询电影详情 */
    Movie getById(Long id);

    /** 查询所有分类 */
    List<Category> listCategories();
}
```

---

#### MovieServiceImpl.java — 业务实现（核心）

> **路径**：`movie-service/src/main/java/com/movieticket/movie/service/impl/MovieServiceImpl.java`
> 
> **作用**：实现分页 + 动态条件筛选 + 分类列表。

```java
package com.movieticket.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.movie.entity.Category;
import com.movieticket.movie.entity.Movie;
import com.movieticket.movie.mapper.CategoryMapper;
import com.movieticket.movie.mapper.MovieMapper;
import com.movieticket.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 电影业务实现。
 *
 * 层级关系：
 *   Controller → Service（本类）→ Mapper（MyBatis-Plus BaseMapper）
 *
 * 为什么用 LambdaQueryWrapper 而非手写 SQL：
 *   wrapper.eq(Movie::getCategoryId, 1L) → 编译器检查列名，重构时自动同步
 *   手写 "category_id = 1" → 列名改了不会报错，运行时才发现
 *
 * 为什么用 MyBatis-Plus 的 Page：
 *   selectPage(page, wrapper) → 自动拦截 SQL 做 COUNT + LIMIT，返回 total、pages 等
 *   不用自己写两条 SQL（一条查数据、一条查总数）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieMapper movieMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 分页查询电影列表。
     *
     * 为什么分页：
     *   电影可能有几百部，一次全返回 → 前端渲染慢 + 数据库压力大 + 传输带宽浪费
     *   分页只取当前页的 10~20 条，用户体验好
     *
     * 为什么用 orderByDesc(Movie::getRating)：
     *   用户打开电影列表，默认看评分最高的，符合用户预期
     *
     * @param page       页码（1-based，MyBatis-Plus 内部自动转 OFFSET）
     * @param size       每页条数
     * @param categoryId 分类ID（null = 全部分类）
     * @param status     状态筛选（null = 全部状态，常用于"仅看热映中"）
     */
    @Override
    public Page<Movie> listByPage(int page, int size, Long categoryId, Integer status) {
        // MyBatis-Plus 分页对象，构造后传给 selectPage，自动做 COUNT + LIMIT
        Page<Movie> pageParam = new Page<>(page, size);

        // 动态条件：传了才加 WHERE，不传则查全部
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Movie::getCategoryId, categoryId);  // WHERE category_id = ?
        }
        if (status != null) {
            wrapper.eq(Movie::getStatus, status);           // WHERE status = ?
        }
        wrapper.orderByDesc(Movie::getRating);  // ORDER BY rating DESC

        // selectPage：MyBatis-Plus 自动执行 COUNT 查总数 + 查当前页数据
        movieMapper.selectPage(pageParam, wrapper);
        log.info("查询电影列表: page={}, size={}, total={}", page, size, pageParam.getTotal());
        return pageParam;
    }

    /**
     * 查电影详情。查不到抛 BizException → GlobalExceptionHandler → Result.error()。
     */
    @Override
    public Movie getById(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null) {
            throw new BizException(BizCodeEnum.MOVIE_NOT_FOUND);
        }
        return movie;
    }

    /**
     * 查全部分类，用于前端筛选栏。
     * 分类数量少（通常 < 20），不需要分页，全返回。
     */
    @Override
    public List<Category> listCategories() {
        return categoryMapper.selectList(null);  // null = SELECT * FROM category
    }
}
```

> **关键点**：
> 
> - `LambdaQueryWrapper` 用方法引用构建 WHERE 条件，列名改了一键重构
> - `Page<>(page, size)` 传给 `selectPage`，MyBatis-Plus 自动执行 COUNT 并分页
> - 查不到电影时抛 `BizException`，由 `GlobalExceptionHandler` 统一转为 `Result.fail()`
> - `categoryMapper.selectList(null)` 传 null 表示无条件全查

---

#### MovieController.java — REST 接口

> **路径**：`movie-service/src/main/java/com/movieticket/movie/controller/MovieController.java`
> 
> **作用**：暴露 3 个 HTTP 接口，接收请求、调用 Service、返回 Result。

```java
package com.movieticket.movie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movieticket.common.result.Result;
import com.movieticket.movie.entity.Category;
import com.movieticket.movie.entity.Movie;
import com.movieticket.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电影控制器。
 *
 * 接口设计：
 *   GET /api/movie/list?page=1&size=10&categoryId=1&status=2   分页列表（公开）
 *   GET /api/movie/{id}                                         电影详情（公开）
 *   GET /api/movie/category/list                                分类列表（公开，前端筛选栏用）
 */
@RestController
@RequestMapping("/api/movie")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * 分页查询电影列表。
     * 请求示例：GET /api/movie/list?page=1&size=10&categoryId=3&status=2
     *
     * 为什么 categoryId / status 是 required=false：
     *   首次进页面不带筛选条件，查全部；用户点分类/状态后才传参数
     */
    @GetMapping("/list")
    public Result<Page<Movie>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) Integer status) {
        Page<Movie> result = movieService.listByPage(page, size, categoryId, status);
        return Result.success(result);
    }

    /** 电影详情 */
    @GetMapping("/{id}")
    public Result<Movie> getById(@PathVariable Long id) {
        Movie movie = movieService.getById(id);
        return Result.success(movie);
    }

    /** 分类列表，前端筛选栏"动作/喜剧/科幻..."下拉用 */
    @GetMapping("/category/list")
    public Result<List<Category>> listCategories() {
        List<Category> categories = movieService.listCategories();
        return Result.success(categories);
    }
}
```

> **关键点**：
> 
> - `@RequestParam(required = false)` 让筛选参数可选，不传则查全部
> - `defaultValue` 为分页参数设默认值，前端不传也能正常工作
> - 返回类型 `Result<Page<Movie>>`，MyBatis-Plus 的 Page 对象自带 `total`、`pages`、`records` 等字段，前端分页组件直接能用

---

## 六、Phase 4：Gateway 网关（3 个文件已完成）

> **状态**：✅ 已编写并编译通过（2026-07-24）
> **核心职责**：统一入口、路由转发、JWT 鉴权

### 6.1 为什么需要 Gateway

在没有网关之前，前端（或客户端）直接调各服务：

```
前端 → user-service:8081     （注册/登录）
前端 → movie-service:8082    （查电影）
前端 → session-service:8083  （查场次/锁座）
前端 → order-service:8084    （下单/支付）
```

**问题**：

1. **前端要知道所有服务的地址** — 每个服务端口不同，配置复杂
2. **鉴权逻辑分散** — 每个服务都要写一套 JWT 解析代码，重复且容易不一致
3. **跨域问题** — 不同端口 = 不同源，前端要配多个 CORS 白名单
4. **无法统一治理** — 限流、日志、灰度发布都得在每个服务里单独实现

**加了 Gateway 后**：

```
前端 → Gateway:8080 ──┬──► user-service:8081
                      ├──► movie-service:8082
                      ├──► session-service:8083
                      └──► order-service:8084
```

前端只和 `localhost:8080` 通信，Gateway 根据路径 `/api/user/**`、`/api/movie/**` 等转发到对应服务。鉴权在 Gateway 层一次完成，下游服务只取 `X-User-Id` 请求头。

---

### 6.2 架构层级

```
HTTP 请求
  ↓ 到达 Gateway:8080
AuthFilter（全局过滤器）
  ├── 白名单路径？ → 直接放行（注册/登录/浏览电影）
  └── 非白名单？ → 取 Authorization 头 → 解析 JWT
        ├── JWT 有效 → 注入 X-User-Id + X-Username 头 → 放行
        └── JWT 无效/过期 → 返回 401 JSON
  ↓
GatewayRouteConfig（路由规则）
  ├── /api/user/**    → lb://user-service
  ├── /api/movie/**   → lb://movie-service
  ├── /api/session/** → lb://session-service
  └── /api/order/**   → lb://order-service
  ↓
Nacos 服务发现 → 找到健康实例 → LoadBalancer 负载均衡转发
```

---

### 6.3 文件清单

```
gateway/src/main/java/com/movieticket/gateway/
├── GatewayApplication.java       # Spring Boot 启动类（@EnableDiscoveryClient）
├── config/
│   └── GatewayRouteConfig.java   # 路由规则定义（路径 → 服务名映射）
└── filter/
    └── AuthFilter.java           # 全局 JWT 鉴权过滤器
```

| 类                    | 职责            | 关键点                        |
| -------------------- | ------------- | -------------------------- |
| `GatewayApplication` | 启动类，注册到 Nacos | 加 `@EnableDiscoveryClient` |
| `GatewayRouteConfig` | 定义 4 条路由规则    | `lb://服务名` 实现负载均衡          |
| `AuthFilter`         | JWT 验签 + 写请求头 | 白名单路径不鉴权                   |

---

### 6.4 核心设计决策

#### 6.4.1 为什么用 `lb://` 而不是 `http://localhost:8081`

```java
// ❌ 写死地址：服务挂了不知道，也无法负载均衡
.uri("http://localhost:8081")

// ✅ lb:// 从 Nacos 发现所有健康实例，自动负载均衡
.uri("lb://user-service")
```

- `lb` = LoadBalancer（Spring Cloud LoadBalancer）
- Gateway 启动时从 Nacos 拉取 `user-service` 的所有实例列表（IP + 端口）
- 请求过来时轮询分发到不同实例
- 实例下线时自动剔除，不用改代码

#### 6.4.2 为什么鉴权放 Gateway 而不是每个服务

| 方案               | 优点                 | 缺点                              |
| ---------------- | ------------------ | ------------------------------- |
| 每个服务自己鉴权         | 服务独立，Gateway 挂了不影响 | JWT 解析代码重复 N 遍；改密钥要改所有服务；容易漏掉   |
| **Gateway 统一鉴权** | 一个地方写逻辑；下游服务无脑取值   | Gateway 是单点（但可通过多实例 + Nginx 解决） |

下游服务获取用户信息只需一行：

```java
@RequestHeader("X-User-Id") Long userId
```

不需要关心 JWT 怎么解析的、密钥是什么，完全解耦。

#### 6.4.3 为什么用 GlobalFilter 而不是 GatewayFilter

- **GatewayFilter**：需要手动在每条路由里配置 `filters: - AuthFilter`，容易漏配
- **GlobalFilter**：对所有路由自动生效，新加路由不需要改配置

#### 6.4.4 为什么返回 401 要手动拼 JSON 而不是抛异常

Spring Cloud Gateway 基于 **WebFlux 响应式栈**（非阻塞 IO），不是传统的 Servlet 容器。`GlobalExceptionHandler`（`@RestControllerAdvice`）是 Servlet 体系的，在 WebFlux 下不生效。所以 Gateway 里只能手动构造响应字节流。

#### 6.4.5 白名单用 `startsWith` 而不是 `equals`

```java
// ❌ equals：/api/movie/list?page=1 匹配不上
path.equals("/api/movie/list")

// ✅ startsWith：忽略查询参数
path.startsWith("/api/movie/list")
```

前端请求通常带查询参数（分页、筛选），`equals` 会因为 `?` 后面部分匹配失败。

---

### 6.5 鉴权流程详解

```
1. 请求到达 Gateway
   路径：/api/order/create
   请求头：Authorization: Bearer eyJhbGciOi...

2. AuthFilter.filter() 执行
   ├── isWhiteListed("/api/order/create") → false（下单不在白名单）
   ├── 取 "Authorization" 头 → "Bearer eyJ..."
   ├── 剥掉 "Bearer " 前缀 → token = "eyJ..."
   ├── JwtUtil.parse(token, secret)
   │   ├── 验签通过 → Claims { sub=1, username="zhangsan" }
   │   └── 验签失败 → 返回 401 "Token 无效或已过期"
   └── 写请求头：
       X-User-Id: 1
       X-Username: zhangsan

3. 路由转发
   /api/order/create → lb://order-service → order-service:8084

4. order-service 的 Controller
   @RequestHeader("X-User-Id") Long userId  ← 拿到 1
   完全不碰 JWT 解析
```

---

### 6.6 请求头透传链路

一个完整的请求链路中，用户身份是如何传递的：

```
Client
  │  Authorization: Bearer <JWT>
  ▼
Gateway AuthFilter
  │  解析 JWT → userId=1, username="zhangsan"
  │  注入: X-User-Id=1, X-Username=zhangsan
  ▼
order-service Controller
  │  @RequestHeader("X-User-Id") → 1
  │  调用 Feign 时需要把身份传给下游
  ▼
Feign RequestInterceptor（自动）
  │  从 RequestContextHolder 取当前请求的 Authorization 头
  │  注入到 Feign 请求 → 下游也能拿到用户身份
  ▼
session-service Controller
  │  Gateway 透传的 X-User-Id 也能拿到
```

---

### 6.7 验证步骤

```bash
# 1. 编译
mvn clean package -DskipTests -pl gateway

# 2. 构建 + 启动
docker compose build gateway
docker compose up -d gateway

# 3. 测试公开接口（不带 token，应正常返回）
curl -X POST "http://localhost:8080/api/user/register?username=test&password=123456"

# 4. 登录获取 token
curl -X POST "http://localhost:8080/api/user/login?username=test&password=123456"
# 记下返回的 token

# 5. 带 token 请求需要鉴权的接口
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/user/current"

# 6. 不带 token 请求鉴权接口 → 应返回 401
curl "http://localhost:8080/api/user/current"
# → {"code":401,"msg":"未登录或 Token 格式错误","data":null}
```

### 6.8 详细代码

#### GatewayApplication.java — 启动类

> **路径**：`gateway/src/main/java/com/movieticket/gateway/GatewayApplication.java`
> 
> **作用**：Gateway 的核心注意事项 —— 基于 WebFlux，不能引入 `spring-boot-starter-web`（会冲突），也不需要 `scanBasePackages` 扫 common 包（因为 common 的 `GlobalExceptionHandler` 是 Servlet 体系的，在 WebFlux 下不生效）。

```java
package com.movieticket.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关启动类。
 *
 * 为什么需要网关：
 *   微服务架构下，user-service:8081、movie-service:8082、session-service:8083、order-service:8084
 *   前端不可能记住四个地址。网关统一入口（8080），根据路径转发到对应服务。
 *
 *   前端 → http://localhost:8080/api/user/login → Gateway → user-service:8081/api/user/login
 *   前端 → http://localhost:8080/api/movie/list  → Gateway → movie-service:8082/api/movie/list
 *
 * 为什么用 Spring Cloud Gateway 而不是 Zuul：
 *   Zuul 1.x 已停维护，Zuul 2.x 生态弱。Gateway 基于 WebFlux（响应式），性能更好。
 *
 * 注意：Gateway 基于 WebFlux，不能引入 spring-boot-starter-web（会冲突）。
 *       这里不 scan common 包，因为 common 的 GlobalExceptionHandler 是 Servlet 体系的，
 *       在 WebFlux 下不生效（Gateway 需要自己的异常处理）。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

> **关键点**：
> 
> - Gateway 启动类**不配** `scanBasePackages = "com.movieticket"`，因为 common 模块的异常处理是基于 Servlet 体系的，在 WebFlux 下不兼容
> - Gateway 的 pom.xml 里不能引入 `spring-boot-starter-web`，应该用 `spring-boot-starter-webflux`（Spring Cloud Gateway 自带）

---

#### GatewayRouteConfig.java — 路由配置

> **路径**：`gateway/src/main/java/com/movieticket/gateway/config/GatewayRouteConfig.java`
> 
> **作用**：定义路径 → 服务名映射。`lb://服务名` 表示从 Nacos 发现实例并负载均衡。

```java
package com.movieticket.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway 路由配置。
 *
 * 路由逻辑（前端只和 Gateway:8080 通信）：
 *   /api/user/**    → lb://user-service     （Nacos 负载均衡 → 找 user-service 实例）
 *   /api/movie/**   → lb://movie-service
 *   /api/session/** → lb://session-service
 *   /api/order/**   → lb://order-service
 *
 * 为什么用 lb:// 而不是 http://localhost:8081：
 *   lb = LoadBalancer，从 Nacos 发现所有健康的服务实例，自动负载均衡。
 *   如果 user-service 有 3 个实例，lb://user-service 会轮询分发请求。
 *   如果用 http://localhost:8081，写死了地址，服务挂了都不知道。
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f.stripPrefix(0))  // 不剥离前缀，原样转发
                        .uri("lb://user-service"))

                // 电影服务
                .route("movie-service", r -> r
                        .path("/api/movie/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://movie-service"))

                // 场次服务
                .route("session-service", r -> r
                        .path("/api/session/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://session-service"))

                // 订单服务
                .route("order-service", r -> r
                        .path("/api/order/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://order-service"))

                .build();
    }
}
```

> **关键点**：
> 
> - `.stripPrefix(0)` 表示不剥离路径前缀，因为我们的接口路径在 Gateway 和下游服务中是一致的（都是 `/api/user/xxx`）
> - 如果想剥离前缀（比如 Gateway 收到 `/api/user/register`，下游收到 `/register`），改成 `.stripPrefix(1)` 即可
> - 路由规则按 `path()` 匹配，优先级从上到下，先匹配到先转发

---

#### AuthFilter.java — JWT 鉴权过滤器（核心）

> **路径**：`gateway/src/main/java/com/movieticket/gateway/filter/AuthFilter.java`
> 
> **作用**：全局过滤器，拦截所有请求。白名单路径放行，其他路径验证 JWT，解析成功后把 userId 和 username 写入请求头透传给下游。

```java
package com.movieticket.gateway.filter;

import com.movieticket.common.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权全局过滤器。
 *
 * 为什么鉴权放 Gateway 而不是每个服务自己做：
 *   1. 集中管理：只在一个地方写 JWT 解析逻辑，子服务不用重复代码
 *   2. 安全：外部请求必须先过 Gateway，未登录的请求到不了子服务
 *   3. 解耦：子服务只管从 X-User-Id 头取值，不用关心 JWT 怎么解析的
 *
 * 执行流程：
 *   请求 → AuthFilter → 判断是否公开路径
 *     ├── 公开路径（/api/user/register, /api/user/login）→ 直接放行
 *     └── 其他路径 → 取 Authorization 头 → 解析 JWT
 *           ├── 解析成功 → 加 X-User-Id + X-Username 头 → 放行
 *           └── 解析失败（过期/签名错/无token）→ 返回 401
 *
 * 为什么用 GlobalFilter 而不是 GatewayFilter：
 *   GlobalFilter 对所有路由生效，不需要在每个路由配置里单独声明
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /** 不需要鉴权的白名单路径 */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/register",   // 注册
            "/api/user/login",      // 登录
            "/api/movie/list",      // 电影列表（可浏览不登录）
            "/api/movie/category/list" // 分类列表
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 核心过滤逻辑。
     *
     * 为什么用 WebFlux（Mono/ServerWebExchange）而不是 Servlet（HttpServletRequest）：
     *   Spring Cloud Gateway 基于 WebFlux 响应式栈，整个请求链都是非阻塞的。
     *   Servlet 的 HttpServletRequest 是阻塞 IO，和 Gateway 不兼容。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单路径 → 直接放行，不检查 JWT
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 取 Authorization 头
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没有 Token 或格式不对 → 401
            return unauthorized(exchange, "未登录或 Token 格式错误");
        }

        // 3. 解析 JWT（剥掉 "Bearer " 前缀，拿到纯 token）
        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.parseToken(token, jwtSecret);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);

            // 4. 将 userId 和 username 写入请求头，透传给下游服务
            //    下游服务直接从 X-User-Id 取值，不用再解析 JWT
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .build();

            log.info("JWT 验证通过: userId={}, username={}, path={}", userId, username, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException | NumberFormatException e) {
            // Token 过期、签名不对、格式错误 → 401
            log.warn("JWT 验证失败: path={}, error={}", path, e.getMessage());
            return unauthorized(exchange, "Token 无效或已过期");
        }
    }

    /**
     * 判断当前路径是否在白名单中。
     * 为什么用 startsWith 而不是 equals：
     *   /api/movie/list?page=1 带查询参数，equals 匹配不上，startsWith 忽略参数
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回 401 JSON 响应。
     * 为什么不抛异常：GlobalExceptionHandler 是 Servlet 体系的，在 WebFlux 下不生效。
     * Gateway 只能手动构造响应字节流返回。
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 手动拼 JSON 返回体（不用 Jackson，避免引入额外依赖）
        String body = String.format("{\"code\":401,\"msg\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器执行顺序，值越小越先执行。
     * 设为 -100，确保在其他过滤器之前执行。
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
```

> **关键点**：
> 
> - `request.mutate().header(...)` 创建新的 request 对象并添加请求头，原始 request 是不可变的
> - `return chain.filter(exchange.mutate().request(mutatedRequest).build())` 把带新请求头的 exchange 传给下一个过滤器
> - 401 响应手动拼 JSON 字符串，因为 WebFlux 下不能依赖 Jackson 自动序列化
> - `getOrder()` 返回 -100 确保鉴权过滤器在其他过滤器之前执行

---

## 七、Phase 5：session-service 场次服务（10 个文件已完成）

> **状态**：✅ 已编写并编译通过（2026-07-24）
> **核心难点**：锁座的并发控制（Redis 分布式锁 + DB 乐观锁 双保险）

### 7.1 业务背景

电影院卖票的场景：用户 A 和用户 B 同时看到座位 "3排5座" 是可选的，同时点击锁定。

```
时间线：
T1: 用户A 读到座位 3-5 status=0（可用）
T2: 用户B 读到座位 3-5 status=0（可用）     ← 脏读！
T3: 用户A 执行 UPDATE SET status=1 WHERE id=3-5
T4: 用户B 执行 UPDATE SET status=1 WHERE id=3-5  ← 也执行成功了！
```

**结果：两个人同时"锁"到了同一个座位。** 这就是典型的并发写冲突。

---

### 7.2 解决方案：双层防御

```
请求进来
  │
  ▼
第1层：Redis 分布式锁（SETNX）
  │  快速拦截 99% 的并发冲突
  │  同一个 seatId 只能被一个人操作
  │  失败 → 直接返回"座位已被锁定"（不阻塞、不等）
  │
  ▼（获取 Redis 锁成功）
第2层：DB 乐观锁（version 字段）
  │  UPDATE ... WHERE version=? AND status=0
  │  affected rows = 0 → 冲突，回滚事务 + 释放 Redis 锁
  │  affected rows = 1 → 锁座成功
  │
  ▼
扣减场次 available_seats（原子 UPDATE）
```

#### 为什么需要两层？只用一层不行吗？

| 方案            | 问题                                     |
| ------------- | -------------------------------------- |
| **只用 Redis**  | Redis 挂了 → 锁全部丢失 → 多人同时锁到同一座位          |
| **只用 DB 乐观锁** | 高并发下大量 UPDATE 冲突 + 框架自动重试 → 数据库压力大、响应慢 |
| **两层结合**      | Redis 挡住大部分冲突（快速失败），DB 保证数据绝对正确（兜底）    |

#### 为什么用 SETNX 而不是 Redisson？

|               | SETNX       | Redisson         |
| ------------- | ----------- | ---------------- |
| 复杂度           | 一行命令        | 需要引入 Redisson 依赖 |
| WatchDog 自动续期 | ❌ 需手动设过期    | ✅                |
| 可重入锁          | ❌           | ✅                |
| 适用场景          | 锁持有时间短（毫秒级） | 锁持有时间长（秒级以上）     |

这个 Demo 的锁座操作是毫秒级，`SETNX key value EX 30` 足够。30 秒过期是兜底时间，正常流程 100ms 内完成。

---

### 7.3 架构层级

```
HTTP 请求（所有请求都经过 Gateway）
  ↓
SessionController —— 接收请求、取 X-User-Id、返回 Result
  ↓
SessionService（接口）/ SessionServiceImpl（实现）—— 业务逻辑
  ├── 锁座：Redis SETNX → DB 乐观锁 UPDATE → 扣减 available_seats
  ├── 释放：DB UPDATE status=0 + Redis DELETE + 恢复 available_seats
  └── 查询：查场次列表、查座位图
  ↓
SessionMapper / SeatMapper —— 自定义 SQL（乐观锁更新、原子扣减）
  ↓
MySQL db_session（session 表 + seat 表）
  +
Redis（分布式锁 key = lock:seat:{seatId}）
```

---

### 7.4 文件清单

```
session-service/src/main/java/com/movieticket/session/
├── SessionApplication.java              # Spring Boot 启动类
├── entity/
│   ├── Session.java                     # 场次实体（movieId、影厅、时间、票价、座位数）
│   └── Seat.java                        # 座位实体（排号、列号、status、version 乐观锁）
├── mapper/
│   ├── SessionMapper.java               # 场次 Mapper + 原子更新 available_seats
│   └── SeatMapper.java                  # 座位 Mapper + 乐观锁 lockSeat/releaseSeat/confirmSeat
├── service/
│   ├── SessionService.java              # 业务接口
│   └── impl/
│       └── SessionServiceImpl.java      # 核心：双锁锁座 + 释放 + 查询
├── controller/
│   └── SessionController.java           # REST 接口（/api/session/**）
└── client/
    ├── MovieFeignClient.java            # Feign 调用 movie-service 查电影
    └── fallback/
        └── MovieFeignClientFallback.java # 降级：movie-service 不可用时的兜底
```

| 类                          | 职责      | 关键点                                                           |
| -------------------------- | ------- | ------------------------------------------------------------- |
| `Session`                  | 场次实体    | `available_seats` 记录剩余座位，原子扣减防超卖                              |
| `Seat`                     | 座位实体    | `version` 字段是乐观锁核心，`@Version` 注解让 MP 自动处理                     |
| `SessionMapper`            | 场次数据访问  | `updateAvailableSeats` 用 `available_seats + delta >= 0` 保证原子性 |
| `SeatMapper`               | 座位数据访问  | `lockSeat` 带 `AND version=?` 条件，`releaseSeat` 重置 status=0     |
| `SessionServiceImpl`       | 核心业务    | 锁座 = Redis SETNX → DB 乐观锁 → 扣座位数，三步要么全成功要么全回滚                 |
| `SessionController`        | HTTP 接口 | 公开接口（列表/座位图）+ 登录接口（锁座/释放）                                     |
| `MovieFeignClient`         | 跨服务调用   | Feign 声明式调用 movie-service，验证电影是否存在                            |
| `MovieFeignClientFallback` | 降级      | movie-service 挂了返回友好提示，不拖死 session-service                    |

---

### 7.5 核心设计决策

#### 7.5.1 为什么座位状态用 Integer 而不是 Enum

```java
// 不用 Enum：JPA/MP 直接映射数据库 int 值，不用转换
private Integer status;  // 0=AVAILABLE, 1=LOCKED, 2=SOLD

// 如果用 Enum：需要 @Enumerated 注解，序列化/反序列化多一道转换
```

简单状态（3 种）用 int + 常量注释更直观，不需要额外的枚举类。

#### 7.5.2 为什么 `available_seats` 用原子更新而不是先查再改

```sql
-- ❌ 先查再改（有并发问题）
SELECT available_seats FROM session WHERE id = ?;  -- 读到 10
-- 另一个并发请求也读到 10
UPDATE session SET available_seats = 9 WHERE id = ?;  -- 两个都写 9，实际应该是 8

-- ✅ 原子更新（MySQL 行锁保证）
UPDATE session SET available_seats = available_seats + #{delta}
WHERE id = #{sessionId} AND available_seats + #{delta} >= 0;
```

`available_seats + delta >= 0` 保证不会超卖（扣到负数）。

#### 7.5.3 为什么乐观锁用 version 字段而不是 status 字段

```sql
-- ❌ 用 status 做乐观锁条件
UPDATE seat SET status=1 WHERE id=? AND status=0
-- 问题：如果 status 被其他事务改成 2（SOLD），这个 UPDATE 也失败了 → 合理
-- 但如果释放座位后又想重新锁座，status=0 的条件就会和并发释放冲突

-- ✅ 用 version 字段
UPDATE seat SET status=1, version=version+1 WHERE id=? AND status=0 AND version=?
-- version 是独立递增的，不受 status 影响，每次成功更新都 +1
```

MyBatis-Plus 的 `@Version` 注解会自动在 `updateById` 时检查 version，但这里用了自定义 SQL，所以手动写 `AND version=?`。

#### 7.5.4 锁座的 @Transactional 设计

整个 `lockSeats` 方法在 `@Transactional(rollbackFor = Exception.class)` 下：

```java
for (Long seatId : seatIds) {
    // ① Redis SETNX ─── 不在事务里（Redis 不支持 XA 事务回滚）
    // ② DB 乐观锁 ───── 在事务里，失败抛异常 → 全部回滚
    // ③ 如果 DB 失败 ── 手动 redis.delete(lockKey) 清理 Redis 锁
}
```

**为什么 DB 乐观锁失败时手动删 Redis 锁？** Redis 操作不在 Spring 事务管理范围内，事务回滚不会自动回滚 Redis。如果不手动删，那个 seatId 的 Redis 锁要等 30 秒才过期，其他人在 30 秒内无法操作那个座位。

---

### 7.6 座位状态机

```
         ┌──[lock]──→ LOCKED(1) ──[confirm]──→ SOLD(2)
         │               │    ▲                    ▲
AVAILABLE(0)             │    │                    │
         ▲               │    │                    │
         │  [unlock]     │    │                    │
         └───────────────┘    │                    │
                              │                    │
            [expire/fail] ────┘                    │
                                                   │
         （SOLD 不可逆，除非退款流程独立处理）←─────────┘
```

三种状态，两个关键动作：

- **AVAILABLE → LOCKED**：用户锁座，扣减 `available_seats`，设 15 分钟过期
- **LOCKED → SOLD**：用户支付，确认售出
- **LOCKED → AVAILABLE**：用户取消 / 超时释放，恢复 `available_seats`

---

### 7.7 验证步骤

```bash
# 编译
mvn clean package -DskipTests -pl session-service
docker compose build session-service
docker compose up -d session-service

# 查场次列表
curl "http://localhost:8080/api/session/list?movieId=1"

# 查座位图
curl "http://localhost:8080/api/session/1/seats"

# 锁座（需要 token）
TOKEN="<从登录接口获取>"
curl -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/session/lock?seatIds=1&sessionId=1"

# 验证重复锁座 → 应返回"座位已被锁定"
curl -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/session/lock?seatIds=1&sessionId=1"
```

### 7.8 详细代码

#### SessionApplication.java — 启动类

> **路径**：`session-service/src/main/java/com/movieticket/session/SessionApplication.java`
> 
> **作用**：启动类，额外加 `@EnableFeignClients` 因为需要调 movie-service 验证电影是否存在。

```java
package com.movieticket.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * session-service 启动类。
 *
 * 为什么需要 @EnableFeignClients：
 *   场次服务需要调用 movie-service 验证电影是否存在（Feign 远程调用），
 *   这个注解告诉 Spring 扫描 @FeignClient 接口并生成代理实现类。
 */
@SpringBootApplication(scanBasePackages = "com.movieticket")
@EnableFeignClients(basePackages = "com.movieticket.session.client")
public class SessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionApplication.class, args);
    }
}
```

> **关键点**：
> 
> - `@EnableFeignClients(basePackages = "com.movieticket.session.client")` 只扫描本模块的 Feign 接口，避免扫到其他模块的
> - `scanBasePackages = "com.movieticket"` 加载 common 模块的 Bean

---

#### Session.java — 场次实体

> **路径**：`session-service/src/main/java/com/movieticket/session/entity/Session.java`
> 
> **作用**：映射 `db_session` 库的 `session` 表，记录每场电影的放映信息。

```java
package com.movieticket.session.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场次实体，映射 db_session 库的 session 表。
 * 一场电影 = 一个 session，包含放映时间、影厅、票价、座位统计。
 */
@Data
@TableName("session")
public class Session {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long movieId;           // 电影ID（来自 movie-service）
    private String cinemaHall;      // 放映厅（IMAX厅 / 3号厅...）
    private LocalDateTime startTime; // 开始时间
    private LocalDateTime endTime;   // 结束时间
    private BigDecimal price;       // 票价
    private Integer totalSeats;     // 总座位数
    private Integer availableSeats; // 剩余可用座位数（锁座 -1，释放 +1）
    private Integer status;         // 1=即将开始，2=售票中，3=已结束，4=已取消

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

> **关键点**：
> 
> - `available_seats` 是并发关键字段，增减用原子 UPDATE 而非先查后改
> - `status` 枚举：1-即将开始 / 2-售票中 / 3-已结束 / 4-已取消

---

#### Seat.java — 座位实体

> **路径**：`session-service/src/main/java/com/movieticket/session/entity/Seat.java`
> 
> **作用**：映射 `db_session` 库的 `seat` 表，`version` 字段是实现乐观锁的核心。

```java
package com.movieticket.session.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 座位实体，映射 db_session 库的 seat 表。
 *
 * 乐观锁 version 字段说明：
 *   每次锁座/售出时检查 version：UPDATE seat SET status=1, version=version+1 WHERE id=? AND version=?
 *   如果 version 已被其他事务修改（并发冲突），affected rows = 0，上层重试。
 *   比悲观锁（SELECT ... FOR UPDATE）轻量，不阻塞读操作。
 *
 * 座位状态：
 *   0 = AVAILABLE（可锁）
 *   1 = LOCKED（已锁定，15分钟未支付则释放）
 *   2 = SOLD（已售出，不可逆）
 */
@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;         // 所属场次
    private Integer seatRow;        // 排号（1-based）
    private Integer seatCol;        // 列号（1-based）
    private String seatCode;        // 座位编号（如 A-03，前端展示用）
    private Integer status;         // 0=AVAILABLE，1=LOCKED，2=SOLD

    @Version                        // MyBatis-Plus 乐观锁注解
    private Integer version;

    private LocalDateTime expireTime; // 锁定过期时间（锁座时 = now + 15min）
}
```

> **关键点**：
> 
> - `@Version` 让 MyBatis-Plus 的 `updateById()` 自动带 version 条件，但自定义 SQL 需要手动写 `AND version=?`
> - `expireTime` 用于 XXL-Job 扫描已过期的锁定座位，自动释放

---

#### SessionMapper.java — 场次数据访问

> **路径**：`session-service/src/main/java/com/movieticket/session/mapper/SessionMapper.java`
> 
> **作用**：自定义 `updateAvailableSeats` 原子更新，防超卖。

```java
package com.movieticket.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.session.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 场次 Mapper。
 * availableSeats 用自定义 SQL 更新，保证原子性。
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    /**
     * 原子更新剩余座位数（锁座 -1，释放 +1）。
     * 为什么要原子 UPDATE 而不是先 SELECT 再 UPDATE：
     *   两个并发锁座请求同时 SELECT available_seats=10，都减1，都 UPDATE 回 9
     *   → 实际只减了1个座位，数据不一致。
     *   用 available_seats = available_seats + delta 让 MySQL 的写锁保证原子性。
     */
    @Update("UPDATE session SET available_seats = available_seats + #{delta} " +
            "WHERE id = #{sessionId} AND available_seats + #{delta} >= 0")
    int updateAvailableSeats(@Param("sessionId") Long sessionId,
                             @Param("delta") int delta);
}
```

> **关键点**：
> 
> - `available_seats + delta >= 0` 防止超卖（delta 是负数时，确保不会扣到负数）
> - MySQL 行锁 + 原子更新的组合保证并发安全

---

#### SeatMapper.java — 座位数据访问

> **路径**：`session-service/src/main/java/com/movieticket/session/mapper/SeatMapper.java`
> 
> **作用**：定义乐观锁锁座 SQL、释放 SQL、确认售出 SQL。

```java
package com.movieticket.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.session.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 座位 Mapper。
 * 锁座和释放需要带乐观锁条件的自定义 SQL，不能简单用 MyBatis-Plus 的 updateById。
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 乐观锁锁座：只有当 status=0（可用）且 version 匹配时才更新。
     * affected rows = 1 → 锁成功，= 0 → 座位已被别人锁了。
     *
     * 为什么用乐观锁而不是悲观锁（SELECT ... FOR UPDATE）：
     *   乐观锁不阻塞读（用户查看座位图不受影响），冲突时上层重试即可。
     *   悲观锁在锁座期间阻塞所有读，电影院场景不划算——锁座冲突率低，不需要那么重。
     */
    @Update("UPDATE seat SET status = 1, expire_time = #{expireTime}, version = version + 1 " +
            "WHERE id = #{seatId} AND status = 0 AND version = #{version}")
    int lockSeat(@Param("seatId") Long seatId,
                 @Param("version") Integer version,
                 @Param("expireTime") LocalDateTime expireTime);

    /**
     * 释放座位：将 status 重置为 0，清除过期时间。
     * 调用场景：用户取消锁座、超时未支付（XXL-Job 定时扫）。
     */
    @Update("UPDATE seat SET status = 0, expire_time = NULL, version = version + 1 " +
            "WHERE id = #{seatId} AND status = 1")
    int releaseSeat(@Param("seatId") Long seatId);

    /**
     * 确认售出：将已锁定的座位标记为已售。
     */
    @Update("UPDATE seat SET status = 2, expire_time = NULL, version = version + 1 " +
            "WHERE id = #{seatId} AND status = 1")
    int confirmSeat(@Param("seatId") Long seatId);

    /**
     * 查询已过期的锁定座位（供 XXL-Job 定时释放用）。
     */
    @Select("SELECT * FROM seat WHERE status = 1 AND expire_time < NOW()")
    List<Seat> selectExpiredLockedSeats();
}
```

> **关键点**：
> 
> - `lockSeat` 的三重条件：`status=0`（可用）+ `version=?`（乐观锁）+ `AND version=?`（防止 T4 重复更新）
> - `releaseSeat` 只释放 `status=1`（LOCKED）的座位，不碰 SOLD 的
> - `confirmSeat` 是 LOCKED→SOLD 的最终确认，不可逆

---

#### SessionService.java — 业务接口

> **路径**：`session-service/src/main/java/com/movieticket/session/service/SessionService.java`
> 
> **作用**：定义场次业务的方法契约。

```java
package com.movieticket.session.service;

import com.movieticket.session.entity.Seat;
import com.movieticket.session.entity.Session;

import java.util.List;

/**
 * 场次业务接口。
 */
public interface SessionService {

    /** 根据电影ID查询该电影的所有场次 */
    List<Session> listByMovieId(Long movieId);

    /** 查场次详情 */
    Session getById(Long id);

    /** 查某场次的座位图（按排号、列号排序） */
    List<Seat> getSeatsBySessionId(Long sessionId);

    /**
     * 锁座：Redis 分布式锁 + DB 乐观锁，双重保障。
     * @param userId    用户ID
     * @param seatIds   要锁定的座位ID列表
     * @param sessionId 场次ID
     */
    void lockSeats(Long userId, List<Long> seatIds, Long sessionId);

    /** 释放座位（用户取消或超时释放） */
    void releaseSeats(List<Long> seatIds, Long sessionId);
}
```

---

#### SessionServiceImpl.java — 业务实现（最核心）

> **路径**：`session-service/src/main/java/com/movieticket/session/service/impl/SessionServiceImpl.java`
> 
> **作用**：锁座 = Redis SETNX → DB 乐观锁 → 扣减 available_seats。三步要么全成功要么全回滚。

```java
package com.movieticket.session.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.session.entity.Seat;
import com.movieticket.session.entity.Session;
import com.movieticket.session.mapper.SeatMapper;
import com.movieticket.session.mapper.SessionMapper;
import com.movieticket.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 场次业务实现 —— 核心难点在锁座。
 *
 * 锁座流程（Redis 分布式锁 + DB 乐观锁 双保险）：
 *
 *   第1层：Redis 分布式锁（快速拦截）
 *     SETNX lock:seat:{seatId} userId EX 30     ← 30秒锁，防同一座位被同一请求重复处理
 *     失败 → 座位正被别人操作，直接拒绝，不等
 *
 *   第2层：DB 乐观锁（最终一致性）
 *     UPDATE seat SET status=1, version=version+1 WHERE id=? AND status=0 AND version=?
 *     affected rows = 0 → 乐观锁冲突，抛异常
 *
 * 为什么两层：
 *   - 只用 Redis：Redis 挂了锁全丢，多人同时锁到同一座位
 *   - 只用 DB 乐观锁：高并发下大量 UPDATE 冲突 + 重试，性能差
 *   - 两层结合：Redis 挡掉大部分并发冲突（快速失败），DB 保证数据绝对正确
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final SeatMapper seatMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY_PREFIX = "lock:seat:";
    private static final int LOCK_EXPIRE_SECONDS = 30;
    private static final int SEAT_EXPIRE_MINUTES = 15;

    @Override
    public List<Session> listByMovieId(Long movieId) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getMovieId, movieId)
                .eq(Session::getStatus, 2)      // 只查"售票中"的场次
                .orderByAsc(Session::getStartTime);
        return sessionMapper.selectList(wrapper);
    }

    @Override
    public Session getById(Long id) {
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_FOUND);
        }
        return session;
    }

    @Override
    public List<Seat> getSeatsBySessionId(Long sessionId) {
        LambdaQueryWrapper<Seat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Seat::getSessionId, sessionId)
                .orderByAsc(Seat::getSeatRow)
                .orderByAsc(Seat::getSeatCol);
        return seatMapper.selectList(wrapper);
    }

    /**
     * 锁座 —— 整个操作一个事务，要么全成功要么全回滚。
     *
     * 为什么 @Transactional：
     *   多个座位的锁操作 + 扣减 available_seats 必须在同一个事务里。
     *   如果第3个座位锁失败，前2个也要回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSeats(Long userId, List<Long> seatIds, Long sessionId) {
        for (Long seatId : seatIds) {
            // ---- 第1层：Redis 分布式锁 ----
            String lockKey = LOCK_KEY_PREFIX + seatId;
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, String.valueOf(userId),
                            LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(acquired)) {
                throw new BizException(BizCodeEnum.SEAT_LOCKED);
            }

            try {
                // ---- 第2层：DB 乐观锁 ----
                Seat seat = seatMapper.selectById(seatId);
                if (seat == null || seat.getStatus() != 0) {
                    throw new BizException(BizCodeEnum.SEAT_LOCKED);
                }

                LocalDateTime expireTime = LocalDateTime.now()
                        .plusMinutes(SEAT_EXPIRE_MINUTES);
                int rows = seatMapper.lockSeat(seatId, seat.getVersion(), expireTime);
                if (rows == 0) {
                    throw new BizException(BizCodeEnum.SEAT_LOCKED);
                }
            } catch (BizException e) {
                // DB 锁失败 → 释放 Redis 锁
                redisTemplate.delete(lockKey);
                throw e;
            }
        }

        // 所有座位锁成功 → 扣减场次的 available_seats
        int rows = sessionMapper.updateAvailableSeats(sessionId, -seatIds.size());
        if (rows == 0) {
            throw new BizException(BizCodeEnum.SEAT_SOLD);
        }

        log.info("锁座成功: userId={}, seatIds={}, sessionId={}", userId, seatIds, sessionId);
    }

    /**
     * 释放座位：DB 重置 status + Redis 删锁 + 恢复 available_seats。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeats(List<Long> seatIds, Long sessionId) {
        for (Long seatId : seatIds) {
            seatMapper.releaseSeat(seatId);
            redisTemplate.delete(LOCK_KEY_PREFIX + seatId);
        }
        sessionMapper.updateAvailableSeats(sessionId, seatIds.size());
        log.info("释放座位: seatIds={}, sessionId={}", seatIds, sessionId);
    }
}
```

> **关键点**：
> 
> - `@Transactional(rollbackFor = Exception.class)` 确保任何异常都回滚
> - DB 乐观锁失败时**手动删 Redis 锁**（Redis 不在事务管理范围内，不会自动回滚）
> - `Boolean.FALSE.equals(acquired)` 而不是 `!acquired`，因为 `setIfAbsent` 可能返回 null

---

#### SessionController.java — REST 接口

> **路径**：`session-service/src/main/java/com/movieticket/session/controller/SessionController.java`

```java
package com.movieticket.session.controller;

import com.movieticket.common.result.Result;
import com.movieticket.session.entity.Seat;
import com.movieticket.session.entity.Session;
import com.movieticket.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场次控制器。
 *
 * 接口设计：
 *   GET    /api/session/list?movieId=1      某电影的场次列表（公开）
 *   GET    /api/session/{id}                场次详情（公开）
 *   GET    /api/session/{id}/seats          座位图（公开，选座页用）
 *   POST   /api/session/lock                锁座（需登录，取 X-User-Id）
 *   POST   /api/session/release             释放座位（需登录）
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/list")
    public Result<List<Session>> list(@RequestParam Long movieId) {
        List<Session> list = sessionService.listByMovieId(movieId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Session> getById(@PathVariable Long id) {
        Session session = sessionService.getById(id);
        return Result.success(session);
    }

    @GetMapping("/{id}/seats")
    public Result<List<Seat>> seats(@PathVariable Long id) {
        List<Seat> seats = sessionService.getSeatsBySessionId(id);
        return Result.success(seats);
    }

    @PostMapping("/lock")
    public Result<?> lock(@RequestHeader("X-User-Id") Long userId,
                          @RequestParam List<Long> seatIds,
                          @RequestParam Long sessionId) {
        sessionService.lockSeats(userId, seatIds, sessionId);
        return Result.success(null);
    }

    @PostMapping("/release")
    public Result<?> release(@RequestParam List<Long> seatIds,
                              @RequestParam Long sessionId) {
        sessionService.releaseSeats(seatIds, sessionId);
        return Result.success(null);
    }
}
```

---

#### MovieFeignClient.java — Feign 调用电影服务

> **路径**：`session-service/src/main/java/com/movieticket/session/client/MovieFeignClient.java`
> 
> **作用**：声明式 HTTP 客户端，调 movie-service 验证电影是否存在。

```java
package com.movieticket.session.client;

import com.movieticket.common.result.Result;
import com.movieticket.session.client.fallback.MovieFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端：调用 movie-service。
 *
 * 为什么用 Feign 而不是 RestTemplate：
 *   Feign 声明式调用，写一个接口 + 注解就行，不用手动拼 URL、序列化、反序列化。
 *   底层用动态代理生成 HTTP 请求代码，跟调用本地方法一样自然。
 *
 * 为什么 name = "movie-service"：
 *   这是 Nacos 注册中心里 movie-service 的服务名，
 *   Feign + LoadBalancer 自动从 Nacos 发现所有 movie-service 实例并负载均衡。
 */
@FeignClient(name = "movie-service", fallback = MovieFeignClientFallback.class)
public interface MovieFeignClient {

    /** 查询电影详情，验证电影是否存在 */
    @GetMapping("/api/movie/{id}")
    Result<Map<String, Object>> getMovieById(@PathVariable("id") Long id);
}
```

---

#### MovieFeignClientFallback.java — 降级

> **路径**：`session-service/src/main/java/com/movieticket/session/client/fallback/MovieFeignClientFallback.java`

```java
package com.movieticket.session.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.session.client.MovieFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MovieFeignClient 降级：电影服务不可用时的兜底响应。
 */
@Slf4j
@Component
public class MovieFeignClientFallback implements MovieFeignClient {

    @Override
    public Result<Map<String, Object>> getMovieById(Long id) {
        log.warn("MovieFeignClient 降级: getMovieById({})", id);
        return Result.error(500, "电影服务暂不可用，请稍后重试");
    }
}
```

---

## 八、Phase 6：order-service 订单服务（13 个文件已完成）

> **状态**：✅ 已编写并编译通过（2026-07-24）
> **核心难点**：跨服务编排（Feign 调 user + session）、定时任务（XXL-Job 超时释放）

### 8.1 业务流程

用户下单不是 order-service 一个服务能完成的，需要串联多个服务：

```
用户点击"提交订单"
  │
  ▼
order-service
  ├── ① Feign → user-service:    验证用户是否存在
  ├── ② Feign → session-service: 查场次票价 + 获取座位信息
  ├── ③ 本地：计算总价、生成订单号、INSERT orders + order_item
  ├── ④ Feign → session-service: 确认座位（LOCKED → SOLD）
  └── 返回：订单详情
```

**注意**：锁座在第 ③ 步之前已经由用户在前一步（选座页面）完成。下单 = 确认已锁定的座位并创建支付订单。

---

### 8.2 为什么锁座和下单要分开

```
方案A（分开）：
  用户浏览座位图 → 选座 → 锁座（15分钟倒计时）→ 填写信息 → 下单 → 支付

方案B（合并）：
  用户浏览座位图 → 选座 → 填写信息 → 下单（同时锁座+创建订单）
```

**方案 A 更好**，因为：

1. **用户体验**：用户选座后可能犹豫（换座位、对比价格），锁座给 15 分钟缓冲
2. **公平性**：A 先选座 → A 有 15 分钟优先权；B 方案的话，谁先填完信息谁就抢到
3. **减少无效订单**：用户可能在填写信息时放弃，此时只需释放锁座，不需要创建/取消订单

---

### 8.3 架构层级

```
HTTP 请求（经过 Gateway）
  ↓
OrderController —— 接收请求、取 X-User-Id、返回 Result
  ↓
OrderService（接口）/ OrderServiceImpl（实现）—— 业务编排
  ├── create() → Feign(user + session) → INSERT orders + items
  ├── pay()    → 改状态 PAID → Feign(session 确认售出)
  ├── cancel() → 改状态 CANCELLED → Feign(session 释放座位)
  └── list()   → 按 userId 查订单列表
  ↓
OrderMapper / OrderItemMapper —— 自定义 SQL（查过期订单、取消、支付）
  ↓
MySQL db_order（orders 表 + order_item 表）
  +
XXL-Job 定时任务（扫描过期订单 → 自动取消 → 释放座位）
```

---

### 8.4 文件清单

```
order-service/src/main/java/com/movieticket/order/
├── OrderApplication.java                 # Spring Boot 启动类（+ @EnableFeignClients）
├── entity/
│   ├── Orders.java                       # 订单实体（订单号、金额、status、过期时间）
│   └── OrderItem.java                    # 订单明细（每个座位一条记录）
├── mapper/
│   ├── OrderMapper.java                  # 订单 Mapper + 查过期订单 + 取消 + 支付
│   └── OrderItemMapper.java              # 订单明细 Mapper（BaseMapper）
├── service/
│   ├── OrderService.java                 # 业务接口
│   └── impl/
│       └── OrderServiceImpl.java         # 核心：下单编排 + 支付 + 取消
├── controller/
│   └── OrderController.java              # REST 接口（/api/order/**）
├── client/
│   ├── UserFeignClient.java              # Feign 调用 user-service
│   ├── SessionFeignClient.java           # Feign 调用 session-service（查/确认/释放）
│   └── fallback/
│       ├── UserFeignClientFallback.java  # 用户服务不可用时的降级
│       └── SessionFeignClientFallback.java # 场次服务不可用时的降级
└── job/
    └── OrderTimeoutJob.java              # XXL-Job 定时任务：扫描过期订单
```

| 类                    | 职责      | 关键点                                                    |
| -------------------- | ------- | ------------------------------------------------------ |
| `Orders`             | 订单实体    | status 流转：PENDING→PAID / PENDING→CANCELLED             |
| `OrderItem`          | 订单明细    | 一个座位一条记录，关联 `order_id`                                 |
| `OrderMapper`        | 订单数据访问  | `selectExpiredOrders`、`cancelOrder`、`payOrder` 自定义 SQL |
| `OrderServiceImpl`   | 业务编排    | 跨服务 Feign 调用链：验用户 → 查场次 → 算总价 → 建订单 → 写明细              |
| `OrderController`    | HTTP 接口 | 下单/支付/取消 都要 `X-User-Id`                                |
| `UserFeignClient`    | 跨服务调用   | `@FeignClient(name="user-service")`，Nacos 发现 + 负载均衡    |
| `SessionFeignClient` | 跨服务调用   | 3 个接口：查场次、确认售出、释放座位                                    |
| `*Fallback`          | 熔断降级    | 下游服务挂了返回友好提示，不抛 500                                    |
| `OrderTimeoutJob`    | 定时任务    | 每分钟扫描过期订单 → 调 cancel() 释放座位                            |

---

### 8.5 核心设计决策

#### 8.5.1 为什么用 Feign 而不是 RestTemplate

```java
// ❌ RestTemplate：手动拼 URL、序列化/反序列化、错误处理
String url = "http://user-service/api/user/" + userId;
Result<Map> result = restTemplate.getForObject(url, Result.class);

// ✅ Feign：声明式接口，像调本地方法一样自然
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/api/user/{id}")
    Result<Map<String, Object>> getUserById(@PathVariable("id") Long id);
}
```

Feign 底层是**动态代理**：

1. Spring 启动时扫描 `@FeignClient` 接口
2. 为每个接口生成代理实现类
3. 调用 `getUserById(1L)` 时，代理类自动：拼接 URL → 发 HTTP 请求 → 解析 JSON 响应 → 返回 `Result<Map>`
4. 结合 LoadBalancer，从 Nacos 发现 `user-service` 的实例列表并负载均衡

#### 8.5.2 为什么需要 Fallback 降级

```
正常流程：
  order-service → Feign → user-service ✅ 返回用户信息

异常流程（user-service 挂了）：
  order-service → Feign → user-service ❌ 连接超时/拒绝
  → Feign 调用 UserFeignClientFallback.getUserById()
  → 返回 Result.error(500, "用户服务暂不可用")
  → 前端收到友好提示，不会白屏
```

没有降级的话，Feign 异常直接抛到 Controller → `GlobalExceptionHandler` 兜底返回 500 → 前端只知道"服务器内部错误"，不知道是哪个服务出了问题。

#### 8.5.3 为什么用 XXL-Job 而不是 @Scheduled

|         | @Scheduled     | XXL-Job        |
| ------- | -------------- | -------------- |
| Cron 配置 | 写在代码里，改频率要重新部署 | 控制台动态修改，实时生效   |
| 任务日志    | 混在应用日志里        | 单独的任务执行历史，支持追溯 |
| 失败重试    | 需要自己实现         | 内置失败重试 + 告警    |
| 多实例     | 每个实例都执行一遍（重复！） | 路由策略保证只执行一次    |
| 分片      | 不支持            | 支持分片（大数据量并发处理） |

这个项目里 `OrderTimeoutJob` 每分钟扫描 `status=0 AND expire_time < NOW()` 的订单。如果用 `@Scheduled` 且 order-service 部署了 3 个实例，同一个订单会被取消 3 次，重复调 session-service 释放座位。XXL-Job 通过"第一个"或"轮询"路由策略避免这个问题。

#### 8.5.4 为什么取消过期订单要逐一处理而不是批量 UPDATE

```sql
-- 批量 UPDATE 只能改本地库
UPDATE orders SET status=2 WHERE status=0 AND expire_time < NOW();
-- 问题：座位的释放需要调 session-service，这一步没办法批量！
```

所以流程是：

```
1. SELECT * FROM orders WHERE status=0 AND expire_time < NOW()  → 查到 5 条过期
2. for each order:
   a. 查 order_item 拿到 seatIds
   b. Feign → session-service.releaseSeats(seatIds)  ← 跨服务，只能逐条
   c. UPDATE orders SET status=2 WHERE id=?
3. log: 共5条，成功5条
```

每条订单独立 try-catch，一条失败不影响其他订单处理。

#### 8.5.5 订单状态为什么不用 Enum

和座位状态同样的原因：3 种简单状态用 int + 常量注释即可，不需要额外的枚举映射层。

```
0 = PENDING（待支付）
1 = PAID（已支付）
2 = CANCELLED（已取消）
3 = REFUNDED（已退款）
```

状态流转：

```
PENDING(0) ──支付──→ PAID(1)
    │                   │
    └──超时/取消──→ CANCELLED(2)
                        │
                 PAID(1) ──退款──→ REFUNDED(3)
```

---

### 8.6 下单全流程时序

```
前端              Gateway           order-service      user-service    session-service
 │                   │                    │                  │               │
 │ POST /api/order/create                │                  │               │
 │ Authorization: Bearer <jwt>           │                  │               │
 ├──────────────────►│                    │                  │               │
 │                   │ JWT 验签            │                  │               │
 │                   │ 注入 X-User-Id:1    │                  │               │
 │                   ├───────────────────►│                  │               │
 │                   │                    │ ① Feign 验用户    │               │
 │                   │                    ├─────────────────►│               │
 │                   │                    │◄───Result.ok─────┤               │
 │                   │                    │                  │               │
 │                   │                    │ ② Feign 查场次    │               │
 │                   │                    ├──────────────────────────────────►│
 │                   │                    │◄───Result.ok(price)───────────────┤
 │                   │                    │                  │               │
 │                   │                    │ ③ 本地：计算总价   │               │
 │                   │                    │   INSERT orders   │               │
 │                   │                    │   INSERT items    │               │
 │                   │                    │                  │               │
 │                   │                    │ ④ Feign 确认售出   │               │
 │                   │                    ├──────────────────────────────────►│
 │                   │                    │◄───座位 LOCKED→SOLD───────────────┤
 │                   │                    │                  │               │
 │                   │◄──Result(订单详情)──┤                  │               │
 │◄──订单详情────────┤                    │                  │               │
```

---

### 8.7 XXL-Job 超时释放流程

```
XXL-Job Admin 调度中心
  │  每分钟触发 "orderTimeoutHandler"
  ▼
order-service::OrderTimeoutJob.handle()
  │
  ├── 1. SELECT * FROM orders WHERE status=0 AND expire_time < NOW()
  │       查到 3 条过期订单
  │
  ├── 2. for each order:
  │       ├── 查 order_item → 拿到 seatIds
  │       ├── Feign → session-service.releaseSeats(seatIds)
  │       │     └── session-service: UPDATE seat SET status=0, expire_time=NULL
  │       │                       恢复 available_seats
  │       └── 本地: UPDATE orders SET status=2 WHERE id=?
  │
  └── 3. 日志: "共3条，成功2条，失败1条（原因: ...）"
```

**为什么用 XXL-Job Admin 而不是 order-service 内置定时器？**

- 管理界面：能看到每次执行的日志、耗时、成功/失败
- 动态启停：线上发现 bug 可以随时暂停任务
- 统一管理：一个控制台管理所有服务的定时任务

---

### 8.8 验证步骤

```bash
# 编译
mvn clean package -DskipTests -pl order-service
docker compose build order-service
docker compose up -d order-service xxl-job-admin

# 完整链路测试（需要先注册登录获取 token）
TOKEN="<登录获取的 token>"

# 1. 下单（先确保已锁座）
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"movieId":1,"seatIds":[1,2]}' \
  "http://localhost:8080/api/order/create"

# 2. 查订单详情
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/order/1"

# 3. 支付
curl -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/order/pay?orderId=1"

# 4. 测试超时释放（等 15 分钟，或手动改 expire_time）
# 然后检查 XXL-Job 控制台 http://localhost:8088/xxl-job-admin
```

### 8.9 详细代码

#### OrderApplication.java — 启动类

> **路径**：`order-service/src/main/java/com/movieticket/order/OrderApplication.java`
> 
> **作用**：启动类，加 `@EnableFeignClients` 以调用 user-service 和 session-service。

```java
package com.movieticket.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * order-service 启动类。
 *
 * @EnableFeignClients 告诉 Spring 扫描 @FeignClient 接口，
 * 让 order-service 可以远程调用 user-service 和 session-service。
 */
@SpringBootApplication(scanBasePackages = "com.movieticket")
@EnableFeignClients(basePackages = "com.movieticket.order.client")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

---

#### Orders.java — 订单实体

> **路径**：`order-service/src/main/java/com/movieticket/order/entity/Orders.java`
> 
> **作用**：映射 `db_order` 库的 `orders` 表，`expireTime` 是超时取消的关键字段。

```java
package com.movieticket.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，映射 db_order 库的 orders 表。
 *
 * 订单状态流转：
 *   PENDING(0) ──支付──→ PAID(1)
 *       │                  │
 *       └──超时/取消──→ CANCELLED(2)
 *                          │
 *                   PAID(1) ──退款──→ REFUNDED(3)
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;         // 订单编号（UUID 前20位）
    private Long userId;            // 用户ID
    private Long movieId;           // 电影ID
    private Long sessionId;         // 场次ID
    private BigDecimal totalAmount; // 订单总金额
    private Integer status;         // 0=PENDING，1=PAID，2=CANCELLED，3=REFUNDED

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime expireTime; // 过期时间（创建时间 + 15分钟）
}
```

> **关键点**：
> 
> - `orderNo` 用 `UUID.randomUUID()` 前 20 位生成，简单够用
> - `expireTime` = `createTime + 15分钟`，超时后 XXL-Job 自动取消
> - `totalAmount` = 票价 × 座位数

---

#### OrderItem.java — 订单明细

> **路径**：`order-service/src/main/java/com/movieticket/order/entity/OrderItem.java`
> 
> **作用**：映射 `db_order` 库的 `order_item` 表，一个座位一条记录。

```java
package com.movieticket.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单明细实体，映射 db_order 库的 order_item 表。
 * 一个订单可以买多个座位，每个座位一条明细。
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;       // 所属订单ID
    private Long seatId;        // 座位ID
    private String seatCode;    // 座位编号（冗余存储，方便展示）
    private BigDecimal price;   // 该座位价格
}
```

> **关键点**：
> 
> - `seatCode` 冗余存储，避免每次展示订单详情时回查 session-service
> - 一个订单多条明细，通过 `orderId` 关联

---

#### OrderMapper.java — 订单数据访问

> **路径**：`order-service/src/main/java/com/movieticket/order/mapper/OrderMapper.java`
> 
> **作用**：自定义 SQL —— 查过期订单、取消、支付。

```java
package com.movieticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.order.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /** 查询已过期的待支付订单（XXL-Job 定时扫） */
    @Select("SELECT * FROM orders WHERE status = 0 AND expire_time < NOW()")
    List<Orders> selectExpiredOrders();

    /** 取消订单：PENDING → CANCELLED */
    @Update("UPDATE orders SET status = 2 WHERE id = #{orderId} AND status = 0")
    int cancelOrder(@Param("orderId") Long orderId);

    /** 支付订单：PENDING → PAID */
    @Update("UPDATE orders SET status = 1 WHERE id = #{orderId} AND status = 0")
    int payOrder(@Param("orderId") Long orderId);
}
```

> **关键点**：
> 
> - `cancelOrder` 和 `payOrder` 都加了 `AND status = 0` 条件，防止重复操作
> - `selectExpiredOrders` 不传参数，直接用 `NOW()` 对比 `expire_time`

---

#### OrderItemMapper.java — 订单明细数据访问

> **路径**：`order-service/src/main/java/com/movieticket/order/mapper/OrderItemMapper.java`

```java
package com.movieticket.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
    // 基础 CRUD 已由 BaseMapper 提供
}
```

---

#### OrderService.java — 业务接口

> **路径**：`order-service/src/main/java/com/movieticket/order/service/OrderService.java`

```java
package com.movieticket.order.service;

import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;

import java.util.List;

/**
 * 订单业务接口。
 */
public interface OrderService {

    /**
     * 创建订单：验用户、验场次、生成订单、保存明细。
     */
    Orders create(Long userId, Long sessionId, Long movieId, List<Long> seatIds);

    /** 模拟支付：PENDING → PAID，通知 session 确认售出 */
    void pay(Long orderId, Long userId);

    /** 取消订单：释放座位 + CANCELLED */
    void cancel(Long orderId, Long userId);

    /** 订单详情（含明细） */
    Orders getById(Long id);

    /** 查某用户的所有订单 */
    List<Orders> listByUserId(Long userId);

    /** 查某订单的座位明细 */
    List<OrderItem> getOrderItems(Long orderId);
}
```

---

#### OrderServiceImpl.java — 业务编排（核心）

> **路径**：`order-service/src/main/java/com/movieticket/order/service/impl/OrderServiceImpl.java`
> 
> **作用**：跨服务编排 —— Feign 调 user + session，本地建订单。

```java
package com.movieticket.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.common.result.Result;
import com.movieticket.order.client.SessionFeignClient;
import com.movieticket.order.client.UserFeignClient;
import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.mapper.OrderItemMapper;
import com.movieticket.order.mapper.OrderMapper;
import com.movieticket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订单业务实现。
 *
 * 下单流程（跨服务调用链）：
 *   1. Feign 调 user-service   → 验证用户是否存在
 *   2. Feign 调 session-service → 查场次信息（获取票价等）
 *   3. 生成订单号 + 订单记录 → 本地入库
 *   4. 生成订单明细（每个座位一条）→ 本地入库
 *
 * 为什么下单和锁座分两步：
 *   用户先选座 → 锁座（15分钟倒计时开始）→ 填写确认信息 → 下单
 *   如果下单和锁座合并：用户还在犹豫时座位就被锁了，其他用户无法选
 *   分开：锁座给用户缓冲时间，下单确认后才真正占用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserFeignClient userFeignClient;
    private final SessionFeignClient sessionFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders create(Long userId, Long sessionId, Long movieId, List<Long> seatIds) {
        // 1. Feign 验证用户存在
        Result<Map<String, Object>> userResult = userFeignClient.getUserById(userId);
        if (userResult.getCode() != 200) {
            throw new BizException(BizCodeEnum.USER_NOT_FOUND);
        }

        // 2. Feign 查场次，获取票价
        Result<Map<String, Object>> sessionResult = sessionFeignClient.getSessionById(sessionId);
        if (sessionResult.getCode() != 200 || sessionResult.getData() == null) {
            throw new BizException(BizCodeEnum.SESSION_NOT_FOUND);
        }
        BigDecimal price = new BigDecimal(
                String.valueOf(sessionResult.getData().get("price")));

        // 3. 创建订单
        Orders order = new Orders();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setMovieId(movieId);
        order.setSessionId(sessionId);
        order.setTotalAmount(price.multiply(new BigDecimal(seatIds.size())));
        order.setStatus(0);  // PENDING
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));
        orderMapper.insert(order);

        // 4. 创建订单明细（每个座位一条）
        for (Long seatId : seatIds) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSeatId(seatId);
            item.setSeatCode("");
            item.setPrice(price);
            orderItemMapper.insert(item);
        }

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}, seatCount={}",
                order.getOrderNo(), userId, order.getTotalAmount(), seatIds.size());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId, Long userId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CANNOT_CANCEL);
        }

        orderMapper.payOrder(orderId);
        log.info("支付成功: orderNo={}, userId={}, amount={}",
                order.getOrderNo(), userId, order.getTotalAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BizException(BizCodeEnum.ORDER_CANNOT_CANCEL);
        }

        orderMapper.cancelOrder(orderId);

        // 获取订单明细 → 释放座位
        List<OrderItem> items = getOrderItems(orderId);
        List<Long> seatIds = items.stream().map(OrderItem::getSeatId).toList();
        sessionFeignClient.releaseSeats(seatIds, order.getSessionId());

        log.info("订单取消成功: orderNo={}, userId={}", order.getOrderNo(), userId);
    }

    @Override
    public Orders getById(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public List<Orders> listByUserId(Long userId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
                .orderByDesc(Orders::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }
}
```

> **关键点**：
> 
> - 创建订单时 `totalAmount = price × seatIds.size()`，总价在 order-service 本地计算
> - `expireTime = now + 15min`，超时后 XXL-Job 自动取消
> - 取消订单时同时调 `sessionFeignClient.releaseSeats()` 释放座位

---

#### OrderController.java — REST 接口

> **路径**：`order-service/src/main/java/com/movieticket/order/controller/OrderController.java`

```java
package com.movieticket.order.controller;

import com.movieticket.common.result.Result;
import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器。
 *
 * 接口设计：
 *   POST   /api/order/create        下单（需登录）
 *   POST   /api/order/pay           支付（需登录）
 *   POST   /api/order/cancel        取消（需登录）
 *   GET    /api/order/{id}          订单详情（需登录）
 *   GET    /api/order/list          我的订单列表（需登录）
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 下单 */
    @PostMapping("/create")
    public Result<Orders> create(@RequestHeader("X-User-Id") Long userId,
                                  @RequestBody Map<String, Object> body) {
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        Long movieId = Long.valueOf(body.get("movieId").toString());
        @SuppressWarnings("unchecked")
        List<Integer> seatIdInts = (List<Integer>) body.get("seatIds");
        List<Long> seatIds = seatIdInts.stream().map(Long::valueOf).toList();

        Orders order = orderService.create(userId, sessionId, movieId, seatIds);
        return Result.success(order);
    }

    /** 支付 */
    @PostMapping("/pay")
    public Result<?> pay(@RequestHeader("X-User-Id") Long userId,
                          @RequestParam Long orderId) {
        orderService.pay(orderId, userId);
        return Result.success(null);
    }

    /** 取消 */
    @PostMapping("/cancel")
    public Result<?> cancel(@RequestHeader("X-User-Id") Long userId,
                             @RequestParam Long orderId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<Orders> getById(@PathVariable Long id) {
        Orders order = orderService.getById(id);
        return Result.success(order);
    }

    /** 我的订单列表 */
    @GetMapping("/list")
    public Result<List<Orders>> list(@RequestHeader("X-User-Id") Long userId) {
        List<Orders> orders = orderService.listByUserId(userId);
        return Result.success(orders);
    }

    /** 订单明细（座位列表） */
    @GetMapping("/{id}/items")
    public Result<List<OrderItem>> items(@PathVariable Long id) {
        List<OrderItem> items = orderService.getOrderItems(id);
        return Result.success(items);
    }
}
```

---

#### UserFeignClient.java — Feign 调用用户服务

> **路径**：`order-service/src/main/java/com/movieticket/order/client/UserFeignClient.java`

```java
package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.fallback.UserFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端：调用 user-service 验证用户是否存在。
 */
@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    /** 按 ID 查用户，code=200 即存在 */
    @GetMapping("/api/user/{id}")
    Result<Map<String, Object>> getUserById(@PathVariable("id") Long id);
}
```

---

#### SessionFeignClient.java — Feign 调用场次服务

> **路径**：`order-service/src/main/java/com/movieticket/order/client/SessionFeignClient.java`

```java
package com.movieticket.order.client;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.fallback.SessionFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端：调用 session-service 查场次信息 + 确认售出 + 释放座位。
 */
@FeignClient(name = "session-service", fallback = SessionFeignClientFallback.class)
public interface SessionFeignClient {

    /** 查场次详情（获取票价等信息） */
    @GetMapping("/api/session/{id}")
    Result<Map<String, Object>> getSessionById(@PathVariable("id") Long id);

    /** 确认座位已售（支付成功后） */
    @PostMapping("/api/session/confirm")
    Result<?> confirmSeats(@RequestParam List<Long> seatIds,
                           @RequestParam Long sessionId);

    /** 释放座位（取消订单时） */
    @PostMapping("/api/session/release")
    Result<?> releaseSeats(@RequestParam List<Long> seatIds,
                           @RequestParam Long sessionId);
}
```

---

#### UserFeignClientFallback.java — 降级

> **路径**：`order-service/src/main/java/com/movieticket/order/client/fallback/UserFeignClientFallback.java`

```java
package com.movieticket.order.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * UserFeignClient 的降级实现。
 *
 * 为什么需要降级：
 *   user-service 挂了或超时 → Feign 抛异常 → 如果不处理，下单链路直接报 500
 *   有了降级 → 返回一个友好错误，前端能正常展示提示，不会白屏
 *
 * 触发条件（可在 Nacos 控制台动态配置）：
 *   - 慢调用比例：超过阈值自动熔断
 *   - 异常比例：N次调用中异常超过比例 → 熔断
 *   - 异常数：N次调用中M次异常 → 熔断
 */
@Slf4j
@Component
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public Result<Map<String, Object>> getUserById(Long id) {
        log.warn("UserFeignClient 降级: getUserById({})", id);
        return Result.error(500, "用户服务暂不可用，请稍后重试");
    }
}
```

---

#### SessionFeignClientFallback.java — 降级

> **路径**：`order-service/src/main/java/com/movieticket/order/client/fallback/SessionFeignClientFallback.java`

```java
package com.movieticket.order.client.fallback;

import com.movieticket.common.result.Result;
import com.movieticket.order.client.SessionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SessionFeignClient 降级：场次服务不可用时的兜底响应。
 */
@Slf4j
@Component
public class SessionFeignClientFallback implements SessionFeignClient {

    @Override
    public Result<Map<String, Object>> getSessionById(Long id) {
        log.warn("SessionFeignClient 降级: getSessionById({})", id);
        return Result.error(500, "场次服务暂不可用，请稍后重试");
    }

    @Override
    public Result<?> confirmSeats(List<Long> seatIds, Long sessionId) {
        log.warn("SessionFeignClient 降级: confirmSeats(sessionId={})", sessionId);
        return Result.error(500, "场次服务暂不可用，请稍后重试");
    }

    @Override
    public Result<?> releaseSeats(List<Long> seatIds, Long sessionId) {
        log.warn("SessionFeignClient 降级: releaseSeats(sessionId={})", sessionId);
        return Result.error(500, "场次服务暂不可用，请稍后重试");
    }
}
```

---

#### OrderTimeoutJob.java — XXL-Job 超时释放

> **路径**：`order-service/src/main/java/com/movieticket/order/job/OrderTimeoutJob.java`
> 
> **作用**：每分钟扫描过期订单 → Feign 调 session-service 释放座位 → 改订单状态为 CANCELLED。

```java
package com.movieticket.order.job;

import com.movieticket.order.entity.OrderItem;
import com.movieticket.order.entity.Orders;
import com.movieticket.order.mapper.OrderMapper;
import com.movieticket.order.service.OrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * XXL-Job 定时任务：扫描过期订单 → 释放座位 → 取消订单。
 *
 * 为什么需要这个定时任务：
 *   用户锁座+下单后，如果15分钟内不支付，座位应该释放给其他人。
 *   不能依赖用户主动取消——有些人直接关页面走了。
 *
 * XXL-Job 调度：每分钟执行一次
 *   在 XXL-Job Admin 控制台配置：JobHandler = "orderTimeoutHandler"，Cron = "0 * * * * ?"
 *
 * 为什么用 XXL-Job 而不是 @Scheduled：
 *   @Scheduled 写死在代码里，改频率要重新部署。
 *   XXL-Job 在控制台动态改 Cron，且有任务日志、失败重试、分片等功能。
 *   多实例部署时，@Scheduled 会重复执行，XXL-Job 通过路由策略保证只执行一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 定时释放过期订单。
     * 逻辑：扫 orders WHERE status=0 AND expire_time < NOW() → 逐一取消。
     *
     * 为什么逐一取消而不是批量 UPDATE：
     *   取消订单需要 Feign 调 session-service 释放座位，涉及跨服务调用。
     *   批量 UPDATE 只能改本地库，座位的释放需要一条一条通知 session-service。
     */
    @XxlJob("orderTimeoutHandler")
    public void handle() {
        log.info("开始扫描过期订单...");

        List<Orders> expiredOrders = orderMapper.selectExpiredOrders();
        if (expiredOrders.isEmpty()) {
            log.info("没有过期订单");
            return;
        }

        int count = 0;
        for (Orders order : expiredOrders) {
            try {
                // 获取订单的座位明细 → 释放座位
                List<OrderItem> items = orderService.getOrderItems(order.getId());
                List<Long> seatIds = items.stream().map(OrderItem::getSeatId).toList();

                // cancel 内部会调 Feign 释放座位 + 改订单状态
                orderService.cancel(order.getId(), order.getUserId());
                log.info("过期订单已取消: orderNo={}, seatIds={}", order.getOrderNo(), seatIds);
                count++;
            } catch (Exception e) {
                log.error("取消过期订单失败: orderNo={}, error={}",
                        order.getOrderNo(), e.getMessage());
            }
        }

        log.info("过期订单扫描完成: 共{}条, 成功取消{}条", expiredOrders.size(), count);
    }
}
```

> **关键点**：
> 
> - `@XxlJob("orderTimeoutHandler")` 将方法注册为 XXL-Job 执行器，在控制台配置同名 JobHandler
> - 每条订单独立 try-catch，一条失败不影响其他订单
> - cancel 内部调 `sessionFeignClient.releaseSeats()` 确保座位被释放

---

## 九、Phase 7：Sentinel 熔断降级 + 治理收尾

> **状态**：✅ Sentinel 依赖已在 order-service 和 session-service 的 pom.xml 中引入
> 
> **文件数**：依赖配置已到位，Spring Cloud Alibaba 2023.x 的 Sentinel 自动接入，不需要额外 Java 代码

### 9.1 什么是熔断降级，为什么需要

微服务架构下，一个服务挂了可能引发**雪崩**：

```
session-service 挂了
  → order-service 调 session-service 超时
    → order-service 的线程都被卡住等待超时
      → order-service 也变慢/不可用
        → Gateway 调 order-service 超时
          → Gateway 也卡住
            → 整个系统不可用
```

Sentinel 做的事情：**快速失败，阻断雪崩**。

```
session-service 挂了
  → order-service Feign 调 session 连续失败超过阈值
    → Sentinel 触发熔断（打开断路器）
      → 后续请求直接走 Fallback，不再调到 session-service（省下等待时间）
        → order-service 自己不受影响，正常返回降级提示
          → 15 秒后尝试半开 → 如果恢复了就关闭断路器
```

---

### 9.2 三种 Sentinel 规则

#### 9.2.1 熔断（Circuit Breaker）— 最核心

| 规则        | 含义                       | 适用场景             |
| --------- | ------------------------ | ---------------- |
| **慢调用比例** | 响应时间 > N ms 的请求超过比例 → 熔断 | 下游服务变慢（DB 慢查询导致） |
| **异常比例**  | 异常数/总数 > 阈值 → 熔断         | 下游服务报错           |
| **异常数**   | N 分钟内异常数 > 阈值 → 熔断       | 下游偶发错误但次数多了也熔断   |

熔断三态：

```
CLOSED（正常）──[达到阈值]──→ OPEN（熔断，直接走 Fallback）
                                  │
                            [等待 N 秒]
                                  │
                                  ▼
                            HALF-OPEN（放行一个请求试探）
                              ├── 成功 → CLOSED（恢复）
                              └── 失败 → OPEN（继续熔断）
```

#### 9.2.2 限流（Flow Control）

| 规则        | 含义                | 适用场景       |
| --------- | ----------------- | ---------- |
| **QPS**   | 每秒请求数超过阈值 → 限流    | 防止流量尖峰打垮服务 |
| **并发线程数** | 同时处理的请求数超过阈值 → 限流 | 保护慢接口不被挤爆  |

这个项目里 session-service 的锁座接口是瓶颈（涉及 Redis + DB 双层锁，并发高了可能冲突多），可以限 QPS=100。

#### 9.2.3 热点参数限流

针对特定参数值做限制（比如某个热门场次的座位被疯狂抢），一般秒杀场景用，这个 Demo 不需要。

---

### 9.3 这个项目里的 Sentinel 配置

#### 9.3.1 依赖（已引入）

```xml
<!-- order-service 和 session-service 的 pom.xml -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

#### 9.3.2 配置（在 Nacos 控制台）

在 `order-service.yml` 中添加：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8089    # Sentinel 控制台地址
      datasource:
        ds1:
          nacos:
            server-addr: ${NACOS_ADDR:nacos}:8848
            data-id: order-service-sentinel
            group-id: DEFAULT_GROUP
            data-type: json
            rule-type: flow            # 限流规则持久化到 Nacos
```

**为什么 Sentinel 规则放 Nacos？** Sentinel 默认把规则存内存，服务重启就丢了。配 Nacos 数据源后规则持久化，重启还在。

#### 9.3.3 降级（已通过 Fallback 类实现）

每个 `@FeignClient(fallback = XxxFallback.class)` 就是 Sentinel 的降级入口。Fallback 类在 `@FeignClient` 注解里声明，Sentinel 自动接管：

```java
@FeignClient(name = "user-service", fallback = UserFeignClientFallback.class)
```

当熔断触发时，Sentinel 自动调用 `UserFeignClientFallback` 的实现，返回友好错误而不是抛异常。

---

### 9.4 Phase 7 收尾检查清单

- [ ] **启动 Sentinel 控制台**
  
  ```bash
  # 启动 Sentinel Dashboard 容器（如果需要）
  docker run -d -p 8089:8089 --name sentinel-dashboard \
    bladex/sentinel-dashboard:1.8.6
  ```

- [ ] **创建 Sentinel 规则**
  
  - session-service `lockSeats` 接口：限流 QPS=100
  - order-service `create` 接口：慢调用比例熔断（>1s 且比例>50%）
  - 所有 Feign 调用：异常比例熔断（>50%）

- [ ] **全链路联调**
  
  ```
  注册 → 登录 → 浏览电影列表 → 选场次 → 看座位图 →
  锁座 → 下单 → 支付 → 查订单
  ```

- [ ] **故障演练**
  
  ```bash
  # 停掉 session-service，验证熔断
  docker compose stop session-service
  # 然后下单 → 应返回"场次服务暂不可用"而不是 500
  # 重启 session-service
  docker compose start session-service
  # 等 Sentinel 半开恢复 → 再下单 → 应恢复正常
  ```

- [ ] **超时释放验收**
  
  ```bash
  # 创建一个订单但不支付
  # 等 15 分钟（或手动改 expire_time 到过去）
  # 检查 XXL-Job 日志：订单应被自动取消
  # 检查座位：status 应恢复为 0（AVAILABLE）
  # 检查 available_seats：应 +1
  ```

- [ ] **docker-compose up -d 一键启动**
  
  ```bash
  cd D:/codeSpace/movie-ticketing
  mvn clean package -DskipTests
  docker compose build
  docker compose up -d
  # 预期：9 个容器全部 healthy
  ```

---

## 十、常用命令速查

```bash
# ========== 基础设施 ==========
# 启动基础设施
docker compose up -d mysql redis nacos

# 全部启动
docker compose up -d

# 查看状态
docker compose ps

# 查看某个服务日志
docker logs -f mt-user-service
docker logs -f mt-gateway

# 停止全部
docker compose down

# 停止并清数据（重来）
docker compose down -v

# ========== 构建 ==========
# 编译全项目
mvn clean package -DskipTests

# 只编译某个模块
mvn clean compile -pl common

# 构建镜像 + 启动
docker compose build
docker compose up -d

# ========== Nacos ==========
# 控制台
# http://localhost:8848/nacos

# ========== XXL-Job ==========
# 控制台
# http://localhost:8088/xxl-job-admin
# 账号: admin  密码: 123456

# ========== 测试 ==========
# 注册
curl -X POST "http://localhost:8080/api/user/register?username=test&password=123456"

# 登录（获取 token）
curl -X POST "http://localhost:8080/api/user/login?username=test&password=123456"

# 电影列表
curl "http://localhost:8080/api/movie/list?page=1&size=10"

# 场次列表
curl "http://localhost:8080/api/session/list?movieId=1"

# 座位图
curl "http://localhost:8080/api/session/1/seats"

# 锁座（需 token）
curl -X POST -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/session/lock?seatIds=1&sessionId=1"

# 下单（需 token）
curl -X POST -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":1,"movieId":1,"seatIds":[1]}' \
  "http://localhost:8080/api/order/create"

# 支付（需 token）
curl -X POST -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/order/pay?orderId=1"
```
