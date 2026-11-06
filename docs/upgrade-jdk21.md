# JDK 21 升级与部署说明

university 保留原 master 的 be53508 快照；main 为默认分支，升级只在 main 进行。本文说明本轮技术基线、运行方式和需要补充的部署信息。

## 版本与兼容边界

| 层次 | 已实现基线 | 说明 |
| --- | --- | --- |
| Java | JDK 21 | 编译、Maven 和应用运行使用同一 JDK |
| Web / IoC / 事务 | Spring Framework 7.0.10 | 保留 Spring MVC 与 XML 配置 |
| 页面模板 | Thymeleaf 3.1.5.RELEASE | 三个 Web 模块使用服务端渲染；接入 thymeleaf-spring6 |
| 数据访问 | MyBatis 3.6、MyBatis-Spring 4.1 | 保留现有 Mapper 和数据模型 |
| RPC | Apache Dubbo 3.3.6 | 可配置直连；Spring 7 组合真实 TCP 冒烟已通过 |
| Jakarta API | Jakarta EE 11 对应 API | Servlet 6.1 使用 Jakarta 命名空间；页面已移除 JSP/JSTL 依赖 |
| 开发容器 | Jetty 12.1.14，EE11 Maven 插件 | 只在六个 WAR 模块启用 |
| 生产容器 | Tomcat 11 部署目标 | WAR 已打包；生产容器部署尚未验证 |
| 数据库 | MySQL 8.x，现有实例 8.1.0 | 通过 DBX 的 nas-mysql 查验 |
| 缓存 / 会话 | Redis 8.6.2 standalone | 通过 DBX 的 nas-redis 查验 |
| 消息总线 | Spring Kafka 4.1.2 / Kafka 客户端 4.2.2 | 真实 Broker 与 Redis 缓存失效链路已验证 |
| 图片存储 | MySQL MEDIUMBLOB | manager-web 通过 /images/{id} 读取 |

上述版本为本轮已实现的升级基线，实际依赖坐标和补丁版本以根目录 pom.xml 为准。本机 JDK 21 到 MySQL、Redis 与 Kafka 的运行时连通已验证；16 个 Maven 模块 clean install 成功，29 个测试零失败，六个 WAR 真实启动后的 15 项 HTTP 检查全部通过。Tomcat 11 生产部署尚未验证。

Spring 7 采用 Jakarta EE 11 对应 API，应用使用 Servlet 6.1，由 Thymeleaf 渲染 HTML。迁移涉及 javax.servlet 等 Jakarta API；javax.sql 属于 Java SE，无需迁移命名空间。后续页面迁移说明与验证记录见 [Thymeleaf 页面说明](thymeleaf.md)。官方依据见文末链接。

### Dubbo 分页响应兼容

Dubbo 保持默认 STRICT 类检查。manager 在 RPC 返回前将 PageInfo 转为标准 LinkedHashMap，保留原分页 HTTP JSON 的全部字段；portal 的分页数据使用普通 ArrayList。RPC 边界传输标准 JDK 集合，Web 工程无需依赖 PageHelper。

该结构已通过最终真实 HTTP 复测，六个应用日志中未出现 ERROR、ClassNotFound 或 STRICT 类型拒绝。类检查机制参见 [Dubbo 官方说明](https://dubbo.apache.org/en/overview/mannual/java-sdk/tasks/security/class-check/)。

## 运行拓扑

main 保留六个 WAR、三组 Dubbo 服务与消费方。移除应用侧 ZooKeeper 注册发现和 ActiveMQ / JMS 配置；Web 工程通过显式提供方地址调用 Dubbo。文章变更事件通过 Kafka 发送，由 portal 服务消费并清理 Redis 文章缓存。图片改为写入 MySQL 的 MEDIUMBLOB 字段，由 manager-web 的 /images/{id} HTTP 接口读取。无需对象存储、图片持久目录或 Nginx 图片服务器。

DBX 仅用于发现连接配置、检查服务状态和辅助验证。运行时，Java 应用使用 JDBC 连接 MySQL，使用 Jedis 连接 Redis，使用 Kafka 客户端连接 Broker，不经由 DBX 转发业务请求。

| Maven 模块 | HTTP | Dubbo | 角色 |
| --- | --- | --- | --- |
| myblog-portal/myblog-portal-service | 8081 | 20881 | 文章查询、文章缓存、Kafka 消费 |
| myblog-portal-web | 8082 | — | 前台页面，直连 portal 服务 |
| myblog-manager/myblog-manager-service | 8083 | 20880 | 文章管理、Kafka 事件生产 |
| myblog-manager-web | 8084 | — | 后台页面、数据库图片上传与读取，直连 manager 服务 |
| myblog-SingleSignOn/myblog-SingleSignOn-service | 8085 | 20882 | 注册、登录、Redis 会话 |
| myblog-SingleSignOn-web | 8086 | — | 登录和注册页面，直连 SSO 服务 |

HTTP 端口保留原 POM 的 8081–8086。前台页面是 8082，后台管理页面是 8084；旧 README 中写反的说明已纠正。

跨主机或容器部署时，Dubbo 直连 URL 必须是消费方可达的地址；应用配置中的 localhost 指向应用所在运行环境。Kafka 除了 bootstrap 地址，还会向客户端返回 Broker 地址，这些地址也必须可达。

## 连接与配置清单

运行默认配置集中在 myblog-common/src/main/resources/myblog-defaults.properties，已包含当前组网环境的 NAS MySQL / Redis 地址、账号密码、测试 Kafka 地址及本机 Dubbo 直连地址。按项目约定，这份环境连接配置随仓库管理；在当前组网环境使用 JDK 21 即可启动应用。

MYBLOG_CONFIG_LOCATION 是可选覆盖入口，默认启动无需设置。切换到其他环境时，可将根目录 myblog-local.example.properties 复制为所需环境的配置文件，再指向该文件，例如：

~~~bash
export MYBLOG_CONFIG_LOCATION="file:$PWD/myblog-local.properties"
~~~

外部文件覆盖仓库默认值；移除 MYBLOG_CONFIG_LOCATION 设置后恢复默认配置。DBX 不负责向 Java 进程注入密码，Java 直接读取仓库默认配置或可选覆盖文件。

下表使用应用配置键名，当前组网环境的值以默认文件为准，其他环境可参考样例覆盖。DBX 不会自动注入这些配置。

| 组件 | 配置键 | 用途 |
| --- | --- | --- |
| MySQL | jdbc.driver、jdbc.url、jdbc.username、jdbc.password | 使用 com.mysql.cj.jdbc.Driver，文章、用户及图片位于独立 myblog 库 |
| Redis | redis.host、redis.port、redis.username、redis.password、redis.database、redis.ssl | 单机缓存和 SSO 会话 |
| Kafka | kafka.bootstrap-servers、kafka.topic、kafka.consumer-group、kafka.security-protocol、kafka.sasl-mechanism、kafka.sasl-jaas-config | 文章变更后的缓存失效 |
| manager RPC | dubbo.manager.url、dubbo.manager.port、dubbo.host | manager-web 直连 manager-service |
| portal RPC | dubbo.portal.url、dubbo.portal.port、dubbo.host | portal-web 直连 portal-service |
| SSO RPC | dubbo.sso.url、dubbo.sso.port、dubbo.host | SSO-web 直连 SSO-service |
| 图片读取 | image.public-base-url | 开发值为 http://localhost:8084，拼接 /images/{id} 供浏览器读取 |
| 门户入口 | portal.public-base-url | 开发值为 http://localhost:8082，SSO 导航与登录默认回跳使用，可包含上下文路径 |
| 登录入口 | sso.public-base-url | 开发值为 http://localhost:8086，门户的登录与注册链接使用，可包含上下文路径 |

模块仍通过各自的 Spring XML 消费配置：

| 内容 | 配置位置 |
| --- | --- |
| 默认值与外部覆盖 | myblog-common/src/main/resources/myblog-defaults.properties；根目录 myblog-local.example.properties |
| 数据源 | 三个服务模块的 src/main/resources/spring/applicationContext-dao.xml |
| Redis | portal、SSO 服务的 src/main/resources/spring/applicationContext-redis.xml |
| Kafka | manager、portal 服务的 Spring 消息配置 |
| Dubbo 提供方 | 三个服务模块的 src/main/resources/spring/applicationContext-service.xml |
| Dubbo 直连消费方 | 三个 Web 模块的 src/main/resources/spring/springmvc.xml |
| 图片上传与读取 | myblog-manager-web 的上传组件、图片读取 Controller 和数据库访问配置 |
| 开发 HTTP 端口 | 六个 WAR 模块各自的 pom.xml |

三个服务模块位于 myblog-manager/myblog-manager-service、myblog-portal/myblog-portal-service、myblog-SingleSignOn/myblog-SingleSignOn-service；Web 模块位于根目录下对应的三个 -web 工程。

### 已查验与待补充的信息

| 范围 | 当前结果与待补充项 |
| --- | --- |
| nas-mysql | DBX 确认 MySQL 8.1.0，本机通过 JDBC 连接 192.168.20.5 成功；独立 myblog 库的 V1 三张空业务表和 V2 图片表已创建，连接地址和认证由仓库默认配置提供 |
| nas-redis | Redis 8.6.2 standalone；本机 Jedis 携认证连接 192.168.20.5 并执行 PING 成功 |
| Kafka | 本机 AdminClient 已连接 192.168.20.10:9092；myblog.article-cache-invalidation 已创建，1 分区、1 副本；myblog-portal-cache 消费组已消费文章变更事件并递增 Redis 缓存版本 |
| Dubbo | 本机三组提供方与 Web 消费方的真实 RPC 已验证；跨主机部署仍需配置可达地址 |
| 应用部署 | 本机 / 服务器 / 容器、各应用主机和端口、前台 / 后台 / SSO 域名与上下文路径 |

DBX 的 MySQL / Redis endpoint 使用 host.docker.internal 容器别名。它在 DBX 环境中可用，不代表 Mac 上的 JVM 能解析或访问；本机 JVM 使用已确认的 192.168.20.5。部署到其他主机或容器时，仍需从应用所在环境验证可达性。

## 数据与业务行为

db/migration/V1__initial_schema.sql 创建 tb_article、tb_user、tb_userlog 三张业务表；db/migration/V2__images.sql 新增 tb_image，包括 UUID 主键、content_type、MEDIUMBLOB 数据和创建时间。应用不会自动执行迁移。新环境先创建独立 myblog 库，再按 V1、V2 顺序执行。

本轮已通过 DBX 创建独立 myblog 库及四张表，并通过本机 JDBC 确认表存在；没有导入大学版演示记录，也未改动实例内其他业务库。原始 myblog.sql 包含历史数据，保留为大学版资料和结构参考。文章和用户表已有业务调用，用户行为表目前主要保留数据模型与 Mapper。

portal 文章缓存异常时可回退数据库。SSO 登录写会话、token 查询直接依赖 Redis，因此启用 SSO 时必须提供可用 Redis。

Kafka 事件用于文章变更后的缓存失效。生产与消费统一使用 myblog.article-cache-invalidation；portal 消费组为 myblog-portal-cache。生产者在数据库事务提交后发送，并等待 acks=all 确认；消费者成功递增 Redis 缓存版本号后才提交 offset，Redis 操作失败会重试；新请求使用新版本的分页缓存，旧版本随 TTL 过期。多实例部署时需结合缓存共享关系确认消费组策略。

应用不自动创建 Kafka topic。新环境需预创建 myblog.article-cache-invalidation，或由部署工具通过 Kafka Admin API 创建；consumer group 则在首次消费时建立。当前环境采用 1 分区、1 副本。

当前未采用数据库事务 outbox。数据库提交后、消息发出前若进程崩溃，仍存在事件丢失窗口，文章缓存 TTL 作为最终过期兜底，默认 300 秒。该机制不保证数据库提交与 Kafka 发布原子完成。

旧版图片写入本地 Windows 目录并由 Nginx 暴露。main 将图片存入 MySQL MEDIUMBLOB。POST /upload 保留原 error/url 响应格式，GET /images/{id} 返回图片，支持 Content-Type、ETag 和一年 immutable 缓存。上传限制为 5 MiB，multipart 请求总上限为 6 MiB，只接受通过文件内容识别的 PNG、JPEG、GIF、WebP；如需保留旧文章图片，应确认旧文件导入和历史 URL 的兼容方式。

保存接近上限的图片时，MySQL 服务端和客户端允许的包大小需容纳图片与 SQL 开销；建议 max_allowed_packet 至少为 8 MiB。超过限制会导致写入失败或连接关闭，参见 [MySQL 包大小说明](https://dev.mysql.com/doc/refman/8.4/en/packet-too-large.html)。

页面模板位于三个 Web 模块的 WEB-INF/templates，站内链接使用 Thymeleaf 的上下文路径处理；跨应用跳转使用 portal.public-base-url 和 sso.public-base-url。图片读取地址使用 image.public-base-url。部署时按实际域名及上下文路径覆盖这些配置。

## 开发构建与启动

1. 安装 JDK 21 和 Maven，确认 JAVA_HOME、IDEA 项目 SDK 与 Maven 运行 JDK 均指向 JDK 21。本机 Homebrew Maven 在未设置 JAVA_HOME 时默认运行于 JDK 26，与 java -version 的默认 JDK 不同；本项目的 enforcer 要求 JDK 21。
2. 当前组网环境直接使用仓库默认的 MySQL、Redis、Kafka 和 Dubbo 连接配置。切换其他环境时可选用 MYBLOG_CONFIG_LOCATION 覆盖；新环境完成 V1 / V2 建表并预创建 Kafka topic，当前 NAS 环境已完成初始化。
3. 从仓库根目录构建并安装全部模块：

   ~~~bash
   export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
   java -version
   mvn -version
   mvn clean install
   ~~~

4. 从仓库根目录，在独立终端依次启动以下 WAR。每个终端先设置 JAVA_HOME，再执行对应的 Maven 启动命令；默认组网环境无需额外配置环境变量。先确认服务工程就绪，再启动 Web 工程：

   ~~~bash
   export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
   mvn -pl myblog-manager/myblog-manager-service jetty:run
   mvn -pl myblog-portal/myblog-portal-service jetty:run
   mvn -pl myblog-SingleSignOn/myblog-SingleSignOn-service jetty:run
   mvn -pl myblog-manager-web jetty:run
   mvn -pl myblog-portal-web jetty:run
   mvn -pl myblog-SingleSignOn-web jetty:run
   ~~~

5. 访问前台 http://localhost:8082/、后台 http://localhost:8084/、登录页 http://localhost:8086/page/login。

jetty:run 持续占用终端，使用 Ctrl+C 停止。Jetty EE11 插件只在 WAR 模块启用；聚合工程和公共 JAR 工程不直接启动 Web 应用。各 WAR 使用已安装到本地 Maven 仓库的共享模块，修改接口或公共模块后需重新构建安装。

## 生产 WAR 部署

用 JDK 21 构建，将六个 WAR 部署到 Tomcat 11。产物位于各 WAR 模块的 target/ 目录。开发插件的端口不会自动成为生产 Tomcat 的 Connector 端口，需通过容器配置确定。

现有页面按根上下文 / 组织路由。同一 Tomcat Host 下不能让六个应用同时占用根上下文，应结合拓扑安排独立实例、虚拟主机或上下文路径，并同步页面跳转、上传入口和反向代理。保留本地默认访问方式时，可通过六个独立容器实例映射原端口。

生产运行时需要应用能够访问 MySQL、Redis、Kafka Broker 和 Dubbo 提供方。图片由应用读取数据库并响应 HTTP 请求，不需要外部图片存储服务。DBX 中的连通结果只能证明 DBX 所在环境的可达性，还需要在 Java 应用实际运行位置验证。

## 验证范围

JDK 21 升级阶段已完成（迁移 Thymeleaf 前的记录；当前页面验证见 [Thymeleaf 页面说明](thymeleaf.md)）：

- JDK 21 下 16 个 Maven 模块 clean install 成功，29 个测试零失败。
- 六个 WAR 真实启动，15 项 HTTP 检查全部通过；四个 JSP 页面返回 HTTP 200。
- manager 分页列表经标准 Map / ArrayList 的 Dubbo RPC 正常返回，维持原 HTTP JSON 结构。
- SSO 缺失 token 查询经 Dubbo 与 Redis 正常返回，数据库连接已单独验证。
- PNG 上传成功，读取结果与上传字节一致，条件请求返回 HTTP 304。
- 文章新增后门户列表与详情可见；经 HTTP 删除后，Kafka 消费触发 Redis 缓存失效，门户立即不再显示该文章。
- 本机 JDBC、Jedis 与 Kafka AdminClient 连通；独立 myblog 四张表及专用 Kafka topic 已准备完成。

最终复测的六个应用日志未出现 ERROR、ClassNotFound 或 STRICT 类型拒绝。测试创建的文章和图片已按 ID 精确清理，未导入历史演示数据或改动其他业务库。

尚未覆盖的范围：

- 完整注册、登录、Cookie 共享和会话续期 / 过期流程；当前 SSO 实测覆盖缺失 token 查询。
- Tomcat 11 生产容器、生产域名和跨主机 / 多实例部署。
- 历史图片迁移、历史 URL 兼容，以及接近 5 MiB 上限的大图片实测。

## 官方资料

- [Spring Framework 7 官方文档](https://docs.spring.io/spring-framework/reference/overview.html)
- [Jakarta EE 11 平台规范](https://jakarta.ee/specifications/platform/11/)
- [Apache Dubbo 3.3.6 官方说明](https://github.com/apache/dubbo/blob/dubbo-3.3.6/README.md)
- [Dubbo 直连提供方](https://dubbo.apache.org/en/overview/mannual/java-sdk/tasks/framework/more/explicit-target/)
- [Dubbo STRICT 类检查机制](https://dubbo.apache.org/en/overview/mannual/java-sdk/tasks/security/class-check/)
- [Jetty 12.1：EE11 Maven 插件](https://jetty.org/docs/jetty/12.1/programming-guide/maven-jetty/jetty-maven-plugin.html)
- [Tomcat 11：Servlet 6.1 与 JSP 4.0](https://tomcat.apache.org/tomcat-11.0-doc/index.html)
- [Kafka 客户端配置](https://kafka.apache.org/41/configuration/consumer-configs/)
