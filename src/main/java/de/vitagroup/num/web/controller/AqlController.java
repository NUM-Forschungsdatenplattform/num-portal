package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AqlController {

  private final AqlService aqlService;

  @GetMapping("/aql")
  @ApiOperation(value = "Retrieves a list of aql queries")
  @PreAuthorize(Role.STUDY_COORDINATOR)
  public ResponseEntity<List<Aql>> getAllAqls() {
    List<Aql> aqls = aqlService.getAllAqls();
    return ResponseEntity.ok(aqls);
  }
}
