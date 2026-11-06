(function () {
    'use strict';
    var root = document.getElementById('comment-admin');
    if (!root) return;
    var form = document.getElementById('comment-filter');
    var list = document.getElementById('comment-list');
    var message = document.getElementById('comment-message');
    var summary = document.getElementById('comment-summary');
    var previous = document.getElementById('comment-previous');
    var next = document.getElementById('comment-next');
    var refresh = document.getElementById('comment-refresh');
    var state = {status: form.elements.status.value, articleId: form.elements.articleId.value,
        pageNum: Number(root.dataset.page), pageSize: Number(root.dataset.pageSize)};
    var pages = 0;
    var busy = false;
    var names = {PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', DELETED: '已删除'};
    var actions = {APPROVED: '通过', REJECTED: '拒绝', DELETED: '删除'};

    function text(tag, value, className) {
        var node = document.createElement(tag);
        node.textContent = value == null ? '' : String(value);
        if (className) node.className = className;
        return node;
    }
    function notify(value, failed) {
        message.textContent = value;
        message.className = failed ? 'text-danger' : 'text-success';
    }
    function setBusy(value) {
        busy = value;
        list.setAttribute('aria-busy', String(value));
        Array.from(form.elements).forEach(function (control) { control.disabled = value; });
        Array.from(list.querySelectorAll('button')).forEach(function (control) { control.disabled = value; });
        previous.disabled = value || state.pageNum <= 1;
        next.disabled = value || state.pageNum >= pages;
    }
    function date(value) {
        var parsed = new Date(value);
        return value == null || isNaN(parsed.getTime()) ? '—' : parsed.toLocaleString();
    }
    function request(url, options) {
        var controller = new AbortController();
        // Moderation may wait for the existing Kafka cache invalidation path.
        var timeout = window.setTimeout(function () { controller.abort(); }, 50000);
        options = options || {};
        options.credentials = 'same-origin';
        options.signal = controller.signal;
        return fetch(url, options).then(function (response) {
            return response.json().then(function (result) {
                if (!response.ok || !result || result.status !== 200 || result.data == null) {
                    var error = new Error(result && result.msg ? result.msg : '评论请求失败，请稍后重试');
                    error.userFacing = true;
                    throw error;
                }
                return result.data;
            });
        }).finally(function () { window.clearTimeout(timeout); });
    }
    function render(page) {
        list.replaceChildren();
        pages = page.pages;
        state.pageNum = page.pageNum;
        summary.textContent = '共 ' + page.total + ' 条评论';
        document.getElementById('comment-page').textContent = pages ? '第 ' + page.pageNum + ' / ' + pages + ' 页' : '第 0 / 0 页';
        if (!page.comments.length) list.appendChild(text('p', '没有符合条件的评论。', 'alert alert-info'));
        page.comments.forEach(function (comment) {
            var card = text('article', '', 'comment-card');
            card.appendChild(text('h2', comment.articleTitle + '（文章 ' + comment.articleId + '）'));
            card.appendChild(text('p', comment.username + ' · ' + date(comment.created)
                + ' · ' + (names[comment.status] || comment.status), 'text-muted'));
            card.appendChild(text('p', comment.content, 'comment-content'));
            if (comment.moderatedAt) card.appendChild(text('p', '最近审核：' + date(comment.moderatedAt), 'text-muted'));
            var controls = text('div', '', 'comment-actions');
            if (comment.status !== 'DELETED') {
                Object.keys(actions).forEach(function (target) {
                    if (target === comment.status) return;
                    var button = text('button', actions[target], 'btn ' + (target === 'DELETED' ? 'btn-danger' : 'btn-default'));
                    button.type = 'button';
                    button.addEventListener('click', function () { moderate(comment.id, target); });
                    controls.appendChild(button);
                });
            }
            card.appendChild(controls);
            list.appendChild(card);
        });
    }
    function query() {
        var params = new URLSearchParams({status: state.status, pageNum: state.pageNum, pageSize: state.pageSize});
        if (state.articleId) params.set('articleId', state.articleId);
        return params;
    }
    function loadPage() {
        return request(root.dataset.listUrl + '?' + query()).then(function (page) {
            if (state.pageNum > Math.max(1, page.pages)) {
                state.pageNum = Math.max(1, page.pages);
                return loadPage();
            }
            render(page);
            window.history.replaceState(null, '', form.action + '?' + query());
        });
    }
    function reload(successMessage) {
        if (busy) return;
        setBusy(true);
        notify('', false);
        loadPage().then(function () { notify(successMessage || '', false); })
            .catch(function (error) { notify(error.userFacing ? error.message : '列表加载失败，已保留当前内容，请点击刷新列表重试。', true); })
            .finally(function () { setBusy(false); });
    }
    function moderate(id, status) {
        if (busy) return;
        if (status === 'DELETED' && !window.confirm('删除这条评论？删除后不能恢复。')) return;
        setBusy(true);
        notify('', false);
        var csrf = document.querySelector('meta[name="csrf-token"]');
        request(root.dataset.actionUrl + encodeURIComponent(String(id)) + '/status', {
            method: 'POST', headers: {'X-CSRF-Token': csrf ? csrf.content : '',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            body: 'status=' + encodeURIComponent(status)
        }).then(function () {
            return loadPage().then(function () { notify('评论' + names[status] + '。', false); });
        }).catch(function (error) {
            notify(error.userFacing ? error.message : '操作结果暂时无法确认。请刷新列表，或重试同一操作；不会重复改变评论数。', true);
        }).finally(function () { setBusy(false); });
    }
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        if (busy) return;
        state.status = form.elements.status.value;
        state.articleId = form.elements.articleId.value;
        state.pageNum = 1;
        reload();
    });
    previous.addEventListener('click', function () { if (!busy && state.pageNum > 1) { state.pageNum--; reload(); } });
    next.addEventListener('click', function () { if (!busy && state.pageNum < pages) { state.pageNum++; reload(); } });
    refresh.addEventListener('click', function () { reload(); });
    window.addEventListener('pageshow', function (event) { if (event.persisted) window.location.reload(); });
    reload();
}());
