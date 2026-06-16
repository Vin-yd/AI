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
