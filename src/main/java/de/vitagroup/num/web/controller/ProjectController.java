package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.ExportType;
import de.vitagroup.num.domain.Project;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.CommentDto;
import de.vitagroup.num.domain.dto.ManagerProjectDto;
import de.vitagroup.num.domain.dto.ProjectDto;
import de.vitagroup.num.domain.dto.RawQueryDto;
import de.vitagroup.num.mapper.CommentMapper;
import de.vitagroup.num.mapper.ProjectMapper;
import de.vitagroup.num.service.CommentService;
import de.vitagroup.num.service.ProjectService;
import de.vitagroup.num.service.ehrbase.Pseudonymity;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/project", produces = "application/json")
public class ProjectController {

  private final ProjectService projectService;
  private final CommentService commentService;
  private final ProjectMapper projectMapper;
  private final CommentMapper commentMapper;
  private final Pseudonymity pseudonymity;

  @AuditLog
  @GetMapping()
  @ApiOperation(value = "Retrieves a list of projects the user is allowed to see")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<ProjectDto>> getProjects(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        projectService.getProjects(principal.getSubject(), Roles.extractRoles(principal)).stream()
            .map(projectMapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves a project by id")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<ProjectDto> getProjectById(@NotNull @NotEmpty @PathVariable Long id) {
    Optional<Project> project = projectService.getProjectById(id);

    if (project.isEmpty()) {
      throw new ResourceNotFound("Project not found");
    }

    return ResponseEntity.ok(projectMapper.convertToDto(project.get()));
  }

  @AuditLog
  @PostMapping()
  @ApiOperation(
      value = "Creates a project; the logged in user is assigned as coordinator of the project")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<ProjectDto> createProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody ProjectDto projectDto) {

    Project project =
        projectService.createProject(
            projectDto, principal.getSubject(), Roles.extractRoles(principal));

    return ResponseEntity.ok(projectMapper.convertToDto(project));
  }

  @AuditLog
  @PutMapping(value = "/{id}")
  @ApiOperation(
      value =
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

  @AuditLog
  @PostMapping("/{projectId}/execute")
  @ApiOperation(value = "Executes the aql")
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
  @ApiOperation(
      value =
          "Executes the manager project aql in the cohort returning medical data matching the templates")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<String> executeManagerProject(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid ManagerProjectDto cohortTemplates,
      @RequestParam(required = false) Boolean defaultConfiguration) {
    return ResponseEntity.ok(
        projectService.executeManagerProject(
            cohortTemplates.getQuery(),
            cohortTemplates.getCohort(),
            cohortTemplates.getTemplates(),
            principal.getSubject(), defaultConfiguration));
  }

  @AuditLog
  @PostMapping(value = "/{projectId}/export")
  @ApiOperation(value = "Executes the aql and returns the result as a csv file attachment")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<StreamingResponseBody> exportResults(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @NotEmpty @PathVariable Long projectId,
      @RequestParam(required = false)
          @ApiParam(
              value =
                  "A string defining the output format. Valid values are 'csv' and 'json'. Default is csv.")
          ExportType format) {
    StreamingResponseBody streamingResponseBody =
        projectService.getExportResponseBody(
            query.getQuery(), projectId, principal.getSubject(), format);
    MultiValueMap<String, String> headers = projectService.getExportHeaders(format, projectId);

    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }

  @AuditLog
  @GetMapping("/{projectId}/comment")
  @ApiOperation(value = "Retrieves the list of attached comments to a particular project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<CommentDto>> getComments(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long projectId) {
    return ResponseEntity.ok(
        commentService.getComments(projectId, principal.getSubject()).stream()
            .map(commentMapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @PostMapping("/{projectId}/comment")
  @ApiOperation(value = "Adds a comment to a particular project")
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
  @ApiOperation(value = "Updates a comment")
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

  @AuditLog
  @DeleteMapping("/{id}")
  @ApiOperation(value = "Deletes a project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_SUPER_ADMIN)
  void deleteProject(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    projectService.deleteProject(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  @AuditLog
  @PostMapping("/{id}/archive")
  @ApiOperation(value = "Archive a project")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_SUPER_ADMIN)
  void archiveProject(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    projectService.archiveProject(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  @AuditLog
  @GetMapping("/{id}/resolve/{pseudonym}")
  @ApiOperation(value = "Archive a project")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<String> resolvePseudonym(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable Long id,
      @NotEmpty @PathVariable String pseudonym) {
    return ResponseEntity.ok(pseudonymity.getEhrIdFromPseudonym(pseudonym, id));
  }
}
