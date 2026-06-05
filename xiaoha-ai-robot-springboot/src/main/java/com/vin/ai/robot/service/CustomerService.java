package com.vin.ai.robot.service;

import com.vin.ai.robot.model.vo.customerService.*;
import com.vin.ai.robot.utils.PageResponse;
import com.vin.ai.robot.utils.Response;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerService {

//    /**
//     * 上传 Markdown 问答文件
//     * @param file
//     * @return
//     */
//    Response<?> uploadMarkdownFile(MultipartFile file);

    /**
     * 删除 Markdown 问答文件
     * @param deleteMarkdownFileReqVO
     * @return
     */
    Response<?> deleteMarkdownFile(DeleteMarkdownFileReqVO deleteMarkdownFileReqVO);

    /**
     * 分页查询 Markdown 问答文件
     * @param findMarkdownFilePageListReqVO
     * @return
     */
    PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO);

    /**
     * 修改  Markdown 问答文件信息
     * @param updateMarkdownFileReqVO
     * @return
     */
    Response<?> updateMarkdownFile(UpdateMarkdownFileReqVO updateMarkdownFileReqVO);

    /**
     * 检查文件是否存在
     * @param checkFileReqVO
     * @return
     */
    Response<CheckFileRspVO> checkFile(CheckFileReqVO checkFileReqVO);

    /**
     * 文件分片上传
     * @param uploadChunkReqVO
     * @return
     */
    Response<?> uploadChunk(UploadChunkReqVO uploadChunkReqVO);
    /**
     * 文件分片合并
     * @param mergeChunkReqVO
     * @return
     */
    Response<?> mergeChunk(MergeChunkReqVO mergeChunkReqVO);
}
