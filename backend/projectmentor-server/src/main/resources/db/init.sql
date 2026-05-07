CREATE DATABASE IF NOT EXISTS projectmentor_ai
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE projectmentor_ai;

CREATE TABLE IF NOT EXISTS pm_user (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
                                       username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常，0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS pm_user_plan (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                            user_id BIGINT NOT NULL COMMENT '用户ID',
                                            plan_type VARCHAR(30) DEFAULT 'FREE' COMMENT '套餐类型',
    remaining_credits INT DEFAULT 3 COMMENT '剩余额度',
    expire_time DATETIME NULL COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户套餐与额度表';

CREATE TABLE IF NOT EXISTS pm_credit_log (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                             user_id BIGINT NOT NULL COMMENT '用户ID',
                                             change_amount INT NOT NULL COMMENT '额度变化数量，正数增加，负数扣减',
                                             before_amount INT NOT NULL COMMENT '变化前额度',
                                             after_amount INT NOT NULL COMMENT '变化后额度',
                                             operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    business_id BIGINT NULL COMMENT '关联业务ID',
    remark VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='额度流水表';