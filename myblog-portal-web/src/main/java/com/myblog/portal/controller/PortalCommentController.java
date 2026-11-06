package com.myblog.portal.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.CommentPage;
import com.myblog.pojo.TbUser;
import com.myblog.service.CommentService;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
public class PortalCommentController {
    private final CommentService comments;
    private final TokenService tokens;

    public PortalCommentController(CommentService comments, TokenService tokens) {
        this.comments = comments;
        this.tokens = tokens;
    }

    @ModelAttribute
    public void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
    }

    @GetMapping("/articles/{articleId}/comments")
    public ResponseEntity<Result> published(@PathVariable int articleId,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return execute(() -> {
            validatePage(articleId, pageNum, pageSize);
            CommentPage page = comments.getPublishedComments(articleId, pageNum, pageSize);
            return page == null ? null : pageView(page, false);
        });
    }

    @GetMapping("/articles/{articleId}/comments/mine")
    public ResponseEntity<Result> mine(@PathVariable int articleId,
            @RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return execute(() -> {
            validatePage(articleId, pageNum, pageSize);
            TbUser user = account(SessionSecurity.readToken(request));
            CommentPage page = comments.getOwnComments(articleId, user.getId(), pageNum, pageSize);
            return page == null ? null : pageView(page, true);
        });
    }

    @PostMapping("/articles/{articleId}/comments")
    public ResponseEntity<Result> submit(@PathVariable int articleId, @RequestParam String content,
            @RequestParam String requestId, HttpServletRequest request) {
        return execute(() -> {
            if (articleId < 1) throw new IllegalArgumentException("文章编号有误");
            TbUser user = writeAccount(request);
            ArticleComment comment = comments.submitComment(articleId, user.getId(), content, requestId);
            return comment == null ? null : commentView(comment, true);
        });
    }

    @PostMapping("/comments/{commentId}/delete")
    public ResponseEntity<Result> delete(@PathVariable long commentId, HttpServletRequest request) {
        return execute(() -> {
            if (commentId < 1) throw new IllegalArgumentException("评论编号有误");
            TbUser user = writeAccount(request);
            ArticleComment comment = comments.deleteOwnComment(commentId, user.getId());
            return comment == null ? null : commentView(comment, true);
        });
    }

    private TbUser writeAccount(HttpServletRequest request) {
        String token = SessionSecurity.readToken(request);
        if (token == null) throw new AccessFailure(401, "请先登录后再操作评论");
        if (!SessionSecurity.validCsrf(token, request.getHeader("X-CSRF-Token"))) {
            throw new AccessFailure(403, "请求验证失败，请刷新页面后重试");
        }
        return account(token);
    }

    private TbUser account(String token) {
        if (token == null) throw new AccessFailure(401, "请先登录后再操作评论");
        Result result = tokens.getUserByToken(token);
        if (result == null) throw new AccessFailure(503, "暂时无法验证登录状态，请稍后重试");
        if (!Integer.valueOf(200).equals(result.getStatus()) || !(result.getData() instanceof TbUser user)
                || user.getId() == null || user.getId() < 1) {
            throw new AccessFailure(401, "登录已失效，请重新登录");
        }
        return user;
    }

    private static void validatePage(int articleId, int pageNum, int pageSize) {
        if (articleId < 1 || pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new IllegalArgumentException("文章或分页参数有误，每页评论数应为 1 到 50");
        }
    }

    // Public responses do not disclose account IDs or moderator identities.
    private static Map<String, Object> pageView(CommentPage page, boolean own) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("comments", page.getComments().stream().map(comment -> commentView(comment, own)).toList());
        view.put("total", page.getTotal());
        view.put("pageNum", page.getPageNum());
        view.put("pageSize", page.getPageSize());
        view.put("pages", page.getPages());
        return view;
    }

    private static Map<String, Object> commentView(ArticleComment comment, boolean own) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", comment.getId());
        view.put("articleId", comment.getArticleId());
        view.put("username", comment.getUsername());
        view.put("content", comment.getContent());
        view.put("created", comment.getCreated());
        if (own) {
            view.put("status", comment.getStatus());
            view.put("moderatedAt", comment.getModeratedAt());
        }
        return view;
    }

    private static ResponseEntity<Result> execute(Supplier<Object> operation) {
        try {
            Object data = operation.get();
            return data == null ? failure(404, "文章或评论不存在，或当前不可访问")
                    : ResponseEntity.ok(Result.ok(data));
        } catch (AccessFailure e) {
            return failure(e.status, e.getMessage());
        } catch (IllegalArgumentException e) {
            return failure(400, e.getMessage());
        } catch (RuntimeException e) {
            return failure(503, "评论服务暂时不可用，请重试；重复提交同一条评论不会重复发布");
        }
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, TypeMismatchException.class})
    public ResponseEntity<Result> invalidRequest() {
        return failure(400, "请提供有效的评论内容、请求标识和分页参数");
    }

    private static ResponseEntity<Result> failure(int status, String message) {
        return ResponseEntity.status(status).body(Result.build(status, message));
    }

    private static final class AccessFailure extends RuntimeException {
        private final int status;
        private AccessFailure(int status, String message) { super(message); this.status = status; }
    }
}
