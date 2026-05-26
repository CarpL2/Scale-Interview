-- 创建数据库
CREATE DATABASE IF NOT EXISTS ace_interviewer CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ace_interviewer;

-- 用户面试画像表（存储简历和JD）
CREATE TABLE IF NOT EXISTS `user_interview_profile` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户唯一标识（实现隔离）',
    `user_name` VARCHAR(50) COMMENT '用户名',
    `resume_content` LONGTEXT COMMENT '解析后的简历纯文本内容',
    `jd_content` LONGTEXT COMMENT '当前申请岗位的JD描述',
    `interview_style` VARCHAR(20) DEFAULT 'PROFESSIONAL' COMMENT '面试风格：PROFESSIONAL-专业, STERN-严厉, ENCOURAGING-鼓励',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户面试画像表';

-- 模拟插入一条测试数据
INSERT INTO `user_interview_profile` (`user_id`, `user_name`, `resume_content`, `jd_content`) 
VALUES (1, '测试候选人', 'Java开发经验3年，精通Spring Cloud...', '要求精通高并发处理，有大厂经验者优先...');
