package com.myblog.controller;

import com.myblog.common.utils.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ManagerErrors {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerErrors.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> invalid(IllegalArgumentException error) {
        int status = "文章不存在".equals(error.getMessage()) ? 404 : 400;
        return ResponseEntity.status(status).body(Result.build(status, error.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> uploadTooLarge() {
        return ResponseEntity.status(413).body(Result.build(413, "图片不能超过5MB"));
    }

    @ExceptionHandler({IllegalStateException.class, RpcException.class})
    public ResponseEntity<Result> serviceFailure(RuntimeException error) {
        LOGGER.error("Management service request failed", error);
        String message = error.getMessage() != null && error.getMessage().startsWith("文章变更已提交")
                ? "文章变更已保存，但前台缓存更新失败。请先刷新文章列表确认，避免重复新增。"
                : "服务暂时不可用。请保留编辑内容并先检查文章列表，确认保存结果后再操作。";
        return ResponseEntity.status(503).body(Result.build(503, message));
    }
}
