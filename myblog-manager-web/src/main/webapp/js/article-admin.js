var articleAdminUrls = document.getElementById('article-admin').dataset;
var updateArticleFlag = false;
var updateArticleId = 0;
var currentArticleStatus = 'DRAFT';
var currentPage = 1;
var savedArticleSnapshot = '';
var articleLoadInProgress = false;

function articleCsrfHeaders() {
    return {'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]').content};
}

function showAdminMessage(message, isError) {
    $('#admin-message, #editor-message').text(message).removeClass('alert-danger alert-success')
        .addClass(isError ? 'alert-danger' : 'alert-success').show();
}

function requestFailure(xhr, fallback) {
    var result = xhr && xhr.responseJSON;
    showAdminMessage(result && result.msg ? result.msg : fallback, true);
}

function articleSnapshot() {
    return JSON.stringify([$('#articleTitle').val(), $('#articleTag').val(), editor.txt.html()]);
}

function articleHasChanges() {
    return savedArticleSnapshot && articleSnapshot() !== savedArticleSnapshot;
}

function showArticleEditor() {
    $('#writeBlog').addClass('on');
    $('#articleManager').removeClass('on');
    $('#articleAttributes').hide();
    $('#articleEdit').show();
}

function statusName(status) {
    return {DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下架'}[status] || '未知状态';
}

function to_page(page) {
    $.ajax({
        url: articleAdminUrls.listUrl,
        data: {pageNum: page},
        type: 'GET',
        dataType: 'json',
        success: function (result) {
            if (!result || result.status !== 200 || !result.data || !Array.isArray(result.data.list)) {
                showAdminMessage(result && result.msg ? result.msg : '文章列表加载失败', true);
                return;
            }
            if (result.data.list.length === 0 && page > 1) {
                to_page(page - 1);
                return;
            }
            build_article_table(result.data.list);
            build_page_info(result.data);
            build_page_nav(result.data);
        },
        error: function (xhr) { requestFailure(xhr, '网络异常，文章列表加载失败，请重试'); }
    });
}

function build_article_table(articles) {
    var body = $('#tbRecord tbody').empty();
    $('#check_all').prop('checked', false);
    if (!articles.length) {
        $('<tr>').append($('<td>').attr('colspan', 11).text('暂无文章，点击“写博客”开始创作')).appendTo(body);
    }
    $.each(articles, function (index, item) {
        var checkbox = $('<input>').attr('type', 'checkbox').addClass('check_item').val(item.articleId);
        var row = $('<tr>').append($('<td>').append(checkbox));
        $.each([item.articleTitle, item.articleTag, statusName(item.articleStatus), item.articlePageviews,
            item.articleLikecount, item.articleComment, item.articleHeat, formatArticleDate(item.created),
            formatArticleDate(item.updated)], function (i, value) { row.append($('<td>').text(value)); });
        var actions = $('<td>');
        $('<button>').attr('type', 'button').addClass('btn btn-primary btn-sm edit_btn')
            .attr('data-id', item.articleId).text('编辑').appendTo(actions);
        if (item.articleStatus === 'PUBLISHED') {
            actions.append(' ');
            $('<button>').attr('type', 'button').addClass('btn btn-warning btn-sm offline_btn')
                .attr('data-id', item.articleId).text('下架').appendTo(actions);
        }
        actions.append(' ');
        $('<button>').attr('type', 'button').addClass('btn btn-danger btn-sm delete_btn')
            .attr('data-id', item.articleId).text('删除').appendTo(actions);
        row.append(actions).appendTo(body);
    });
}

function build_page_info(page) {
    currentPage = page.pageNum || 1;
    $('#page_info_area').text('当前' + currentPage + '页，共有' + page.pages + '页，共有' + page.total + '条记录');
}

function build_page_nav(page) {
    var ul = $('<ul>').addClass('pagination');
    function addLink(label, target, enabled, active) {
        var li = $('<li>').toggleClass('disabled', !enabled).toggleClass('active', !!active);
        var link = $('<a>').attr('href', '#').text(label).appendTo(li);
        link.on('click', function (event) {
            event.preventDefault();
            if (enabled) to_page(target);
        });
        ul.append(li);
    }
    if (page.pages > 0) {
        addLink('首页', 1, page.hasPreviousPage);
        addLink('«', page.pageNum - 1, page.hasPreviousPage);
        $.each(page.navigatepageNums, function (i, number) { addLink(number, number, true, number === page.pageNum); });
        addLink('»', page.pageNum + 1, page.hasNextPage);
        addLink('末页', page.pages, page.hasNextPage);
    }
    $('#page_nav_area').empty().append(ul);
}

function deleteArticles(ids) {
    $.ajax({
        url: articleAdminUrls.deleteUrl + ids.join('-'),
        type: 'POST', dataType: 'json', headers: articleCsrfHeaders(),
        success: function (result) {
            if (!result || result.status !== 200) {
                showAdminMessage(result && result.msg ? result.msg : '删除失败', true);
                return;
            }
            showAdminMessage('文章已删除', false);
            to_page(currentPage);
        },
        error: function (xhr) { requestFailure(xhr, '网络异常，删除失败，请重试'); }
    });
}

$(document).on('click', '.delete_btn', function () {
    if (articleSaveInProgress || articleLoadInProgress) return;
    var title = $(this).closest('tr').find('td').eq(1).text();
    if (confirm('确定删除【' + title + '】？此操作无法撤销。')) deleteArticles([$(this).attr('data-id')]);
});

$(document).on('click', '.offline_btn', function () {
    if (articleSaveInProgress || articleLoadInProgress) return;
    var id = $(this).attr('data-id');
    if (!confirm('确定下架这篇文章？下架后访客将无法阅读。')) return;
    $.ajax({
        url: articleAdminUrls.statusUrl + id,
        type: 'POST', dataType: 'json', headers: articleCsrfHeaders(), data: {articleStatus: 'OFFLINE'},
        success: function (result) {
            if (!result || result.status !== 200) {
                showAdminMessage(result && result.msg ? result.msg : '下架失败', true);
                return;
            }
            if (String(updateArticleId) === id) {
                currentArticleStatus = 'OFFLINE';
                $('#articleStatusLabel').text(statusName(currentArticleStatus));
            }
            showAdminMessage('文章已下架，可以编辑后重新发布', false);
            to_page(currentPage);
        },
        error: function (xhr) { requestFailure(xhr, '网络异常，下架失败，请重试'); }
    });
});

$('#check_all').on('click', function () { $('.check_item').prop('checked', this.checked); });
$(document).on('click', '.check_item', function () {
    $('#check_all').prop('checked', $('.check_item').length === $('.check_item:checked').length);
});
$('#emp_delete_all_btn').on('click', function () {
    if (articleSaveInProgress || articleLoadInProgress) return;
    var ids = $('.check_item:checked').map(function () { return this.value; }).get();
    if (!ids.length) {
        showAdminMessage('请先选择要删除的文章', true);
        return;
    }
    if (confirm('确定删除选中的 ' + ids.length + ' 篇文章？此操作无法撤销。')) deleteArticles(ids);
});

$(document).on('click', '.edit_btn', function () {
    if (articleSaveInProgress || articleLoadInProgress) return;
    if (articleHasChanges() && !confirm('当前编辑内容尚未保存，确定打开另一篇文章？')) return;
    var id = $(this).attr('data-id');
    articleLoadInProgress = true;
    showAdminMessage('正在加载文章，请稍候…', false);
    $('#saveDraftBtn, #submitBtn, #newArticleBtn').prop('disabled', true);
    $.ajax({
        url: articleAdminUrls.detailUrl + id, type: 'GET', dataType: 'json',
        success: function (result) {
            if (!result || result.status !== 200 || !result.data) {
                showAdminMessage(result && result.msg ? result.msg : '文章加载失败', true);
                return;
            }
            var article = result.data;
            updateArticleFlag = true;
            updateArticleId = article.articleId;
            currentArticleStatus = article.articleStatus;
            $('#articleTitle').val(article.articleTitle);
            $('#articleTag').val(article.articleTag);
            editor.txt.html(article.articleText || '');
            $('#articleStatusLabel').text(statusName(currentArticleStatus));
            $('#articlePreview').hide();
            savedArticleSnapshot = articleSnapshot();
            showArticleEditor();
            $('#admin-message, #editor-message').hide();
        },
        error: function (xhr) { requestFailure(xhr, '网络异常，文章加载失败，当前编辑内容已保留'); },
        complete: function () {
            articleLoadInProgress = false;
            $('#saveDraftBtn, #submitBtn, #newArticleBtn').prop('disabled', false);
        }
    });
});

$('#writeBlog').on('click', function () { if (!articleLoadInProgress) showArticleEditor(); });
$('#articleManager').on('click', function () {
    $('#writeBlog').removeClass('on');
    $('#articleManager').addClass('on');
    $('#articleAttributes').show();
    $('#articleEdit').hide();
});
$('#newArticleBtn').on('click', function () {
    if (articleSaveInProgress || articleLoadInProgress) return;
    if (articleHasChanges() && !confirm('当前编辑内容尚未保存，确定新建文章？')) return;
    updateArticleFlag = false;
    updateArticleId = 0;
    currentArticleStatus = 'DRAFT';
    $('#form')[0].reset();
    editor.txt.clear();
    $('#articleStatusLabel').text('新草稿');
    $('#articlePreview').hide();
    savedArticleSnapshot = articleSnapshot();
});

function formatArticleDate(value) {
    if (!value) return '—';
    var date = new Date(value);
    if (isNaN(date.getTime())) return '—';
    function pad(number) { return number < 10 ? '0' + number : String(number); }
    return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' '
        + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds());
}

$(function () { to_page(1); });
window.addEventListener('beforeunload', function (event) {
    if (articleHasChanges()) { event.preventDefault(); event.returnValue = ''; }
});
