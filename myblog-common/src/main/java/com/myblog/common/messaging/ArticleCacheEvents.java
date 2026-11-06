package com.myblog.common.messaging;

/** Shared wire contract for article cache invalidation. */
public final class ArticleCacheEvents {
    public static final String KEY = "article-list";
    public static final String INVALIDATE = "delete_cache";

    private ArticleCacheEvents() {
    }
}
