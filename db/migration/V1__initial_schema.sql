-- Create the current application schema without importing or replacing historical content.
-- Run against a dedicated myblog database; no DROP or DELETE statements.

CREATE TABLE IF NOT EXISTS `tb_article` (
  `article_id` int NOT NULL AUTO_INCREMENT,
  `article_title` varchar(50) NOT NULL DEFAULT '',
  `article_tag` varchar(50) DEFAULT NULL,
  `article_author_id` int NOT NULL DEFAULT '0',
  `article_text` text,
  `article_image_title` varchar(500) DEFAULT NULL,
  `article_pageviews` int DEFAULT '0',
  `article_likecount` int NOT NULL DEFAULT '0',
  `article_comment` int NOT NULL DEFAULT '0',
  `article_heat` int NOT NULL DEFAULT '0',
  `created` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  PRIMARY KEY (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(32) NOT NULL COMMENT '密码，加密存储',
  `phone` varchar(20) DEFAULT NULL COMMENT '注册手机号',
  `email` varchar(50) DEFAULT NULL COMMENT '注册邮箱',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`) USING BTREE,
  UNIQUE KEY `phone` (`phone`) USING BTREE,
  UNIQUE KEY `email` (`email`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `tb_userlog` (
  `Id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL DEFAULT '0' COMMENT '用户id',
  `action` int NOT NULL DEFAULT '0' COMMENT '1-浏览，2-点赞，3-评论',
  `crated` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
