package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.mapper.CohortMapper;
import de.vitagroup.num.service.CohortService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cohort")
public class CohortController {

  private final CohortService cohortService;

  private final CohortMapper cohortMapper;

  @GetMapping
  @ApiOperation(value = "Retrieves a list of cohorts")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<List<CohortDto>> getAllCohorts() {
    List<Cohort> cohorts = cohortService.getAllCohorts();
    return ResponseEntity.ok(
        cohorts.stream().map(cohortMapper::convertToDto).collect(Collectors.toList()));
  }

  @PostMapping
  @ApiOperation(value = "Stores a cohort")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<CohortDto> createCohort(@Valid @NotNull @RequestBody CohortDto cohort) {
    Cohort cohortEntity = cohortService.createCohort(cohortMapper.convertToEntity(cohort));
    return ResponseEntity.ok(cohortMapper.convertToDto(cohortEntity));
  }

  @PostMapping("{cohortId}/execute")
  @ApiOperation(value = "Executes the cohort")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<Set<String>> executeCohort(@PathVariable String cohortId) {
    return ResponseEntity.ok(cohortService.executeCohort(Long.parseLong(cohortId)));
  }

  @PostMapping("{cohortId}/size")
  @ApiOperation(value = "Retrieves the cohort size")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<Long> retrieveCohortSize(@PathVariable String cohortId) {
    return ResponseEntity.ok(cohortService.getCohortSize(Long.parseLong(cohortId)));
  }
}
