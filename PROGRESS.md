# 项目进度

> 最后更新: 2026-07-24

---

## 一、当前进度

- [ ] **Phase 1：基础设施 + 公共模块** ← 正在做
- [ ] Phase 2：user-service 注册/登录
- [ ] Phase 3：movie-service 电影列表/详情
- [ ] Phase 4：Gateway 路由/JWT 鉴权
- [ ] Phase 5：session-service 场次/锁座
- [ ] Phase 6：order-service 下单/XXL-Job
- [ ] Phase 7：Sentinel 熔断降级/联调

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

## 三、Phase 1：公共模块 common（5 个文件）

> **目标**：写出 `common` 模块的所有 Java 类，编译通过。
> 全部在 `common/src/main/java/com/movieticket/common/` 目录下。

---

### 3.1 Result.java — 统一响应体

> **路径**：`common/src/main/java/com/movieticket/common/result/Result.java`
>
> **作用**：封装所有接口返回值，前端和 Feign 调用方拿到统一格式 `{ code, message, data }`。
> 提供两个静态工厂：`Result.ok(data)` 成功、`Result.fail(code, msg)` 失败。

```java
package com.movieticket.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private int code;       // 200=成功，其他=异常
    private String message; // 提示信息
    private T data;         // 实际返回数据

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

> **关键点**：
> - 泛型 `<T>` 让每种接口返回自己的数据类型
> - `ok()` 的 code 固定 200
> - 后续 GlobalExceptionHandler 会调用 `fail()` 来包装异常

---

### 3.2 BizException.java — 业务异常

> **路径**：`common/src/main/java/com/movieticket/common/exception/BizException.java`
>
> **作用**：自定义运行时异常，带上错误码和消息。
> Service 层遇到业务规则不满足（用户名重复、密码错误等）直接 `throw new BizException("xxx")`。

```java
package com.movieticket.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;     // 错误码，默认 400

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

> **关键点**：
> - 继承 `RuntimeException`，不需要在方法签名上声明 throws
> - 默认 code=400（Bad Request），也可以传自定义码（如 401 未授权）
> - `@Getter` 让 GlobalExceptionHandler 能读到 code 字段

---

### 3.3 GlobalExceptionHandler.java — 全局异常处理

> **路径**：`common/src/main/java/com/movieticket/common/exception/GlobalExceptionHandler.java`
>
> **作用**：Spring MVC 切面，拦截 Controller 抛出的异常，转为统一的 `Result.fail()` 返回。
> 拦截两类：`BizException`（业务异常）和 `Exception`（未知异常兜底）。

```java
package com.movieticket.common.exception;

import com.movieticket.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice   // 对所有 @RestController 生效
public class GlobalExceptionHandler {

    /** 业务异常 → 直接取出 code 和 message 返回 */
    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 未知异常 → 兜底，打印堆栈，返回 500 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "服务器内部错误");
    }
}
```

> **关键点**：
> - `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`
> - `BizException` 处理方法拿 e.code 和 e.message 透传
> - 未知异常返回 500，不暴露堆栈给前端
> - 注意：不要让 common 模块扫描到子模块的包，子模块通过 `@SpringBootApplication(scanBasePackages = "com.movieticket")` 扫描 common

---

### 3.4 JwtUtil.java — JWT 工具类

> **路径**：`common/src/main/java/com/movieticket/common/auth/JwtUtil.java`
>
> **作用**：签发 JWT（登录时调用）、验证 JWT（Gateway 鉴权时调用）、解析 userId（下游服务用）。
> 依赖 `io.jsonwebtoken:jjwt-api`（common 的 pom.xml 已引入）。

```java
package com.movieticket.common.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    /** 签发 Token，把 userId 和 username 写进 payload */
    public static String generate(Long userId, String username, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))    // sub = userId
                .claim("username", username)        // 自定义 claim
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)) // 7 天
                .signWith(key)
                .compact();
    }

    /** 验证并解析 Token，返回 Claims；校验失败抛 JwtException */
    public static Claims parse(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 Claims 中取 userId */
    public static Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** 从 Claims 中取 username */
    public static String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }
}
```

> **关键点**：
> - `secret` 从 Nacos 配置中心读取，不硬编码
> - `generate()` 的三个调用方：userId、username 来自登录查库结果，secret 来自 `@Value`
> - `parse()` 校验失败会抛 `JwtException`，Gateway filter 里 catch 后返回 401
> - 过期时间 7 天，生产环境建议缩短到 2 小时 + refresh token

---

### 3.5 JacksonConfig.java — Jackson 序列化配置

> **路径**：`common/src/main/java/com/movieticket/common/config/JacksonConfig.java`
>
> **作用**：统一 JSON 格式：日期返回 `yyyy-MM-dd HH:mm:ss`、时区 Asia/Shanghai、Long 不转 String。

```java
package com.movieticket.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mapper.registerModule(module);
        return mapper;
    }
}
```

> **关键点**：
> - `JavaTimeModule` 处理 Java 8 时间类型（`LocalDateTime` 等）
> - 不加这个配置的话，`LocalDateTime` 默认序列化成数组 `[2026,7,22,10,30,0]`，前端没法用

---

### 3.6 Nacos 配置检查清单

在 Nacos 控制台 `http://localhost:8848/nacos` 确认：

| 配置 ID         | Group          | 内容                                         | 状态 |
| --------------- | -------------- | -------------------------------------------- | ---- |
| `common.yml`    | DEFAULT_GROUP   | `jwt.secret: movie-ticketing-secret-must-be-256-bits-long-!!!` | ✅    |
| `user-service.yml` | DEFAULT_GROUP | datasource 连 `mysql:3306/db_user`            | ✅    |

> Phase 1 只需读 `common.yml` 中的 `jwt.secret`，各服务的 datasource 配置到 Phase 2+ 才用。

---

### 3.7 Phase 1 验证步骤

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

@Data                       // Lombok：自动生成 getter/setter/toString
@TableName("user")          // 指定映射的数据库表名
public class User {

    @TableId(type = IdType.AUTO)  // 主键，数据库自增
    private Long id;

    private String username;      // 用户名，唯一
    private String password;      // 密码，存储 MD5 加密后的值
    private String nickname;      // 昵称
    private String phone;         // 手机号
    private String email;         // 邮箱
    private Integer status;       // 0-禁用，1-正常

    @TableField(fill = FieldFill.INSERT)        // 插入时自动填充当前时间
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

@Mapper  // 标记为 MyBatis 的 Mapper，Spring 会自动扫描
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper 已经提供了 selectById、selectOne、insert 等方法
    // 如果后续有复杂查询，在这里写方法 + 对应的 XML 或用 @Select 注解
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

public interface UserService {

    /**
     * 用户注册
     * @param username 用户名
     * @param password 明文密码（存库前加密）
     * @param nickname 昵称（可选，不传则默认用 username）
     * @return 注册成功的用户对象（不含密码）
     */
    User register(String username, String password, String nickname);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 明文密码，和库里的加密密码比对
     * @return JWT token 字符串，前端后续请求都带这个
     */
    String login(String username, String password);

    /**
     * 根据 ID 查询用户（供内部 Feign 调用和 /current 接口使用）
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
import com.movieticket.common.auth.JwtUtil;          // common 模块提供的 JWT 工具
import com.movieticket.common.exception.BizException; // common 模块提供的业务异常
import com.movieticket.user.entity.User;
import com.movieticket.user.mapper.UserMapper;
import com.movieticket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Slf4j                       // 日志，可以用 log.info() 打日志
@Service                     // 标记为 Spring 的 Service Bean
@RequiredArgsConstructor     // Lombok：为 final 字段自动生成构造函数（代替 @Autowired）
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;  // final + RequiredArgsConstructor = 自动注入

    @Value("${jwt.secret}")  // 从 Nacos 配置中心读取 JWT 密钥
    private String jwtSecret;

    /**
     * 注册逻辑：
     * ① 查用户名是否已被注册 → 抛 BizException
     * ② MD5 加密密码
     * ③ 入库
     */
    @Override
    public User register(String username, String password, String nickname) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);  // WHERE username = ?
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException("用户名已存在");
        }

        // 2. MD5 加密密码（生产环境建议用 BCrypt，这里用 MD5 演示）
        String encryptedPwd = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8));

        // 3. 组装 User 对象并入库
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPwd);
        user.setNickname(nickname != null ? nickname : username); // 没传昵称就用用户名
        user.setStatus(1);  // 1=正常，0=禁用
        userMapper.insert(user);  // MyBatis-Plus 自动生成 INSERT SQL

        log.info("用户注册成功: {}", username);
        return user;
    }

    /**
     * 登录逻辑：
     * ① 查用户是否存在
     * ② 密码 MD5 后和库里比对
     * ③ 检查是否被禁用
     * ④ 用 common 模块的 JwtUtil 生成 token
     */
    @Override
    public String login(String username, String password) {
        // 1. 查用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            // 不明确说是"用户不存在"还是"密码错误"，防止撞库
            throw new BizException("用户名或密码错误");
        }

        // 2. 密码比对：将输入密码 MD5 后和库里比对
        String encrypted = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8));
        if (!encrypted.equals(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }

        // 3. 检查状态
        if (user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }

        // 4. 生成 JWT（userId 和 username 写进 token 的 payload）
        String token = JwtUtil.generate(user.getId(), user.getUsername(), jwtSecret);
        log.info("用户登录成功: {}", username);
        return token;
    }

    /**
     * 根据 ID 查用户，查不到抛异常
     */
    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
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

@RestController               // = @Controller + 每个方法返回 JSON
@RequestMapping("/api/user")   // 统一前缀，所有接口都以 /api/user 开头
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     * 请求示例: POST /api/user/register?username=zhangsan&password=123456&nickname=张三
     * 返回: { code:200, message:"success", data:{ id:1, username:"zhangsan", ... } }
     *
     * 注意：返回前把 password 置 null，不暴露密码
     */
    @PostMapping("/register")
    public Result<User> register(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam(required = false) String nickname) {
        User user = userService.register(username, password, nickname);
        user.setPassword(null);  // 安全：永远不要在响应里返回密码
        return Result.ok(user);
    }

    /**
     * 用户登录
     * 请求示例: POST /api/user/login?username=zhangsan&password=123456
     * 返回: { code:200, message:"success", data:"eyJhbGciOi..." }
     *
     * 前端拿到 token 后存 localStorage，后续请求放 Authorization 头里
     */
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username,
                                 @RequestParam String password) {
        String token = userService.login(username, password);
        return Result.ok(token);
    }

    /**
     * 获取当前登录用户信息
     * 请求示例: GET /api/user/current
     * 请求头: Authorization: Bearer <JWT>
     *
     * 注意：这个接口不自己解析 JWT！
     * Gateway 已经解析过了，把 userId 放到了 X-User-Id 请求头，
     * 这里直接用 @RequestHeader 取值即可。
     */
    @GetMapping("/current")
    public Result<User> current(@RequestHeader("X-User-Id") Long userId) {
        User user = userService.getById(userId);
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 根据 ID 查询用户（供其他服务 Feign 内部调用）
     * 请求示例: GET /api/user/1
     * 调用方: order-service（下单时验用户是否存在）
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

## 五、常用命令速查

```bash
# 启动基础设施
docker compose up -d mysql redis nacos

# 查看状态
docker compose ps

# 停止全部
docker compose down

# 停止并清数据
docker compose down -v

# 一键启动全部（业务代码写完后）
mvn clean package -DskipTests
docker compose build
docker compose up -d
```
