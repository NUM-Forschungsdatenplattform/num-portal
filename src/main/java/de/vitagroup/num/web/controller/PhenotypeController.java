package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.domain.dto.PhenotypeDto;
import de.vitagroup.num.mapper.PhenotypeMapper;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PhenotypeController {

  private final PhenotypeService phenotypeService;
  private final PhenotypeMapper mapper;

  @GetMapping("/phenotype")
  @ApiOperation(value = "Retrieves a list of phenotypes")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<PhenotypeDto>> getAllPhenotypes(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        phenotypeService.getAllPhenotypes(principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @PostMapping("/phenotype")
  @ApiOperation(value = "Stores a phenotype")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<PhenotypeDto> createPhenotype(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @Valid @RequestBody PhenotypeDto phenotypeDto) {
    Phenotype phenotype = phenotypeService.createPhenotypes(mapper.convertToEntity(phenotypeDto), principal.getSubject());
    return ResponseEntity.ok(mapper.convertToDto(phenotype));
  }
}
