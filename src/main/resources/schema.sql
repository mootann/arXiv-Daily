CREATE DATABASE arxiv_daily CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE arxiv_daily;

-- 创建用户表
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建论文表
CREATE TABLE `arxiv_papers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `arxiv_id` VARCHAR(20) NOT NULL UNIQUE,
    `title` TEXT NOT NULL,
    `summary` TEXT,
    `authors` TEXT,
    `published_date` DATE,
    `updated_date` DATE,
    `primary_category` VARCHAR(50),
    `categories` VARCHAR(255),
    `pdf_url` VARCHAR(500),
    `latex_url` VARCHAR(500),
    `arxiv_url` VARCHAR(500),
    `doi` VARCHAR(100),
    `version` INT DEFAULT 1,
    `comment` TEXT,
    `journal_ref` TEXT,
    `github_url` VARCHAR(500),
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建用户收藏论文表
CREATE TABLE `user_collects` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `arxiv_id` VARCHAR(20) NOT NULL,
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_arxiv_collect` (`user_id`, `arxiv_id`),
    INDEX `idx_user_id` (`user_id`)
);

-- 创建用户关注分类表
CREATE TABLE `user_follow_categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `category` VARCHAR(50) NOT NULL,
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_category` (`user_id`, `category`),
    INDEX `idx_user_id` (`user_id`)
);

-- 创建用户点赞论文表
CREATE TABLE `user_likes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `arxiv_id` VARCHAR(20) NOT NULL,
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_arxiv_like` (`user_id`, `arxiv_id`),
    INDEX `idx_user_id` (`user_id`)
);

-- 创建论文评论表
CREATE TABLE `paper_comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `arxiv_id` VARCHAR(20) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `content` TEXT NOT NULL,
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_arxiv_id` (`arxiv_id`),
    INDEX `idx_user_id` (`user_id`)
);