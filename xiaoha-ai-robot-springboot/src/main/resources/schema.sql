-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    nickname    VARCHAR(50)  DEFAULT '',
    role        VARCHAR(20)  DEFAULT 'user',
    status      SMALLINT     DEFAULT 1,
    create_time TIMESTAMP    DEFAULT NOW(),
    update_time TIMESTAMP    DEFAULT NOW()
);

-- 短信验证码表
CREATE TABLE IF NOT EXISTS t_sms_code (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    used        BOOLEAN      DEFAULT FALSE,
    expire_time TIMESTAMP    NOT NULL,
    create_time TIMESTAMP    DEFAULT NOW()
);

-- 对话表新增 user_id 字段
ALTER TABLE t_chat ADD COLUMN IF NOT EXISTS user_id BIGINT;
