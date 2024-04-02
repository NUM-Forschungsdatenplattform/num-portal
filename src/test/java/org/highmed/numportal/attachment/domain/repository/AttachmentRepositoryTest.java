package org.highmed.numportal.attachment.domain.repository;

import org.highmed.numportal.attachment.domain.dto.AttachmentDto;
import org.highmed.numportal.attachment.domain.model.Attachment;
import org.highmed.numportal.attachment.domain.repository.AttachmentRepositoryImpl;
import org.highmed.numportal.attachment.domain.repository.AttachmentRepositoryJpa;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentRepositoryTest {

    @Mock
    private AttachmentRepositoryJpa attachmentRepositoryJpa;

    @InjectMocks
    private AttachmentRepositoryImpl attachmentRepository;

    @Test
    public void getAttachmentsTest() {
        Mockito.when(attachmentRepositoryJpa.getAttachments())
                .thenReturn(List.of(new Attachment(3L, "dummy name", "dummy description", OffsetDateTime.now(), 0)));
        attachmentRepository.getAttachments();
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).getAttachments();
    }

    @Test
    public void saveAttachmentTest() {
        AttachmentDto model = AttachmentDto.builder()
                .content("content".getBytes())
                .name("dummyFile.pdf")
                .type("application/pdf")
                .authorId("authorId")
                .uploadDate(OffsetDateTime.now())
                .build();
        Mockito.when(attachmentRepositoryJpa.save(Mockito.any(Attachment.class)))
                .thenReturn(Attachment.builder()
                        .id(1L)
                        .name("dummyFile.pdf")
                        .authorId("authorId")
                        .build());
        attachmentRepository.saveAttachment(model);
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).save(Mockito.any(Attachment.class));
    }

    @Test
    public void deleteAttachmentTest() {
        attachmentRepository.deleteAttachment(1L);
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).deleteById(1L);
    }

    @Test
    public void findByIdTest() {
        attachmentRepository.findById(1L);
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).findById(1L);
    }

    @Test
    public void updateReviewCounterByProjectIdTest() {
        attachmentRepository.updateReviewCounterByProjectId(9L);
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).updateReviewCounterByProjectId(9L);
    }

    @Test
    public void findByIdAndProjectIdTest() {
        attachmentRepository.findByIdAndProjectId(3L, 9L);
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).findByIdAndProjectId(3L, 9L);
    }

    @Test
    public void findAttachmentsByProjectIdTest() {
        attachmentRepository.findAttachmentsByProjectId(9L);
        Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).findAttachmentsByProjectId(9L);
    }

    @Test
    public void deleteByProjectIdTest() {
       attachmentRepository.deleteByProjectId(3L);
       Mockito.verify(attachmentRepositoryJpa, Mockito.times(1)).deleteByProjectId(3L);
    }
}
