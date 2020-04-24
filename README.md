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

**使用技术 :** Spring、SpringMVC、MyBatis、JSP、Ajax、JSON、MySQL数据库、redis缓存、negix服务器、dubbo发布服务、ActiveMQ消息队列

**项目内容 ：** 后台文章管理模块、前台文章显示模块

**详细信息 ：** 给项目是一个分布式的SOA架构，后台用Spring+MyBatis实现，模块之间的通信用的dubbo，静态资源由nginx服务器访问，redis做缓存服务，将前台界面放入缓存，ActiveMQ消息队列。目前还没有登录功能，后续要增加SSO单点登录系统。

## 项目部署

**1.建表和导入数据：** 数据库用的是MySQL5.7，当前建立3张表，tb_article表，tb_user表，tb_userlog(用户行为记录)表。由于目前系统还不是很完整，所有暂时只对tb_article表进行操作。

**2.导入项目：** 项目下载后，将父工程导入进IntelliJ IDEA Ultimate，我的IDEA版本是2018.3。

**3.项目启动：** 项目导入后，配置maven启动项，此项目是用tomcat插件方式启动。方法如下图：

![](https://s1.ax1x.com/2020/04/24/JBI6D1.jpg)
*1.点击`+`,选择maven*  
*2.点击2号标记的文件夹，选择要配置的项目，此项目中要配置4个项目启动项，分别是：*  
**1.myblog-portal-web**  
**2.myblog-portal**  
**3.myblog-manager-web**  
**4.myblog-manager**  
*注意，是一个一个项目模块单独配置*  
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

****
> 3.项目已经加入redis缓存，需要启动redis，(当然，不启动也没关系，只是没有缓存而已，数据库还是能用的，增加缓存的意义是为了提高系统的性能，应对更大的并发)。这里我只提供Ubuntu下的安装方法：  
1)先更新APT缓存，然后安装redis,输入如下命令
```
sudo apt-get update
sudo apt-get install redis-server

```
> 2)以配置文件启动方式启动redis(默认是后台启动)
```
sudo redis-server /etc/redis/redis.conf #这个是redis.conf的路径，根据你实际路径输入
```
> 没有提示就表示启动成功了！！！
****
> 4.项目已经加入ActiveMQ消息队列，所以还要启动项目已经加入ActiveMQ消息队列，我这边使用的是apache-activemq-5.12.0 Linux版，下载解压好activemq后，进入activemq/bin文件夹下 输入如下命令：
```
@DESKTOP-2BGCAV8:/apache-activemq-5.12.0/bin$ ./activemq start
```
> 输出INFO提示信息就启动成功了，可以测试一下，打开浏览器，输入`http://localhost:8161`,可以进入Apache ActiveMQ!欢迎页面。

**4.运行：** 以上配置部署完成和4个项目模块启动成功后，就可以看看项目情况了，打开浏览器输入`localhost:8082`是后台管理界面，输入`localhost:8084`是前台界面。

### 运行效果

**前台界面，文章列表**

![cmd-markdown-logo](https://s1.ax1x.com/2020/04/24/JB4k5D.jpg)

**文章详情**

![cmd-markdown-logo](https://s1.ax1x.com/2020/04/24/JB4V8H.jpg)

**后台界面**

![cmd-markdown-logo](https://s1.ax1x.com/2020/04/24/JB4EPe.jpg)

![cmd-markdown-logo](https://s1.ax1x.com/2020/04/24/JB4FUO.jpg)
