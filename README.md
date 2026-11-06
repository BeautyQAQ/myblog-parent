# 个人博客系统

这是大学时期写的一个 SOA 个人博客，也是保留下来的一段回忆。

- [university](https://github.com/BeautyQAQ/myblog-parent/tree/university)：保留原 master 的 be53508 快照，保存大学时期的代码和部署说明。
- main：新的默认分支，承载后续升级与修复，保留原来的文章管理、前台展示和服务拆分思路。

## 项目介绍

项目将 Web 页面与业务服务拆成独立工程。后台提供文章管理与评论审核，前台提供文章列表、详情、标题搜索与标签筛选，SSO 工程包含注册、登录与 token 查询。已支持阅读去重、登录点赞 / 取消点赞、真实热度排序，以及先审核后公开的评论。

博客核心功能已加入管理员登录与后台保护、退出、草稿 / 发布 / 下架、后台预览、正文清洗和友好 404。公开注册默认普通用户，需明确指定管理员才能管理文章。升级现有环境先执行尚未应用的 V3、V4、V5、V6 数据迁移，管理员初始化与功能说明见 [博客核心功能](docs/core-features.md)。

核心功能改造已完成 JDK 21 下 16 模块 clean install、91 项测试、六个 WAR 的 29 项真实 HTTP 检查，以及 Chrome 完整写作与退出流程验证。

文章互动已实现同一访客 30 分钟阅读去重、同一账号幂等点赞 / 取消点赞，热度为阅读量 + 点赞数 × 5。详情即时显示互动结果，列表与最热排序缓存最多 30 秒。升级需执行 [V5](db/migration/V5__article_interactions.sql)，详见 [阅读与点赞](docs/core-features.md#阅读与点赞)。

阅读与点赞改造时 JDK 21 全仓 16 模块构建、141 项自动测试通过；互动功能另完成两轮共 35 项真实 HTTP 检查、6 项 JavaScript 行为检查及应用内浏览器登录 / 点赞 / 取消 / 刷新 / 退出验证。临时测试数据已清理。

评论支持登录提交、查看本人审核状态和删除本人评论；后台提供状态 / 文章筛选、分页及通过 / 拒绝 / 删除操作。前台只公开已通过评论，重复提交和重复审核不会重复计数。升级需执行 [V6](db/migration/V6__article_comments.sql)，详见 [评论与后台审核](docs/core-features.md#评论与后台审核)。

当前评论版本已通过 JDK 21 下 16 模块构建、178 项自动测试、43 项真实 HTTP 检查、15 项 JavaScript 行为检查，以及应用内浏览器提交 / 审核 / 公开展示 / 拒绝撤下流程验证。NAS 环境已应用 V6，临时测试数据已清理。

基本内容检索已完成标题关键词与整标签组合筛选、条件保留的分页 / 排序、匹配总数和无结果提示。检索改造时全仓 115 项测试通过，另完成 21 项真实检索检查及 Chrome 操作验证；检索功能本身无需新数据迁移。使用说明见 [内容检索](docs/core-features.md#内容检索)。

main 使用 Spring MVC、Thymeleaf、MyBatis 和 Dubbo 独立服务。门户、后台与登录注册页面由 Java 服务端渲染，模板与静态资源随 WAR 部署，无需独立前端工程或 Node.js 运行环境。MySQL 存储业务数据，Redis 提供文章缓存和登录会话，Kafka 传递文章变更后的缓存失效事件。图片以 MEDIUMBLOB 保存在 MySQL，通过 manager-web 的 /images/{id} 接口读取。Dubbo 使用可配置地址直连，应用不依赖 ZooKeeper 或 ActiveMQ。

## 升级基线与进度

本轮已实现 JDK 21、Spring Framework 7、MyBatis 3.6 / MyBatis-Spring 4.1、Dubbo 3.3 和 Jakarta EE 11 对应 API 的升级基线。开发环境通过 Jetty 12.1 启动，生产部署目标为 Tomcat 11 WAR。依赖的确切版本以根目录 [pom.xml](pom.xml) 为准。

页面层已迁为 Thymeleaf 3.1.5，三个 Web 模块的模板统一位于 src/main/webapp/WEB-INF/templates/。Controller 继续通过 Model 传递页面数据，文章管理、上传和登录注册保留原有 AJAX 接口。16 个模块 clean install 与 40 项测试全部通过，并完成六个 WAR 的真实启动、页面与 Chrome 文章操作检查。模板配置和验证边界见 [Thymeleaf 页面说明](docs/thymeleaf.md)。

MySQL 和 Redis 连接信息通过 DBX 的 nas-mysql、nas-redis 获取，Kafka 连接也通过 DBX 检查。**DBX 用于查验连接与辅助测试；Java 应用运行时直接通过 JDBC、Jedis 和 Kafka 客户端访问对应服务。**

DBX 已确认 nas-mysql 为 MySQL 8.1.0，nas-redis 为 Redis 8.6.2 单机。独立 myblog 库及四张表已初始化，未导入旧演示数据，未修改其他业务库。按项目约定，当前组网环境的连接地址、账号和密码统一保存在 myblog-common/src/main/resources/myblog-defaults.properties，随仓库管理。

此前的 JDK 21 升级已完成 16 个 Maven 模块 clean install、29 个测试及六个 WAR 的 15 项真实 HTTP 检查，覆盖当时的 JSP、分页列表、SSO 缺失 token 查询、图片上传与读取、文章新增和删除后的 Kafka 缓存失效。RPC 分页响应使用标准 Map / ArrayList，保留原 JSON 结构并维持 Dubbo STRICT 检查。当前 Thymeleaf 迁移的验证记录见 [Thymeleaf 页面说明](docs/thymeleaf.md)；JDK 基线与部署边界见 [JDK 21 升级说明](docs/upgrade-jdk21.md)。

## 本地构建与启动

安装 JDK 21 和 Maven，确认 Maven 使用 JDK 21。macOS 先显式设置 JAVA_HOME；本机 Homebrew Maven 在未设置时默认使用 JDK 26，即使 java -version 显示 21，也会被本项目的 JDK 21 检查拒绝：

~~~bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
java -version
mvn -version
mvn clean install
~~~

仓库默认配置已包含 NAS MySQL / Redis 地址、账号密码、测试 Kafka 地址及本机 Dubbo 直连地址，在当前组网环境可直接启动。默认配置位于 [myblog-defaults.properties](myblog-common/src/main/resources/myblog-defaults.properties)。

切换其他环境时，可使用 [myblog-local.example.properties](myblog-local.example.properties) 作为可选覆盖模板，并通过 MYBLOG_CONFIG_LOCATION 指向外部文件，例如 file:/绝对路径/myblog-local.properties。使用仓库默认环境时无需复制模板或设置该变量。

新环境先对独立 myblog 库按顺序执行 [V1](db/migration/V1__initial_schema.sql)、[V2](db/migration/V2__images.sql)、[V3](db/migration/V3__account_security.sql)、[V4](db/migration/V4__article_lifecycle.sql)、[V5](db/migration/V5__article_interactions.sql)、[V6](db/migration/V6__article_comments.sql)，并预创建 Kafka topic；这些步骤不会由应用自动执行。当前 NAS 环境已完成六次迁移。已有环境只执行尚未应用的迁移，各脚本只执行一次。原始 [myblog.sql](myblog.sql) 作为历史资料保留，详见升级说明。

从仓库根目录，在**独立终端**按以下顺序启动六个 WAR。每个终端都需设置 JDK 21：

~~~bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
~~~

先确认业务服务就绪，再启动 Web 工程：

~~~bash
mvn -pl myblog-manager/myblog-manager-service jetty:run
mvn -pl myblog-portal/myblog-portal-service jetty:run
mvn -pl myblog-SingleSignOn/myblog-SingleSignOn-service jetty:run
mvn -pl myblog-manager-web jetty:run
mvn -pl myblog-portal-web jetty:run
mvn -pl myblog-SingleSignOn-web jetty:run
~~~

jetty:run 持续占用当前终端，按 Ctrl+C 停止。Jetty 插件只在 WAR 模块启用。

| 应用 | HTTP 端口 | Dubbo 端口 | 用途 |
| --- | --- | --- | --- |
| myblog-portal-service | 8081 | 20881 | 前台文章服务 |
| myblog-portal-web | 8082 | — | [前台页面](http://localhost:8082/) |
| myblog-manager-service | 8083 | 20880 | 后台文章管理服务 |
| myblog-manager-web | 8084 | — | [后台页面](http://localhost:8084/) |
| myblog-SingleSignOn-service | 8085 | 20882 | 注册、登录和会话服务 |
| myblog-SingleSignOn-web | 8086 | — | [登录页面](http://localhost:8086/page/login) |

生产环境将构建生成的 WAR 部署到 Tomcat 11，配置实际域名、上下文路径、RPC 直连地址和数据库连接。图片由应用提供读取接口，无需单独部署图片目录、对象存储或 Nginx 图片服务器。Jetty Maven 插件用于开发启动。

跨应用页面跳转使用 portal.public-base-url 与 sso.public-base-url，默认分别为 http://localhost:8082 和 http://localhost:8086。部署到其他域名或上下文路径时，连同 image.public-base-url 一并覆盖。

## 官方参考

- [Spring Framework 7](https://docs.spring.io/spring-framework/reference/overview.html)
- [Thymeleaf 与 Spring MVC](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html)
- [Jakarta EE 11](https://jakarta.ee/specifications/platform/11/)
- [Apache Dubbo 3.3.6](https://github.com/apache/dubbo/blob/dubbo-3.3.6/README.md) 与 [直连配置](https://dubbo.apache.org/en/overview/mannual/java-sdk/tasks/framework/more/explicit-target/)
- [Jetty 12.1 Maven 插件](https://jetty.org/docs/jetty/12.1/programming-guide/maven-jetty/jetty-maven-plugin.html)
- [Tomcat 11](https://tomcat.apache.org/tomcat-11.0-doc/index.html)

## 大学时期的运行效果

以下截图保留当年的界面。原始实现和当时的部署方式可在 [university](https://github.com/BeautyQAQ/myblog-parent/tree/university) 分支查看。

**前台文章列表**

![大学时期的前台文章列表](https://s1.ax1x.com/2020/04/24/JB4k5D.jpg)

**文章详情**

![大学时期的文章详情](https://s1.ax1x.com/2020/04/24/JB4V8H.jpg)

**后台界面**

![大学时期的后台界面](https://s1.ax1x.com/2020/04/24/JB4EPe.jpg)

![大学时期的后台文章管理](https://s1.ax1x.com/2020/04/24/JB4FUO.jpg)
