package de.vitagroup.num.attachment.domain.repository;

import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
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
                .thenReturn(List.of(new Attachment(3L, "dummy name", "dummy description", OffsetDateTime.now())));
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
}
