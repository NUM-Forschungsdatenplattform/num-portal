package de.vitagroup.num.attachment;

import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository {

    List<Attachment> getAttachments();

    void saveAttachment(AttachmentDto model);

    void deleteAttachment(Long id);

    Optional<Attachment> findById(Long id);

    void updateReviewCounterByProjectId(Long projectId);

    Optional<Attachment> findByIdAndProjectId(Long id, Long projectId);

    List<Attachment> findAttachmentsByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
