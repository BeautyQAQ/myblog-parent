<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    pageContext.setAttribute("APP_PATH",request.getContextPath());
%>
<!DOCTYPE html>
<html>
<head>
    <title>NewStarBlog</title>
    <jsp:include page="fragments/header.jsp"></jsp:include>
    <!-- Font-Awesome CSS -->
    <link href="css/font-awesome.css" rel="stylesheet">
</head>
<body>

<!-- Page Content -->
<div class="container blog-content-container">

    <div class="row">

        <!-- Blog Entries Column -->
        <div class="col-md-8">

            <!-- Blog Post -->
            <div id="mainContainer">
                <div id="mainContainerRepleace">
                    <c:forEach var="item" items="${articleList}">
                    <div class="card mb-4">
                        <div class="card-block">
                            <h2 class="card-title">
		                 		<span>
				                  <a href="#" id="userAvatar" title="${item.articleAuthorId}">
				                    <img src="resources/images/avatar-defualt.jpg" class="blog-avatar-50">
				                  </a>
						        </span>
                                <a id="articleTitle" href="/blogs/?articleId=${item.articleId}" class="card-link">${item.articleTitle}</a>
                            </h2>
                            <div id="articleText" class="card-text">
                                    ${item.articleText}
                            </div>
                            <div class="card-text">
                                <%--显示作者昵称--%>
                                <a id="userAvatar2" href=""  class="card-link">管理员</a>
                                <span id="createTime">发表于 <fmt:formatDate value="${item.updated}"  type="both" /></span>
                                <i id="articlePageviews" class="fa fa-eye" aria-hidden="true">${item.articlePageviews}</i>
                                <i id="articleLikecount" class="fa fa-heart-o" aria-hidden="true">${item.articleLikecount}</i>
                                <i id="articleComment" class="fa fa-comment-o" aria-hidden="true">${item.articleComment}</i>
                            </div>
                        </div>
                    </div>
                    </c:forEach>

                    <%--分页--%>
                    <%--分页条信息--%>
                    <div class="col-md-6" id="page_nav_area">
                        <nav>
                            <ul class="pagination">
                                <c:if test="${!hasPreviousPage}">
                                    <li class="disabled"><a href="/">首页</a></li>
                                    <li class="disabled"><a href="/?pageNum=${pageNum-1}">&laquo;</a></li>
                                </c:if>
                                <c:if test="${hasPreviousPage}">
                                    <li><a href="/">首页</a></li>
                                    <li><a href="/?pageNum=${pageNum-1}">&laquo;</a></li>
                                </c:if>

                                <c:forEach var="item" items="${navigatepageNums}">
                                    <c:if test="${item==pageNum}">
                                        <li class="active"><a href="#">${item}</a></li>
                                    </c:if>
                                    <c:if test="${item!=pageNum}">
                                        <li><a href="/?pageNum=${item}">${item}</a></li>
                                    </c:if>
                                </c:forEach>

                                <c:if test="${!hasNextPage}">
                                    <li class="disabled"><a>&raquo;</a></li>
                                    <li class="disabled"><a href="#">末页</a></li>
                                </c:if>
                                <c:if test="${hasNextPage}">
                                    <li><a href="/?pageNum=${pageNum+1}">&raquo;</a></li>
                                    <li><a href="/?pageNum=${pages}">末页</a></li>
                                </c:if>
                            </ul>
                        </nav>
                    </div>
                    <div>

                    </div>
                </div>
            </div>

        </div>

    </div>
    <!-- /.row -->

</div>
<!-- /.container -->


<div>
    <jsp:include page="fragments/footer.jsp"></jsp:include>
</div>

<!-- JavaScript -->
<!--引入bootstrp样式-->
<link href="resources/bootstrap-3.3.7-dist/css/bootstrap.min.css" rel="stylesheet">
<script src="resources/bootstrap-3.3.7-dist/js/bootstrap.min.js"></script>
<script>
</script>
</body>
</html>