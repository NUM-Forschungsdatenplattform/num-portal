package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.service.PhenotypeService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PhenotypeController {

  private final PhenotypeService phenotypeService;

  @GetMapping("/phenotype")
  @ApiOperation(value = "Retrieves a list of phenotypes")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<List<Phenotype>> getAllPhenotypes() {
    return ResponseEntity.ok(phenotypeService.getAllPhenotypes());
  }

  @PostMapping("/phenotype")
  @ApiOperation(value = "Stores a phenotype")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<Phenotype> createPhenotype(
      @NotNull @Valid @RequestBody Phenotype phenotype) {
    return ResponseEntity.ok(phenotypeService.createPhenotypes(phenotype));
  }
}
