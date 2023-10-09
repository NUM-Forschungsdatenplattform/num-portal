package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.DOCUMENT_TYPE_MISMATCH;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_FILE_MISSING_CONTENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PDF_FILE_SIZE_EXCEEDED;

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
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "content".getBytes());
        attachmentService.saveAttachment(mockFile, null, "author-id");
        Mockito.verify(attachmentRepository, Mockito.times(1)).saveAttachment(Mockito.any(AttachmentDto.class));
    }

    @Test
    public void checkEmptyFileError() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "".getBytes());
        try {
            attachmentService.saveAttachment(mockFile, null, "author-id");
        }catch (BadRequestException fe) {
            Assert.assertEquals(INVALID_FILE_MISSING_CONTENT, fe.getMessage());
        }
    }

    @Test
    public void checkDocumentTypeMismatchException() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile", "application/pdf", "content".getBytes());
        try {
            attachmentService.saveAttachment(mockFile, null, "author-id");
        }catch (BadRequestException fe) {
            Assert.assertEquals(DOCUMENT_TYPE_MISMATCH, fe.getMessage());
        }
    }

    @Test
    public void pdfFileExceededException() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 1);
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "content".getBytes());
        try {
            attachmentService.saveAttachment(mockFile, null, "author-id");
        }catch (BadRequestException fe) {
            Assert.assertEquals(String.format(PDF_FILE_SIZE_EXCEEDED, 0, 0), fe.getMessage());
        }
    }
}
