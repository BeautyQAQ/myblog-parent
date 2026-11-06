(function () {
    var form = document.getElementById('logout-form');
    if (!form) return;
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var button = form.querySelector('button');
        var message = document.getElementById('session-message');
        button.disabled = true;
        message.textContent = '';
        fetch(form.action, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]').content}
        }).then(function (response) {
            return response.json();
        }).then(function (result) {
            if (result && result.status === 200) {
                window.location.reload();
                return;
            }
            message.textContent = result && result.msg ? result.msg : '退出失败，请重试';
        }).catch(function () {
            message.textContent = '网络异常，退出失败，请重试';
        }).then(function () {
            button.disabled = false;
        });
    });
}());
