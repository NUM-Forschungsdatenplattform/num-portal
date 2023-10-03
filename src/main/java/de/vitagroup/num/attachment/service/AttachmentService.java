package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional("attachmentTransactionManager")
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

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
        if (file.isEmpty()) {
            throw new BadRequestException(AttachmentService.class, "Invalid file. Missing content");
        }
        AttachmentDto model = AttachmentDto.builder()
                .name(file.getOriginalFilename())
                .description(description)
                .authorId(loggedInUserId)
                .type(file.getContentType())
                .content(file.getBytes())
                .build();
        attachmentRepository.saveAttachment(model);
    }

    public void deleteById(Long id, String loggedInUserId) {
        attachmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFound(AttachmentService.class, ExceptionsTemplate.ATTACHMENT_NOT_FOUND,
                        String.format(ExceptionsTemplate.ATTACHMENT_NOT_FOUND, id)));
        attachmentRepository.deleteAttachment(id);
        log.info("Attachment {} was deleted by user {}", id, loggedInUserId);
    }
}
