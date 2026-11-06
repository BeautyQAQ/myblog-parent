package com.myblog.controller;

import com.myblog.common.utils.Result;
import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.TbUser;
import com.myblog.service.CommentService;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
public class CommentController {
    private static final Set<String> FILTERS = Set.of("PENDING", "APPROVED", "REJECTED", "DELETED", "ALL");
    private static final Set<String> ACTIONS = Set.of("APPROVED", "REJECTED", "DELETED");
    private final CommentService commentService;

    public CommentController(CommentService commentService) { this.commentService = commentService; }

    @GetMapping("/comment_admin")
    public String page(@RequestParam(defaultValue = "PENDING") String status,
                       @RequestParam(required = false) Integer articleId,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize, Model model) {
        validateFilter(status, articleId, pageNum, pageSize);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedArticleId", articleId);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        return "comment_admin";
    }

    @GetMapping("/comment/list")
    @ResponseBody
    public Result list(@RequestParam(defaultValue = "PENDING") String status,
                       @RequestParam(required = false) Integer articleId,
                       @RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize) {
        validateFilter(status, articleId, pageNum, pageSize);
        return Result.ok(commentService.getModerationQueue(status, articleId, pageNum, pageSize));
    }

    @PostMapping("/comment/{commentId}/status")
    @ResponseBody
    public ResponseEntity<Result> moderate(@PathVariable long commentId, @RequestParam String status,
                                           @RequestAttribute TbUser currentUser) {
        if (commentId < 1 || !ACTIONS.contains(status)) throw new IllegalArgumentException("请选择有效的评论和审核操作");
        ArticleComment comment = commentService.moderateComment(commentId, currentUser.getId(), status);
        return comment == null ? ResponseEntity.status(404).body(Result.build(404, "评论不存在"))
                : ResponseEntity.ok(Result.ok(comment));
    }

    private static void validateFilter(String status, Integer articleId, int pageNum, int pageSize) {
        if (!FILTERS.contains(status) || (articleId != null && articleId < 1)
                || pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new IllegalArgumentException("筛选参数有误，每页评论数应为 1 到 50");
        }
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class, TypeMismatchException.class})
    @ResponseBody
    public ResponseEntity<Result> invalid(Exception error) {
        String message = error instanceof IllegalArgumentException ? error.getMessage() : "评论请求参数有误";
        return ResponseEntity.status(400).body(Result.build(400, message));
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseBody
    public ResponseEntity<Result> forbidden() {
        return ResponseEntity.status(403).body(Result.build(403, "当前账号没有评论审核权限"));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity<Result> unavailable() {
        return ResponseEntity.status(503).body(Result.build(503,
                "评论服务暂时不可用。审核结果可能已保存，请刷新列表确认或重试同一操作。"));
    }
}
