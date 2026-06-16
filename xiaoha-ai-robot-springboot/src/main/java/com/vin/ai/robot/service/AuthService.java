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
