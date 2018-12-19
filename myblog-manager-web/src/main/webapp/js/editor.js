/**
 * Created by Zhang on 18-11-7.
 */
var E = window.wangEditor
var editor = new E('#articleEditor')
// 自定义菜单配置
editor.customConfig.menus = [
    'head',  // 标题
    'bold',  // 粗体
    'fontSize',  // 字号
    'fontName',  // 字体
    'italic',  // 斜体
    'underline',  // 下划线
    'strikeThrough',  // 删除线
    'foreColor',  // 文字颜色
    'backColor',  // 背景颜色
    'link',  // 插入链接
    'list',  // 列表
    'justify',  // 对齐方式
    'quote',  // 引用
    'image',  // 插入图片
    'table',  // 表格
    'code',  // 插入代码
    'undo',  // 撤销
    //'redo'  // 重复
]
//设置粘贴时是否原有样式
editor.customConfig.pasteFilterStyle = false
editor.customConfig.pasteTextHandle = function (content) {
    // content 即粘贴过来的内容（html 或 纯文本），可进行自定义处理然后返回
    return content + '<br>'
}
//上传图片保存到服务器
editor.customConfig.uploadImgMaxSize = 4 * 1024 * 1024
editor.customConfig.uploadImgServer = '/upload'
editor.customConfig.uploadFileName = 'picture'
editor.customConfig.uploadImgHooks = {
    before: function (xhr, editor, files) {
        // 图片上传之前触发
        // xhr 是 XMLHttpRequst 对象，editor 是编辑器对象，files 是选择的图片文件

        // 如果返回的结果是 {prevent: true, msg: 'xxxx'} 则表示用户放弃上传
        // return {
        //     prevent: true,
        //     msg: '放弃上传'
        // }
    },
    success: function (xhr, editor, result) {
        // 图片上传并返回结果，图片插入成功之后触发
        // xhr 是 XMLHttpRequst 对象，editor 是编辑器对象，result 是服务器端返回的结果
    },
    fail: function (xhr, editor, result) {
        // 图片上传并返回结果，但图片插入错误时触发
        // xhr 是 XMLHttpRequst 对象，editor 是编辑器对象，result 是服务器端返回的结果
    },
    error: function (xhr, editor) {
        // 图片上传出错时触发
        // xhr 是 XMLHttpRequst 对象，editor 是编辑器对象
    },
    timeout: function (xhr, editor) {
        // 图片上传超时时触发
        // xhr 是 XMLHttpRequst 对象，editor 是编辑器对象
    },

    // 如果服务器端返回的不是 {errno:0, data: [...]} 这种格式，可使用该配置
    // （但是，服务器端返回的必须是一个 JSON 格式字符串！！！否则会报错）
    customInsert: function (insertImg, result, editor) {
        // 图片上传并返回结果，自定义插入图片的事件（而不是编辑器自动插入图片！！！）
        // insertImg 是插入图片的函数，editor 是编辑器对象，result 是服务器端返回的结果

        // 举例：假如上传图片成功后，服务器端返回的是 {url:'....'} 这种格式，即可这样插入图片：
        var url = result.url;
        insertImg(url);

        // result 必须是一个 JSON 格式字符串！！！否则报错
    }
}
//打开Debug模式
editor.customConfig.debug = location.href.indexOf('wangeditor_debug_mode=1') > 0
editor.create()

//上传事件
$(document).ready(function(){
    $("#submitBtn").click(function(){
        var articleText = editor.txt.html();
        var articledata = $("#form").serialize();
        articledata = articledata +"&" +"articleText=" + encodeURIComponent(articleText);
        //判断updateArticleFlag的值，为true则表示更新文章
        if (!updateArticleFlag) {
            $.ajax({
                type:'POST',
                datatype:'json',
                url:"/article/add",
                data:articledata,
                success:function(result){
                    if (result.status==200){
                        alert("发布成功");
                    }
                    if (result.status==400) {
                        alert(result.msg);
                    }
                    location.reload();
                }
            });
        }
         else if (updateArticleFlag) {
             if (0!=updateArticleId) {
                 articledata=articledata+"&articleId="+updateArticleId;
                 $.ajax({
                     type:'POST',
                     datatype:'json',
                     url:"/article/update",
                     data:articledata,
                     success:function(result){
                         if (result.status==200){
                             alert("更新成功");
                         }
                         if (result.status==400) {
                             alert(result.msg);
                         }
                         location.reload();
                     }
                 });
             }
        }

        return false;
        });
});