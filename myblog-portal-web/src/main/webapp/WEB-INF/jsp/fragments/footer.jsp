<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
</head>
<header class="bs-docs-nav navbar navbar-static-top" id="top"></header>
<body>

<footer class="blog-footer bg-inverse" data-th-fragment="footer">
    <%--回顶部--%>
	<a id="goToTop" href="#top">
		<b style="font-size: 25px" class="back-to-top" aria-hidden="true">↑</b>
	</a>
    <div class="container">
 		<p class="m-0 text-center text-white">© 2018 <a href="#">myBlog</a></p>
    </div>
	
   	<!-- JavaScript -->
    <script src="<%=basePath%>/js/jquery-3.1.1.min.js"></script>
    <script src="<%=basePath%>/js/jquery.form.min.js"></script>
    <script src="<%=basePath%>/js/tether.min.js"></script>
    <script src="<%=basePath%>/js/bootstrap.min.js"></script>
    <script src="<%=basePath%>/js/nprogress.js"></script>
    <%--<script src="/js/bootstrap-tagsinput.js"></script>--%>
    <script src="<%=basePath%>/js/thinker-md.vendor.min.js"></script>
    <script src="<%=basePath%>/js/jquery.tag-editor.min.js"></script>
    <script src="<%=basePath%>/js/chosen.jquery.js"></script>
    <script src="<%=basePath%>/js/toastr.min.js"></script>
    <script src="<%=basePath%>/js/cropbox.js"></script>
    <script src="<%=basePath%>/js/thymeleaf-bootstrap-paginator.js"></script>
    <script src="<%=basePath%>/js/catalog-generator.js"></script>
    <script src="<%=basePath%>/js/main.js"></script>
    <script src="<%=basePath%>/js/my.js"></script>
    <!--<script src="/js/blogedit.js" th:src="@{/js/blogedit.js}"></script>-->
 </footer>

</body>
</html>