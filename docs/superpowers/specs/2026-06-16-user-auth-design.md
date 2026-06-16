# 用户登录与数据隔离 — 设计文档

> 日期：2026-06-16
> 状态：已确认

## 目标

为后端引入用户体系，实现手机号+短信验证码登录/注册、个人中心、不同用户间对话数据隔离。

## 技术选型

- **认证框架：** Sa-Token（轻量，注解鉴权，会话管理）
- **登录方式：** 手机号 + 阿里云短信验证码（注册与登录合一）
- **Token 传递：** Header `Authorization: Bearer <token>`
- **权限控制：** 注解式 `@SaCheckLogin`

## 数据库设计

### 新增表

```sql
-- 用户表
CREATE TABLE t_user (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    nickname    VARCHAR(50)  DEFAULT '',
    role        VARCHAR(20)  DEFAULT 'user',        -- user/admin，默认注册为 user，管理员由数据库手动设置
    status      SMALLINT     DEFAULT 1,            -- 1:正常 0:禁用
    create_time TIMESTAMP    DEFAULT NOW(),
    update_time TIMESTAMP    DEFAULT NOW()
);

-- 短信验证码表
CREATE TABLE t_sms_code (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    used        BOOLEAN      DEFAULT FALSE,
    expire_time TIMESTAMP    NOT NULL,
    create_time TIMESTAMP    DEFAULT NOW()
);
```

### 现有表改动

```sql
ALTER TABLE t_chat ADD COLUMN user_id BIGINT;
```

`t_chat_message` 不改动——消息通过 `chat_uuid` 关联对话，对话已有 `user_id`，自然隔离。

## API 设计

| 接口 | 方法 | 鉴权 | 说明 |
|------|------|------|------|
| `/api/auth/sms/send` | POST | 无 | 发送短信验证码，60s 内同手机号限发 1 次（查 `t_sms_code` 表实现） |
| `/api/auth/login` | POST | 无 | 手机号+验证码 → 校验 → 用户不存在则自动注册 → Sa-Token 登录，返回 token |
| `/api/auth/logout` | POST | 需登录 | Sa-Token 注销当前会话 |
| `/api/user/me` | GET | 需登录 | 返回当前用户信息（手机号脱敏为 `138****8000`） |
| `/api/user/profile` | PUT | 需登录 | 修改昵称 |
| `/chat/*` | — | 需登录 | 所有现有接口加 `@SaCheckLogin`，数据隔离 |
| `/customer-service/file/check` | POST | 需登录 | 检查文件是否存在（所有登录用户） |
| `/customer-service/file/list` | POST | 需登录 | 文件列表（所有登录用户） |
| `/customer-service/completion` | POST | 需登录 | 客服流式对话（所有登录用户） |
| `/customer-service/file/upload-chunk` | POST | 需登录 + admin | 分片上传（仅管理员） |
| `/customer-service/file/merge-chunk` | POST | 需登录 + admin | 分片合并（仅管理员） |
| `/customer-service/file/delete` | POST | 需登录 + admin | 删除文件（仅管理员） |
| `/customer-service/file/update` | POST | 需登录 + admin | 修改文件（仅管理员） |

> **角色说明：** `t_user.role` 字段，默认 `user`。管理员通过在数据库执行 `UPDATE t_user SET role='admin' WHERE phone='xxx'` 手动设置。无需注册入口。

### 请求/响应示例

**发送验证码：**
```
POST /api/auth/sms/send
Body: { "phone": "13800138000" }
→ 200 { "success": true }
→ 429 { "success": false, "message": "验证码发送过于频繁，请60秒后再试" }
```

**登录：**
```
POST /api/auth/login
Body: { "phone": "13800138000", "code": "12345" }
→ 200 { "success": true, "data": { "token": "xxx", "userInfo": { "id": 1, "phone": "138****8000", "nickname": "" } } }
→ 400 { "success": false, "message": "验证码错误或已过期" }
```

**当前用户：**
```
GET /api/user/me
→ 200 { "success": true, "data": { "id": 1, "phone": "138****8000", "nickname": "小明" } }
```

**修改昵称：**
```
PUT /api/user/profile
Body: { "nickname": "小明" }
→ 200 { "success": true }
```

## 数据隔离方案

所有对话查询/操作通过 `StpUtil.getLoginIdAsLong()` 获取当前 `userId`，在 SQL 中加入 `user_id` 过滤。

**隔离点清单：**

| 操作 | 隔离方式 |
|------|----------|
| 新建对话 | 写入 `user_id` |
| 对话列表 | 分页查询加 `.eq(ChatDO::getUserId, userId)` |
| 对话重命名 | 校验 `uuid + userId` 归属 |
| 删除对话 | 校验 `uuid + userId` 归属后删除 |
| 查询消息 | 先验证 `chat_uuid` 对应的对话归属当前用户 |
| 流式对话 | 将 `userId` 透传到 Advisor，写消息时不需改表但确保 chat 归属 |

归属校验失败统一抛 `CHAT_NOT_EXISTED`，不区分"不存在"和"无权访问"。

## Sa-Token 配置

```yaml
sa-token:
  token-name: Authorization
  timeout: 2592000            # 30 天
  is-read-header: true
  token-prefix: "Bearer"
```

## 异常处理

`GlobalExceptionHandler` 新增捕获 `NotLoginException` → 返回 `401`。

## 安全约束

- 阿里云 SMS AK/SK 走环境变量，不写入配置文件
- 手机号返回时脱敏：`138****8000`
- 验证码 60s 频控（查 `t_sms_code` 最近一条记录实现，无需 Redis）
- 验证码 5 分钟过期，用完标记 `used=true`
- 同一条验证码不可重复使用

## 新建文件

```
controller/AuthController.java
controller/UserController.java
service/AuthService.java
service/UserService.java
service/SmsService.java
service/impl/AuthServiceImpl.java
service/impl/UserServiceImpl.java
service/impl/AliyunSmsServiceImpl.java
domain/dos/UserDO.java
domain/dos/SmsCodeDO.java
domain/mapper/UserMapper.java
domain/mapper/SmsCodeMapper.java
model/vo/auth/SendSmsReqVO.java
model/vo/auth/LoginReqVO.java
model/vo/auth/LoginRspVO.java
model/vo/user/UpdateProfileReqVO.java
model/vo/user/UserInfoRspVO.java
```

## 修改文件

```
pom.xml                              — 加 sa-token + aliyun-sms 依赖
application-dev.yml                  — sa-token + aliyun 配置
application-prod.yml                 — 同上
domain/dos/ChatDO.java               — 加 userId 字段
domain/mapper/ChatMapper.java        — 查询加 userId 过滤
controller/ChatController.java       — @SaCheckLogin + 传递 userId
service/ChatService.java             — 接口方法加 userId 参数
service/impl/ChatServiceImpl.java    — 实现数据隔离
exception/GlobalExceptionHandler.java — NotLoginException → 401
enums/ResponseCodeEnum.java          — 新增短信/登录相关错误码
```
