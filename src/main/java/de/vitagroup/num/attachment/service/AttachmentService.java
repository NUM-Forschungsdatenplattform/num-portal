package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ForbiddenException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.web.controller.NumAttachmentController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.*;

@Service
@Transactional(value = "attachmentTransactionManager")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${num.pdfFileSize:10485760}")
    private long pdfFileSize;

    @Value("${num.fileVirusScanEnabled}")
    private boolean fileVirusScanEnabled;
    private final FileScanService fileScanService;

    public List<Attachment> listAttachments() {
        return attachmentRepository.getAttachments();
    }

    public Attachment getAttachmentById(Long id) {
        return attachmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFound(AttachmentService.class, ExceptionsTemplate.ATTACHMENT_NOT_FOUND,
                        String.format(ExceptionsTemplate.ATTACHMENT_NOT_FOUND, id)));
    }

    public void saveAttachment(MultipartFile file, String description, String loggedInUserId) throws IOException {

        validate(file);
        AttachmentDto model = AttachmentDto.builder()
                .name(file.getOriginalFilename())
                .description(description)
                .authorId(loggedInUserId)
                .type(file.getContentType())
                .content(file.getBytes())
                .projectId(96L)
                .build();
        attachmentRepository.saveAttachment(model);
    }

    private void validate(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            log.error("File content is missing for uploaded file {}", file.getOriginalFilename());
            throw new BadRequestException(AttachmentService.class, INVALID_FILE_MISSING_CONTENT);
        }
        if (Boolean.TRUE.equals(fileVirusScanEnabled)) {
            fileScanService.virusScan(file);
        } else {
            log.warn("File scan for virus/malware is not enabled");
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".pdf") || !checkIsPDFContent(file.getBytes())){
            log.error("Invalid document type received for {}", file.getOriginalFilename());
            throw new BadRequestException(NumAttachmentController.class, DOCUMENT_TYPE_MISMATCH);
        }
        if (file.getSize() > pdfFileSize){
            throw new BadRequestException(NumAttachmentController.class,
                    String.format(PDF_FILE_SIZE_EXCEEDED, pdfFileSize/1048576, file.getSize()/1048576));
        }
    }

    private boolean checkIsPDFContent(byte[] data) {
        if(data.length < 5) {
            return false;
        }

        //%PDF-
        if(!((data[0] == 0x25) &&(data[1] == 0x50)&&(data[2] == 0x44)&&(data[3] == 0x46)&&(data[4] == 0x2D))){
            return false;
        }

        // version is 1.<PDF version>
        if(!(data[5]==0x31 && data[6]==0x2E )){
            return false;
        }
        return true;
    }

    public void deleteById(Long id, String loggedInUserId) {
        Optional<Attachment> attachment = attachmentRepository.findById(id);
        if (attachment.isPresent()) {
            attachmentRepository.deleteAttachment(id);
            log.info("Attachment {} was deleted by user {}", id, loggedInUserId);
        } else {
            log.error("Could not delete attachment {} because was not found", id);
            throw new ResourceNotFound(AttachmentService.class, ExceptionsTemplate.ATTACHMENT_NOT_FOUND,
                    String.format(ExceptionsTemplate.ATTACHMENT_NOT_FOUND, id));
        }
    }
    @Transactional(rollbackFor = ForbiddenException.class)
    public void updateStatusChangeCounter(Long projectId) {
        log.info("Update counter for project's {} attachments ", projectId);
        attachmentRepository.updateReviewCounterByProjectId(projectId);
    }

    public void deleteAttachments(Set<Long> attachmentsId, Long projectId, String loggedInUser, Boolean userIsApprover) {
        for(Long attachmentId : attachmentsId) {
            Optional<Attachment> attachment = attachmentRepository.findByIdAndProjectId(attachmentId, projectId);
            if(attachment.isEmpty()) {
                throw new ResourceNotFound(AttachmentService.class, "Attachment not found", String.format("Attachment not found: %s", attachmentId));
            }
            Attachment currentAttachment = attachment.get();
            if  (Boolean.FALSE.equals(userIsApprover) && currentAttachment.getReviewCounter() > 1) {
                log.error("Not allowed to delete attachment with id {} and name {} because it was at least once in review {}", attachmentId, currentAttachment.getName(), currentAttachment.getReviewCounter());
                throw new ForbiddenException(AttachmentService.class, CANNOT_DELETE_ATTACHMENT_INVALID_REVIEW_STATUS_COUNTER,
                        String.format(CANNOT_DELETE_ATTACHMENT_INVALID_REVIEW_STATUS_COUNTER, currentAttachment.getId()));
            }
            attachmentRepository.deleteAttachment(attachmentId);
        }
        log.info("Attachments with id {} from project {} deleted by user {} ", attachmentsId, projectId, loggedInUser);
    }
}
