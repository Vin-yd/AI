# 用户登录与数据隔离 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后端引入 Sa-Token 用户认证体系 + 阿里云短信验证码登录/注册 + 基于 `user_id` 的对话数据隔离 + 基于角色的客服知识库接口权限控制

**Architecture:** Sa-Token 做会话管理和注解鉴权，新建 `t_user` / `t_sms_code` 两张表，在现有 `t_chat` 表加 `user_id` 字段。`t_user` 含 `role` 字段（`user`/`admin`），默认注册为 `user`，管理员由数据库手动指定。客服知识库接口按角色鉴权——查看类接口所有登录用户可访问，上传/修改/删除仅 `admin` 可操作

**Tech Stack:** Sa-Token 1.39.0, Aliyun SMS SDK 3.0.1, PostgreSQL, MyBatis Plus, Spring Boot 3.4.5

---

## 文件结构

```
新建：
  src/main/resources/schema.sql
  domain/dos/UserDO.java
  domain/dos/SmsCodeDO.java
  domain/mapper/UserMapper.java
  domain/mapper/SmsCodeMapper.java
  model/vo/auth/SendSmsReqVO.java
  model/vo/auth/LoginReqVO.java
  model/vo/auth/LoginRspVO.java
  model/vo/user/UpdateProfileReqVO.java
  model/vo/user/UserInfoRspVO.java
  service/SmsService.java
  service/impl/AliyunSmsServiceImpl.java
  service/AuthService.java
  service/impl/AuthServiceImpl.java
  controller/AuthController.java
  service/UserService.java
  service/impl/UserServiceImpl.java
  controller/UserController.java

修改：
  pom.xml
  application-dev.yml
  application-prod.yml
  enums/ResponseCodeEnum.java
  domain/dos/ChatDO.java
  domain/mapper/ChatMapper.java
  service/ChatService.java
  service/impl/ChatServiceImpl.java
  controller/ChatController.java
  controller/AiCustomerServiceController.java
  exception/GlobalExceptionHandler.java
```

---

### Task 1: 添加依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 Sa-Token 和 Aliyun SMS 依赖**

在 `pom.xml` 的 `<dependencies>` 末尾（`</dependencies>` 之前）插入：

```xml
        <!-- Sa-Token 认证框架 -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>1.39.0</version>
        </dependency>

        <!-- 阿里云短信服务 -->
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>dysmsapi20170525</artifactId>
            <version>3.0.1</version>
        </dependency>
```

- [ ] **Step 2: 验证依赖下载**

```bash
cd xiaoha-ai-robot-springboot && mvn dependency:resolve -q
```

Expected: BUILD SUCCESS，无错误

- [ ] **Step 3: Commit**

```bash
git add xiaoha-ai-robot-springboot/pom.xml
git commit -m "chore: add sa-token and aliyun-sms dependencies"
```

---

### Task 2: 添加配置

**Files:**
- Modify: `application-dev.yml`
- Modify: `application-prod.yml`

- [ ] **Step 1: 在 application-dev.yml 末尾添加配置**

```yaml
# Sa-Token 配置
sa-token:
  token-name: Authorization
  timeout: 2592000
  is-read-header: true
  token-prefix: "Bearer"

# 阿里云短信配置
aliyun:
  sms:
    access-key-id: ${ALIYUN_SMS_ACCESS_KEY_ID:}
    access-key-secret: ${ALIYUN_SMS_ACCESS_KEY_SECRET:}
    sign-name: 小哈AI机器人
    template-code: ${ALIYUN_SMS_TEMPLATE_CODE:SMS_123456789}
    endpoint: dysmsapi.aliyuncs.com
```

- [ ] **Step 2: 在 application-prod.yml 末尾添加相同配置**

配置内容同上。

- [ ] **Step 3: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/resources/application-dev.yml xiaoha-ai-robot-springboot/src/main/resources/application-prod.yml
git commit -m "chore: add sa-token and aliyun sms config"
```

---

### Task 3: 数据库 Schema

**Files:**
- Create: `src/main/resources/schema.sql`
- Modify: `application-dev.yml`

- [ ] **Step 1: 创建 schema.sql**

```sql
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
```

- [ ] **Step 2: 启用 SQL 初始化**

在 `application-dev.yml` 的 `spring:` 块下添加（与 `datasource:` 同级）：

```yaml
  sql:
    init:
      mode: always
```

- [ ] **Step 3: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/resources/schema.sql xiaoha-ai-robot-springboot/src/main/resources/application-dev.yml
git commit -m "chore: add schema.sql for user tables and chat.user_id column"
```

---

### Task 4: Domain 层 — 实体类 & Mapper

**Files:**
- Create: `domain/dos/UserDO.java`
- Create: `domain/dos/SmsCodeDO.java`
- Create: `domain/mapper/UserMapper.java`
- Create: `domain/mapper/SmsCodeMapper.java`

- [ ] **Step 1: 创建 UserDO.java**

```java
package com.vin.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String nickname;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 SmsCodeDO.java**

```java
package com.vin.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_sms_code")
public class SmsCodeDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String code;
    private Boolean used;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 创建 UserMapper.java**

```java
package com.vin.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vin.ai.robot.domain.dos.UserDO;

public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据手机号查用户
     */
    default UserDO selectByPhone(String phone) {
        return selectOne(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getPhone, phone));
    }
}
```

- [ ] **Step 4: 创建 SmsCodeMapper.java**

```java
package com.vin.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vin.ai.robot.domain.dos.SmsCodeDO;

public interface SmsCodeMapper extends BaseMapper<SmsCodeDO> {

    /**
     * 查某手机号最新一条验证码记录（用于 60s 限频）
     */
    default SmsCodeDO selectLatestByPhone(String phone) {
        return selectOne(Wrappers.<SmsCodeDO>lambdaQuery()
                .eq(SmsCodeDO::getPhone, phone)
                .orderByDesc(SmsCodeDO::getCreateTime)
                .last("LIMIT 1"));
    }
}
```

- [ ] **Step 5: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/domain/dos/UserDO.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/domain/dos/SmsCodeDO.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/domain/mapper/UserMapper.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/domain/mapper/SmsCodeMapper.java
git commit -m "feat: add UserDO, SmsCodeDO and their mappers"
```

---

### Task 5: VO 类

**Files:**
- Create: `model/vo/auth/SendSmsReqVO.java`
- Create: `model/vo/auth/LoginReqVO.java`
- Create: `model/vo/auth/LoginRspVO.java`
- Create: `model/vo/user/UpdateProfileReqVO.java`
- Create: `model/vo/user/UserInfoRspVO.java`

- [ ] **Step 1: 创建 SendSmsReqVO.java**

```java
package com.vin.ai.robot.model.vo.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendSmsReqVO {

    @NotBlank(message = "手机号不能为空")
    private String phone;
}
```

- [ ] **Step 2: 创建 LoginReqVO.java**

```java
package com.vin.ai.robot.model.vo.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginReqVO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

- [ ] **Step 3: 创建 LoginRspVO.java**

```java
package com.vin.ai.robot.model.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRspVO {

    private String token;
    private UserInfo userInfo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String phone;
        private String nickname;
        private String role;
    }
}
```

- [ ] **Step 4: 创建 UpdateProfileReqVO.java**

```java
package com.vin.ai.robot.model.vo.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileReqVO {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长50个字符")
    private String nickname;
}
```

- [ ] **Step 5: 创建 UserInfoRspVO.java**

```java
package com.vin.ai.robot.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoRspVO {

    private Long id;
    private String phone;
    private String nickname;
    private String role;
}
```

- [ ] **Step 6: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/model/vo/auth/ xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/model/vo/user/
git commit -m "feat: add auth and user VO classes"
```

---

### Task 6: 错误码

**Files:**
- Modify: `enums/ResponseCodeEnum.java`

- [ ] **Step 1: 在 ResponseCodeEnum 中添加新错误码**

在 `CHUNK_NUM_NOT_COMPLETE("20007", ...)` 之后、`;` 之前插入：

```java
    // ----------- 短信/登录相关 -----------
    SMS_TOO_FREQUENT("30001", "验证码发送过于频繁，请60秒后再试"),
    SMS_SEND_FAILED("30002", "短信发送失败，请稍后重试"),
    SMS_CODE_INVALID("30003", "验证码错误或已过期"),
    USER_NOT_LOGIN("30004", "请先登录"),
    USER_DISABLED("30005", "账号已被禁用"),
    FORBIDDEN("30006", "无权限，仅管理员可操作"),
    ;
```

- [ ] **Step 2: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/enums/ResponseCodeEnum.java
git commit -m "feat: add sms/auth error codes"
```

---

### Task 7: 短信服务

**Files:**
- Create: `service/SmsService.java`
- Create: `service/impl/AliyunSmsServiceImpl.java`

- [ ] **Step 1: 创建 SmsService.java**

```java
package com.vin.ai.robot.service;

public interface SmsService {

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param code  验证码
     */
    void sendCode(String phone, String code);
}
```

- [ ] **Step 2: 创建 AliyunSmsServiceImpl.java**

```java
package com.vin.ai.robot.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.service.SmsService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AliyunSmsServiceImpl implements SmsService {

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;
    @Value("${aliyun.sms.sign-name}")
    private String signName;
    @Value("${aliyun.sms.template-code}")
    private String templateCode;
    @Value("${aliyun.sms.endpoint}")
    private String endpoint;

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = endpoint;
        this.client = new Client(config);
    }

    @Override
    public void sendCode(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            client.sendSms(request);
            log.info("sms_sent phone={}", phone);
        } catch (Exception e) {
            log.error("sms_send_failed phone={}", phone, e);
            throw new BizException(ResponseCodeEnum.SMS_SEND_FAILED);
        }
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/SmsService.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/impl/AliyunSmsServiceImpl.java
git commit -m "feat: add aliyun sms service"
```

---

### Task 8: 认证模块（登录/注册/退出/发送验证码）

**Files:**
- Create: `service/AuthService.java`
- Create: `service/impl/AuthServiceImpl.java`
- Create: `controller/AuthController.java`

- [ ] **Step 1: 创建 AuthService.java**

```java
package com.vin.ai.robot.service;

import com.vin.ai.robot.model.vo.auth.LoginRspVO;
import com.vin.ai.robot.utils.Response;

public interface AuthService {

    /**
     * 发送短信验证码
     */
    Response<?> sendSmsCode(String phone);

    /**
     * 手机号+验证码登录（用户不存在则自动注册）
     */
    Response<LoginRspVO> login(String phone, String code);

    /**
     * 退出登录
     */
    Response<?> logout();
}
```

- [ ] **Step 2: 创建 AuthServiceImpl.java**

```java
package com.vin.ai.robot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.vin.ai.robot.domain.dos.SmsCodeDO;
import com.vin.ai.robot.domain.dos.UserDO;
import com.vin.ai.robot.domain.mapper.SmsCodeMapper;
import com.vin.ai.robot.domain.mapper.UserMapper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.model.vo.auth.LoginRspVO;
import com.vin.ai.robot.service.AuthService;
import com.vin.ai.robot.service.SmsService;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private SmsCodeMapper smsCodeMapper;
    @Resource
    private SmsService smsService;

    @Override
    public Response<?> sendSmsCode(String phone) {
        // 60s 限频：查该手机号最新一条验证码记录
        SmsCodeDO latest = smsCodeMapper.selectLatestByPhone(phone);
        if (latest != null) {
            long seconds = Duration.between(latest.getCreateTime(), LocalDateTime.now()).getSeconds();
            if (seconds < 60) {
                throw new BizException(ResponseCodeEnum.SMS_TOO_FREQUENT);
            }
        }

        // 生成 5 位数字验证码，有效期 5 分钟
        String code = RandomUtil.randomNumbers(5);

        // 入库
        smsCodeMapper.insert(SmsCodeDO.builder()
                .phone(phone)
                .code(code)
                .used(false)
                .expireTime(LocalDateTime.now().plusMinutes(5))
                .createTime(LocalDateTime.now())
                .build());

        // 调阿里云发短信
        smsService.sendCode(phone, code);

        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<LoginRspVO> login(String phone, String code) {
        // 1. 校验验证码（取最新一条未使用、未过期的）
        SmsCodeDO smsCode = smsCodeMapper.selectLatestByPhone(phone);
        if (smsCode == null
                || smsCode.getUsed()
                || smsCode.getExpireTime().isBefore(LocalDateTime.now())
                || !smsCode.getCode().equals(code)) {
            throw new BizException(ResponseCodeEnum.SMS_CODE_INVALID);
        }

        // 2. 标记验证码已使用
        smsCode.setUsed(true);
        smsCodeMapper.updateById(smsCode);

        // 3. 查用户是否存在，不存在则自动注册
        UserDO user = userMapper.selectByPhone(phone);
        if (user == null) {
            user = UserDO.builder()
                    .phone(phone)
                    .nickname("")
                    .role("user")
                    .status(1)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            log.info("user_registered phone={} id={}", phone, user.getId());
        }

        // 4. 检查用户状态
        if (user.getStatus() != 1) {
            throw new BizException(ResponseCodeEnum.USER_DISABLED);
        }

        // 5. Sa-Token 登录
        StpUtil.login(user.getId());

        // 6. 构建返参（手机号脱敏）
        String maskedPhone = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");

        LoginRspVO.UserInfo userInfo = LoginRspVO.UserInfo.builder()
                .id(user.getId())
                .phone(maskedPhone)
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();

        return Response.success(LoginRspVO.builder()
                .token(StpUtil.getTokenValue())
                .userInfo(userInfo)
                .build());
    }

    @Override
    public Response<?> logout() {
        StpUtil.logout();
        return Response.success();
    }
}
```

- [ ] **Step 3: 创建 AuthController.java**

```java
package com.vin.ai.robot.controller;

import com.vin.ai.robot.aspect.ApiOperationLog;
import com.vin.ai.robot.model.vo.auth.LoginReqVO;
import com.vin.ai.robot.model.vo.auth.LoginRspVO;
import com.vin.ai.robot.model.vo.auth.SendSmsReqVO;
import com.vin.ai.robot.service.AuthService;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/sms/send")
    @ApiOperationLog(description = "发送短信验证码")
    public Response<?> sendSmsCode(@RequestBody @Validated SendSmsReqVO reqVO) {
        return authService.sendSmsCode(reqVO.getPhone());
    }

    @PostMapping("/login")
    @ApiOperationLog(description = "手机号验证码登录")
    public Response<LoginRspVO> login(@RequestBody @Validated LoginReqVO reqVO) {
        return authService.login(reqVO.getPhone(), reqVO.getCode());
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "退出登录")
    public Response<?> logout() {
        return authService.logout();
    }
}
```

- [ ] **Step 4: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/AuthService.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/impl/AuthServiceImpl.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/controller/AuthController.java
git commit -m "feat: add auth module (sms code + login/logout)"
```

---

### Task 9: 个人中心模块

**Files:**
- Create: `service/UserService.java`
- Create: `service/impl/UserServiceImpl.java`
- Create: `controller/UserController.java`

- [ ] **Step 1: 创建 UserService.java**

```java
package com.vin.ai.robot.service;

import com.vin.ai.robot.model.vo.user.UserInfoRspVO;
import com.vin.ai.robot.utils.Response;

public interface UserService {

    /**
     * 获取当前登录用户信息
     */
    Response<UserInfoRspVO> getCurrentUser();

    /**
     * 修改当前用户昵称
     */
    Response<?> updateProfile(String nickname);
}
```

- [ ] **Step 2: 创建 UserServiceImpl.java**

```java
package com.vin.ai.robot.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.vin.ai.robot.domain.dos.UserDO;
import com.vin.ai.robot.domain.mapper.UserMapper;
import com.vin.ai.robot.model.vo.user.UserInfoRspVO;
import com.vin.ai.robot.service.UserService;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public Response<UserInfoRspVO> getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userMapper.selectById(userId);

        // 手机号脱敏
        String maskedPhone = user.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");

        return Response.success(UserInfoRspVO.builder()
                .id(user.getId())
                .phone(maskedPhone)
                .nickname(user.getNickname())
                .role(user.getRole())
                .build());
    }

    @Override
    public Response<?> updateProfile(String nickname) {
        Long userId = StpUtil.getLoginIdAsLong();
        userMapper.updateById(UserDO.builder()
                .id(userId)
                .nickname(nickname)
                .updateTime(LocalDateTime.now())
                .build());
        return Response.success();
    }
}
```

- [ ] **Step 3: 创建 UserController.java**

```java
package com.vin.ai.robot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.vin.ai.robot.model.vo.user.UpdateProfileReqVO;
import com.vin.ai.robot.model.vo.user.UserInfoRspVO;
import com.vin.ai.robot.service.UserService;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@SaCheckLogin
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/me")
    public Response<UserInfoRspVO> getCurrentUser() {
        return userService.getCurrentUser();
    }

    @PutMapping("/profile")
    public Response<?> updateProfile(@RequestBody @Validated UpdateProfileReqVO reqVO) {
        return userService.updateProfile(reqVO.getNickname());
    }
}
```

- [ ] **Step 4: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/UserService.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/impl/UserServiceImpl.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/controller/UserController.java
git commit -m "feat: add user profile module"
```

---

### Task 10: 对话数据隔离（Service + Domain 层）

**Files:**
- Modify: `domain/dos/ChatDO.java`
- Modify: `domain/mapper/ChatMapper.java`
- Modify: `service/ChatService.java`
- Modify: `service/impl/ChatServiceImpl.java`

- [ ] **Step 1: ChatDO 新增 userId 字段**

在 `ChatDO.java` 的 `private String uuid;` 之后添加：

```java
    private Long userId;
```

完整文件变为：

```java
package com.vin.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_chat")
public class ChatDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private Long userId;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: ChatMapper 查询加 userId 过滤**

修改 `ChatMapper.java` — `selectPageList` 方法加 `userId` 参数：

```java
package com.vin.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vin.ai.robot.domain.dos.ChatDO;

public interface ChatMapper extends BaseMapper<ChatDO> {

    /**
     * 分页查询（按用户过滤）
     */
    default Page<ChatDO> selectPageList(Long current, Long size, Long userId) {
        Page<ChatDO> page = new Page<>(current, size);

        LambdaQueryWrapper<ChatDO> wrapper = Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUserId, userId)
                .orderByDesc(ChatDO::getUpdateTime);

        return selectPage(page, wrapper);
    }
}
```

- [ ] **Step 3: ChatService 接口方法加 userId 参数**

```java
package com.vin.ai.robot.service;

import com.vin.ai.robot.model.vo.chat.*;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;

public interface ChatService {

    /**
     * 新建对话
     */
    Response<NewChatRspVO> newChat(NewChatReqVO newChatReqVO, Long userId);

    /**
     * 查询历史消息（含归属校验）
     */
    PageResponse<FindChatHistoryMessagePageListRspVO> findChatHistoryMessagePageList(
            FindChatHistoryMessagePageListReqVO reqVO, Long userId);

    /**
     * 查询历史对话
     */
    PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(
            FindChatHistoryPageListReqVO reqVO, Long userId);

    /**
     * 重命名对话摘要（含归属校验）
     */
    Response<?> renameChatSummary(RenameChatReqVO reqVO, Long userId);

    /**
     * 删除对话（含归属校验）
     */
    Response<?> deleteChat(DeleteChatReqVO reqVO, Long userId);
}
```

- [ ] **Step 4: ChatServiceImpl 实现数据隔离**

```java
package com.vin.ai.robot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vin.ai.robot.domain.dos.ChatDO;
import com.vin.ai.robot.domain.dos.ChatMessageDO;
import com.vin.ai.robot.domain.mapper.ChatMapper;
import com.vin.ai.robot.domain.mapper.ChatMessageMapper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.model.vo.chat.*;
import com.vin.ai.robot.service.ChatService;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;
import com.vin.ai.robot.utils.StringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatMapper chatMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;

    /**
     * 新建对话
     */
    @Override
    public Response<NewChatRspVO> newChat(NewChatReqVO newChatReqVO, Long userId) {
        String message = newChatReqVO.getMessage();

        String uuid = UUID.randomUUID().toString();
        String summary = StringUtil.truncate(message, 20);

        chatMapper.insert(ChatDO.builder()
                .summary(summary)
                .uuid(uuid)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        return Response.success(NewChatRspVO.builder()
                .uuid(uuid)
                .summary(summary)
                .build());
    }

    /**
     * 查询历史消息（先校验对话归属）
     */
    @Override
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatHistoryMessagePageList(
            FindChatHistoryMessagePageListReqVO reqVO, Long userId) {
        Long current = reqVO.getCurrent();
        Long size = reqVO.getSize();
        String chatId = reqVO.getChatId();

        // 校验对话归属当前用户
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUuid, chatId)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        Page<ChatMessageDO> chatMessageDOPage = chatMessageMapper.selectPageList(current, size, chatId);

        List<ChatMessageDO> chatMessageDOS = chatMessageDOPage.getRecords();
        List<FindChatHistoryMessagePageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(chatMessageDOS)) {
            vos = chatMessageDOS.stream()
                    .map(chatMessageDO -> FindChatHistoryMessagePageListRspVO.builder()
                            .id(chatMessageDO.getId())
                            .chatId(chatMessageDO.getChatUuid())
                            .content(chatMessageDO.getContent())
                            .role(chatMessageDO.getRole())
                            .reasoning(chatMessageDO.getReasoningContent())
                            .createTime(chatMessageDO.getCreateTime())
                            .build())
                    .sorted(Comparator.comparing(FindChatHistoryMessagePageListRspVO::getCreateTime))
                    .collect(Collectors.toList());
        }

        return PageResponse.success(chatMessageDOPage, vos);
    }

    /**
     * 查询历史对话
     */
    @Override
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(
            FindChatHistoryPageListReqVO reqVO, Long userId) {
        Long current = reqVO.getCurrent();
        Long size = reqVO.getSize();

        Page<ChatDO> chatDOPage = chatMapper.selectPageList(current, size, userId);

        List<ChatDO> chatDOS = chatDOPage.getRecords();
        List<FindChatHistoryPageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(chatDOS)) {
            vos = chatDOS.stream()
                    .map(chatDO -> FindChatHistoryPageListRspVO.builder()
                            .id(chatDO.getId())
                            .uuid(chatDO.getUuid())
                            .summary(chatDO.getSummary())
                            .updateTime(chatDO.getUpdateTime())
                            .build())
                    .collect(Collectors.toList());
        }

        return PageResponse.success(chatDOPage, vos);
    }

    /**
     * 重命名对话摘要（归属校验）
     */
    @Override
    public Response<?> renameChatSummary(RenameChatReqVO reqVO, Long userId) {
        Long chatId = reqVO.getId();
        String summary = reqVO.getSummary();

        // 归属校验
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getId, chatId)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        chatMapper.updateById(ChatDO.builder()
                .id(chatId)
                .summary(summary)
                .build());

        return Response.success();
    }

    /**
     * 删除对话（归属校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteChat(DeleteChatReqVO reqVO, Long userId) {
        String uuid = reqVO.getUuid();

        // 归属校验
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUuid, uuid)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        chatMapper.deleteById(chat.getId());

        chatMessageMapper.delete(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, uuid));

        return Response.success();
    }
}
```

- [ ] **Step 5: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/domain/dos/ChatDO.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/domain/mapper/ChatMapper.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/ChatService.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/service/impl/ChatServiceImpl.java
git commit -m "feat: add user_id data isolation to chat service layer"
```

---

### Task 11: 对话接口鉴权 & 异常处理

**Files:**
- Modify: `controller/ChatController.java`
- Modify: `exception/GlobalExceptionHandler.java`

- [ ] **Step 1: ChatController 加 @SaCheckLogin + 传递 userId**

```java
package com.vin.ai.robot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.collect.Lists;
import com.vin.ai.robot.advisor.CustomChatMemoryAdvisor;
import com.vin.ai.robot.advisor.CustomStreamLoggerAndMessage2DBAdvisor;
import com.vin.ai.robot.advisor.NetworkSearchAdvisor;
import com.vin.ai.robot.aspect.ApiOperationLog;
import com.vin.ai.robot.domain.dos.ChatDO;
import com.vin.ai.robot.domain.mapper.ChatMapper;
import com.vin.ai.robot.domain.mapper.ChatMessageMapper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.model.vo.chat.*;
import com.vin.ai.robot.service.ChatService;
import com.vin.ai.robot.service.SearXNGService;
import com.vin.ai.robot.service.SearchResultContentFetcherService;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/chat")
@Slf4j
@SaCheckLogin
public class ChatController {

    @Resource
    private ChatService chatService;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Resource
    private ChatMapper chatMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SearXNGService searXNGService;
    @Resource
    private SearchResultContentFetcherService searchResultContentFetcherService;

    @PostMapping("/new")
    @ApiOperationLog(description = "新建对话")
    public Response<?> newChat(@RequestBody @Validated NewChatReqVO newChatReqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.newChat(newChatReqVO, userId);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "流式对话")
    public Flux<AIResponse> chat(@RequestBody @Validated AiChatReqVO aiChatReqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        String userMessage = aiChatReqVO.getMessage();
        String modelName = aiChatReqVO.getModelName();
        Double temperature = aiChatReqVO.getTemperature();
        boolean networkSearch = aiChatReqVO.getNetworkSearch();
        String chatId = aiChatReqVO.getChatId();

        // 校验对话归属
        if (StringUtils.isNotBlank(chatId)) {
            ChatDO chat = chatMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<ChatDO>lambdaQuery()
                    .eq(ChatDO::getUuid, chatId)
                    .eq(ChatDO::getUserId, userId));
            if (chat == null) {
                throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
            }
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .build())
                .user(userMessage);

        List<Advisor> advisors = Lists.newArrayList();
        if (networkSearch) {
            advisors.add(new NetworkSearchAdvisor(searXNGService, searchResultContentFetcherService));
        } else {
            advisors.add(new CustomChatMemoryAdvisor(chatMessageMapper, aiChatReqVO, 50));
        }

        advisors.add(new CustomStreamLoggerAndMessage2DBAdvisor(chatMessageMapper, aiChatReqVO, transactionTemplate));
        advisors.add(new CustomChatMemoryAdvisor(chatMessageMapper, aiChatReqVO, 50));

        chatClientRequestSpec.advisors(advisors);

        return chatClientRequestSpec
                .stream()
                .chatResponse()
                .mapNotNull(chatResponse -> {
                    if (Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())) {
                        AssistantMessage message = chatResponse.getResult().getOutput();

                        String text = message.getText();

                        String reasoningContent = message.getMetadata().get("reasoningContent").toString();

                        if (StringUtils.isNotBlank(reasoningContent)) {
                            return AIResponse.builder().reasoning(reasoningContent).build();
                        }

                        return AIResponse.builder().v(text).build();
                    }
                    return null;
                });
    }

    @PostMapping("/message/list")
    @ApiOperationLog(description = "查询对话历史消息")
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatMessagePageList(
            @RequestBody @Validated FindChatHistoryMessagePageListReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.findChatHistoryMessagePageList(reqVO, userId);
    }

    @PostMapping("/list")
    @ApiOperationLog(description = "查询历史对话")
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(
            @RequestBody @Validated FindChatHistoryPageListReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.findChatHistoryPageList(reqVO, userId);
    }

    @PostMapping("/summary/rename")
    @ApiOperationLog(description = "重命名对话摘要")
    public Response<?> renameChatSummary(@RequestBody @Validated RenameChatReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.renameChatSummary(reqVO, userId);
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除对话")
    public Response<?> deleteChat(@RequestBody @Validated DeleteChatReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.deleteChat(reqVO, userId);
    }
}
```

- [ ] **Step 2: GlobalExceptionHandler 处理 NotLoginException**

在 `GlobalExceptionHandler.java` 中添加 import 和 handler：

新增 import（在文件顶部 import 块末尾）：

```java
import cn.dev33.satoken.exception.NotLoginException;
```

在最后一个 `@ExceptionHandler` 方法之后、类的闭合 `}` 之前，新增：

```java
    /**
     * 未登录异常
     */
    @ExceptionHandler({ NotLoginException.class })
    @ResponseBody
    public Response<Object> handleNotLoginException(HttpServletRequest request, HttpServletResponse httpResponse, NotLoginException e) {
        log.warn("{} request fail, not login", request.getRequestURI());
        Response<Object> result = Response.fail(ResponseCodeEnum.USER_NOT_LOGIN);
        if (isSseRequest(request)) {
            writeSseErrorResponse(httpResponse, result);
            return null;
        }
        return result;
    }
```

- [ ] **Step 3: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 启动应用验证**

```bash
cd xiaoha-ai-robot-springboot && mvn spring-boot:run
```

启动后检查日志，确认 Sa-Token 和 schema.sql 执行成功，无报错。

- [ ] **Step 5: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/controller/ChatController.java xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/exception/GlobalExceptionHandler.java
git commit -m "feat: add @SaCheckLogin to chat controller and handle NotLoginException"
```

---

### Task 12: 客服知识库接口角色鉴权

**Files:**
- Modify: `controller/AiCustomerServiceController.java`

- [ ] **Step 1: 改造 AiCustomerServiceController 按角色鉴权**

`/customer-service` 下的接口分两类：
- **所有登录用户可访问：** 文件检查、文件列表查询
- **仅管理员可访问：** 文件上传（分片上传/合并）、文件删除、文件修改、流式对话

改造后的文件：

```java
package com.vin.ai.robot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.google.common.collect.Lists;
import com.vin.ai.robot.advisor.CustomerServiceAdvisor;
import com.vin.ai.robot.aspect.ApiOperationLog;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.model.vo.chat.AIResponse;
import com.vin.ai.robot.model.vo.customerService.*;
import com.vin.ai.robot.service.CustomerService;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer-service")
@Slf4j
@SaCheckLogin
public class AiCustomerServiceController {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private CustomerService customerService;

    @Value("${customer-service.model}")
    private String model;
    @Value("${customer-service.temperature}")
    private Double temperature;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * 校验当前用户是否为管理员
     */
    private void checkAdmin() {
        if (!StpUtil.hasRole("admin")) {
            throw new BizException(ResponseCodeEnum.FORBIDDEN);
        }
    }

    @PostMapping("/file/check")
    @ApiOperationLog(description = "检查文件是否存在")
    public Response<CheckFileRspVO> checkFile(@RequestBody @Validated CheckFileReqVO checkFileReqVO) {
        return customerService.checkFile(checkFileReqVO);
    }

    @PostMapping("/file/upload-chunk")
    public Response<?> uploadChunk(@ModelAttribute UploadChunkReqVO uploadChunkReqVO) {
        checkAdmin();
        return customerService.uploadChunk(uploadChunkReqVO);
    }

    @PostMapping("/file/merge-chunk")
    @ApiOperationLog(description = "文件分片合并")
    public Response<?> mergeChunk(@RequestBody @Validated MergeChunkReqVO mergeChunkReqVO) {
        checkAdmin();
        return customerService.mergeChunk(mergeChunkReqVO);
    }

    @PostMapping("/file/delete")
    @ApiOperationLog(description = "删除 Markdown 问答文件")
    public Response<?> deleteMarkdownFile(@RequestBody @Validated DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        checkAdmin();
        return customerService.deleteMarkdownFile(deleteMarkdownFileReqVO);
    }

    @PostMapping("/file/list")
    @ApiOperationLog(description = "Markdown 问答文件分页查询")
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(
            @RequestBody @Validated FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        return customerService.findMarkdownFilePageList(findMarkdownFilePageListReqVO);
    }

    @PostMapping("/file/update")
    @ApiOperationLog(description = "修改 Markdown 问答文件信息")
    public Response<?> updateMarkdownFile(@RequestBody @Validated UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        checkAdmin();
        return customerService.updateMarkdownFile(updateMarkdownFileReqVO);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "AI 智能客服对话")
    public Flux<AIResponse> chat(@RequestBody @Validated AiCustomerServiceChatReqVO chatReqVO) {
        String userMessage = chatReqVO.getMessage();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .user(userMessage);

        List<Advisor> advisors = Lists.newArrayList();
        advisors.add(new CustomerServiceAdvisor(vectorStore));

        chatClientRequestSpec.advisors(advisors);

        return chatClientRequestSpec
                .stream()
                .content()
                .mapNotNull(text -> AIResponse.builder().v(text).build());
    }
}
```

**接口权限对照：**

| 接口 | 角色 |
|------|------|
| `/customer-service/file/check` | 登录用户 |
| `/customer-service/file/list` | 登录用户 |
| `/customer-service/completion` | 登录用户 |
| `/customer-service/file/upload-chunk` | 仅 admin |
| `/customer-service/file/merge-chunk` | 仅 admin |
| `/customer-service/file/delete` | 仅 admin |
| `/customer-service/file/update` | 仅 admin |

- [ ] **Step 2: 验证编译**

```bash
cd xiaoha-ai-robot-springboot && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add xiaoha-ai-robot-springboot/src/main/java/com/vin/ai/robot/controller/AiCustomerServiceController.java
git commit -m "feat: add role-based access control to customer service controller"
```

---

## 管理员手动添加方式

部署后在数据库中执行：

```sql
-- 将指定手机号的用户升级为管理员
UPDATE t_user SET role = 'admin' WHERE phone = '你的手机号';
```

---

## 验证清单

完成所有 Task 后，按以下清单验证：

1. **未登录访问 `/chat/list`** → 返回 `{"success":false,"errorCode":"30004","message":"请先登录"}`（401 语义）
2. **发送验证码** `POST /api/auth/sms/send` → 短信收到 5 位数字
3. **60s 内重复发送** → 返回限频错误
4. **错误验证码登录** → 返回 `验证码错误或已过期`
5. **正确验证码登录** → 返回 token + userInfo，手机号脱敏
6. **`GET /api/user/me` 带 token** → 返回当前用户信息
7. **`PUT /api/user/profile`** → 修改昵称成功
8. **带 token 访问 `/chat/list`** → 仅返回当前用户的对话
9. **用户 A 尝试访问用户 B 的对话消息** → 返回 `此对话不存在`
10. **用户 A 尝试删除用户 B 的对话** → 返回 `此对话不存在`
11. **退出登录后访问 `/chat/list`** → 返回未登录
12. **普通用户访问 `/customer-service/file/upload-chunk`** → 返回 `无权限，仅管理员可操作`
13. **管理员访问 `/customer-service/file/upload-chunk`** → 正常执行
14. **普通用户访问 `/customer-service/file/list`** → 正常返回（所有登录用户可查看）
15. **数据库中执行 `UPDATE t_user SET role='admin' WHERE phone='xxx'`** → 该用户获得管理员权限
