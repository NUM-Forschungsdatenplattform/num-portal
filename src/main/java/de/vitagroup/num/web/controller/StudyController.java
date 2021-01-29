package de.vitagroup.num.web.controller;

import com.nimbusds.openid.connect.sdk.claims.ClaimsSet;
import de.vitagroup.num.domain.Comment;
import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.StudyStatus;
import de.vitagroup.num.domain.dto.CommentDto;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.mapper.CommentMapper;
import de.vitagroup.num.mapper.StudyMapper;
import de.vitagroup.num.service.CommentService;
import de.vitagroup.num.service.StudyService;
import de.vitagroup.num.web.config.Role;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/study")
public class StudyController {

  private final StudyService studyService;
  private final CommentService commentService;
  private final StudyMapper studyMapper;
  private final CommentMapper commentMapper;

  @GetMapping()
  @ApiOperation(
      value =
          "Retrieves a list of studies the user is allowed to see with optional filtering based on study status")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<StudyDto>> searchStudies(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestParam(required = false) StudyStatus status) {
    Map<String, Object> access = principal.getClaimAsMap("realm_access");
    List<String> roles = (List<String>) access.get("roles");
    return ResponseEntity.ok(
        studyService.searchStudies(principal.getSubject(), roles, status).stream()
            .map(studyMapper::convertToDto)
            .collect(Collectors.toList()));
  }

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

  @PostMapping()
  @ApiOperation(
      value = "Creates a study; the logged in user is assigned as coordinator of the study")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<StudyDto> createStudy(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    Study study = studyService.createStudy(studyDto, principal.getSubject());

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }

  @PutMapping(value = "/{id}")
  @ApiOperation(
      value =
          "Updates a study; the logged in user is assigned as coordinator of the study at creation time")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<StudyDto> updateStudy(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long studyId,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    Study study = studyService.updateStudy(studyDto, studyId, principal.getSubject());

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }

  @GetMapping("/{studyId}/comment")
  @ApiOperation(value = "Retrieves the list of attached comments to a particular study")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<CommentDto>> getComments(
      @NotNull @NotEmpty @PathVariable Long studyId) {
    return ResponseEntity.ok(
        commentService.getComments(studyId).stream()
            .map(commentMapper::convertToDto)
            .collect(Collectors.toList()));
  }

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

  @DeleteMapping("/{studyId}/comment/{commentId}")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  void deleteComment(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @NotEmpty @PathVariable Long studyId,
      @NotNull @NotEmpty @PathVariable Long commentId) {
    commentService.deleteComment(commentId, studyId, principal.getSubject());
  }
}
