# 个人博客系统

------

此项目是一个基于SOA架构的项目，目前为止完成的功能有：

> * 后台文章管理
> * 前台文章显示

后续将增加：

> * 单点登录
> * 用户评论，点赞，浏览量，热度增加等

## 什么是 SOA架构

**SOA：Service Oriented Architecture ：** 面向服务的架构。也就是把工程拆分成服务层、表现层两个工程。服务层中包含业务逻辑，只需要对外提供服务即可。表现层只需要处理和页面的交互，业务逻辑都是调用服务层的服务来实现。

**使用技术 :** Spring、SpringMVC、MyBatis、JSP、Ajax、JSON、MySQL数据库、redis缓存、negix服务器、dubbo发布服务

**项目内容 ：** 后台文章管理模块、前台文章显示模块

**详细信息 ：** 给项目是一个分布式的SOA架构，后台用Spring+MyBatis实现，模块之间的通信用的dubbo，静态资源由nginx服务器访问，redis做缓存服务，将前台界面放入缓存。后续会增加ActiveMQ做消息队列，并且目前还没有登录功能，后续要增加SSO单点登录系统。

## 项目部署

**1.建表和导入数据：** 数据库用的是MySQL5.7，当前建立3张表，tb_article表，tb_user表，tb_userlog(用户行为记录)表。由于目前系统还不是很完整，所有暂时只对tb_article表进行操作。

**2.导入项目：** 项目下载后，将父工程导入进IntelliJ IDEA Ultimate，我的IDEA版本是2018.3。

**3.项目启动：** 项目导入后，配置maven启动项，此项目是用tomcat插件方式启动。方法如下图：

![](http://ww1.sinaimg.cn/large/85b66755gy1fyd57fv14qj21an0q90xc.jpg)
*1.点击`+`,选择maven*
*2.点击2号标记的文件夹，选择要配置的项目，此项目中要配置4个项目启动项，分别是：*
    **1.myblog-portal-web**
    **2.myblog-portal**
    **3.myblog-manager-web**
    **4.myblog-manager**
*当然，是一个一个项目模块单独配置*
*3.在3号输入框中输入`clean tomcat7:run`*
*4.然后就可以启动了，注意要先启动后台模块，再启动前台web模块哦！！！*

> 1.项目想要成功启动，需要安装zookeeper注册中心(因为此项目用到了dubbo服务治理框架，需要zookeeper)，我用的是zookeeper 3.4.10 Linux版(Ubuntu 16.4)，下载安装好zookeeper之后，进入zookeeper/bin文件夹下 输入如下命令：

```
@DESKTOP-2BGCAV8:/zookeeper-3.4.10/bin$ ./zkServer.sh start
```
> 提示Starting zookeeper ... STARTED 就启动完成啦
****
> 2.要想访问项目的静态资源(图片等)，还需要启动nginx(因为此项目是由nginx作为静态资源服务器，访问图片需要nginx，我用的是windows下的nginx-1.14.1版本，之所以不用linux版是windows向Linux传文件的时候，需要其他的工具，例如FTP服务等等),下载好nginx后还需要配置nginx-1.14.1\conf\nginx.conf文件，在server配置中添加一个location配置：
```
    location /images{
        root  D:\img; #需要在创建D:\img\images目录作为图片上传文件夹
                      #访问图片的url为：http://127.0.0.1:80/images/xxxxx.jpg
    }
```
>配置完成后启动nginx

**4.运行：** 4个项目模块启动成功后，`localhost:8082`是后台管理界面，`localhost:8084`是前台界面。

### 运行效果

**前台界面，文章列表**

![cmd-markdown-logo](http://wx4.sinaimg.cn/large/85b66755gy1fycj727h2rj21gr0rjwi5.jpg)

**文章详情**

![cmd-markdown-logo](http://wx4.sinaimg.cn/large/85b66755gy1fycj727h2rj21gr0rjwi5.jpg)

**后台界面**

![cmd-markdown-logo](http://wx2.sinaimg.cn/large/85b66755gy1fycj8ayl78j21gs0p2q7s.jpg)

![cmd-markdown-logo](http://wx3.sinaimg.cn/large/85b66755gy1fycj8g17fbj21gv0ozdhr.jpg)
