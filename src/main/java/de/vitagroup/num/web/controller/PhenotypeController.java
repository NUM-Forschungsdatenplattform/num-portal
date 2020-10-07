package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Phenotype;
import de.vitagroup.num.service.PhenotypeService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@AllArgsConstructor
public class PhenotypeController {

    private final PhenotypeService phenotypeService;

    @GetMapping("/phenotype")
    @ApiOperation(value = "Retrieves a list of phenotypes")
    public ResponseEntity<List<Phenotype>> getAllPhenotypes() {
        return ResponseEntity.ok(phenotypeService.getAllPhenotypes());
    }

    @PostMapping("/phenotype")
    @ApiOperation(value = "Stores a phenotype")
    public ResponseEntity<Phenotype> createPhenotype(@NotNull @Valid @RequestBody Phenotype phenotype) {
        return ResponseEntity.ok(phenotypeService.createPhenotypes(phenotype));
    }

    @PutMapping("/phenotype")
    @ApiOperation(value = "Updates an existing phenotype")
    public ResponseEntity<List<Phenotype>> updatePhenotype() {
        throw new NotImplementedException();
    }

    @DeleteMapping("/phenotype")
    @ApiOperation(value = "Deletes an existing phenotype")
    public ResponseEntity<List<Phenotype>> deletePhenotype() {
        throw new NotImplementedException();
    }

}
