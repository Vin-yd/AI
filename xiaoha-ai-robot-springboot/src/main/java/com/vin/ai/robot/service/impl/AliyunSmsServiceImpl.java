package com.vin.ai.robot.service.impl;

import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import com.vin.ai.robot.enums.ResponseCodeEnum;
import com.vin.ai.robot.exception.BizException;
import com.vin.ai.robot.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Slf4j
public class AliyunSmsServiceImpl implements SmsService {

    private static final String REGION_ID = "cn-hangzhou";
    private static final String VERSION = "2017-05-25";
    private static final String ACTION = "SendSms";
    private static final String SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String SIGNATURE_VERSION = "1.0";

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;
    @Value("${aliyun.sms.sign-name}")
    private String signName;
    @Value("${aliyun.sms.template-code}")
    private String templateCode;
    @Value("${aliyun.sms.endpoint}")
    private String endpoint;

    @Override
    public void sendCode(String phone, String code) {
        try {
            TreeMap<String, String> params = new TreeMap<>();
            params.put("AccessKeyId", accessKeyId);
            params.put("Action", ACTION);
            params.put("Format", "JSON");
            params.put("PhoneNumbers", phone);
            params.put("RegionId", REGION_ID);
            params.put("SignName", signName);
            params.put("SignatureMethod", SIGNATURE_METHOD);
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", SIGNATURE_VERSION);
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", "{\"code\":\"" + code + "\"}");
            params.put("Timestamp", ZonedDateTime.now(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
            params.put("Version", VERSION);

            // 计算签名
            String signature = sign(params, accessKeySecret + "&");
            params.put("Signature", signature);

            // 构建 URL 并发送 GET 请求
            String url = "https://" + endpoint + "/?" + buildQueryString(params);
            String response = HttpUtil.get(url, 5000);
            log.info("sms_sent phone={} response={}", phone, response);

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("sms_send_failed phone={}", phone, e);
            throw new BizException(ResponseCodeEnum.SMS_SEND_FAILED);
        }
    }

    /**
     * 阿里云 V1 签名算法
     */
    private String sign(TreeMap<String, String> params, String keySecret) throws Exception {
        String canonicalizedQueryString = buildCanonicalizedQueryString(params);
        String stringToSign = "GET" + "&" + percentEncode("/") + "&"
                + percentEncode(canonicalizedQueryString);

        byte[] signData = SecureUtil.hmacSha1(keySecret).digest(stringToSign);
        String signature = Base64.getEncoder().encodeToString(signData);
        return signature;
    }

    /**
     * 构建规范化的查询字符串（用于签名计算）
     */
    private String buildCanonicalizedQueryString(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(percentEncode(entry.getKey()))
                    .append("=")
                    .append(percentEncode(entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * 构建普通查询字符串（用于 URL，Signature 不编码）
     */
    private String buildQueryString(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            // Signature 需要特殊编码（阿里云要求）
            if ("Signature".equals(entry.getKey())) {
                sb.append("Signature=").append(URLUtil.encodeAll(entry.getValue()));
            } else {
                sb.append(percentEncode(entry.getKey()))
                        .append("=")
                        .append(percentEncode(entry.getValue()));
            }
        }
        return sb.toString();
    }

    /**
     * 阿里云 API 专用百分号编码
     * 不编码：A-Z a-z 0-9 - _ . ~
     * 空格编码为 %20
     */
    private String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append(c);
            } else {
                sb.append(String.format("%%%02X", b & 0xFF));
            }
        }
        return sb.toString();
    }
}
