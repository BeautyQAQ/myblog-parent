# Thymeleaf 页面说明

三个 Web 模块使用 Spring MVC + Thymeleaf 服务端渲染，替代原 JSP。页面、CSS、JavaScript 随 WAR 一起构建和部署，不需要前后端分离、Node.js 或单独的前端构建步骤。JDK 21、Spring Framework 7、Dubbo 和数据服务的运行方式见 [部署说明](upgrade-jdk21.md)。

本文记录页面迁移阶段。后续新增的登录权限、CSRF、草稿 / 发布 / 下架及真实账号验证见 [博客核心功能](core-features.md)。

## 模板与配置

根 pom.xml 管理 Thymeleaf 3.1.5.RELEASE。三个 Web 模块引用 thymeleaf-spring6，并在各自 src/main/resources/spring/springmvc.xml 配置：

- SpringResourceTemplateResolver：从 /WEB-INF/templates/ 读取 .html 模板，UTF-8、HTML 模式。
- SpringTemplateEngine：使用 Spring EL 与现有 Spring MVC Model。
- ThymeleafViewResolver：将 Controller 返回的 index、blog、login 等视图名渲染成 HTML。

模板受 WEB-INF 保护，通过 Controller 访问；不要直接链接到模板文件。公共页头、导航和页脚使用 th:fragment / th:replace，避免嵌套完整 HTML 文档。模板缓存用于重复请求，修改模板后重新启动应用生效。

| Web 模块 | 页面入口 | 模板目录 |
| --- | --- | --- |
| myblog-portal-web | /、/blogs?articleId=… | src/main/webapp/WEB-INF/templates/ |
| myblog-manager-web | /、/article_admin | src/main/webapp/WEB-INF/templates/ |
| myblog-SingleSignOn-web | /page/login、/page/register | src/main/webapp/WEB-INF/templates/ |

原 10 个 JSP（含页眉页脚）、JSP 视图解析器和直接 JSP/JSTL 依赖已移除。原 WEB-INF/jsp 下不可达、缺少数据和片段的历史 HTML 不作为业务页面保留，历史代码仍可从 Git 或 university 分支查阅。

## 页面数据与交互

页面 Controller 继续准备 Model，Thymeleaf 用 th:each、th:if、th:text 等生成 HTML。普通文本通过模板转义；文章正文保留原有富文本 HTML 显示方式。现有文章管理、图片上传、登录注册和 token 查询继续使用原 AJAX / JSON 接口。

本应用内的链接、静态资源和表单地址使用 @{…} 生成，支持应用上下文路径。向 JavaScript 传递服务端值使用 Thymeleaf JavaScript 内联或 HTML data 属性，避免把 JSP EL 直接拼入脚本。

跨应用入口在 myblog-common/src/main/resources/myblog-defaults.properties 提供，可通过 MYBLOG_CONFIG_LOCATION 指向的外部 properties 覆盖：

| 配置键 | 本地默认值 | 用途 |
| --- | --- | --- |
| portal.public-base-url | http://localhost:8082 | SSO 导航和登录默认回跳 |
| sso.public-base-url | http://localhost:8086 | 门户的登录和注册链接 |
| image.public-base-url | http://localhost:8084 | 文章图片读取 |

这些地址应包含实际协议、主机、端口和应用上下文路径，例如 https://example.com/blog。登录 Cookie 仍使用现有 SSO 实现，跨主机共享会话应按实际域名验证。

## 验证

构建与测试使用 JDK 21：

~~~bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
mvn clean install
~~~

2026-11-06 在 JDK 21 下完成以下验证：

- 16 个 Maven 模块执行 mvn clean install 成功，40 项测试全部通过，无失败、错误或跳过。相较迁移前新增 11 项模板渲染和表单测试，使用真实 MVC XML 与替代业务服务，覆盖非根上下文路径、分页、富文本、标题转义、公共片段、登录注册表单绑定与 Cookie。
- 检查三个 Web WAR：包含 9 个业务模板及片段文件，无 .jsp 文件、JSP API 或 JSTL 库残留。
- 六个 WAR 在 Jetty 12.1 / Servlet 6.1 下真实启动；门户首页、后台首页、文章管理、登录与注册页均返回 UTF-8 HTML，引用的静态资源可正常读取。后台分页和 SSO token 查询仍返回原 JSON 结构。
- 本机 Chrome 验证后台 iframe 初始加载、列表与编辑器布局，通过界面完成文章发布、编辑和删除；门户列表与详情正确显示富文本，并在修改、删除后通过 Kafka / Redis 缓存失效展示最新结果。测试文章已按 ID 精确清理，未创建用户或上传图片。
- Chrome 验证登录失败反馈与注册页跳转；最终流程未出现 JavaScript 异常或 HTTP 资源加载失败。检查中修正了后台标签面板固定高度和菜单图片硬编码旧路径。

完整真实账号的注册、登录、跨域 Cookie 共享与会话续期，以及 Tomcat 11 生产部署，仍按 [部署说明](upgrade-jdk21.md) 的边界另行验证。此次登录成功与注册成功的表单 / Cookie 兼容性由替代服务的 MVC 测试覆盖。

## 官方资料

- [Thymeleaf 与 Spring MVC](https://www.thymeleaf.org/doc/tutorials/3.1/thymeleafspring.html)
- [Thymeleaf 模板语法](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html)
