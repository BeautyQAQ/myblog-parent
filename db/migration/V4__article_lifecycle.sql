-- Apply once after V1, V2 and V3, before deploying lifecycle-aware applications.
-- Existing articles retain their public visibility; future inserts default to drafts.
ALTER TABLE tb_article
    ADD COLUMN article_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN published_at DATETIME DEFAULT NULL,
    ADD CONSTRAINT chk_article_status CHECK (article_status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')),
    ADD INDEX idx_article_publication (article_status, published_at, article_id);

UPDATE tb_article
SET article_status = 'PUBLISHED',
    published_at = COALESCE(created, updated, CURRENT_TIMESTAMP);
