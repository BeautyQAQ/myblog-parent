(function () {
    'use strict';
    var root = document.getElementById('article-comments');
    if (!root) return;
    var csrf = document.querySelector('meta[name="csrf-token"]');
    var form = document.getElementById('comment-form');
    var mine = document.getElementById('my-comments');
    var publicPage = 1;
    var ownPage = 1;
    var publicSequence = 0;
    var ownSequence = 0;
    var pendingKey = null;
    var pendingContent = null;
    var statuses = {PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过', DELETED: '已删除'};

    function requestId() {
        if (typeof window.crypto.randomUUID === 'function') return window.crypto.randomUUID();
        // randomUUID is restricted to secure contexts; getRandomValues also works on LAN HTTP.
        var bytes = window.crypto.getRandomValues(new Uint8Array(16));
        bytes[6] = (bytes[6] & 15) | 64;
        bytes[8] = (bytes[8] & 63) | 128;
        var hex = Array.from(bytes, function (value) { return value.toString(16).padStart(2, '0'); }).join('');
        return hex.slice(0, 8) + '-' + hex.slice(8, 12) + '-' + hex.slice(12, 16) + '-'
            + hex.slice(16, 20) + '-' + hex.slice(20);
    }

    function text(tag, value, className) {
        var element = document.createElement(tag);
        element.textContent = value == null ? '' : String(value);
        if (className) element.className = className;
        return element;
    }

    function request(url, data) {
        var controller = new AbortController();
        var timeout = window.setTimeout(function () { controller.abort(); }, 15000);
        var options = {credentials: 'same-origin', signal: controller.signal, cache: 'no-store'};
        if (data !== undefined) {
            options.method = 'POST';
            options.headers = {'X-CSRF-Token': csrf ? csrf.content : '',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'};
            options.body = new URLSearchParams(data).toString();
        }
        return fetch(url, options).then(function (response) {
            return response.json().then(function (result) {
                if (!response.ok || !result || result.status !== 200) {
                    throw new Error(result && result.msg ? result.msg : '评论操作失败，请重试');
                }
                return result.data;
            });
        }).catch(function (error) {
            if (error.name === 'AbortError' || error instanceof TypeError) {
                throw new Error('请求未完成，请重试。你的输入已保留，重复提交不会新增同一条评论。');
            }
            throw error;
        }).finally(function () { window.clearTimeout(timeout); });
    }

    function date(value) {
        var parsed = new Date(value);
        return value && !isNaN(parsed.getTime()) ? parsed.toLocaleString() : '';
    }

    function pagination(target, result, change) {
        target.replaceChildren();
        if (result.pages < 2) return;
        var previous = text('button', '上一页', 'btn btn-outline-secondary btn-sm');
        previous.type = 'button';
        previous.disabled = result.pageNum <= 1;
        previous.addEventListener('click', function () { change(result.pageNum - 1); });
        var next = text('button', '下一页', 'btn btn-outline-secondary btn-sm');
        next.type = 'button';
        next.disabled = result.pageNum >= result.pages;
        next.addEventListener('click', function () { change(result.pageNum + 1); });
        target.append(previous, text('span', result.pageNum + ' / ' + result.pages), next);
    }

    function render(target, comments, own) {
        target.replaceChildren();
        if (!comments.length) {
            target.append(text('p', own ? '你还没有发表过评论。' : '暂无已通过审核的评论。', 'text-muted'));
            return;
        }
        comments.forEach(function (comment) {
            var entry = text('article', '', 'comment-entry');
            entry.append(text('p', (comment.username || '用户') + ' · ' + date(comment.created), 'comment-meta'));
            entry.append(text('p', comment.content, 'comment-content'));
            if (own) {
                entry.append(text('p', statuses[comment.status] || '状态未知', 'comment-status'));
                if (comment.status !== 'DELETED') {
                    var button = text('button', '删除评论', 'btn btn-outline-danger btn-sm');
                    button.type = 'button';
                    var feedback = text('p', '');
                    feedback.setAttribute('role', 'status');
                    button.addEventListener('click', function () {
                        if (button.disabled) return;
                        button.disabled = true;
                        feedback.textContent = '';
                        request(root.getAttribute('data-delete-base') + encodeURIComponent(comment.id) + '/delete', {})
                            .then(function () {
                                feedback.textContent = '评论已删除';
                                loadOwn(ownPage);
                                loadPublic(publicPage);
                            }).catch(function (error) {
                                feedback.textContent = error.message;
                                button.disabled = false;
                            });
                    });
                    entry.append(button, feedback);
                }
            }
            target.append(entry);
        });
    }

    function loadPublic(page) {
        var sequence = ++publicSequence;
        var message = document.getElementById('comments-message');
        message.textContent = '正在加载评论…';
        return request(root.getAttribute('data-public-url') + '?pageNum=' + page + '&pageSize=10')
            .then(function (result) {
                if (sequence !== publicSequence) return;
                if (page > 1 && page > result.pages) return loadPublic(Math.max(1, result.pages));
                publicPage = result.pageNum;
                message.textContent = '';
                document.getElementById('comments-total').textContent = result.total + ' 条已通过评论';
                document.getElementById('article-comment-count').textContent = result.total;
                render(document.getElementById('comments-list'), result.comments, false);
                pagination(document.getElementById('comments-pagination'), result, loadPublic);
            }).catch(function (error) {
                if (sequence !== publicSequence) return;
                document.getElementById('comments-list').replaceChildren();
                document.getElementById('comments-pagination').replaceChildren();
                message.textContent = error.message + ' 可点击“刷新评论”重试。';
            });
    }

    function loadOwn(page) {
        if (!mine) return Promise.resolve();
        var sequence = ++ownSequence;
        var message = document.getElementById('my-comments-message');
        message.textContent = '正在加载你的评论…';
        return request(mine.getAttribute('data-url') + '?pageNum=' + page + '&pageSize=10')
            .then(function (result) {
                if (sequence !== ownSequence) return;
                if (page > 1 && page > result.pages) return loadOwn(Math.max(1, result.pages));
                ownPage = result.pageNum;
                message.textContent = '';
                render(document.getElementById('my-comments-list'), result.comments, true);
                pagination(document.getElementById('my-comments-pagination'), result, loadOwn);
            }).catch(function (error) {
                if (sequence !== ownSequence) return;
                document.getElementById('my-comments-list').replaceChildren();
                document.getElementById('my-comments-pagination').replaceChildren();
                message.textContent = error.message + ' 可点击“刷新评论”重试。';
            });
    }

    if (form) {
        var input = document.getElementById('comment-content');
        var submit = document.getElementById('comment-submit');
        var feedback = document.getElementById('comment-submit-message');
        submit.disabled = false;
        input.addEventListener('input', function () {
            // A changed payload is a new submission; an unchanged retry retains its idempotency key.
            if (input.value !== pendingContent) { pendingKey = null; pendingContent = null; }
        });
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            if (submit.disabled) return;
            var content = input.value.trim();
            if (!content || Array.from(content).length > 1000) {
                feedback.textContent = '请输入 1 到 1000 个字符的评论';
                return;
            }
            if (!pendingKey || pendingContent !== input.value) {
                pendingKey = requestId();
                pendingContent = input.value;
            }
            var original = input.value;
            var key = pendingKey;
            submit.disabled = true;
            feedback.textContent = '';
            request(form.action, {content: content, requestId: key}).then(function (comment) {
                feedback.textContent = comment.status === 'PENDING' ? '评论已提交，等待管理员审核。'
                    : '这条评论已经提交过，当前状态：' + (statuses[comment.status] || comment.status);
                if (input.value === original) input.value = '';
                if (pendingKey === key) { pendingKey = null; pendingContent = null; }
                loadOwn(1);
                loadPublic(publicPage);
            }).catch(function (error) {
                feedback.textContent = error.message;
            }).finally(function () { submit.disabled = false; });
        });
    }
    document.getElementById('comments-refresh').addEventListener('click', function () {
        loadPublic(publicPage);
        loadOwn(ownPage);
    });
    loadPublic(1);
    loadOwn(1);
}());
