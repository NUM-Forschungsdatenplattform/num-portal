package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.mapper.AqlMapper;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.web.config.Role;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Optional;
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
@RequestMapping("/aql")
public class AqlController {

  private final AqlService aqlService;
  private final AqlMapper mapper;

  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves an aql query by id")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<AqlDto> getAqlById(@NotNull @NotEmpty @PathVariable Long id) {
    Optional<Aql> aql = aqlService.getAqlById(id);

    if (aql.isEmpty()) {
      throw new ResourceNotFound("Aql not found");
    }

    return ResponseEntity.ok(mapper.convertToDto(aql.get()));
  }

  @PostMapping()
  @ApiOperation(value = "Creates an aql; the logged in user is assigned as owner of the aql")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<AqlDto> createAql(
      @AuthenticationPrincipal @NotNull Jwt principal, @Valid @NotNull @RequestBody AqlDto aqlDto) {

    Aql aql = aqlService.createAql(mapper.convertToEntity(aqlDto), principal.getSubject());
    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

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

  @DeleteMapping("/{id}")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  void deleteAql(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    aqlService.deleteById(id, principal.getSubject());
  }

  @GetMapping()
  @ApiOperation(
      value =
          "Retrieves a list of aqls based on a search string and flags, if no parameters are specified retrieves all the aqls")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<AqlDto>> searchAqls(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @ApiParam(value = "Flag for filtering aqls based on ownership", required = false)
          @RequestParam(required = false)
          Boolean owned,
      @ApiParam(value = "Flag for filtering aqls based on organization", required = false)
          @RequestParam(required = false)
          Boolean ownedBySameOrganization,
      @ApiParam(value = "A string contained in the name of the aqls", required = false)
          @RequestParam(required = false)
          String name) {

    return ResponseEntity.ok(
        aqlService.searchAqls(name, owned, ownedBySameOrganization, principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @PostMapping("{aqlId}/execute")
  @ApiOperation(value = "Executes the aql")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<String> executeAql(
      @NotNull @NotEmpty @PathVariable Long aqlId,
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(aqlService.executeAql(aqlId, principal.getSubject()));
  }
}
