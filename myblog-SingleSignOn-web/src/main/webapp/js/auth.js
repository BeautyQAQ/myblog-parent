"use strict";

$(function () {
    $(".auth-form").on("submit", function (event) {
        event.preventDefault();
        var form = $(this);
        var button = form.find("button[type='submit']");
        if (button.prop("disabled")) {
            return;
        }
        var errorText = form.find("#errorText");
        errorText.text("");
        button.prop("disabled", true);
        $.ajax({
            url: form.attr("action"),
            type: "POST",
            data: form.serialize(),
            dataType: "json"
        }).done(function (data) {
            if (data.status === 200) {
                window.location.assign(form.attr("data-success-url"));
            } else {
                errorText.text(data.msg || form.attr("data-error-message"));
            }
        }).fail(function () {
            errorText.text("请求失败，请稍后重试");
        }).always(function () {
            button.prop("disabled", false);
        });
    });
});
