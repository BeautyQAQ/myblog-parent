<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    pageContext.setAttribute("APP_PATH",request.getContextPath());
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <title></title>
    <link rel="stylesheet"/>
    <link rel="stylesheet" href="css/Site.css"/>
    <link rel="stylesheet" href="css/all.css"/>
    <link rel="stylesheet" href="css/font-awesome.min.css"/>
    <link rel="stylesheet" href="css/amazeui.min.css"/>
    <link rel="stylesheet" href="css/admin.css"/>
    <style>

    </style>
<body>
<div class="dvcontent">

    <div>
        <!--tab start-->
        <div class="tabs">
            <div class="hd">
                <ul style="">
                    <li id="articleManager" style="box-sizing: initial;-webkit-box-sizing: initial;" class="on">文章管理</li>
                    <li id="writeBlog" class="" style="box-sizing: initial;-webkit-box-sizing: initial;">写博客</li>
                </ul>
            </div>
            <div class="bd">
                <ul id="articleAttributes" style="display: block;padding: 20px;">
                    <li>
                        <!--分页显示角色信息 start-->
                        <div id="dv1">
                            <table class="table" id="tbRecord">
                                <thead>
                                <tr>
                                    <th><input type="checkbox" id="check_all" /></th>
                                    <th>文章标题</th>
                                    <th>文章标签</th>
                                    <th>浏览量</th>
                                    <th>点赞数</th>
                                    <th>评论数</th>
                                    <th>热度</th>
                                    <th>创建时间</th>
                                    <th>更新时间</th>
                                    <th>操作</th>
                                </tr>
                                </thead>
                                <tbody>
                                <%--<tr>--%>
                                    <%--<td>1</td>--%>
                                    <%--<td>肉类</td>--%>
                                    <%--<td></td>--%>
                                    <%--<td></td>--%>
                                    <%--<td class="edit">--%>
                                        <%--<button onclick="btn_edit(1)"><i class="icon-edit bigger-120"></i>编辑</button>--%>
                                    <%--</td>--%>
                                    <%--<td class="delete">--%>
                                        <%--<button onclick="btn_delete(1)"><i class="icon-trash bigger-120"></i>删除</button>--%>
                                    <%--</td>--%>
                                <%--</tr>--%>
                                </tbody>
                            </table>
                        </div>
                        <%--显示分页数据--%>
                        <div class="row">
                            <%--分页信息--%>
                            <div class="col-md-6" id="page_info_area"></div>
                            <%--分页条信息--%>
                            <div class="col-md-6" id="page_nav_area"></div>
                            &nbsp;&nbsp;<button class="btn btn-danger" id="emp_delete_all_btn">批量删除</button>
                        </div>
                        <!--分页显示角色信息 end-->
                    </li>
                </ul>
                <ul id="articleEdit" class="theme-popbod dform" style="display: none;">
                    <ul class="am-cf admin-main" style="padding-top: 0px;">
                        <!-- content start -->
                        <div class="modal-body">
                            <form id="form" method="post">
                                <div class="form-group">
                                    <label>标题</label>
                                    <input id="articleTitle" class="form-control" type="text" name="articleTitle" placeholder="标题">
                                </div>
                                <label>标签</label>
                                <input id="articleTag" class="form-control" type="text" name="articleTag" placeholder="标签">
                                <div id="articleEditor" name="articleText">
                                    <!--编辑器-->
                                </div>
                                <div class="modal-footer">
                                    <button id="submitBtn" type="button" class="btn btn-primary">发布文章</button>
                                </div>
                            </form>
                        </div>
                        <!-- end-->
                    </ul>
                </ul>
            </div>
        </div>
        <!--tab end-->

    </div>


    <script src="js/jquery-1.12.4.min.js" type="text/javascript"></script>
    <script src="js/plugs/Jqueryplugs.js" type="text/javascript"></script>
    <script src="js/_layout.js"></script>
    <script src="js/plugs/jquery.SuperSlide.source.js"></script>
    <!--引入bootstrp样式-->
    <link href="resources/bootstrap-3.3.7-dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="resources/bootstrap-3.3.7-dist/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="js/wangEditor.min.js"></script>
    <script type="text/javascript" src="js/editor.js"></script>
    <script>
        var updateArticleFlag=false;//文章更新记号
        var updateArticleId=0;//要更新的文章id
        var totalRecord, currentPage; //总记录数
        //1.页面加载完成以后，直接发送ajax请求，要到分页数据
        $(function () {
            to_page(1);
        });

        //ajax方法
        function to_page(pn) {
            $.ajax({
                url: "${APP_PATH}/article/list",
                data:"pageNum="+pn,
                type:"GET",
                success:function (result) {
                    //console.log(result);
                    //1.解析并显示文章数据
                    build_article_table(result);
                    //2.解析并显示分页信息
                    build_page_info(result);
                    //3.解析显示分页条
                    build_page_nav(result);
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
                var createDate=item.created;
                var created = $("<td></td>").append(Format(createDate,"yyyy-MM-dd hh:mm:ss"));
                var updateDate=item.updated;
                var updated = $("<td></td>").append(Format(updateDate,"yyyy-MM-dd hh:mm:ss"));
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
        //解析显示分页信息
        function build_page_info(result) {
            $("#page_info_area").empty();
            $("#page_info_area").append("当前" + result.data.pageNum + "页,共有" + result.data.pages +
                "页,共有" + result.data.total + "条记录");
            totalRecord = result.data.total;
            currentPage = result.data.pageNum;
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
            //先添加首页和前一页
            ul.append(firstPageLi).append(prePageLi);
            var nextPageLi = $("<li></li>").append($("<a></a>").append("&raquo;"));
            var lastPageLi = $("<li></li>").append($("<a></a>").append("末页").attr("href", "#"));
            if (result.data.hasNextPage == false) {
                lastPageLi.addClass("disabled");
                nextPageLi.addClass("disabled");
            } else {
                lastPageLi.click(function () {
                    to_page(result.data.pages);
                });
                nextPageLi.click(function () {
                    to_page(result.data.pageNum + 1);
                });
            }



            //遍历分页条
            $.each(result.data.navigatepageNums, function (index, item) {
                var numLi = $("<li></li>").append($("<a></a>").append(item));
                if (result.data.pageNum == item) {
                    numLi.addClass("active");
                }
                numLi.click(function () {
                    to_page(item);
                });
                ul.append(numLi);
            });

            //最后添加末页和后一页
            ul.append(nextPageLi).append(lastPageLi);

            //把ul添加到nav中
            var navElm = $("<nav></nav>").append(ul);
            navElm.appendTo("#page_nav_area");
        }





        //单个删除
        $(document).on("click", ".delete_btn", function () {
            //alert($(this).parents("tr").find("td:eq(1)").text());
            var title = $(this).parents("tr").find("td:eq(1)").text();
            var articleId = $(this).attr("del-id");
            if (confirm("你确定要删除【" + title + "】?")) {
                $.ajax({
                    url: "${APP_PATH}/article/del/" + articleId,
                    type: "POST",
                    success: function (result) {
                        alert(result.msg);
                        to_page(currentPage);
                    }
                })
            }
        });






        //全选/全不选
        $("#check_all").click(function () {
            //attr获取checked是undefined
            //dom原生的属性，用prop获取属性值，自定义属性，用attr获取
            $(".check_item").prop("checked", $(this).prop("checked"));
        });

        $(document).on("click", ".check_item", function () {
            //判断当前选中元素是否为10个
            var flag = $(".check_item:checked").length == $(".check_item").length;
            $("#check_all").prop("checked", flag);
        });

        //点击全部删除，就执行批量删除
        $("#emp_delete_all_btn").click(function () {
            if ($(".check_item:checked").length > 0) {
                var articleTitles = "";
                var del_idstr = "";
                $.each($(".check_item:checked"), function () {
                    articleTitles += $(this).parents("tr").find("td:eq(1)").text() + ",";
                    del_idstr += $(this).parents("tr").find("td:eq(9)").find("button:eq(1)").attr("del-id") + "-";
                });
                console.log(del_idstr);
                //去除多余的，
                articleTitles = articleTitles.substring(0, articleTitles.length - 1);
                //去除多余的-
                del_idstr = del_idstr.substring(0, del_idstr.length - 1);
                if (confirm("确认删除【" + articleTitles + "】吗？")) {
                    //true表示确认删除，发送ajax请求
                    $.ajax({
                        url: "${APP_PATH}/article/del/" + del_idstr,
                        type: "POST",
                        success: function (result) {
                            $("#check_all").prop("checked",false);
                            alert(result.msg);
                            to_page(currentPage);
                        }
                    });
                }
            }
        });


        //1.在按钮创建之前就绑定单击事件，所以绑定失败。
        //解决办法1.在按钮创建时绑定
        //2.live()方法
        //jQuery新版没有live方法，用on方法替代
        $(document).on("click",".edit_btn",function () {
            updateArticleFlag=true;
            updateArticleId=$(this).attr("edit-id");
            $("#writeBlog").addClass("on");
            $("#articleManager").removeClass("on");
            $("#articleAttributes").toggle();
            $("#articleEdit").toggle();
            //查询信息，显示在富文本编辑框中
            getArticle($(this).attr("edit-id"));
            //将文章id传递给富文本编辑器的更新按钮
            $("#submitBtn").attr("edit-id",$(this).attr("edit-id"));
        });

        function getArticle(articleId){
            $.ajax({
                url:"${APP_PATH}/article/get/"+articleId,
                type:"GET",
                success:function (result) {
                    console.log(result);
                    var articleData=result.data;
                    $("#articleTitle").val(articleData.articleTitle);
                    $("#articleTag").val(articleData.articleTag);
                    editor.txt.html(articleData.articleText);
                }
            });
        }

        $("#articleManager").click(function () {
            updateArticleFlag=false;
            updateArticleId=0;
            $('#form')[0].reset();
            editor.txt.clear();
            $("#writeBlog").removeClass("on");
            $("#articleManager").addClass("on");
            $("#articleAttributes").css("display","block");
            $("#articleEdit").css("display","none");
        });


        function Format(now,mask)
        {
            var d = new Date(now);
            var zeroize = function (value, length)
            {
                if (!length) length = 2;
                value = String(value);
                for (var i = 0, zeros = ''; i < (length - value.length); i++)
                {
                    zeros += '0';
                }
                return zeros + value;
            };

            return mask.replace(/"[^"]*"|'[^']*'|\b(?:d{1,4}|m{1,4}|yy(?:yy)?|([hHMstT])\1?|[lLZ])\b/g, function ($0)
            {
                switch ($0)
                {
                    case 'd': return d.getDate();
                    case 'dd': return zeroize(d.getDate());
                    case 'ddd': return ['Sun', 'Mon', 'Tue', 'Wed', 'Thr', 'Fri', 'Sat'][d.getDay()];
                    case 'dddd': return ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'][d.getDay()];
                    case 'M': return d.getMonth() + 1;
                    case 'MM': return zeroize(d.getMonth() + 1);
                    case 'MMM': return ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'][d.getMonth()];
                    case 'MMMM': return ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'][d.getMonth()];
                    case 'yy': return String(d.getFullYear()).substr(2);
                    case 'yyyy': return d.getFullYear();
                    case 'h': return d.getHours() % 12 || 12;
                    case 'hh': return zeroize(d.getHours() % 12 || 12);
                    case 'H': return d.getHours();
                    case 'HH': return zeroize(d.getHours());
                    case 'm': return d.getMinutes();
                    case 'mm': return zeroize(d.getMinutes());
                    case 's': return d.getSeconds();
                    case 'ss': return zeroize(d.getSeconds());
                    case 'l': return zeroize(d.getMilliseconds(), 3);
                    case 'L': var m = d.getMilliseconds();
                        if (m > 99) m = Math.round(m / 10);
                        return zeroize(m);
                    case 'tt': return d.getHours() < 12 ? 'am' : 'pm';
                    case 'TT': return d.getHours() < 12 ? 'AM' : 'PM';
                    case 'Z': return d.toUTCString().match(/[A-Z]+$/);
                    // Return quoted strings with the surrounding quotes removed
                    default: return $0.substr(1, $0.length - 2);
                }
            });
        };


        $(function () {
            $(".tabs").slide({trigger: "click"});
        });
    </script>

</div>
</body>

</html>