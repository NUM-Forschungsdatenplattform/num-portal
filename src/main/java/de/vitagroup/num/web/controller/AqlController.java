package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Aql;
import de.vitagroup.num.domain.AqlCategory;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.*;
import de.vitagroup.num.mapper.AqlMapper;
import de.vitagroup.num.service.AqlService;
import de.vitagroup.num.service.ehrbase.ParameterService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/aql", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class AqlController extends CustomizedExceptionHandler {

  private final AqlService aqlService;
  private final ParameterService parameterService;
  private final AqlMapper mapper;
  private final ModelMapper modelMapper;

  @AuditLog
  @GetMapping("/{id}")
  @Operation(description = "Retrieves public or owned aql query by id.")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<AqlDto> getAqlById(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @NotEmpty @PathVariable Long id) {
    return ResponseEntity.ok(
        mapper.convertToDto(aqlService.getAqlById(id, principal.getSubject())));
  }

  @AuditLog
  @PostMapping()
  @Operation(description = "Creates an aql; the logged in user is assigned as owner of the aql.")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlDto> createAql(
      @AuthenticationPrincipal @NotNull Jwt principal, @Valid @NotNull @RequestBody AqlDto aqlDto) {

    var aql = aqlService.createAql(mapper.convertToEntity(aqlDto), principal.getSubject(), aqlDto.getCategoryId());
    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

  @AuditLog
  @PutMapping(value = "/{id}")
  @Operation(description = "Updates an aql; the logged in user is assigned as owner of the aql at creation time")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlDto> updateAql(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long aqlId,
      @Valid @NotNull @RequestBody AqlDto aqlDto) {
    var aql = aqlService.updateAql(mapper.convertToEntity(aqlDto), aqlId, principal.getSubject(), aqlDto.getCategoryId());

    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

  @AuditLog
  @DeleteMapping("/{id}")
  @Operation(description = "Delete AQL criteria")
  @PreAuthorize(Role.CRITERIA_EDITOR_OR_SUPER_ADMIN)
  public void deleteAql(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    aqlService.deleteById(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  @AuditLog
  @GetMapping("/search")
  @Operation(description = "Retrieves a list of aqls based on a search string and search type")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER)
  public ResponseEntity<List<AqlDto>> searchAqls(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Parameter(description = "A string contained in the name of the aqls")
          @RequestParam(required = false)
          String name,
      @Parameter(description = "Type of the search", required = true) @RequestParam @Valid @NotNull
      SearchFilter filter) {
    return ResponseEntity.ok(
        aqlService.searchAqls(name, filter, principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping()
  @Operation(description = "Retrieves a list of visible aqls, all owned by logged in user and all public")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<List<AqlDto>> getAqls(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(
        aqlService.getVisibleAqls(principal.getSubject()).stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping("/all")
  @Operation(description = "Retrieves a list of visible aqls, all owned by logged in user and all public")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<Page<AqlDto>> getAqlsWithPagination(@AuthenticationPrincipal @NotNull Jwt principal,
                                                            @PageableDefault(size = 50) Pageable pageable, SearchCriteria searchCriteria) {
    Page<Aql> searchResult = aqlService.getVisibleAqls(principal.getSubject(), pageable, searchCriteria);
    List<AqlDto> content = searchResult.getContent().stream()
            .map(mapper::convertToDto)
            .collect(Collectors.toList());
    return ResponseEntity.ok(new PageImpl<>(content, pageable, searchResult.getTotalElements()));
  }

  @AuditLog
  @PostMapping("/size")
  @Operation(description = "Executes an aql and returns the count of matching ehr ids")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<Long> getAqlSize(
      @AuthenticationPrincipal @NotNull Jwt principal, @Valid @RequestBody SlimAqlDto aql) {
    return ResponseEntity.ok(aqlService.getAqlSize(aql, principal.getSubject()));
  }

  @AuditLog
  @PostMapping(value = "/category")
  @Operation(description = "Creates a category. If there is an id in the DTO, it is ignored.")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlCategoryDto> createCategory(
      @Valid @NotNull @RequestBody AqlCategoryDto aqlCategoryDto) {

    var aqlCategory =
        aqlService.createAqlCategory(AqlCategory.builder().name(aqlCategoryDto.getName()).build());
    return ResponseEntity.ok(modelMapper.map(aqlCategory, AqlCategoryDto.class));
  }

  @AuditLog
  @PutMapping(value = "/category/{id}")
  @Operation(description = "Updates a category. If present, the id in the DTO is ignored.")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlCategoryDto> updateCategory(
      @PathVariable("id") Long categoryId,
      @Valid @NotNull @RequestBody AqlCategoryDto aqlCategoryDto) {

    var aqlCategory =
        aqlService.updateAqlCategory(
            AqlCategory.builder().name(aqlCategoryDto.getName()).build(), categoryId);

    return ResponseEntity.ok(modelMapper.map(aqlCategory, AqlCategoryDto.class));
  }

  @AuditLog
  @DeleteMapping(value = "/category/{id}")
  @Operation(description = "Delete a category")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public void deleteAqlCategory(@PathVariable Long id) {
    aqlService.deleteCategoryById(id);
  }

  @AuditLog
  @GetMapping(value = "/category")
  @Operation(description = "Retrieves the list of categories.")
  public ResponseEntity<List<AqlCategoryDto>> getAqlCategories() {
    return ResponseEntity.ok(
        aqlService.getAqlCategories().stream()
            .map(category -> modelMapper.map(category, AqlCategoryDto.class))
            .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping(value = "/category/all")
  @Operation(description = "Retrieves the list of categories.")
  public ResponseEntity<Page<AqlCategoryDto>> getAqlCategoriesWithPagination(@PageableDefault(size = 50) Pageable pageable, SearchCriteria searchCriteria) {
    Page<AqlCategory> searchResult = aqlService.getAqlCategories(pageable, searchCriteria);
    List<AqlCategoryDto> content = searchResult.getContent().stream()
            .map(category -> modelMapper.map(category, AqlCategoryDto.class))
            .collect(Collectors.toList());
    return ResponseEntity.ok(new PageImpl<>(content, pageable, searchResult.getTotalElements()));
  }

  @AuditLog
  @GetMapping("/parameter/values")
  @Operation(description = "Retrieves a list of possible values for an aql path")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<ParameterOptionsDto> getParameterValues(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @RequestParam @NotNull @NotEmpty String aqlPath,
      @RequestParam @NotNull @NotEmpty String archetypeId) {
    return ResponseEntity.ok(
        parameterService.getParameterValues(principal.getSubject(), aqlPath, archetypeId));
  }
}
