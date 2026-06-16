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
