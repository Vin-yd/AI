package com.vin.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vin.ai.robot.domain.dos.SmsCodeDO;

public interface SmsCodeMapper extends BaseMapper<SmsCodeDO> {

    /**
     * 查某手机号最新一条验证码记录（用于 60s 限频）
     */
    default SmsCodeDO selectLatestByPhone(String phone) {
        return selectOne(Wrappers.<SmsCodeDO>lambdaQuery()
                .eq(SmsCodeDO::getPhone, phone)
                .orderByDesc(SmsCodeDO::getCreateTime)
                .last("LIMIT 1"));
    }
}
