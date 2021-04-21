package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.mapper.CohortMapper;
import de.vitagroup.num.service.CohortService;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/cohort", produces = "application/json")
public class CohortController {

  private final CohortService cohortService;

  private final CohortMapper cohortMapper;

  @AuditLog
  @GetMapping("{cohortId}")
  @ApiOperation(value = "Retrieves a single cohort.")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CohortDto> getCohort(
      @AuthenticationPrincipal @NotNull Jwt principal, @PathVariable String cohortId) {
    Cohort cohort = cohortService.getCohort(Long.parseLong(cohortId), principal.getSubject());
    return ResponseEntity.ok(cohortMapper.convertToDto(cohort));
  }

  @AuditLog
  @PostMapping
  @ApiOperation(value = "Stores a cohort")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<CohortDto> createCohort(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody CohortDto cohortDto) {
    Cohort cohortEntity = cohortService.createCohort(cohortDto, principal.getSubject());
    return ResponseEntity.ok(cohortMapper.convertToDto(cohortEntity));
  }

  @AuditLog
  @PutMapping(value = "/{id}")
  @ApiOperation(value = "Updates a cohort")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<CohortDto> updateCohort(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody CohortDto cohortDto,
      @PathVariable("id") Long cohortId) {
    Cohort cohortEntity = cohortService.updateCohort(cohortDto, cohortId, principal.getSubject());
    return ResponseEntity.ok(cohortMapper.convertToDto(cohortEntity));
  }

  @AuditLog
  @PostMapping("size")
  @ApiOperation(value = "Retrieves the cohort group size without saving")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<Long> getCohortGroupSize(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @RequestBody CohortGroupDto cohortGroupDto) {
    return ResponseEntity.ok(
        cohortService.getCohortGroupSize(cohortGroupDto, principal.getSubject()));
  }
}
