var editor = new window.wangEditor('#articleEditor');
editor.customConfig.menus = ['head', 'bold', 'fontSize', 'fontName', 'italic', 'underline', 'strikeThrough',
    'foreColor', 'backColor', 'link', 'list', 'justify', 'quote', 'image', 'table', 'code', 'undo'];
editor.customConfig.pasteFilterStyle = false;
editor.customConfig.uploadImgMaxSize = 5 * 1024 * 1024;
editor.customConfig.uploadImgServer = articleAdminUrls.uploadUrl;
editor.customConfig.uploadImgHeaders = articleCsrfHeaders();
editor.customConfig.uploadFileName = 'picture';
editor.customConfig.uploadImgHooks = {
    fail: function () { showAdminMessage('图片上传失败，文章内容已保留', true); },
    error: function () { showAdminMessage('网络异常，图片上传失败，文章内容已保留', true); },
    timeout: function () { showAdminMessage('图片上传超时，请重试', true); },
    customInsert: function (insertImg, result) {
        if (!result || result.error !== 0 || !result.url) {
            showAdminMessage(result && result.message ? result.message : '图片上传失败', true);
            return;
        }
        insertImg(result.url);
    }
};
editor.create();
savedArticleSnapshot = articleSnapshot();
var articleSaveInProgress = false;

function saveArticle(status) {
    if (articleSaveInProgress || articleLoadInProgress) return;
    if (!$('#articleTitle').val().trim()) {
        showAdminMessage('请输入文章标题', true);
        $('#articleTitle').focus();
        return;
    }
    var creating = !updateArticleFlag;
    var snapshot = articleSnapshot();
    var data = $('#form').serializeArray();
    data.push({name: 'articleText', value: editor.txt.html()});
    data.push({name: 'articleStatus', value: status});
    if (updateArticleFlag) data.push({name: 'articleId', value: updateArticleId});
    articleSaveInProgress = true;
    $('#saveDraftBtn, #submitBtn, #newArticleBtn').prop('disabled', true);
    showAdminMessage('正在保存，请稍候…', false);
    $.ajax({
        type: 'POST', dataType: 'json', headers: articleCsrfHeaders(),
        url: updateArticleFlag ? articleAdminUrls.updateUrl : articleAdminUrls.addUrl,
        data: $.param(data),
        success: function (result) {
            if (!result || result.status !== 200) {
                showAdminMessage(result && result.msg ? result.msg : '保存失败，当前内容已保留，请重试', true);
                return;
            }
            if (!result.data || !result.data.articleId) {
                showAdminMessage('未能确认保存结果，当前内容已保留。请先检查文章列表，确认未保存后再提交，避免重复新增。', true);
                return;
            }
            updateArticleFlag = true;
            updateArticleId = result.data.articleId;
            currentArticleStatus = result.data.articleStatus || status;
            $('#articleStatusLabel').text(statusName(currentArticleStatus));
            // Text entered while the request was pending remains marked as unsaved.
            savedArticleSnapshot = snapshot;
            showAdminMessage((status === 'PUBLISHED' ? '文章已发布' : '草稿已保存')
                + (articleHasChanges() ? '，还有新的编辑内容未保存' : '，可以继续编辑'), false);
            to_page(currentPage);
        },
        error: function (xhr) {
            var uncertainCreation = creating && (!xhr.status || xhr.status >= 500);
            requestFailure(xhr, uncertainCreation
                ? '未能确认保存结果，当前内容已保留。请先检查文章列表，确认未保存后再提交，避免重复新增。'
                : '保存失败，当前内容已保留，请稍后重试');
        },
        complete: function () {
            articleSaveInProgress = false;
            $('#saveDraftBtn, #submitBtn, #newArticleBtn').prop('disabled', false);
        }
    });
}

$('#saveDraftBtn').on('click', function () { saveArticle('DRAFT'); });
$('#submitBtn').on('click', function () { saveArticle('PUBLISHED'); });
$('#form').on('submit', function (event) { event.preventDefault(); });
$('#previewBtn').on('click', function () {
    $('#previewTitle').text($('#articleTitle').val() || '未命名文章');
    document.getElementById('previewFrame').srcdoc = '<!DOCTYPE html><html><head><meta charset="UTF-8">'
        + '<style>body{font-family:sans-serif;line-height:1.7;padding:16px;overflow-wrap:anywhere}img{max-width:100%}pre{overflow:auto}</style>'
        + '</head><body>' + editor.txt.html() + '</body></html>';
    $('#articlePreview').show();
});
