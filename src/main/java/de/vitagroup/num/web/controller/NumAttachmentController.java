package de.vitagroup.num.web.controller;

import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.attachment.service.AttachmentService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    public ResponseEntity<StreamingResponseBody> downloadAttachment(@NotNull @NotEmpty @PathVariable Long attachmentId) {
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
