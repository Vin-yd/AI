package com.vin.ai.robot.service;

public interface SmsService {

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param code  验证码
     */
    void sendCode(String phone, String code);
}
