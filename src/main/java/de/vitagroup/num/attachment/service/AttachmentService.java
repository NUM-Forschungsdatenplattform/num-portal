package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import de.vitagroup.num.service.exception.BadRequestException;
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

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.DOCUMENT_TYPE_MISMATCH;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_FILE_MISSING_CONTENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PDF_FILE_SIZE_EXCEEDED;

@Service
@Transactional("attachmentTransactionManager")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${num.pdfFileSize:10485760}")
    private long pdfFileSize;

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
                .build();
        attachmentRepository.saveAttachment(model);
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException(AttachmentService.class, INVALID_FILE_MISSING_CONTENT);
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".pdf")){
            throw new BadRequestException(NumAttachmentController.class, DOCUMENT_TYPE_MISMATCH);
        }
        if (file.getSize() > pdfFileSize){
            throw new BadRequestException(NumAttachmentController.class,
                    String.format(PDF_FILE_SIZE_EXCEEDED, pdfFileSize/1048576, file.getSize()/1048576));
        }
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
}
