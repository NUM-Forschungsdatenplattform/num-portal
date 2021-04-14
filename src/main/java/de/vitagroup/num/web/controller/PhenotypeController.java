package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.ExpressionDto;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import de.vitagroup.num.mapper.PhenotypeMapper;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/phenotype", produces = "application/json")
@AllArgsConstructor
public class PhenotypeController {

  private final PhenotypeService phenotypeService;
  private final PhenotypeMapper mapper;

  @AuditLog
  @GetMapping
  @ApiOperation(value = "Retrieves a list of phenotypes")
  @ApiResponses({
    @ApiResponse(code = 401, message = "Unauthorized"),
    @ApiResponse(code = 403, message = "Forbidden"),
    @ApiResponse(code = 404, message = "Not found"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<List<PhenotypeDto>> getAllPhenotypes(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        phenotypeService.getAllPhenotypes(principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves a phenotype by id.")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER_OR_APPROVER)
  public ResponseEntity<PhenotypeDto> getPhenotypeById(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @NotEmpty @PathVariable Long id) {
    return ResponseEntity.ok(
        mapper.convertToDto(phenotypeService.getPhenotypeById(id, principal.getSubject())));
  }

  @AuditLog
  @PostMapping
  @ApiOperation(value = "Stores a phenotype")
  @ApiResponses({
    @ApiResponse(code = 400, message = "Bad request"),
    @ApiResponse(code = 401, message = "Unauthorized"),
    @ApiResponse(code = 403, message = "Forbidden"),
    @ApiResponse(code = 404, message = "Not found"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<PhenotypeDto> createPhenotype(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @Valid @RequestBody PhenotypeDto phenotypeDto) {
    Phenotype phenotype =
        phenotypeService.createPhenotypes(
            mapper.convertToEntity(phenotypeDto), principal.getSubject());
    return ResponseEntity.ok(mapper.convertToDto(phenotype));
  }

  @AuditLog
  @PostMapping("/size")
  @ApiOperation(
      value = "Executes a phenotype and returns the count of matching ehr ids in the phenotype")
  @ApiResponses({
    @ApiResponse(code = 400, message = "Bad request"),
    @ApiResponse(code = 401, message = "Unauthorized"),
    @ApiResponse(code = 403, message = "Forbidden"),
    @ApiResponse(code = 404, message = "Not found"),
    @ApiResponse(code = 451, message = "Too few matchers, withheld for privacy reasons"),
    @ApiResponse(code = 500, message = "Internal server error")
  })
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<Long> executePhenotype(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @Valid @RequestBody ExpressionDto expressionDto) {
    return ResponseEntity.ok(
        phenotypeService.getPhenotypeSize(
            mapper.convertToEntity(expressionDto), principal.getSubject()));
  }

  @AuditLog
  @DeleteMapping("/{id}")
  @ApiOperation(
      value = "Deletes a phenotype")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_APPROVER_OR_SUPER_ADMIN)
  void deleteAql(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    phenotypeService.deletePhenotypeById(id, principal.getSubject(), Roles.extractRoles(principal));
  }

}
