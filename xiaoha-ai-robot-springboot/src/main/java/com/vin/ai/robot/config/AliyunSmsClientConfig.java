package com.vin.ai.robot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AliyunSmsClientConfig {

    @Value("${aliyun.sms.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.sms.access-key-secret}")
    private String accessKeySecret;

    @Bean
    public com.aliyun.dypnsapi20170525.Client smsClient() {
        try {
            com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);

            config.endpoint = "dypnsapi.aliyuncs.com";

            return new com.aliyun.dypnsapi20170525.Client(config);
        } catch (Exception e) {
            log.error("初始化阿里云短信客户端失败", e);
            return null;
        }
    }
}
