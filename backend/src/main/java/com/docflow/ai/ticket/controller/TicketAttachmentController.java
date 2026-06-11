package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.ticket.entity.TicketAttachment;
import com.docflow.ai.ticket.mapper.TicketAttachmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Ticket Attachments", description = "工单附件上传/下载/删除")
public class TicketAttachmentController {

    private final TicketAttachmentMapper attachmentMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    @Operation(summary = "获取工单附件列表")
    public ApiResponse<List<TicketAttachment>> listAttachments(@PathVariable Long ticketId) {
        List<TicketAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachment>()
                        .eq(TicketAttachment::getTicketId, ticketId)
                        .eq(TicketAttachment::getDeleted, 0)
                        .orderByDesc(TicketAttachment::getCreateTime));
        return ApiResponse.success(attachments);
    }

    @PostMapping
    @Operation(summary = "上传附件")
    public ApiResponse<TicketAttachment> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthUserPrincipal principal) throws IOException {

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;

        // Create upload directory
        String fullPath = uploadDir + "/" + datePath;
        File dir = new File(fullPath);
        if (!dir.exists()) dir.mkdirs();

        // Save file
        File targetFile = new File(dir, storedName);
        file.transferTo(targetFile);

        // Save to database
        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(ticketId);
        attachment.setFileName(originalName);
        attachment.setFileUrl("/" + datePath + "/" + storedName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploadUserId(principal.getUserId());
        attachment.setDeleted(0);
        attachmentMapper.insert(attachment);

        return ApiResponse.success(attachment);
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "删除附件（软删除）")
    public ApiResponse<Void> deleteAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        TicketAttachment attachment = new TicketAttachment();
        attachment.setId(attachmentId);
        attachment.setDeleted(1);
        attachmentMapper.updateById(attachment);
        return ApiResponse.success();
    }
}
