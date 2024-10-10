package org.highmed.numportal.attachment.domain.repository;

import org.highmed.numportal.attachment.AttachmentRepository;
import org.highmed.numportal.attachment.domain.dto.AttachmentDto;
import org.highmed.numportal.attachment.domain.model.Attachment;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                                  .projectId(model.getProjectId())
                                  .uploadDate(OffsetDateTime.now())
                                  .type(model.getType())
                                  .content(model.getContent())
                                  .projectId(model.getProjectId())
                                  .build();
    entity = attachmentRepositoryJpa.save(entity);
    log.info("New attachment with id {} and name {} saved by {} ", entity.getId(), entity.getName(), entity.getAuthorId());
  }

  @Override
  public void deleteAttachment(Long id) {
    attachmentRepositoryJpa.deleteById(id);
  }

  @Override
  public Optional<Attachment> findById(Long id) {
    return attachmentRepositoryJpa.findById(id);
  }

  @Override
  @Transactional(transactionManager = "attachmentTransactionManager")
  public void updateReviewCounterByProjectId(Long projectId) {
    attachmentRepositoryJpa.updateReviewCounterByProjectId(projectId);
  }

  @Override
  public Optional<Attachment> findByIdAndProjectId(Long id, Long projectId) {
    return attachmentRepositoryJpa.findByIdAndProjectId(id, projectId);
  }

  @Override
  public List<Attachment> findAttachmentsByProjectId(Long projectId) {
    return attachmentRepositoryJpa.findAttachmentsByProjectId(projectId);
  }

  @Override
  public void deleteByProjectId(Long projectId) {
    attachmentRepositoryJpa.deleteByProjectId(projectId);
  }


}
