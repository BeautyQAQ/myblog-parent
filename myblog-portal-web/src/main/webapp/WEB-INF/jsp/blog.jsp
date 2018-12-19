<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<!DOCTYPE html>
<html>
<head>
    <title>${articleWithText.articleTitle}</title>
    <jsp:include page="fragments/header.jsp"></jsp:include>
</head>
<body>
<!-- Page Content -->
<div class="container blog-content-container">

    <div class="row">

        <!-- Blog Entries Column -->
        <div class="col-md-8">

            <!-- Blog Post -->
            <div class="card mb-4">
                <div class="card-block">
                    <h2 class="card-title">
							<span>
			                  <a href="#" title="">
			                    <img src="<%=basePath%>/resources/images/avatar-defualt.jpg" class="blog-avatar-50">
			                  </a>
					        </span>
                        <a href="" class="card-link" title="">
                            ${articleWithText.articleTitle}
                        </a>
                    </h2>
                    <div class="card-text">
                        <a href="" class="card-link" >管理员</a> 发表于 <fmt:formatDate value="${ articleWithText.updated }"  type="both" />
                        <i class="fa fa-eye" aria-hidden="true">${articleWithText.articlePageviews}</i>
                        <i class="fa fa-heart-o" aria-hidden="true">${articleWithText.articleLikecount}</i>
                        <i class="fa fa-comment-o" aria-hidden="true">${articleWithText.articleComment}</i>
                    </div>

                </div>
                <div class="card-block">
                    <h5 >标签：
                        <!--分割tags字符串，组成数组。然后遍历-->
                        <a > <span class="badge badge-default">${articleWithText.articleTag}</span></a>
                    </h5>
                </div>
                <hr>

                <article class="post-content">
                    ${articleWithText.articleText}
                    <%--文章内容--%>
                </article>
                <hr>
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
<script>
    var blogId = [[${blogModel.id}]];
    var blogUrl = '/u/' + [[${blogModel.user.username}]] + '/blogs/'+ [[${blogModel.id}]] ;
</script>
<%--<script src="js/userspace/blog.js" ></script>--%>
</body>
</html>