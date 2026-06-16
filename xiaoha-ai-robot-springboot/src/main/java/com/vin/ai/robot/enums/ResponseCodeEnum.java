package com.vin.ai.robot.enums;

import com.vin.ai.robot.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("10001", "参数错误"),


    // ----------- 业务异常状态码 -----------
    CHAT_NOT_EXISTED("20000", "此对话不存在"),
    UPLOAD_FILE_CANT_EMPTY("20001", "上传文件不能为空"),
    ONLY_SUPPORT_MARKDOWN("20002", "仅支持 Markdown 文件（.md 后缀）"),
    UPLOAD_FILE_FAILED("20003", "文件上传失败"),
    MARKDOWN_FILE_NOT_FOUND("20004", "Markdown 问答文件不存在"),
    MARKDOWN_FILE_CANT_DELETE("20005", "正在处理中的 Markdown 问答文件，不允许删除"),
    MERGE_CHUNK_NOT_FOUND("20006", "合并的分片文件不存在"),
    CHUNK_NUM_NOT_COMPLETE("20007", "分片数量不完整"),

    // ----------- 短信/登录相关 -----------
    SMS_TOO_FREQUENT("30001", "验证码发送过于频繁，请60秒后再试"),
    SMS_SEND_FAILED("30002", "短信发送失败，请稍后重试"),
    SMS_CODE_INVALID("30003", "验证码错误或已过期"),
    USER_NOT_LOGIN("30004", "请先登录"),
    USER_DISABLED("30005", "账号已被禁用"),
    FORBIDDEN("30006", "无权限，仅管理员可操作"),
    ;

    // 异常码
    private String errorCode;
    // 错误信息
    private String errorMessage;

}
