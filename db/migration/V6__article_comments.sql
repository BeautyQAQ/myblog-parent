-- Run once after V5, before deploying comment-aware manager and web applications.
CREATE TABLE `tb_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` int NOT NULL,
  `user_id` bigint NOT NULL,
  `request_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `content` text NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `created` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `moderated_by` bigint DEFAULT NULL,
  `moderated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_request` (`user_id`, `request_id`),
  KEY `idx_comment_public` (`article_id`, `status`, `created`, `id`),
  KEY `idx_comment_moderation` (`status`, `created`, `id`),
  KEY `idx_comment_author` (`user_id`, `article_id`, `created`, `id`),
  CONSTRAINT `chk_comment_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'DELETED')),
  CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`)
    REFERENCES `tb_article` (`article_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_author` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`),
  CONSTRAINT `fk_comment_moderator` FOREIGN KEY (`moderated_by`) REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Legacy comment totals had no supporting comment records. Public totals now count only
-- actual APPROVED rows; the initial empty comment table therefore resets them to zero.
UPDATE tb_article a SET article_comment =
  (SELECT COUNT(*) FROM tb_comment c WHERE c.article_id = a.article_id AND c.status = 'APPROVED');
