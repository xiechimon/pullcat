-- sql/github_app_migration.sql
-- GitHub App 升级迁移脚本，在已有 schema.sql 执行后运行

USE `pullcat`;

-- 新建 github_installation 表
CREATE TABLE IF NOT EXISTS `github_installation`
(
    `installation_id` bigint       NOT NULL COMMENT 'GitHub App Installation ID',
    `account_login`   varchar(255) NOT NULL COMMENT 'GitHub 用户名或 Org 名',
    `account_type`    varchar(32)  NOT NULL COMMENT '''User'' 或 ''Organization''',
    `installed_at`    datetime(3)  NOT NULL COMMENT '安装时间',
    `suspended_at`    datetime(3)  NULL COMMENT 'NULL = 活跃；非 NULL = 已暂停或卸载',
    PRIMARY KEY (`installation_id`),
    KEY `idx_github_installation_account_login` (`account_login`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='GitHub App 安装记录表';

-- user 表新增 installation_id 字段
ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `installation_id` bigint NULL COMMENT 'GitHub App Installation ID' AFTER `created_at`;

-- review 表新增 installation_id 字段
ALTER TABLE `review`
    ADD COLUMN IF NOT EXISTS `installation_id` bigint NULL COMMENT 'GitHub App Installation ID（webhook 触发时填充，与 user_id 互斥）' AFTER `user_id`;
