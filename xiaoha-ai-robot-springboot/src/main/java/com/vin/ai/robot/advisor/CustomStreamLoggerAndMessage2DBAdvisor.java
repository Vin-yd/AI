package com.vin.ai.robot.advisor;

import com.vin.ai.robot.domain.dos.ChatMessageDO;
import com.vin.ai.robot.domain.mapper.ChatMessageMapper;
import com.vin.ai.robot.model.vo.chat.AiChatReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CustomStreamLoggerAndMessage2DBAdvisor implements StreamAdvisor {

    @Override
    public int getOrder() {
        return 99; // order 值越小，越先执行
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    private final ChatMessageMapper chatMessageMapper;
    private final AiChatReqVO aiChatReqVO;
    private final TransactionTemplate transactionTemplate;

    public CustomStreamLoggerAndMessage2DBAdvisor(ChatMessageMapper chatMessageMapper,
                                                  AiChatReqVO aiChatReqVO,
                                                  TransactionTemplate transactionTemplate) {
        this.chatMessageMapper = chatMessageMapper;
        this.aiChatReqVO = aiChatReqVO;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        // 对话 UUID
        String chatUuid = aiChatReqVO.getChatId();
        // 用户消息
        String userMessage = aiChatReqVO.getMessage();

        Flux<ChatClientResponse> chatClientResponseFlux = streamAdvisorChain.nextStream(chatClientRequest);

        // 创建 AI 流式推理过程聚合容器（线程安全）
        AtomicReference<StringBuilder> fullReasoning = new AtomicReference<>(new StringBuilder());

        // 创建 AI 流式回答聚合容器（线程安全）
        AtomicReference<StringBuilder> fullContent = new AtomicReference<>(new StringBuilder());

        // 返回处理后的流
        return chatClientResponseFlux
                .doOnNext(response -> {
                    // 获取响应
                    ChatResponse chatResponse = response.chatResponse();

                    // 判空
                    if (Objects.nonNull(chatResponse) && Objects.nonNull(chatResponse.getResult())) {
                        // 获取 AI 回复的消息
                        AssistantMessage message = chatResponse.getResult().getOutput();

                        // 获取推理内容（如果存在）
                        String reasoningChunk = message.getMetadata().get("reasoningContent").toString();

                        // 逐块收集正式回答
                        String chunk = message.getText();

                        if (reasoningChunk != null) {
                            log.info("## reasoning chunk: {}", reasoningChunk);
                            fullReasoning.get().append(reasoningChunk);
                        }

                        // 若 chunk 块不为空，则追加到 fullContent 中
                        if (chunk != null) {
                            log.info("## chunk: {}", chunk);
                            fullContent.get().append(chunk);
                        }
                    }
                }).doOnComplete(() -> {
                    // 流完成后打印完整推理过程
                    String completeReasoning = fullReasoning.get().toString();
                    log.info("\n==== FULL Reasoning RESPONSE ====\n{}\n========================", completeReasoning);

                    // 流完成后打印完整回答
                    String completeResponse = fullContent.get().toString();
                    log.info("\n==== FULL AI RESPONSE ====\n{}\n========================", completeResponse);

                    // 开启编程式事务
                    transactionTemplate.execute(status -> {
                        try {
                            // 1. 存储用户消息
                            chatMessageMapper.insert(ChatMessageDO.builder()
                                    .chatUuid(chatUuid)
                                    .content(userMessage)
                                    .role(MessageType.USER.getValue()) // 用户消息
                                    .createTime(LocalDateTime.now())
                                    .build());


                            // 2. 存储 AI 回答
                            chatMessageMapper.insert(ChatMessageDO.builder()
                                    .chatUuid(chatUuid)
                                    .content(completeResponse)
                                    .role(MessageType.ASSISTANT.getValue()) // AI 回答
                                    .reasoningContent(completeReasoning) // 推理内容
                                    .createTime(LocalDateTime.now())
                                    .build());

                            return true;
                        } catch (Exception ex) {
                            status.setRollbackOnly(); // 标记事务为回滚
                            log.error("", ex);
                        }
                        return false;
                    });
                })
                .doOnError(error -> {
                    // 客户端主动断开连接属于正常行为，仅记录 WARN 日志
                    if (error instanceof IOException) {
                        log.warn("## 客户端断开 SSE 连接，已收集部分回答: {}", fullContent.get().toString());
                        return;
                    }
                    // 出错时打印已收集的部分
                    String partialResponse = fullContent.get().toString();
                    log.error("## Stream 流出现错误，已收集回答如下: {}", partialResponse, error);
                });
    }
}
