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
@RequestMapping("/auth")
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
