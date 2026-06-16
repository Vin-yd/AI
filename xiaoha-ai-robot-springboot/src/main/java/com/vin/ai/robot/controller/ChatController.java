package com.vin.ai.robot.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.vin.ai.robot.advisor.CustomChatMemoryAdvisor;
import com.vin.ai.robot.advisor.CustomStreamLoggerAndMessage2DBAdvisor;
import com.vin.ai.robot.advisor.NetworkSearchAdvisor;
import com.vin.ai.robot.aspect.ApiOperationLog;
import com.vin.ai.robot.domain.dos.ChatDO;
import com.vin.ai.robot.domain.mapper.ChatMapper;
import com.vin.ai.robot.domain.mapper.ChatMessageMapper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.model.vo.chat.*;
import com.vin.ai.robot.service.ChatService;
import com.vin.ai.robot.service.SearXNGService;
import com.vin.ai.robot.service.SearchResultContentFetcherService;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/chat")
@Slf4j
@SaCheckLogin
public class ChatController {

    @Resource
    private ChatService chatService;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Resource
    private ChatMapper chatMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SearXNGService searXNGService;
    @Resource
    private SearchResultContentFetcherService searchResultContentFetcherService;

    @PostMapping("/new")
    @ApiOperationLog(description = "新建对话")
    public Response<?> newChat(@RequestBody @Validated NewChatReqVO newChatReqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.newChat(newChatReqVO, userId);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "流式对话")
    public Flux<AIResponse> chat(@RequestBody @Validated AiChatReqVO aiChatReqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        String userMessage = aiChatReqVO.getMessage();
        String modelName = aiChatReqVO.getModelName();
        Double temperature = aiChatReqVO.getTemperature();
        boolean networkSearch = aiChatReqVO.getNetworkSearch();
        String chatId = aiChatReqVO.getChatId();

        // 校验对话归属
        if (StringUtils.isNotBlank(chatId)) {
            ChatDO chat = chatMapper.selectOne(Wrappers.<ChatDO>lambdaQuery()
                    .eq(ChatDO::getUuid, chatId)
                    .eq(ChatDO::getUserId, userId));
            if (chat == null) {
                throw new BizException(ResponseCodeEnum.CHAT_NOT_EXISTED);
            }
        }

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .build())
                .user(userMessage);

        List<Advisor> advisors = Lists.newArrayList();
        if (networkSearch) {
            advisors.add(new NetworkSearchAdvisor(searXNGService, searchResultContentFetcherService));
        } else {
            advisors.add(new CustomChatMemoryAdvisor(chatMessageMapper, aiChatReqVO, 50));
        }

        advisors.add(new CustomStreamLoggerAndMessage2DBAdvisor(chatMessageMapper, aiChatReqVO, transactionTemplate));
        advisors.add(new CustomChatMemoryAdvisor(chatMessageMapper, aiChatReqVO, 50));

        chatClientRequestSpec.advisors(advisors);

        return chatClientRequestSpec
                .stream()
                .chatResponse()
                .mapNotNull(chatResponse -> {
                    if (Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())) {
                        AssistantMessage message = chatResponse.getResult().getOutput();

                        String text = message.getText();

                        String reasoningContent = message.getMetadata().get("reasoningContent").toString();

                        if (StringUtils.isNotBlank(reasoningContent)) {
                            return AIResponse.builder().reasoning(reasoningContent).build();
                        }

                        return AIResponse.builder().v(text).build();
                    }
                    return null;
                });
    }

    @PostMapping("/message/list")
    @ApiOperationLog(description = "查询对话历史消息")
    public PageResponse<FindChatHistoryMessagePageListRspVO> findChatMessagePageList(
            @RequestBody @Validated FindChatHistoryMessagePageListReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.findChatHistoryMessagePageList(reqVO, userId);
    }

    @PostMapping("/list")
    @ApiOperationLog(description = "查询历史对话")
    public PageResponse<FindChatHistoryPageListRspVO> findChatHistoryPageList(
            @RequestBody @Validated FindChatHistoryPageListReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.findChatHistoryPageList(reqVO, userId);
    }

    @PostMapping("/summary/rename")
    @ApiOperationLog(description = "重命名对话摘要")
    public Response<?> renameChatSummary(@RequestBody @Validated RenameChatReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.renameChatSummary(reqVO, userId);
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除对话")
    public Response<?> deleteChat(@RequestBody @Validated DeleteChatReqVO reqVO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return chatService.deleteChat(reqVO, userId);
    }
}
