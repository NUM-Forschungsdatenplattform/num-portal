package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.service.AqlService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class AqlController {

    private final AqlService aqlService;

    @GetMapping("/aql")
    @ApiOperation(value = "Retrieves a list of aql queries")
    public ResponseEntity<List<Aql>> getAllAqls() {
        List<Aql> aqls = aqlService.getAllAqls();
        return ResponseEntity.ok(aqls);
    }

}
