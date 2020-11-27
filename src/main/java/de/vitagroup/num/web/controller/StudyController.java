package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Study;
import de.vitagroup.num.domain.dto.StudyDto;
import de.vitagroup.num.mapper.StudyMapper;
import de.vitagroup.num.service.StudyService;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
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
  @ApiOperation(value = "Retrieves a list of all studies")
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
  @ApiOperation(value = "Creates a study")
  public ResponseEntity<StudyDto> createStudy(@Valid @NotNull @RequestBody StudyDto studyDto) {
    Study study = studyService.createStudy(studyMapper.convertToEntity(studyDto));
    return ResponseEntity.ok(studyMapper.convertToDto(study));
  }
}
