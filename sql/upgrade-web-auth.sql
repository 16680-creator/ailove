ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS login_name VARCHAR(50) NULL COMMENT 'Web登录账号',
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NULL COMMENT 'Web登录密码摘要';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_login_name ON sys_user (login_name);
