package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.mapper.StudyMapper;
import de.vitagroup.num.service.StudyService;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
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
  @ApiOperation(value = "Retrieves a list of studies")
  public ResponseEntity<List<StudyDto>> searchStudies(
      @RequestParam(required = false) @NotEmpty String userId) {
    return ResponseEntity.ok(
        studyService.searchStudies(userId).stream()
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
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    Study study =
        studyService.createStudy(studyMapper.convertToEntity(studyDto), principal.getSubject());

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }

  @PutMapping(value = "/{id}")
  @ApiOperation(
      value =
          "Updates a study; the logged in user is assigned as coordinator of the study at creation time")
  public ResponseEntity<StudyDto> updateStudy(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long studyId,
      @Valid @NotNull @RequestBody StudyDto studyDto) {

    Study study = studyService.updateStudy(studyMapper.convertToEntity(studyDto), studyId);

    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }
}
