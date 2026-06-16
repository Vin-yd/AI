package com.vin.ai.robot.model.vo.customerService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckFileRspVO {

    /**
     * 文件是否存在
     */
    private Boolean exists;

    /**
     * 是否需要上传
     */
    private Boolean needUpload;

    /**
     * 已上传成功的分片序号
     */
    private List<Integer> uploadedChunks;

    /**
     * 是否存在同名已完成文件（需用户确认覆盖）
     */
    private Boolean hasSameName;

    /**
     * 同名文件的记录 ID（用于覆盖时删除）
     */
    private Long sameNameFileId;

}
