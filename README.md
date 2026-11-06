# 个人博客系统

这是大学时期写的一个 SOA 个人博客，也是保留下来的一段回忆。

- [university](https://github.com/BeautyQAQ/myblog-parent/tree/university)：保留原 master 的 be53508 快照，保存大学时期的代码和部署说明。
- main：新的默认分支，承载后续升级与修复，保留原来的文章管理、前台展示和服务拆分思路。

## 项目介绍

项目将 Web 页面与业务服务拆成独立工程。后台提供文章管理，前台展示文章列表和详情，SSO 工程包含注册、登录与 token 查询。评论、点赞、浏览量和热度等功能仍属于后续完善方向。

main 保留 Spring MVC、MyBatis、JSP 和 Dubbo 独立服务：MySQL 存储业务数据，Redis 提供文章缓存和登录会话，Kafka 传递文章变更后的缓存失效事件。图片以 MEDIUMBLOB 保存在 MySQL，通过 manager-web 的 /images/{id} 接口读取。Dubbo 改用可配置地址直连，移除应用对 ZooKeeper 和 ActiveMQ 的依赖。

## 升级基线与进度

本轮已实现 JDK 21、Spring Framework 7、MyBatis 3.6 / MyBatis-Spring 4.1、Dubbo 3.3 和 Jakarta EE 11 对应 API 的升级基线。开发环境通过 Jetty 12.1 启动，生产部署目标为 Tomcat 11 WAR。依赖的确切版本以根目录 [pom.xml](pom.xml) 为准。

MySQL 和 Redis 连接信息通过 DBX 的 nas-mysql、nas-redis 获取，Kafka 连接也通过 DBX 检查。**DBX 用于查验连接与辅助测试；Java 应用运行时直接通过 JDBC、Jedis 和 Kafka 客户端访问对应服务。**

DBX 已确认 nas-mysql 为 MySQL 8.1.0，nas-redis 为 Redis 8.6.2 单机。独立 myblog 库及四张表已初始化，未导入旧演示数据，未修改其他业务库。按项目约定，当前组网环境的连接地址、账号和密码统一保存在 myblog-common/src/main/resources/myblog-defaults.properties，随仓库管理。

本轮 16 个 Maven 模块 clean install 成功，29 个测试零失败；六个 WAR 真实启动，15 项 HTTP 检查全部通过，覆盖 JSP、分页列表、SSO 缺失 token 查询、图片上传与读取、文章新增和删除后的 Kafka 缓存失效。RPC 分页响应使用标准 Map / ArrayList，保留原 JSON 结构并维持 Dubbo STRICT 检查。完整注册登录与 Cookie 流程、Tomcat 11 生产部署和历史图片迁移尚未覆盖，详见 [JDK 21 升级说明](docs/upgrade-jdk21.md)。

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

新环境先对独立 myblog 库执行 [V1__initial_schema.sql](db/migration/V1__initial_schema.sql) 和 [V2__images.sql](db/migration/V2__images.sql)，并预创建 Kafka topic；这些步骤不会由应用自动执行。当前 NAS 环境已完成初始化。原始 [myblog.sql](myblog.sql) 作为历史资料保留，详见升级说明。

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

## 官方参考

- [Spring Framework 7](https://docs.spring.io/spring-framework/reference/overview.html)
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
