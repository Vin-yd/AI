package com.vin.ai.robot.config.sms;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AliyunSmsHelper {

    @Resource
    private com.aliyun.dypnsapi20170525.Client client;

    /**
     * 发送短信验证码
     */
    public boolean sendMessage(String signName, String templateCode, String phone, String templateParam) {
        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest request =
                new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                        .setSignName(signName)
                        .setTemplateCode(templateCode)
                        .setPhoneNumber(phone)
                        .setTemplateParam(templateParam);

        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();

        try {
            log.info("==> 开始短信发送, phone: {}, signName: {}, templateCode: {}, templateParam: {}",
                    phone, signName, templateCode, templateParam);

            com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse response =
                    client.sendSmsVerifyCodeWithOptions(request, runtime);

            log.info("==> 短信发送成功, response: {}", response.getBody() != null
                    ? response.getBody().getMessage() : "null");
            return true;
        } catch (Exception e) {
            log.error("==> 短信发送失败, phone: {}", phone, e);
            return false;
        }
    }
}
