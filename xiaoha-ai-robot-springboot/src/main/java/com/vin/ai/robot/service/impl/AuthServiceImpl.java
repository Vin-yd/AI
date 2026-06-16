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
