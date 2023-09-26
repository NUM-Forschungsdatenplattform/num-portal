package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.repository.AttachmentRepositoryJpa;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("attachmentTransactionManager")
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentService {

    private final AttachmentRepositoryJpa attachmentRepository;

    public void uploadAttachment(AttachmentDto attachmentDto) {
    }
}
