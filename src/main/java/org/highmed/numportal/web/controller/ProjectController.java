package org.highmed.numportal.web.controller;

import org.highmed.numportal.domain.dto.CommentDto;
import org.highmed.numportal.domain.dto.ProjectDto;
import org.highmed.numportal.domain.dto.ProjectViewDto;
import org.highmed.numportal.domain.dto.RawQueryDto;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.model.Comment;
import org.highmed.numportal.domain.model.ExportType;
import org.highmed.numportal.domain.model.Project;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.mapper.CommentMapper;
import org.highmed.numportal.mapper.ProjectMapper;
import org.highmed.numportal.mapper.ProjectViewMapper;
import org.highmed.numportal.service.CommentService;
import org.highmed.numportal.service.ProjectService;
import org.highmed.numportal.service.exception.CustomizedExceptionHandler;
import org.highmed.numportal.service.exception.ResourceNotFound;
import org.highmed.numportal.service.logger.ContextLog;
import org.highmed.numportal.service.util.ExportHeaderUtil;
import org.highmed.numportal.web.config.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.PROJECT_NOT_FOUND;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/project", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class ProjectController extends CustomizedExceptionHandler {

  private final ProjectService projectService;
  private final CommentService commentService;
  private final ProjectMapper projectMapper;
  private final CommentMapper commentMapper;
  private final ExportHeaderUtil exportHeaderUtil;

  private final ProjectViewMapper projectViewMapper;

  @GetMapping("/all")
  @Operation(description = "Retrieves a list of projects the user is allowed to see")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<Page<ProjectViewDto>> getProjects(@AuthenticationPrincipal @NotNull Jwt principal,
      @PageableDefault(size = 100) Pageable pageable, SearchCriteria criteria) {
    Page<Project> projectPage = projectService.getProjects(principal.getSubject(), Roles.extractRoles(principal), criteria, pageable);
    List<ProjectViewDto> content = projectPage.getContent()
                                              .stream()
                                              .map(projectViewMapper::convertToDto)
                                              .collect(Collectors.toList());
    return ResponseEntity.ok(new PageImpl<>(content, pageable, projectPage.getTotalElements()));
  }

  @GetMapping("/{id}")
  @Operation(description = "Retrieves a project by id")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<ProjectDto> getProjectById(@AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long id) {
    Optional<Project> project = projectService.getProjectById(principal.getSubject(), id);

    if (project.isEmpty()) {
      throw new ResourceNotFound(ProjectController.class, PROJECT_NOT_FOUND, String.format(PROJECT_NOT_FOUND, id));
    }

    return ResponseEntity.ok(projectMapper.convertToDto(project.get()));
  }

  @ContextLog(type = "ProjektManagement", description = "Create project")
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

  @ContextLog(type = "ProjektManagement", description = "Create multipart project")
  @PostMapping(path = "/new", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      description = "Creates a project; the logged in user is assigned as coordinator of the project")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<ProjectDto> createMultipartProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestPart ProjectDto projectDto,
      @RequestPart(value = "files", required = false) @Valid MultipartFile[] files) {

    Project project =
        projectService.createMultipartProject(
            projectDto, principal.getSubject(), Roles.extractRoles(principal), files);

    return ResponseEntity.ok(projectMapper.convertToDto(project));
  }

  @ContextLog(type = "ProjektManagement", description = "Update project")
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

  @ContextLog(type = "ProjektManagement", description = "Update multipart project")
  @PutMapping(value = "/new/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(description =
      "Updates a project; the logged in user is assigned as coordinator of the project at creation time")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_APPROVER)
  public ResponseEntity<ProjectDto> updateMultipartProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long projectId,
      @Valid @NotNull @RequestPart ProjectDto projectDto,
      @RequestPart(value = "files", required = false) @Valid MultipartFile[] files) {

    Project project =
        projectService.updateMultipartProject(
            projectDto, projectId, principal.getSubject(), Roles.extractRoles(principal), files);

    return ResponseEntity.ok(projectMapper.convertToDto(project));
  }

  @PostMapping("/{projectId}/execute")
  @Operation(description = "Executes the aql")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<String> executeAql(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @PathVariable Long projectId,
      @RequestParam(required = false) Boolean defaultConfiguration) {
    return ResponseEntity.ok(
        projectService.retrieveData(
            query.getQuery(), projectId, principal.getSubject(), defaultConfiguration));
  }


  @PostMapping(value = "/{projectId}/export")
  @Operation(description = "Executes the aql and returns the result as a csv file attachment")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<StreamingResponseBody> exportResults(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @PathVariable Long projectId,
      @RequestParam(required = false) Boolean defaultConfiguration,
      @RequestParam(required = false)
      @Parameter(description = "A string defining the output format. Valid values are 'csv' and 'json'. Default is csv.")
      ExportType format) {
    StreamingResponseBody streamingResponseBody =
        projectService.getExportResponseBody(
            query.getQuery(), projectId, principal.getSubject(), format, defaultConfiguration);
    MultiValueMap<String, String> headers = exportHeaderUtil.getExportHeaders(format, projectId);

    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }

  @GetMapping("/{projectId}/comment")
  @Operation(description = "Retrieves the list of attached comments to a particular project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<CommentDto>> getComments(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long projectId) {
    return ResponseEntity.ok(
        commentService.getComments(projectId, principal.getSubject()).stream()
                      .map(commentMapper::convertToDto)
                      .collect(Collectors.toList()));
  }

  @ContextLog(type = "ProjektManagement", description = "Add comment")
  @PostMapping("/{projectId}/comment")
  @Operation(description = "Adds a comment to a particular project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CommentDto> addComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long projectId,
      @Valid @RequestBody CommentDto commentDto) {

    Comment comment =
        commentService.createComment(
            commentMapper.convertToEntity(commentDto), projectId, principal.getSubject());

    return ResponseEntity.ok(commentMapper.convertToDto(comment));
  }

  @ContextLog(type = "ProjektManagement", description = "Update comment")
  @PutMapping("/{projectId}/comment/{commentId}")
  @Operation(description = "Updates a comment")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CommentDto> updateComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long projectId,
      @NotNull @PathVariable Long commentId,
      @Valid @NotNull @RequestBody CommentDto commentDto) {

    Comment comment =
        commentService.updateComment(
            commentMapper.convertToEntity(commentDto),
            commentId,
            principal.getSubject(),
            projectId);

    return ResponseEntity.ok(commentMapper.convertToDto(comment));
  }

  @ContextLog(type = "ProjektManagement", description = "Delete comment")
  @DeleteMapping("/{projectId}/comment/{commentId}")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  @Operation(description = "Delete a comment")
  public void deleteComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long projectId,
      @NotNull @PathVariable Long commentId) {
    commentService.deleteComment(commentId, projectId, principal.getSubject());
  }

  @ContextLog(type = "ProjektManagement", description = "Delete project")
  @DeleteMapping("/{id}")
  @Operation(description = "Deletes a project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_SUPER_ADMIN)
  public void deleteProject(@AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable Long id) {
    projectService.deleteProject(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  @ContextLog(type = "ProjektManagement", description = "Archive project")
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
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=Project_" + id + ".txt");
    return new ResponseEntity<>(docBytes, headers, HttpStatus.OK);
  }
}
