package com.vin.ai.robot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vin.ai.robot.domain.dos.ChatDO;
import com.vin.ai.robot.domain.dos.ChatMessageDO;
import com.vin.ai.robot.domain.mapper.ChatMapper;
import com.vin.ai.robot.domain.mapper.ChatMessageMapper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.model.vo.chat.*;
import com.vin.ai.robot.service.ChatService;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;
import com.vin.ai.robot.utils.StringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatMapper chatMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;

    /**
     * 新建对话
     */
    @Override
    public Response<NewChatRspVO> newChat(NewChatReqVO newChatReqVO, Long userId) {
        String message = newChatReqVO.getMessage();

        String uuid = UUID.randomUUID().toString();
        String summary = StringUtil.truncate(message, 20);

        chatMapper.insert(ChatDO.builder()
                .summary(summary)
                .uuid(uuid)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build());

        return Response.success(NewChatRspVO.builder()
                .uuid(uuid)
                .summary(summary)
                .build());
    }

    /**
     * 查询历史消息（先校验对话归属）
     */
    @Override
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatHistoryMessagePageList(
            FindChatHistoryMessagePageListReqVO reqVO, Long userId) {
        Long current = reqVO.getCurrent();
        Long size = reqVO.getSize();
        String chatId = reqVO.getChatId();

        // 校验对话归属当前用户
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUuid, chatId)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        Page<ChatMessageDO> chatMessageDOPage = chatMessageMapper.selectPageList(current, size, chatId);

        List<ChatMessageDO> chatMessageDOS = chatMessageDOPage.getRecords();
        List<FindChatHistoryMessagePageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(chatMessageDOS)) {
            vos = chatMessageDOS.stream()
                    .map(chatMessageDO -> FindChatHistoryMessagePageListRspVO.builder()
                            .id(chatMessageDO.getId())
                            .chatId(chatMessageDO.getChatUuid())
                            .content(chatMessageDO.getContent())
                            .role(chatMessageDO.getRole())
                            .reasoning(chatMessageDO.getReasoningContent())
                            .createTime(chatMessageDO.getCreateTime())
                            .build())
                    .sorted(Comparator.comparing(FindChatHistoryMessagePageListRspVO::getCreateTime))
                    .collect(Collectors.toList());
        }

        return PageResponse.success(chatMessageDOPage, vos);
    }

    /**
     * 查询历史对话
     */
    @Override
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(
            FindChatHistoryPageListReqVO reqVO, Long userId) {
        Long current = reqVO.getCurrent();
        Long size = reqVO.getSize();

        Page<ChatDO> chatDOPage = chatMapper.selectPageList(current, size, userId);

        List<ChatDO> chatDOS = chatDOPage.getRecords();
        List<FindChatHistoryPageListRspVO> vos = null;
        if (CollUtil.isNotEmpty(chatDOS)) {
            vos = chatDOS.stream()
                    .map(chatDO -> FindChatHistoryPageListRspVO.builder()
                            .id(chatDO.getId())
                            .uuid(chatDO.getUuid())
                            .summary(chatDO.getSummary())
                            .updateTime(chatDO.getUpdateTime())
                            .build())
                    .collect(Collectors.toList());
        }

        return PageResponse.success(chatDOPage, vos);
    }

    /**
     * 重命名对话摘要（归属校验）
     */
    @Override
    public Response<?> renameChatSummary(RenameChatReqVO reqVO, Long userId) {
        Long chatId = reqVO.getId();
        String summary = reqVO.getSummary();

        // 归属校验
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getId, chatId)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        chatMapper.updateById(ChatDO.builder()
                .id(chatId)
                .summary(summary)
                .build());

        return Response.success();
    }

    /**
     * 删除对话（归属校验）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> deleteChat(DeleteChatReqVO reqVO, Long userId) {
        String uuid = reqVO.getUuid();

        // 归属校验
        ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                .eq(ChatDO::getUuid, uuid)
                .eq(ChatDO::getUserId, userId));
        if (chat == null) {
            throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
        }

        chatMapper.deleteById(chat.getId());

        chatMessageMapper.delete(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, uuid));

        return Response.success();
    }
}
