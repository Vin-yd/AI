package com.vin.ai.robot.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.utils.Response;
import com.vin.ai.robot.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Optional;

/**
 * @author: 犬小哈
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:14
 * @description: 全局异常处理
 **/
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 判断是否为 SSE 流式请求
     */
    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    /**
     * SSE 流式请求异常响应：手动将 JSON 写入 response body
     */
    private void writeSseErrorResponse(HttpServletResponse response, Response<Object> result) {
        try {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
            response.getWriter().flush();
        } catch (Exception ex) {
            log.error("写入 SSE 异常响应失败", ex);
        }
    }

    /**
     * 捕获自定义业务异常
     * @return
     */
    @ExceptionHandler({ BizException.class })
    @ResponseBody
    public Response<Object> handleBizException(HttpServletRequest request, HttpServletResponse httpResponse, BizException e) {
        log.warn("{} request fail, errorCode: {}, errorMessage: {}", request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        Response<Object> result = Response.fail(e);
        if (isSseRequest(request)) {
            writeSseErrorResponse(httpResponse, result);
            return null;
        }
        return result;
    }

    /**
     * 捕获参数校验异常
     * @return
     */
    @ExceptionHandler({ MethodArgumentNotValidException.class })
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, HttpServletResponse httpResponse, MethodArgumentNotValidException e) {
        // 参数错误异常码
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        // 获取 BindingResult
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder sb = new StringBuilder();

        // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确, 当前值: '123124qq.com';
        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(errors -> {
            errors.forEach(error ->
                    sb.append(error.getField())
                            .append(" ")
                            .append(error.getDefaultMessage())
                            .append(", 当前值: '")
                            .append(error.getRejectedValue())
                            .append("'; ")

            );
        });

        // 错误信息
        String errorMessage = sb.toString();

        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        Response<Object> result = Response.fail(errorCode, errorMessage);
        if (isSseRequest(request)) {
            writeSseErrorResponse(httpResponse, result);
            return null;
        }
        return result;
    }


    /**
     * 其他类型异常
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler({ Exception.class })
    @ResponseBody
    public Response<Object> handleOtherException(HttpServletRequest request, HttpServletResponse httpResponse, Exception e) {
        // SSE 流式请求中客户端主动断开连接导致的 IOException，无需返回响应，仅记录 WARN 日志
        if (e instanceof IOException && isSseRequest(request)) {
            log.warn("{} SSE 客户端断开连接: {}", request.getRequestURI(), e.getMessage());
            return null;
        }
        log.error("{} request error, ", request.getRequestURI(), e);
        Response<Object> result = Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
        if (isSseRequest(request)) {
            writeSseErrorResponse(httpResponse, result);
            return null;
        }
        return result;
    }
}

