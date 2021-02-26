package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.service.OrganizationService;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organization")
@AllArgsConstructor
public class OrganizationController {

  private final OrganizationService organizationService;

  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves an organization by external id")
  public ResponseEntity<OrganizationDto> getOrganizationById(
      @NotNull @NotEmpty @PathVariable String id) {
    return ResponseEntity.ok(organizationService.getOrganizationById(id));
  }

  @GetMapping()
  @ApiOperation(value = "Retrieves a list of available organizations")
  public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
    return ResponseEntity.ok(organizationService.getAllOrganizations());
  }
}
