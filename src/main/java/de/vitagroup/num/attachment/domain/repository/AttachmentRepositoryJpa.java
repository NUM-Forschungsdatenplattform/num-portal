package de.vitagroup.num.attachment.domain.repository;

import de.vitagroup.num.attachment.domain.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepositoryJpa extends JpaRepository<Attachment, Long> {
}
