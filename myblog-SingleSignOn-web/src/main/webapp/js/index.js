//# sourceURL=index.js

// DOM 加载完再执行
$(document).read(function() {

	to_page(1);
	
	// 最新\最热切换事件
	// $(".nav-item .nav-link").click(function() {
	//
	// 	var url = $(this).attr("url");
	//
	// 	// 先移除其他的点击样式，再添加当前的点击样式
	// 	$(".nav-item .nav-link").removeClass("active");
	// 	$(this).addClass("active");
	//
	// });

});

//ajax方法
function to_page(pn) {
	$.ajax({
		url: "article/list",
		data:"pageNum="+pn,
		type:"GET",
		success:function (result) {
			//console.log(result);
			//1.解析并显示文章数据
			//build_article_table(result);
			//2.解析显示分页条
			//build_page_nav(result);
		}
	});
}

//解析并显示文章数据
function build_article_table(result) {
	$("#tbRecord tbody").empty();
	var articleList = result.data.list;
	$.each(articleList, function (index, item) {
		var checkBoxTd = $("<td><input type='checkbox' class='check_item'/></td>");
		var articleTitle = $("<td></td>").append(item.articleTitle);
		var articleTag = $("<td></td>").append(item.articleTag);
		var articlePageviews = $("<td></td>").append(item.articlePageviews);
		var articleLikecount = $("<td></td>").append(item.articleLikecount);
		var articleComment = $("<td></td>").append(item.articleComment);
		var articleHeat = $("<td></td>").append(item.articleHeat);
		var created = $("<td></td>").append(item.created);
		var updated = $("<td></td>").append(item.updated);
		var editBtn = $("<button></button>").addClass("btn btn-primary btn-sm edit_btn").append($(
			"<span></span>").addClass("glyphicon glyphicon-pencil")).append("编辑");
		//给编辑按钮添加一个属性，保存文章id
		editBtn.attr("edit-id", item.articleId);
		var delBtn = $("<button></button>").addClass("btn btn-danger btn-sm delete_btn").append($(
			"<span></span>").addClass("glyphicon glyphicon-trash")).append("删除");
		//给删除按钮添加一个属性，保存文章id
		delBtn.attr("del-id", item.articleId);
		var btnTd = $("<td></td>").append(editBtn).append(" ").append(delBtn);
		$("<tr></tr>").append(checkBoxTd).append(articleTitle).append(articleTag).append(articlePageviews)
			.append(articleLikecount).append(articleComment).append(articleHeat).append(created).append(updated).append(btnTd)
			.appendTo("#tbRecord tbody");
	});
}

//解析显示分页条
function build_page_nav(result) {
	$("#page_nav_area").empty();
	var ul = $("<ul></ul>").addClass("pagination");
	var firstPageLi = $("<li></li>").append($("<a></a>").append("首页").attr("href", "#"));
	var prePageLi = $("<li></li>").append($("<a></a>").append("&laquo;"));
	if (result.data.hasPreviousPage == false) {
		firstPageLi.addClass("disabled");
		prePageLi.addClass("disabled");
	} else {
		firstPageLi.click(function () {
			to_page(1);
		});
		prePageLi.click(function () {
			to_page(result.data.pageNum - 1);
		});
	}
}