package com.vin.ai.robot.model.vo.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendSmsReqVO {

    @NotBlank(message = "手机号不能为空")
    private String phone;
}
