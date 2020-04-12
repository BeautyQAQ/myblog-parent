<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" data-th-fragment="header">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <!-- CSRF -->
    <meta name="_csrf" content="${_csrf.token}"/>
    <!-- default header name is X-CSRF-TOKEN -->
    <meta name="_csrf_header" content="${_csrf.headerName}"/>

    <!-- Tether core CSS -->
    <link href="<%=basePath%>/css/tether.min.css" rel="stylesheet">

    <!-- Bootstrap CSS -->
    <link href="<%=basePath%>/css/bootstrap.min.css" rel="stylesheet">

    <!-- Font-Awesome CSS -->
    <link href="<%=basePath%>/css/font-awesome.min.css" rel="stylesheet">

    <!-- NProgress CSS -->
    <link href="<%=basePath%>/css/nprogress.css" rel="stylesheet">

    <!-- thinker-md CSS -->
    <link href="<%=basePath%>/css/thinker-md.vendor.css"rel="stylesheet">

    <!-- bootstrap tags CSS -->
    <link href="<%=basePath%>/css/component-tageditor.css"rel="stylesheet">

    <!-- bootstrap chosen CSS -->
    <link href="<%=basePath%>/css/component-chosen.min.css"rel="stylesheet">

    <!-- toastr CSS -->
    <link href="<%=basePath%>/css/toastr.min.css"rel="stylesheet">

    <!-- jQuery image cropping plugin CSS -->
    <link href="<%=basePath%>/css/cropbox.css"rel="stylesheet">

    <!-- Custom styles -->
    <link href="<%=basePath%>/css/style.css"rel="stylesheet">
    <link href="<%=basePath%>/css/thymeleaf-bootstrap-paginator.css" rel="stylesheet">
    <link href="<%=basePath%>/css/blog.css" rel="stylesheet">
    <%--<link href="css/bootstrap-tagsinput.css" rel="stylesheet">--%>

</head>
<body>
<!--<div th:text="${authentication.name}"></div>-->

<nav class="navbar navbar-inverse bg-inverse navbar-toggleable-md">
    <div class="container">
        <button class="navbar-toggler navbar-toggler-right" type="button" data-toggle="collapse"
                data-target="#navbarsContainer" aria-controls="navbarsExampleContainer" aria-expanded="false"
                aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <a class="navbar-brand" href="http://localhost:8082">NewStarBlog</a>

        <div class="collapse navbar-collapse" id="navbarsContainer">

            <ul class="navbar-nav mr-auto">
                <li class="nav-item">
                    <a class="nav-link" href="/">最新 <span class="sr-only">(current)</span></a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/?orderBy=heat">最热</a>
                </li>
            </ul>


            <%--<div authorize="isAuthenticated()"  class="row" >--%>
                <%--<div class="dropdown">--%>
                    <%--<a class=" dropdown-toggle" href="#" data-toggle="dropdown"><span authentication="name"></span></a>--%>
                    <%--<div class="dropdown-menu" >--%>
                        <%--<a class=" dropdown-item" href="#">个人主页</a>--%>
                        <%--<a class="dropdown-item" href="#" >个人设置</a>--%>
                    <%--</div>--%>
                <%--</div>--%>
                <%--&lt;%&ndash;<div>&ndash;%&gt;--%>
                    <%--&lt;%&ndash;<a href="/u/zhang/blogs/edit" class="btn btn-outline-success my-2 my-sm-0">写博客</a>&ndash;%&gt;--%>
                <%--&lt;%&ndash;</div>&ndash;%&gt;--%>

                <%--<form action="/logout" method="post">--%>
                    <%--<input class="btn btn-outline-success " type="submit" value="退出">--%>
                <%--</form>--%>
            <%--</div>--%>


            <%--<div authorize="isAnonymous()">--%>
                <%--<a href="http://localhost:8086/page/login" class="btn btn-outline-success my-2 my-sm-0" type="submit">登录</a>--%>
                <%--<a href="http://localhost:8086/page/register" class="btn btn-outline-success my-2 my-sm-0" type="submit">注册</a>--%>
            <%--</div>--%>

        </div>

    </div>
</nav>
</body>
</html>