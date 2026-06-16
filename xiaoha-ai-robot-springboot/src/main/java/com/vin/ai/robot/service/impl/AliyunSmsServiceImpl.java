package com.vin.ai.robot.service.impl;

import com.vin.ai.robot.config.sms.AliyunSmsHelper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.service.SmsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AliyunSmsServiceImpl implements SmsService {

    @Value("${aliyun.sms.sign-name}")
    private String signName;
    @Value("${aliyun.sms.template-code}")
    private String templateCode;

    @Resource
    private AliyunSmsHelper aliyunSmsHelper;

    @Override
    public void sendCode(String phone, String code) {
        String templateParam = String.format("{\"code\":\"%s\",\"min\":\"3\"}", code);

        boolean success = aliyunSmsHelper.sendMessage(signName, templateCode, phone, templateParam);

        if (!success) {
            throw new BizException(ResponseCodeEnum.SMS_SEND_FAILED);
        }
    }
}
