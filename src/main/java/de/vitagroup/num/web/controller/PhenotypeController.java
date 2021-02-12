package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import de.vitagroup.num.mapper.PhenotypeMapper;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/phenotype")
@AllArgsConstructor
public class PhenotypeController {

  private final PhenotypeService phenotypeService;
  private final PhenotypeMapper mapper;

  @GetMapping
  @ApiOperation(value = "Retrieves a list of phenotypes")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<PhenotypeDto>> getAllPhenotypes(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        phenotypeService.getAllPhenotypes(principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @PostMapping
  @ApiOperation(value = "Stores a phenotype")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<PhenotypeDto> createPhenotype(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @Valid @RequestBody PhenotypeDto phenotypeDto) {
    Phenotype phenotype =
        phenotypeService.createPhenotypes(
            mapper.convertToEntity(phenotypeDto), principal.getSubject());
    return ResponseEntity.ok(mapper.convertToDto(phenotype));
  }

  @PostMapping("/size")
  @ApiOperation(
      value = "Executes a phenotype and returns the count of matching ehr ids in the phenotype")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<Long> executePhenotype(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @Valid @RequestBody PhenotypeDto phenotypeDto) {
    return ResponseEntity.ok(
        phenotypeService.getPhenotypeSize(
            mapper.convertToEntity(phenotypeDto), principal.getSubject()));
  }
}
