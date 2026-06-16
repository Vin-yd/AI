package com.vin.ai.robot.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vin.ai.robot.domain.dos.ChatDO;

public interface ChatMapper extends BaseMapper<ChatDO> {

    /**
     * 分页查询（按用户过滤）
     */
    default Page<ChatDO> selectPageList(Long current, Long size, Long userId) {
        Page<ChatDO> page = new Page<>(current, size);

        LambdaQueryWrapper<ChatDO> wrapper = Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUserId, userId)
                .orderByDesc(ChatDO::getUpdateTime);

        return selectPage(page, wrapper);
    }
}
