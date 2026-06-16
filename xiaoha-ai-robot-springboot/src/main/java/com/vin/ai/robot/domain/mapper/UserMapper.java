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
