package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.dto.AqlDto;
import de.vitagroup.num.mapper.AqlMapper;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.web.config.Role;
import de.vitagroup.num.web.exception.ResourceNotFound;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping("/aql")
public class AqlController {

  private final AqlService aqlService;
  private final AqlMapper mapper;

  @GetMapping()
  @ApiOperation(value = "Retrieves a list of aql queries")
  @PreAuthorize(Role.STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<AqlDto>> getAllAqls() {
    return ResponseEntity.ok(
        aqlService.getAllAqls().stream().map(mapper::convertToDto).collect(Collectors.toList()));
  }

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
}
