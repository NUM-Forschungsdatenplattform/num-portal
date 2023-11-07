package de.vitagroup.num.attachment.service;

import de.vitagroup.num.attachment.AttachmentRepository;
import de.vitagroup.num.attachment.domain.dto.AttachmentDto;
import de.vitagroup.num.attachment.domain.dto.LightAttachmentDto;
import de.vitagroup.num.attachment.domain.model.Attachment;
import de.vitagroup.num.domain.model.Project;
import de.vitagroup.num.domain.model.ProjectStatus;
import de.vitagroup.num.domain.templates.ExceptionsTemplate;
import de.vitagroup.num.service.ProjectService;
import de.vitagroup.num.service.exception.BadRequestException;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.web.controller.NumAttachmentController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Transactional("attachmentTransactionManager")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ProjectService projectService;

    @Value("${num.pdfFileSize:10485760}")
    private long pdfFileSize;

    @Value("${num.fileVirusScanEnabled}")
    private boolean fileVirusScanEnabled;
    private final FileScanService fileScanService;

    public List<Attachment> listAttachments() {
        return attachmentRepository.getAttachments();
    }

    public Attachment getAttachmentById(Long id) {
        return attachmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFound(AttachmentService.class, ExceptionsTemplate.ATTACHMENT_NOT_FOUND,
                        String.format(ExceptionsTemplate.ATTACHMENT_NOT_FOUND, id)));
    }

    public void saveAttachment(MultipartFile file, String description, String loggedInUserId) throws IOException {

        validate(file);
        AttachmentDto model = buildModel(file, description, loggedInUserId, -1L);
        attachmentRepository.saveAttachment(model);
    }

    private static AttachmentDto buildModel(MultipartFile file, String description, String loggedInUserId, Long projectId) throws IOException {
        return AttachmentDto.builder()
                .name(file.getOriginalFilename())
                .description(description)
                .authorId(loggedInUserId)
                .projectId(projectId)
                .type(file.getContentType())
                .content(file.getBytes())
                .build();
    }

    private void validate(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            log.error("File content is missing for uploaded file {}", file.getOriginalFilename());
            throw new BadRequestException(AttachmentService.class, INVALID_FILE_MISSING_CONTENT);
        }
        if (Boolean.TRUE.equals(fileVirusScanEnabled)) {
            fileScanService.virusScan(file);
        } else {
            log.warn("File scan for virus/malware is not enabled");
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".pdf") || !checkIsPDFContent(file.getBytes())){
            log.error("Invalid document type received for {}", file.getOriginalFilename());
            throw new BadRequestException(NumAttachmentController.class, DOCUMENT_TYPE_MISMATCH);
        }
        if (file.getSize() > pdfFileSize){
            throw new BadRequestException(NumAttachmentController.class,
                    String.format(PDF_FILE_SIZE_EXCEEDED, pdfFileSize/1048576, file.getSize()/1048576));
        }
    }

    private boolean checkIsPDFContent(byte[] data) {
        if(data.length < 5) {
            return false;
        }

        //%PDF-
        if(!((data[0] == 0x25) &&(data[1] == 0x50)&&(data[2] == 0x44)&&(data[3] == 0x46)&&(data[4] == 0x2D))){
            return false;
        }

        // version is 1.<PDF version>
        if(!(data[5]==0x31 && data[6]==0x2E )){
            return false;
        }
        return true;
    }

    public void deleteById(Long id, String loggedInUserId) {
        Optional<Attachment> attachment = attachmentRepository.findById(id);
        if (attachment.isPresent()) {
            attachmentRepository.deleteAttachment(id);
            log.info("Attachment {} was deleted by user {}", id, loggedInUserId);
        } else {
            log.error("Could not delete attachment {} because was not found", id);
            throw new ResourceNotFound(AttachmentService.class, ExceptionsTemplate.ATTACHMENT_NOT_FOUND,
                    String.format(ExceptionsTemplate.ATTACHMENT_NOT_FOUND, id));
        }
    }

    public void saveAttachments(Long projectId, String loggedInUserId, @Valid LightAttachmentDto lightDto, boolean isNewProject) throws IOException {
        if(isNewProject) {
            checkConsistency(lightDto, projectId, ProjectStatus.DRAFT);
        } else {
            Project project = projectService.getProjectById(loggedInUserId, projectId)
                    .orElseThrow(() -> new ResourceNotFound(AttachmentService.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, projectId)));

            checkConsistency(lightDto, projectId, project.getStatus());
        }

        int i = 0;
        for (MultipartFile file: lightDto.getFiles()) {
            if(nonNull(lightDto.getDescription()) && (lightDto.getDescription().size() > i)) {
                saveAttachment(file, lightDto.getDescription().get(i++), loggedInUserId, projectId);
            } else {
                saveAttachment(file, Strings.EMPTY, loggedInUserId, projectId);
            }
        }
    }

    public List<Attachment> getAttachmentsBy(Long projectId) {
        return attachmentRepository.findAttachmentsByProjectId(projectId);
    }

        public boolean isInsertable(ProjectStatus status) {
        return (ProjectStatus.DRAFT.equals(status) || ProjectStatus.CHANGE_REQUEST.equals(status));
    }

    private void checkConsistency(LightAttachmentDto lightDto, Long projectId, ProjectStatus status) {
        if(isNull(lightDto.getFiles()) || lightDto.getFiles().length == 1 && Objects.equals(lightDto.getFiles()[0].getOriginalFilename(), Strings.EMPTY)){
            throw new ResourceNotFound(AttachmentService.class, PDF_FILES_ARE_NOT_ATTACHED);
        }

        if(nonNull(lightDto.getDescription())) {
            for (String description : lightDto.getDescription()) {
                if (description.length() > 255) {
                    throw new BadRequestException(AttachmentService.class, DESCRIPTION_TOO_LONG, String.format(DESCRIPTION_TOO_LONG, description));
                }
            }
        }

        if(!isInsertable(status)){
            throw new BadRequestException(AttachmentService.class, WRONG_PROJECT_STATUS, String.format(WRONG_PROJECT_STATUS, status));
        }

        if(attachmentRepository.findAttachmentsByProjectId(projectId).size() + lightDto.getFiles().length > 10){
            throw new BadRequestException(AttachmentService.class, ATTACHMENT_LIMIT_REACHED);
        }
    }

    private void saveAttachment(MultipartFile file, String description, String loggedInUserId, Long projectId) throws IOException {
        validate(file);
        AttachmentDto model = buildModel(file, description, loggedInUserId, projectId);
        attachmentRepository.saveAttachment(model);
    }

}
