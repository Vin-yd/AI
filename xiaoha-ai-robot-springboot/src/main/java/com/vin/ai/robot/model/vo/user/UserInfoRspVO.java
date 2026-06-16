package com.vin.ai.robot.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoRspVO {

    private Long id;
    private String phone;
    private String nickname;
    private String role;
    private String createTime;
}
