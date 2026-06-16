package com.vin.ai.robot.config;

import cn.dev33.satoken.stp.StpInterface;
import com.vin.ai.robot.domain.dos.UserDO;
import com.vin.ai.robot.domain.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色/权限加载接口实现
 * 每次调用 StpUtil.hasRole() 或 StpUtil.hasPermission() 时从数据库实时查询
 */
@Component
public class SaTokenRoleConfig implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserDO user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user != null && user.getRole() != null) {
            return Collections.singletonList(user.getRole());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
