# 电影票务微服务 Demo

## 项目简介

通过一个完整的电影票务系统，深入理解微服务核心概念：服务拆分与通信、服务治理、容器化部署。

- **技术栈**: Java 21 / Spring Boot 3.x / Spring Cloud Alibaba 2023.x
- **基础设施**: Nacos 2.3.x / MySQL 8.0 / Redis 7.x / XXL-Job 2.4.x
- **构建**: Maven 多模块 + Docker Compose 一键部署

详细设计见 `README.md`，概念手册见 `PREREQUISITES.md`。

---

## 环境状态（2026-07-22 已验证）

| 组件 | 状态 |
|------|------|
| Java 21.0.11 LTS | ✅ |
| Maven 3.9.15 | ✅ |
| Docker 29.5.2 + Compose v5.1.4 | ✅ |
| 磁盘 | 711GB 可用 |
| Docker Hub | 走 Clash 代理 `127.0.0.1:7890` 可达 |
| Clash HTTP 代理 | `http://127.0.0.1:7890` |
| Clash SOCKS5 代理 | `socks5://127.0.0.1:7891` |

**已决定**:
- 所有中间件走 Docker（MySQL、Redis、Nacos、XXL-Job），不再用本地安装
- Java 服务 JVM 参数限制 `-Xmx128m`，控制在 16GB 内存内
- 端口冲突问题：本机 3306/6379 已被占 → compose 里需改映射端口

---

## 网络代理

访问外网时通过 Clash 代理：

```bash
# curl / wget 等工具
export http_proxy=http://127.0.0.1:7890
export https_proxy=http://127.0.0.1:7890

# Git
git config --global http.proxy http://127.0.0.1:7890
```

Docker 需在 Desktop 里单独配置：Settings → Resources → Proxies → `http://127.0.0.1:7890`

---

## 开发路线图

按 `README.md` 第8节的 7 个 Phase 执行，每步完成后进入下一步：

1. ✅ **基础设施 + 公共模块** (2026-07-22 完成): 父 POM、common 模块（Result/异常/JWT）、SQL 脚本、docker-compose、Dockerfile
2. **user-service**: 注册、登录（JWT）、用户信息
3. **movie-service**: 电影列表（分页+分类）、详情
4. **Gateway**: 路由、JWT 鉴权过滤器
5. **session-service**: 场次列表、座位图、锁座（Redis分布式锁+DB乐观锁）
6. **order-service**: 下单（Feign 跨服务）、取消、支付、XXL-Job 超时释放
7. **治理收尾**: Sentinel 熔断降级、全链路联调

---

## 规则

- **代码全部由用户自己写**，任何情况下不要主动写代码
- 只提供：建议、目标、步骤、代码思路，让用户自己敲
- 仅在用户明确说"帮我写"时才写代码
- 进度文档只写到**包级别**（包路径 + 类名 + 一句话职责），不贴完整代码
- 参考 `README.md` 和 `PREREQUISITES.md` 中的设计
