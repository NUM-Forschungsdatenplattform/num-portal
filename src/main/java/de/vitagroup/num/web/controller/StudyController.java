package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.mapper.StudyMapper;
import de.vitagroup.num.service.StudyService;
import de.vitagroup.num.web.exception.NotAuthorizedException;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/study")
public class StudyController {

  private final StudyService studyService;

  private final StudyMapper studyMapper;

  @GetMapping()
  @ApiOperation(value = "Retrieves a list of all studies in the portal")
  public ResponseEntity<List<StudyDto>> getAllStudies() {
    return ResponseEntity.ok(
        studyService.getAllStudies().stream()
            .map(studyMapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves a study by id")
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
  public ResponseEntity<StudyDto> createStudy(
      @AuthenticationPrincipal Jwt principal, @Valid @NotNull @RequestBody StudyDto studyDto) {

    if (principal == null || StringUtils.isEmpty(principal.getSubject())) {
      throw new NotAuthorizedException("Not authorized");
    }

    Study study =
        studyService.createStudy(studyMapper.convertToEntity(studyDto), principal.getSubject());

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }

  @PutMapping(value = "/{id}")
  @ApiOperation(
      value =
          "Updates a study; the logged in user is assigned as coordinator of the study at creation time")
  public ResponseEntity<StudyDto> updateStudy(
      @AuthenticationPrincipal Jwt principal,
      @PathVariable("id") Long studyId,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    if (principal == null || StringUtils.isEmpty(principal.getSubject())) {
      throw new NotAuthorizedException("Not authorized");
    }

    Study study = studyService.updateStudy(studyMapper.convertToEntity(studyDto), studyId);

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }
}
