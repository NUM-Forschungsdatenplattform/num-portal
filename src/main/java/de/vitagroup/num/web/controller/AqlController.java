package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.domain.dto.AqlSearchFilter;
import de.vitagroup.num.domain.dto.SlimAqlDto;
import de.vitagroup.num.mapper.AqlMapper;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.service.policy.ProjectPolicyService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
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
@RequestMapping(value = "/aql", produces = "application/json")
public class AqlController {

  private final AqlService aqlService;
  private final AqlMapper mapper;
  private final ProjectPolicyService projectPolicyService;

  @AuditLog
  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves public or owned aql query by id.")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<AqlDto> getAqlById(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @NotEmpty @PathVariable Long id) {
    return ResponseEntity.ok(
        mapper.convertToDto(aqlService.getAqlById(id, principal.getSubject())));
  }

  @AuditLog
  @PostMapping()
  @ApiOperation(value = "Creates an aql; the logged in user is assigned as owner of the aql.")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<AqlDto> createAql(
      @AuthenticationPrincipal @NotNull Jwt principal, @Valid @NotNull @RequestBody AqlDto aqlDto) {

    Aql aql = aqlService.createAql(mapper.convertToEntity(aqlDto), principal.getSubject());
    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

  @AuditLog
  @PutMapping(value = "/{id}")
  @ApiOperation(
      value = "Updates an aql; the logged in user is assigned as owner of the aql at creation time")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<AqlDto> updateAql(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long aqlId,
      @Valid @NotNull @RequestBody AqlDto aqlDto) {

    Aql aql = aqlService.updateAql(mapper.convertToEntity(aqlDto), aqlId, principal.getSubject());

    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

  @AuditLog
  @DeleteMapping("/{id}")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  void deleteAql(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    aqlService.deleteById(id, principal.getSubject());
  }

  @AuditLog
  @GetMapping("/search")
  @ApiOperation(value = "Retrieves a list of aqls based on a search string and search type")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<AqlDto>> searchAqls(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @ApiParam(value = "A string contained in the name of the aqls", required = false)
          @RequestParam(required = false)
          String name,
      @ApiParam(value = "Type of the search", required = true)
          @RequestParam(required = true)
          @Valid
          @NotNull
          AqlSearchFilter filter) {
    return ResponseEntity.ok(
        aqlService.searchAqls(name, filter, principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping()
  @ApiOperation(
      value = "Retrieves a list of visible aqls, all owned by logged in user and all public")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<AqlDto>> getAqls(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        aqlService.getVisibleAqls(principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @PostMapping("/size")
  @ApiOperation(value = "Executes an aql and returns the count of matching ehr ids")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<Long> getAqlSize(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @RequestBody SlimAqlDto aql) {
    return ResponseEntity.ok(aqlService.getAqlSize(aql, principal.getSubject()));
  }
}
