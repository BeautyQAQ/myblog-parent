<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	pageContext.setAttribute("APP_PATH",request.getContextPath());
%>
<!DOCTYPE html>
<html>
<head>
	<jsp:include page="fragments/header.jsp"></jsp:include>
</head>
    <body>

	<script type="text/javascript">
		function login(){
			console.log("点击登录");
			$.ajax({
				url:"${APP_PATH}/user/login",
				data:$('#loginForm').serialize(),
				dataType: "json",
				type:"POST",
				success:function (data) {
					if (data.status == 200){
						console.log("登录成功");
						location.href = "http://localhost:8082/";
					}
					else if (data.status == 400) {
						alert("账号或密码错误");
						$("#errorText").text("账号或密码错误");
					}
				}
			});
		}
	</script>

	<div class="container blog-content-container">

		<form id="loginForm">
			<h2 >请登录</h2>

			<div class="form-group col-md-5">
				<label for="username" class="col-form-label">账号</label>
				<input type="text" class="form-control" id="username" name="username" maxlength="50" placeholder="请输入账号">

			</div>
			<div class="form-group col-md-5">
				<label for="password" class="col-form-label">密码</label>
				<input type="password" class="form-control" id="password" name="password" maxlength="30" placeholder="请输入密码" >
			</div>
			<div class="form-group col-md-5">
				<a href="/page/register">还没有账号，去注册</a>
			</div>
			<div class="form-group col-md-5">
				<button id="loginBtn" class="btn btn-primary" onclick="login()">登录</button>
			</div>
			<div class=" col-md-5">
				<%--提示错误信息--%>
				<p id="errorText" class="blog-label-error"></p>
			</div>
		</form>
	</div> <!-- /container -->
	
	<div>
		<jsp:include page="fragments/footer.jsp"></jsp:include>
	</div>
    </body>
</html>