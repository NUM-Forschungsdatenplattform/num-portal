package de.vitagroup.num.attachment.domain.repository;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Log4j2
@AllArgsConstructor
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentRepositoryImpl implements AttachmentRepository {

    private final AttachmentRepositoryJpa attachmentRepositoryJpa;
    @Override
    public List<Attachment> getAttachments() {
        return attachmentRepositoryJpa.getAttachments();
    }

    @Override
    public void saveAttachment(AttachmentDto model) {
        Attachment entity = Attachment.builder()
                .name(model.getName())
                .description(model.getDescription())
                .authorId(model.getAuthorId())
                .uploadDate(OffsetDateTime.now())
                .type(model.getType())
                .content(model.getContent())
                .build();
        attachmentRepositoryJpa.save(entity);
    }

    @Override
    public void deleteAttachment(Long id) {
        attachmentRepositoryJpa.deleteById(id);
    }

    @Override
    public Optional<Attachment> findById(Long id) {
        return attachmentRepositoryJpa.findById(id);
    }
}
