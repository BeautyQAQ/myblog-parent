(function () {
    'use strict';
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) window.location.reload();
    });
    var button = document.getElementById('article-like-button');
    if (!button || button.disabled) return;
    var message = document.getElementById('article-interaction-message');
    var count = document.getElementById('article-like-count');
    var pageviews = document.getElementById('article-pageviews');
    var csrf = document.querySelector('meta[name="csrf-token"]');
    button.addEventListener('click', function () {
        if (button.disabled) return;
        // Submit a desired state; retrying a failed response never toggles an already committed like.
        var desired = button.getAttribute('aria-pressed') !== 'true';
        button.disabled = true;
        message.textContent = '';
        var controller = new AbortController();
        var timeout = window.setTimeout(function () { controller.abort(); }, 15000);
        fetch(button.getAttribute('data-url'), {
            method: 'POST',
            credentials: 'same-origin',
            headers: {'X-CSRF-Token': csrf ? csrf.content : '',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            body: 'liked=' + desired,
            signal: controller.signal
        }).then(function (response) {
            return response.json().then(function (result) {
                if (!response.ok || !result || result.status !== 200 || !result.data) {
                    var error = new Error(result && result.msg ? result.msg : '操作失败，请稍后重试');
                    error.userFacing = true;
                    throw error;
                }
                return result.data;
            });
        }).then(function (data) {
            button.setAttribute('aria-pressed', String(data.liked));
            button.classList.toggle('active', data.liked);
            button.textContent = data.liked ? '取消点赞' : '点赞';
            count.textContent = data.likeCount;
            pageviews.textContent = data.pageviews;
            message.textContent = data.liked ? '已点赞' : '已取消点赞';
        }).catch(function (error) {
            message.textContent = error.userFacing ? error.message
                : '请求未完成，状态暂时无法确认，请重试；重复提交不会重复计数。';
        }).then(function () {
            window.clearTimeout(timeout);
            button.disabled = false;
        });
    });
}());
