package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.ExportType;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.*;
import de.vitagroup.num.mapper.CommentMapper;
import de.vitagroup.num.mapper.ProjectMapper;
import de.vitagroup.num.mapper.ProjectViewMapper;
import de.vitagroup.num.service.CommentService;
import de.vitagroup.num.service.ProjectService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.exception.ResourceNotFound;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.vitagroup.num.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/project", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class ProjectController extends CustomizedExceptionHandler {

  private final ProjectService projectService;
  private final CommentService commentService;
  private final ProjectMapper projectMapper;
  private final CommentMapper commentMapper;

  private final ProjectViewMapper projectViewMapper;

  @AuditLog
  @GetMapping("/all")
  @Operation(description = "Retrieves a list of projects the user is allowed to see")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<Page<ProjectViewTO>> getProjects(@AuthenticationPrincipal @NotNull Jwt principal, @PageableDefault(size = 100) Pageable pageable, SearchCriteria criteria) {
    Page<Project> projectPage = projectService.getProjects(principal.getSubject(), Roles.extractRoles(principal), criteria, pageable);
    List<ProjectViewTO> content = projectPage.getContent()
            .stream()
            .map(projectViewMapper::convertToDto)
            .collect(Collectors.toList());
    return ResponseEntity.ok(new PageImpl<>(content, pageable, projectPage.getTotalElements()));
  }

  @AuditLog
  @GetMapping("/{id}")
  @Operation(description = "Retrieves a project by id")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<ProjectDto> getProjectById(@AuthenticationPrincipal @NotNull Jwt principal,
                                                   @NotNull @NotEmpty @PathVariable Long id) {
    Optional<Project> project = projectService.getProjectById(principal.getSubject(), id);

    if (project.isEmpty()) {
      throw new ResourceNotFound(ProjectController.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, id));
    }

    return ResponseEntity.ok(projectMapper.convertToDto(project.get()));
  }

  @AuditLog(description = "Create project")
  @PostMapping()
  @Operation(
      description = "Creates a project; the logged in user is assigned as coordinator of the project")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<ProjectDto> createProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody ProjectDto projectDto) {

    Project project =
        projectService.createProject(
            projectDto, principal.getSubject(), Roles.extractRoles(principal));

    return ResponseEntity.ok(projectMapper.convertToDto(project));
  }

  @AuditLog(description = "Update project")
  @PutMapping(value = "/{id}")
  @Operation(description =
          "Updates a project; the logged in user is assigned as coordinator of the project at creation time")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_APPROVER)
  public ResponseEntity<ProjectDto> updateProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long projectId,
      @Valid @NotNull @RequestBody ProjectDto projectDto) {

    Project project =
        projectService.updateProject(
            projectDto, projectId, principal.getSubject(), Roles.extractRoles(principal));

    return ResponseEntity.ok(projectMapper.convertToDto(project));
  }

  @AuditLog(description = "Retrieve/download project data")
  @PostMapping("/{projectId}/execute")
  @Operation(description = "Executes the aql")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<String> executeAql(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @NotEmpty @PathVariable Long projectId,
      @RequestParam(required = false) Boolean defaultConfiguration) {
    return ResponseEntity.ok(
        projectService.retrieveData(
            query.getQuery(), projectId, principal.getSubject(), defaultConfiguration));
  }

  @AuditLog
  @PostMapping("/manager/execute")
  @Operation(
      description = "Executes the manager project aql in the cohort returning medical data matching the templates")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<String> executeManagerProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid ManagerProjectDto managerProjectDto) {
    return ResponseEntity.ok(
        projectService.executeManagerProject(
            managerProjectDto.getCohort(),
            managerProjectDto.getTemplates(),
            principal.getSubject()));
  }

  @AuditLog
  @PostMapping(value = "/{projectId}/export")
  @Operation(description = "Executes the aql and returns the result as a csv file attachment")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<StreamingResponseBody> exportResults(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @NotEmpty @PathVariable Long projectId,
      @RequestParam(required = false) Boolean defaultConfiguration,
      @RequestParam(required = false)
      @Parameter( description = "A string defining the output format. Valid values are 'csv' and 'json'. Default is csv.")
          ExportType format) {
    StreamingResponseBody streamingResponseBody =
        projectService.getExportResponseBody(
            query.getQuery(), projectId, principal.getSubject(), format, defaultConfiguration);
    MultiValueMap<String, String> headers = projectService.getExportHeaders(format, projectId);

    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }

  @AuditLog
  @PostMapping(value = "/manager/export")
  @Operation(description = "Executes the cohort default configuration returns the result as a csv file attachment")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<StreamingResponseBody> exportManagerResults(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid ManagerProjectDto managerProjectDto,
      @RequestParam(required = false)
      @Parameter(description = "A string defining the output format. Valid values are 'csv' and 'json'. Default is csv.")
          ExportType format) {
    StreamingResponseBody streamingResponseBody =
        projectService.getManagerExportResponseBody(
            managerProjectDto.getCohort(), managerProjectDto.getTemplates(), principal.getSubject(),
            format);
    MultiValueMap<String, String> headers = projectService.getExportHeaders(format, 0L);

    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }

  @AuditLog
  @GetMapping("/{projectId}/comment")
  @Operation(description = "Retrieves the list of attached comments to a particular project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<CommentDto>> getComments(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long projectId) {
    return ResponseEntity.ok(
        commentService.getComments(projectId, principal.getSubject()).stream()
            .map(commentMapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog(description = "Add comment")
  @PostMapping("/{projectId}/comment")
  @Operation(description = "Adds a comment to a particular project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CommentDto> addComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long projectId,
      @Valid @NotNull @RequestBody CommentDto commentDto) {

    Comment comment =
        commentService.createComment(
            commentMapper.convertToEntity(commentDto), projectId, principal.getSubject());

    return ResponseEntity.ok(commentMapper.convertToDto(comment));
  }

  @AuditLog
  @PutMapping("/{projectId}/comment/{commentId}")
  @Operation(description = "Updates a comment")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CommentDto> updateComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long projectId,
      @NotNull @NotEmpty @PathVariable Long commentId,
      @Valid @NotNull @RequestBody CommentDto commentDto) {

    Comment comment =
        commentService.updateComment(
            commentMapper.convertToEntity(commentDto),
            commentId,
            principal.getSubject(),
            projectId);

    return ResponseEntity.ok(commentMapper.convertToDto(comment));
  }

  @AuditLog
  @DeleteMapping("/{projectId}/comment/{commentId}")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public void deleteComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long projectId,
      @NotNull @NotEmpty @PathVariable Long commentId) {
    commentService.deleteComment(commentId, projectId, principal.getSubject());
  }

  @AuditLog(description = "Delete project")
  @DeleteMapping("/{id}")
  @Operation(description = "Deletes a project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_SUPER_ADMIN)
  public void deleteProject(@AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable Long id) {
    projectService.deleteProject(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  @AuditLog
  @PostMapping("/{id}/archive")
  @Operation(description = "Archive a project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_SUPER_ADMIN)
  public void archiveProject(@AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable Long id) {
    projectService.archiveProject(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  @GetMapping(value = "/{id}/document", produces = MediaType.TEXT_PLAIN_VALUE)
  @Operation(description = "Get the project info as a document")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_APPROVER)
  public ResponseEntity<byte[]> getProjectInfoPDF(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long id,
      @RequestParam
      @Parameter(description = "The language the document should be returned in (en/de)")
          String locale) {
    byte[] docBytes =
        projectService.getInfoDocBytes(id, principal.getSubject(), new Locale(locale));
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
    headers.add(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=Project_" + id + ".txt");
    return new ResponseEntity<>(docBytes, headers, HttpStatus.OK);
  }
}
