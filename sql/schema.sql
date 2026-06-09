CREATE DATABASE IF NOT EXISTS `pullcat`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `pullcat`;

CREATE TABLE IF NOT EXISTS `repo`
(
    `full_name`   varchar(191) NOT NULL COMMENT 'owner/repo',
    `owner`       varchar(100) NOT NULL COMMENT '仓库 owner',
    `repo`        varchar(100) NOT NULL COMMENT '仓库名',
    `description` text         NULL COMMENT '仓库描述',
    `stars`       int          NULL COMMENT 'Star 数',
    `language`    varchar(64)  NULL COMMENT '主语言',
    `added_at`    datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '添加时间',
    PRIMARY KEY (`full_name`),
    KEY `idx_repo_owner_repo` (`owner`, `repo`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Pullcat 仓库表';

CREATE TABLE IF NOT EXISTS `rule`
(
    `id`         varchar(64)  NOT NULL COMMENT '规则 ID',
    `repo_owner` varchar(100) NOT NULL COMMENT '仓库 owner',
    `repo_name`  varchar(100) NOT NULL COMMENT '仓库名',
    `type`       varchar(64)  NOT NULL COMMENT '规则类型枚举',
    `pattern`    text         NULL COMMENT '匹配模式',
    `severity`   varchar(32)  NOT NULL COMMENT '严重级别枚举',
    `name`       varchar(255) NOT NULL COMMENT '规则名称',
    `message`    text         NULL COMMENT '问题提示',
    `suggestion` text         NULL COMMENT '修复建议',
    `enabled`    tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_repo` (`repo_owner`, `repo_name`),
    KEY `idx_rule_repo_enabled` (`repo_owner`, `repo_name`, `enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Pullcat 仓库规则表';

CREATE TABLE IF NOT EXISTS `user`
(
    `id`           varchar(64)  NOT NULL COMMENT '用户 ID',
    `github_login` varchar(100) NOT NULL COMMENT 'GitHub 登录名',
    `github_id`    bigint       NULL COMMENT 'GitHub 数字 ID',
    `avatar_url`   varchar(512) NULL COMMENT '头像地址',
    `email`        varchar(255) NULL COMMENT '邮箱',
    `created_at`   datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_github_login` (`github_login`),
    UNIQUE KEY `uk_user_github_id` (`github_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Pullcat 用户表';

CREATE TABLE IF NOT EXISTS `review`
(
    `id`                   varchar(64)  NOT NULL COMMENT '审查 ID',
    `pr_url`               varchar(512) NOT NULL COMMENT 'PR 地址',
    `repository_full_name` varchar(191) NULL COMMENT '仓库全名 owner/repo',
    `user_id`              varchar(100) NULL COMMENT '创建用户登录名',
    `status`               varchar(32)  NULL COMMENT '会话状态',
    `published_comment_id` bigint       NULL COMMENT '已发布评论 ID',
    `created_at`           datetime(6)  NOT NULL COMMENT '创建时间',
    `completed_at`         datetime(6)  NULL COMMENT '完成时间',
    `updated_at`           datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    `snapshot_json`        longtext     NOT NULL COMMENT '完整会话快照 JSON',
    PRIMARY KEY (`id`),
    KEY `idx_review_repo_created` (`repository_full_name`, `created_at`),
    KEY `idx_review_user_created` (`user_id`, `created_at`),
    KEY `idx_review_status_created` (`status`, `created_at`),
    KEY `idx_review_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Pullcat 审查会话表';

CREATE TABLE IF NOT EXISTS `repo_auto_publish`
(
    `full_name`  varchar(191) NOT NULL COMMENT 'owner/repo',
    `owner`      varchar(100) NOT NULL COMMENT '仓库 owner',
    `repo`       varchar(100) NOT NULL COMMENT '仓库名',
    `enabled`    tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用自动发布',
    `created_at` datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `updated_at` datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (`full_name`),
    KEY `idx_repo_auto_publish_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Pullcat 自动发布配置表';
