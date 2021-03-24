package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.ExportType;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.dto.CommentDto;
import de.vitagroup.num.domain.dto.RawQueryDto;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.mapper.CommentMapper;
import de.vitagroup.num.mapper.StudyMapper;
import de.vitagroup.num.service.CommentService;
import de.vitagroup.num.service.StudyService;
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
@RequestMapping(value = "/study", produces = "application/json")
public class StudyController {

  private final StudyService studyService;
  private final CommentService commentService;
  private final StudyMapper studyMapper;
  private final CommentMapper commentMapper;

  @AuditLog
  @GetMapping()
  @ApiOperation(value = "Retrieves a list of studies the user is allowed to see")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<StudyDto>> getStudies(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        studyService.getStudies(principal.getSubject(), Roles.extractRoles(principal)).stream()
            .map(studyMapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves a study by id")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<StudyDto> getStudyById(@NotNull @NotEmpty @PathVariable Long id) {
    Optional<Study> study = studyService.getStudyById(id);

    if (study.isEmpty()) {
      throw new ResourceNotFound("Study not found");
    }

    return ResponseEntity.ok(studyMapper.convertToDto(study.get()));
  }

  @AuditLog
  @PostMapping()
  @ApiOperation(
      value = "Creates a study; the logged in user is assigned as coordinator of the study")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<StudyDto> createStudy(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    Study study =
        studyService.createStudy(studyDto, principal.getSubject(), Roles.extractRoles(principal));

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }

  @AuditLog
  @PutMapping(value = "/{id}")
  @ApiOperation(
      value =
          "Updates a study; the logged in user is assigned as coordinator of the study at creation time")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_APPROVER)
  public ResponseEntity<StudyDto> updateStudy(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long studyId,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    Study study;
    if (Roles.hasRole(Roles.STUDY_APPROVER, principal)) {
      study =
          studyService.updateStudyStatus(
              studyDto, studyId, principal.getSubject(), Roles.extractRoles(principal));
    } else {
      study =
          studyService.updateStudy(
              studyDto, studyId, principal.getSubject(), Roles.extractRoles(principal));
    }
    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }

  @AuditLog
  @PostMapping("/{studyId}/execute")
  @ApiOperation(value = "Executes the aql")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<String> executeAql(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @NotEmpty @PathVariable Long studyId) {
    return ResponseEntity.ok(
        studyService.executeAqlAndJsonify(query.getQuery(), studyId, principal.getSubject()));
  }

  @AuditLog
  @PostMapping(value = "/{studyId}/export")
  @ApiOperation(value = "Executes the aql and returns the result as a csv file attachment")
  @PreAuthorize(Role.RESEARCHER)
  public ResponseEntity<StreamingResponseBody> exportResults(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestBody @Valid RawQueryDto query,
      @NotNull @NotEmpty @PathVariable Long studyId,
      @RequestParam(required = false)
          @ApiParam(
              value =
                  "A string defining the output format. Valid values are 'csv' and 'json'. Default is csv.")
          ExportType format) {
    StreamingResponseBody streamingResponseBody =
        studyService.getExportResponseBody(
            query.getQuery(), studyId, principal.getSubject(), format);
    MultiValueMap<String, String> headers = studyService.getExportHeaders(format, studyId);

    return new ResponseEntity<>(streamingResponseBody, headers, HttpStatus.OK);
  }

  @AuditLog
  @GetMapping("/{studyId}/comment")
  @ApiOperation(value = "Retrieves the list of attached comments to a particular study")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<CommentDto>> getComments(@AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long studyId) {
    return ResponseEntity.ok(
        commentService.getComments(studyId, principal.getSubject()).stream()
            .map(commentMapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @PostMapping("/{studyId}/comment")
  @ApiOperation(value = "Adds a comment to a particular study")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CommentDto> addComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long studyId,
      @Valid @NotNull @RequestBody CommentDto commentDto) {

    Comment comment =
        commentService.createComment(
            commentMapper.convertToEntity(commentDto), studyId, principal.getSubject());

    return ResponseEntity.ok(commentMapper.convertToDto(comment));
  }

  @AuditLog
  @PutMapping("/{studyId}/comment/{commentId}")
  @ApiOperation(value = "Updates a comment")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CommentDto> updateComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long studyId,
      @NotNull @NotEmpty @PathVariable Long commentId,
      @Valid @NotNull @RequestBody CommentDto commentDto) {

    Comment comment =
        commentService.updateComment(
            commentMapper.convertToEntity(commentDto), commentId, principal.getSubject(), studyId);

    return ResponseEntity.ok(commentMapper.convertToDto(comment));
  }

  @AuditLog
  @DeleteMapping("/{studyId}/comment/{commentId}")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public void deleteComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long studyId,
      @NotNull @NotEmpty @PathVariable Long commentId) {
    commentService.deleteComment(commentId, studyId, principal.getSubject());
  }
}
