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
		function reg(){
			console.log("点击注册");
			$.ajax({
				url:"/user/register",
				data:$("#registerForm").serialize(),
				dataType: "json",
				type:"POST",
				success:function (data) {
					console.log(data);
					console.log("请求中");
					if (data.status == 200){
						console.log("注册成功");
						location.href = "http://localhost:8086/page/login";
					}
					else if (data.status == 400) {
						console.log("注册失败");
						alert(result.msg);
					}
				}
			});
		}
	</script>
    <div class="container blog-content-container">
 
		<form id="registerForm">
			<h2 class="form-signin-heading">注册</h2>
			
			<div class="form-group col-md-5">
				<label for="username" class="col-form-label">用户名</label>
				<input type="text" class="form-control" id="username" name="username" maxlength="50" placeholder="请输入账号，至少3个字符，至多20个">
	 
			</div>
			<div class="form-group col-md-5">
				<label for="email" class="col-form-label">邮箱</label>
				<input type="email" class="form-control" id="email" name="email" maxlength="50" placeholder="请输入邮箱">
			</div>
			<div class="form-group col-md-5">
				<label for="phone" class="col-form-label">电话</label>
				<input type="text" class="form-control" id="phone" name="phone" maxlength="20" placeholder="请输入电话" >
			</div>
			<div class="form-group col-md-5">
				<label for="password" class="col-form-label">密码</label>
				<input type="password" class="form-control" id="password" name="password" maxlength="30" placeholder="请输入密码，字母或特殊符号和数字结合" >
			</div>
			<div class="form-group">
				<button id="registerBtn" class="btn btn-primary" onclick="reg()">提交</button>
			</div> 
		</form>
    </div> <!-- /container -->
 
	
	<div>
		<jsp:include page="fragments/footer.jsp"></jsp:include>
	</div>
    </body>
</html>