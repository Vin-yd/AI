package com.vin.ai.robot.service;

import com.vin.ai.robot.model.vo.chat.*;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;

public interface ChatService {

    /**
     * 新建对话
     */
    Response<NewChatRspVO> newChat(NewChatReqVO newChatReqVO, Long userId);

    /**
     * 查询历史消息（含归属校验）
     */
    PageResponse<FindChatHistoryMessagePageListRspVO> findChatHistoryMessagePageList(
            FindChatHistoryMessagePageListReqVO reqVO, Long userId);

    /**
     * 查询历史对话
     */
    PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(
            FindChatHistoryPageListReqVO reqVO, Long userId);

    /**
     * 重命名对话摘要（含归属校验）
     */
    Response<?> renameChatSummary(RenameChatReqVO reqVO, Long userId);

    /**
     * 删除对话（含归属校验）
     */
    Response<?> deleteChat(DeleteChatReqVO reqVO, Long userId);
}
