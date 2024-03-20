package org.highmed.web.controller;

import org.highmed.attachment.domain.dto.AttachmentDto;
import org.highmed.attachment.domain.dto.LightAttachmentDto;
import org.highmed.attachment.domain.model.Attachment;
import org.highmed.attachment.service.AttachmentService;
import org.highmed.service.exception.CustomizedExceptionHandler;
import org.highmed.service.logger.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.highmed.web.config.Role;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/attachment", produces = "application/json")
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class NumAttachmentController extends CustomizedExceptionHandler {

    private final ModelMapper modelMapper;
    private final AttachmentService attachmentService;

    @AuditLog(description = "Create a new attachment")
    @PreAuthorize(Role.SUPER_ADMIN)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createAttachment(@AuthenticationPrincipal @NotNull Jwt principal, @RequestParam Long projectId,
                                                   @RequestParam(required = false) String description,
                                                   @NotNull @RequestPart("file") MultipartFile file) throws IOException {
        attachmentService.saveAttachment(file, description, principal.getSubject(), projectId);
        return ResponseEntity.ok("ok");
    }

    @AuditLog(description = "Create multiple attachments for a project with given ID")
    @Operation(description = "Create multiple attachments for a project with given ID")
    @PreAuthorize(Role.STUDY_COORDINATOR)
    @PostMapping(path = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createMultipleAttachments(@AuthenticationPrincipal @NotNull Jwt principal,
                                                            @NotNull @PathVariable Long projectId,
                                                            @ModelAttribute @Valid LightAttachmentDto lightDto) throws IOException {
        attachmentService.saveAttachments(projectId, principal.getSubject(), lightDto, false);
        return ResponseEntity.ok("ok");
    }

    @AuditLog(description = "Get a list of all attachments for one project (by projectId)")
    @Operation(description = "Get a list of all attachments for one project (by projectId)")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<AttachmentDto>> listAllAttachments(@NotNull @PathVariable Long projectId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsBy(projectId).stream()
                .map(attachment -> modelMapper.map(attachment, AttachmentDto.class))
                .collect(Collectors.toList()));
    }


    @AuditLog(description = "Retrieves a list of existing attachments")
    @Operation(description = "Retrieves a list of existing attachments")
    @GetMapping("/all")
    @PreAuthorize(Role.SUPER_ADMIN)
    public ResponseEntity<List<AttachmentDto>> listAttachments() {
        return ResponseEntity.ok(attachmentService.listAttachments().stream()
                .map(attachment -> modelMapper.map(attachment, AttachmentDto.class))
                .collect(Collectors.toList()));
    }

    @AuditLog(description = "Delete attachment")
    @DeleteMapping("/{attachmentId}")
    @Operation(description = "Delete attachment")
    @PreAuthorize(Role.SUPER_ADMIN)
    public void deleteAql(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long attachmentId) {
        attachmentService.deleteById(attachmentId, principal.getSubject());
    }

    @AuditLog(description = "Download attachment")
    @Operation(description = "Download attachment with given id")
    @GetMapping("/{attachmentId}")
    public ResponseEntity<StreamingResponseBody> downloadAttachment(@NotNull @PathVariable Long attachmentId) {
        Attachment attachment = attachmentService.getAttachmentById(attachmentId);
        HttpHeaders header = new HttpHeaders();
        header.setContentDisposition(ContentDisposition.builder("attachment").filename(attachment.getName()).build());
        StreamingResponseBody responseBody = outputStream -> {
            outputStream.write(attachment.getContent());
            outputStream.flush();
            outputStream.close();
        };
        return new ResponseEntity<>(responseBody, header, HttpStatus.OK);
    }
}
