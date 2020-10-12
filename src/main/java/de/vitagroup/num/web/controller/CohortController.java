package de.vitagroup.num.web.controller;

import de.vitagroup.num.converter.CohortConverter;
import de.vitagroup.num.domain.Cohort;
import de.vitagroup.num.domain.dtos.CohortDto;
import de.vitagroup.num.service.CohortService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class CohortController {

    private final CohortService cohortService;
    private final CohortConverter converter;

    @GetMapping("/cohort")
    @ApiOperation(value = "Retrieves a list of cohorts")
    public ResponseEntity<List<CohortDto>> getAllPhenotypes() {
        List<Cohort> cohorts = cohortService.getAllCohorts();
        return ResponseEntity.ok(cohorts.stream()
                .map(converter::convertToDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/cohort")
    @ApiOperation(value = "Stores a cohort")
    public ResponseEntity<CohortDto> createPhenotype(@Valid @NotNull @RequestBody CohortDto cohort) {
        Cohort cohortEntity = cohortService.createCohort(converter.convertToEntity(cohort));
        return ResponseEntity.ok(converter.convertToDto(cohortEntity));
    }

}
