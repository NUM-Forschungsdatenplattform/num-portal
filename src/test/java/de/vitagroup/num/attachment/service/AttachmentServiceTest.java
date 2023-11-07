package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.dto.LightAttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.domain.model.Project;
import de.vitagroup.num.domain.model.ProjectStatus;
import de.vitagroup.num.domain.model.admin.UserDetails;
import de.vitagroup.num.domain.repository.ProjectRepository;
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
import java.util.List;
import java.util.Optional;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.ATTACHMENT_LIMIT_REACHED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.DESCRIPTION_TOO_LONG;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.DOCUMENT_TYPE_MISMATCH;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.INVALID_FILE_MISSING_CONTENT;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PDF_FILES_ARE_NOT_ATTACHED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PDF_FILE_SIZE_EXCEEDED;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;
import static de.vitagroup.num.domain.templates.ExceptionsTemplate.WRONG_PROJECT_STATUS;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private FileScanService fileScanService;

    @InjectMocks
    private AttachmentService attachmentService;

    @Mock
    private ProjectRepository projectRepository;

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
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5content".getBytes());
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
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile", "application/pdf", "%PDF-1.5 content".getBytes());
        try {
            attachmentService.saveAttachment(mockFile, null, "author-id");
        }catch (BadRequestException fe) {
            Assert.assertEquals(DOCUMENT_TYPE_MISMATCH, fe.getMessage());
        }
    }

    @Test
    public void pdfFileExceededException() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 1);
        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5".getBytes());
        try {
            attachmentService.saveAttachment(mockFile, null, "author-id");
        }catch (BadRequestException fe) {
            Assert.assertEquals(String.format(PDF_FILE_SIZE_EXCEEDED, 0, 0), fe.getMessage());
        }
    }

    @Test
    public void projectExist() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        instantiateProject(ProjectStatus.DRAFT);

        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5 content".getBytes());
        MultipartFile[] multipartFiles = { mockFile };

        LightAttachmentDto attachmentDto = LightAttachmentDto.builder()
                .description(List.of("first file"))
                .files(multipartFiles).build();
        attachmentService.saveAttachments(1L, "author-id", attachmentDto, false);
    }

    private void instantiateProject(ProjectStatus status) {
        Project project =
                Project.builder()
                        .id(1L)
                        .name("Project")
                        .status(status)
                        .coordinator(UserDetails.builder().userId("approvedCoordinatorId").build())
                        .build();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    }

    @Test
    public void projectDoesNotExist() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        instantiateProject(ProjectStatus.DRAFT);

        try {
            attachmentService.saveAttachments(0L, "author-id", null, false);
        }catch (ResourceNotFound fe) {
            Assert.assertEquals(String.format(PROJECT_NOT_FOUND, 0L), fe.getMessage());
        }
    }

    @Test
    public void filesAreNotAttached() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        instantiateProject(ProjectStatus.DRAFT);

        LightAttachmentDto attachmentDto = LightAttachmentDto.builder()
                .description(List.of("first file"))
                .build();
        try {
            attachmentService.saveAttachments(1L, "author-id", attachmentDto, false);
        }catch (ResourceNotFound fe) {
            Assert.assertEquals(PDF_FILES_ARE_NOT_ATTACHED, fe.getMessage());
        }
    }

    @Test
    public void descriptionIsTooLong() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        instantiateProject(ProjectStatus.DRAFT);

        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5 content".getBytes());
        MultipartFile[] multipartFiles = { mockFile };

        String description = " first file description first file description first file description first file description first file description first file description first file description first file description first file description first file description first file description ";
        LightAttachmentDto attachmentDto = LightAttachmentDto.builder()
                .description(List.of(description))
                .files(multipartFiles).build();
        try {
            attachmentService.saveAttachments(1L, "author-id", attachmentDto, false);
        }catch (BadRequestException fe) {
            Assert.assertEquals(String.format(DESCRIPTION_TOO_LONG, description), fe.getMessage());
        }
    }

    @Test
    public void wrongProjectStatus() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        instantiateProject(ProjectStatus.APPROVED);

        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5 content".getBytes());
        MultipartFile[] multipartFiles = { mockFile };

        LightAttachmentDto attachmentDto = LightAttachmentDto.builder()
                .description(List.of("first file description"))
                .files(multipartFiles).build();
        try {
            attachmentService.saveAttachments(1L, "author-id", attachmentDto, false);
        }catch (BadRequestException fe) {
            Assert.assertEquals(String.format(WRONG_PROJECT_STATUS, ProjectStatus.APPROVED), fe.getMessage());
        }
    }

    @Test
    public void tooManyFilesForUpload() throws IOException {
        ReflectionTestUtils.setField(attachmentService, "pdfFileSize", 10485760);
        instantiateProject(ProjectStatus.CHANGE_REQUEST);

        MultipartFile mockFile = new MockMultipartFile("testFile", "testFile.pdf", "application/pdf", "%PDF-1.5 content".getBytes());
        MultipartFile[] multipartFiles = { mockFile, mockFile, mockFile, mockFile, mockFile, mockFile, mockFile, mockFile, mockFile, mockFile, mockFile };

        LightAttachmentDto attachmentDto = LightAttachmentDto.builder()
                .description(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                .files(multipartFiles).build();
        try {
            attachmentService.saveAttachments(1L, "author-id", attachmentDto, false);
        }catch (BadRequestException fe) {
            Assert.assertEquals(ATTACHMENT_LIMIT_REACHED, fe.getMessage());
        }
    }
}
