-- Run once after V4 against the dedicated myblog database, before starting the new portal.
-- Existing aggregate counts are retained; individual historical likes cannot be reconstructed.
CREATE TABLE `tb_article_view` (
  `article_id` int NOT NULL,
  `visitor_key` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `viewed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`article_id`, `visitor_key`),
  CONSTRAINT `fk_article_view_article` FOREIGN KEY (`article_id`)
    REFERENCES `tb_article` (`article_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tb_article_like` (
  `article_id` int NOT NULL,
  `user_id` bigint NOT NULL,
  `created` datetime(6) NOT NULL,
  PRIMARY KEY (`article_id`, `user_id`),
  CONSTRAINT `fk_article_like_article` FOREIGN KEY (`article_id`)
    REFERENCES `tb_article` (`article_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_article_like_user` FOREIGN KEY (`user_id`)
    REFERENCES `tb_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE `tb_article`
SET `article_pageviews` = GREATEST(COALESCE(`article_pageviews`, 0), 0),
    `article_likecount` = GREATEST(`article_likecount`, 0),
    `article_heat` = LEAST(2147483647, `article_pageviews` + 5 * `article_likecount`);
