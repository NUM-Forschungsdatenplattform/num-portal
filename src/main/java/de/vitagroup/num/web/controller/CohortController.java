package de.vitagroup.num.web.controller;

import de.vitagroup.num.converter.CohortConverter;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.service.CohortService;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ehrbase.client.exception.WrongStatusCodeException;
import org.springframework.http.ResponseEntity;
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

  private final CohortConverter converter;

  @GetMapping
  @ApiOperation(value = "Retrieves a list of cohorts")
  public ResponseEntity<List<CohortDto>> getAllCohorts() {
    List<Cohort> cohorts = cohortService.getAllCohorts();
    return ResponseEntity.ok(
        cohorts.stream().map(converter::convertToDto).collect(Collectors.toList()));
  }

  @PostMapping
  @ApiOperation(value = "Stores a cohort")
  public ResponseEntity<CohortDto> createCohort(@Valid @NotNull @RequestBody CohortDto cohort) {
    Cohort cohortEntity = cohortService.createCohort(converter.convertToEntity(cohort));
    return ResponseEntity.ok(converter.convertToDto(cohortEntity));
  }

  @PostMapping("{cohortId}/execute")
  @ApiOperation(value = "Executes the cohort")
  public ResponseEntity<List<String>> executeCohort(@PathVariable String cohortId) {
    try {
      List<String> patientIds = cohortService.executeCohort(Long.parseLong(cohortId));
      return ResponseEntity.ok(patientIds);
    } catch (WrongStatusCodeException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
