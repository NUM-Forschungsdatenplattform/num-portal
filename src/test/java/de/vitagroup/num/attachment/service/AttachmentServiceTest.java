package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.service.exception.ResourceNotFound;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @InjectMocks
    private AttachmentService attachmentService;

    @Before
    public void setup() {
        Mockito.when(attachmentRepository.findById(1L)).thenReturn(Optional.of(Attachment.builder()
                .id(1L)
                .name("dummyFile.pdf")
                .authorId("author-id")
                .content("content".getBytes())
                .build()));
        Mockito.when(attachmentRepository.findById(2L)).thenReturn(Optional.empty());
    }

    @Test
    public void listAttachmentsTest() {
        attachmentService.listAttachments();
        Mockito.verify(attachmentRepository, Mockito.times(1)).getAttachments();
    }

    @Test
    public void getAttachmentByIdTest() {
        attachmentService.getAttachmentById(1L);
        Mockito.verify(attachmentRepository, Mockito.times(1)).findById(1L);
    }

    @Test(expected = ResourceNotFound.class)
    public void getAttachmentByIdAndExpectResourceNotFound() {
        attachmentService.getAttachmentById(2L);
    }

    @Test
    public void deleteByIdTest() {
        attachmentService.deleteById(1L, "loggedIn-user");
        Mockito.verify(attachmentRepository, Mockito.times(1)).deleteAttachment(1L);
    }

    @Test(expected = ResourceNotFound.class)
    public void deleteByIdAndExpectResourceNotFoundTest() {
        attachmentService.deleteById(2L, "loggedIn-user");
    }

    @Test
    public void saveAttachmentTest() throws IOException {
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "content".getBytes());
        attachmentService.saveAttachment(mockFile, null, "author-id");
        Mockito.verify(attachmentRepository, Mockito.times(1)).saveAttachment(Mockito.any(AttachmentDto.class));
    }
}
