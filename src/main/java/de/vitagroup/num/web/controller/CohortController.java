package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.model.Cohort;
import de.vitagroup.num.domain.model.Roles;
import de.vitagroup.num.domain.dto.CohortDto;
import de.vitagroup.num.domain.dto.CohortGroupDto;
import de.vitagroup.num.domain.dto.CohortSizeDto;
import de.vitagroup.num.domain.dto.TemplateSizeRequestDto;
import de.vitagroup.num.mapper.CohortMapper;
import de.vitagroup.num.service.CohortService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.exception.dto.ErrorDetails;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/cohort", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class CohortController extends CustomizedExceptionHandler {

  private final CohortService cohortService;

  private final CohortMapper cohortMapper;

  @AuditLog
  @GetMapping("{cohortId}")
  @Operation(description = "Retrieves a single cohort.")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<CohortDto> getCohort(
      @AuthenticationPrincipal @NotNull Jwt principal, @PathVariable String cohortId) {
    Cohort cohort = cohortService.getCohort(Long.parseLong(cohortId), principal.getSubject());
    return ResponseEntity.ok(cohortMapper.convertToDto(cohort));
  }

  @AuditLog(description = "Create cohort")
  @PostMapping
  @Operation(description = "Stores a cohort")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR)
  public ResponseEntity<CohortDto> createCohort(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody CohortDto cohortDto) {
    Cohort cohortEntity = cohortService.createCohort(cohortDto, principal.getSubject());
    return ResponseEntity.ok(cohortMapper.convertToDto(cohortEntity));
  }

  @AuditLog(description = "Update cohort")
  @PutMapping(value = "/{id}")
  @Operation(description = "Updates a cohort")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR)
  public ResponseEntity<CohortDto> updateCohort(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody CohortDto cohortDto,
      @PathVariable("id") Long cohortId) {
    Cohort cohortEntity = cohortService.updateCohort(cohortDto, cohortId, principal.getSubject());
    return ResponseEntity.ok(cohortMapper.convertToDto(cohortEntity));
  }

  @AuditLog(description = "Read cohort size")
  @PostMapping("/size")
  @Operation(description = "Retrieves the cohort group size without saving")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<Long> getCohortGroupSize(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @RequestBody CohortGroupDto cohortGroupDto,
      @RequestParam(required = false) Boolean allowUsageOutsideEu) {
    long cohortGroupSize =
        cohortService.getCohortGroupSize(
            cohortGroupDto, principal.getSubject(), allowUsageOutsideEu);

    if (!Roles.extractRoles(principal).contains(Roles.MANAGER)) {
      cohortGroupSize = cohortService.getRoundedSize(cohortGroupSize);
    }

    return ResponseEntity.ok(cohortGroupSize);
  }

  @AuditLog
  @PostMapping("/size/template")
  @Operation(description = "Retrieves the size of the templates")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR)
  public ResponseEntity<Map<String, Integer>> getSizePerTemplates(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @RequestBody TemplateSizeRequestDto requestDto) {

    Map<String, Integer> sizePerTemplate =
        cohortService.getSizePerTemplates(principal.getSubject(), requestDto);

    if (!Roles.extractRoles(principal).contains(Roles.MANAGER)) {
      sizePerTemplate =
          sizePerTemplate.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Entry::getKey, entry -> cohortService.getRoundedSize(entry.getValue())));
    }

    return ResponseEntity.ok(sizePerTemplate);
  }

  @AuditLog(description = "Read cohort group size with age distribution and patients number per hospital")
  @PostMapping("/size/distribution")
  @Operation(
      description =
          "Retrieves the cohort group size without saving, provides also age distribution and patient numbers per hospital")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<CohortSizeDto> getCohortGroupSizeWithDistribution(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @RequestBody CohortGroupDto cohortGroupDto,
      @RequestParam(required = false) Boolean allowUsageOutsideEu) {
    return ResponseEntity.ok(
        cohortService.getCohortGroupSizeWithDistribution(
            cohortGroupDto, principal.getSubject(), allowUsageOutsideEu));
  }

  /**
   * Note : is on controller level to avoid overriding behaviour for all controllers
   * @param ex
   * @param headers
   * @param status
   * @param request
   * @return
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("Request for {} failed with error message {} ", request.getDescription(false), ex.getMessage());
    Map<String,String> errors = Map.of("Error", "error.missing_request_body");
    ErrorDetails errorDetails = ErrorDetails
            .builder()
            .message( "Request body is required" )
            .details( errors )
            .build();
    return ResponseEntity.status(status).body( errorDetails );
  }
}
