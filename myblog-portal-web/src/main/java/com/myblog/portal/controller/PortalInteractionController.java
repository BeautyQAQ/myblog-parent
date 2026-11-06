package com.myblog.portal.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.portal.pojo.ArticleInteraction;
import com.myblog.portal.service.ArticleInteractionService;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PortalInteractionController {
    private final ArticleInteractionService interactionService;
    private final TokenService tokenService;

    public PortalInteractionController(ArticleInteractionService interactionService, TokenService tokenService) {
        this.interactionService = interactionService;
        this.tokenService = tokenService;
    }

    @PostMapping("/articles/{articleId}/like")
    @ResponseBody
    public ResponseEntity<Result> setLiked(@PathVariable("articleId") int articleId,
            @RequestParam("liked") boolean liked,
            @RequestHeader(value = "X-CSRF-Token", required = false) String csrf,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        if (articleId < 1) return failure(400, "文章参数有误");
        String token = SessionSecurity.readToken(request);
        if (token == null) return failure(401, "请先登录后再点赞");
        if (!SessionSecurity.validCsrf(token, csrf)) return failure(403, "请求验证失败，请刷新页面后重试");
        try {
            Result account = tokenService.getUserByToken(token);
            if (account == null) return failure(503, "暂时无法验证登录状态，请稍后重试");
            if (!Integer.valueOf(200).equals(account.getStatus()) || !(account.getData() instanceof TbUser user)
                    || user.getId() == null || user.getId() < 1) {
                return failure(401, "登录已失效，请重新登录后再点赞");
            }
            ArticleInteraction interaction = interactionService.setLiked(articleId, user.getId(), liked);
            return interaction == null ? failure(404, "文章不存在或尚未发布")
                    : ResponseEntity.ok(Result.ok(interaction));
        } catch (IllegalArgumentException e) {
            return failure(400, "文章参数有误");
        } catch (RuntimeException e) {
            return failure(503, "点赞状态暂时无法确认，请重试；重复提交不会重复计数");
        }
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, TypeMismatchException.class})
    @ResponseBody
    public ResponseEntity<Result> invalidRequest() {
        return failure(400, "请提供有效的文章编号和点赞状态");
    }

    private static ResponseEntity<Result> failure(int status, String message) {
        return ResponseEntity.status(status).body(Result.build(status, message));
    }
}
