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
                .createTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null)
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
