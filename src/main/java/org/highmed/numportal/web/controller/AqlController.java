package org.highmed.numportal.web.controller;

import org.highmed.numportal.domain.dto.*;
import org.highmed.numportal.domain.model.Aql;
import org.highmed.numportal.domain.model.AqlCategory;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.mapper.AqlMapper;
import org.highmed.numportal.service.AqlService;
import org.highmed.numportal.service.ehrbase.ParameterService;
import org.highmed.numportal.service.exception.CustomizedExceptionHandler;
import org.highmed.numportal.service.logger.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.highmed.numportal.web.config.Role;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<AqlDto> getAqlById(
      @AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable Long id) {
    return ResponseEntity.ok(
        mapper.convertToDto(aqlService.getAqlById(id, principal.getSubject())));
  }

  @AuditLog(description = "Create AQL criteria")
  @PostMapping()
  @Operation(description = "Creates an aql; the logged in user is assigned as owner of the aql.")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlDto> createAql(
      @AuthenticationPrincipal @NotNull Jwt principal, @Valid @NotNull @RequestBody AqlDto aqlDto) {

    var aql = aqlService.createAql(mapper.convertToEntity(aqlDto), principal.getSubject(), aqlDto.getCategoryId());
    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

  @AuditLog(description = "Update AQL criteria")
  @PutMapping(value = "/{id}")
  @Operation(description = "Updates an aql; the logged in user is assigned as owner of the aql at creation time")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlDto> updateAql(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable("id") Long aqlId,
      @Valid @NotNull @RequestBody AqlDto aqlDto) {
    var aql = aqlService.updateAql(mapper.convertToEntity(aqlDto), aqlId, principal.getSubject(), aqlDto.getCategoryId());

    return ResponseEntity.ok(mapper.convertToDto(aql));
  }

  @AuditLog(description = "Delete AQL criteria")
  @DeleteMapping("/{id}")
  @Operation(description = "Delete AQL criteria")
  @PreAuthorize(Role.CRITERIA_EDITOR_OR_SUPER_ADMIN)
  public void deleteAql(@AuthenticationPrincipal @NotNull Jwt principal, @NotNull @PathVariable Long id) {
    aqlService.deleteById(id, principal.getSubject(), Roles.extractRoles(principal));
  }

  /**
   * endpoint required in search area
   * @param principal
   * @return
   */
  @AuditLog
  @GetMapping()
  @Operation(description = "Retrieves a list of visible aqls, all owned by logged in user and all public")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<List<AqlDto>> getAqls(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(aqlService.getVisibleAqls(principal.getSubject()).stream()
                    .map(mapper::convertToDto)
                    .collect(Collectors.toList()));
  }

  @AuditLog
  @GetMapping("/all")
  @Operation(description = "Retrieves a list of visible aqls, all owned by logged in user and all public")
  @PreAuthorize(Role.MANAGER_OR_STUDY_COORDINATOR_OR_RESEARCHER_OR_CRITERIA_EDITOR)
  public ResponseEntity<Page<AqlDto>> getAqls(@AuthenticationPrincipal @NotNull Jwt principal,
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
  public ResponseEntity<Long> getAqlSize(@AuthenticationPrincipal @NotNull Jwt principal, @Valid @RequestBody SlimAqlDto aql) {
    return ResponseEntity.ok(aqlService.getAqlSize(aql, principal.getSubject()));
  }

  @AuditLog(description = "Create AQL category")
  @PostMapping(value = "/category")
  @Operation(description = "Creates a category. If there is an id in the DTO, it is ignored.")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlCategoryDto> createCategory(@AuthenticationPrincipal @NotNull Jwt principal,
                                                       @Valid @NotNull @RequestBody AqlCategoryDto aqlCategoryDto) {
    var aqlCategory =
        aqlService.createAqlCategory(principal.getSubject(), AqlCategory.builder().name(aqlCategoryDto.getName()).build());
    return ResponseEntity.ok(modelMapper.map(aqlCategory, AqlCategoryDto.class));
  }

  @AuditLog(description = "Update AQL category")
  @PutMapping(value = "/category/{id}")
  @Operation(description = "Updates a category. If present, the id in the DTO is ignored.")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public ResponseEntity<AqlCategoryDto> updateCategory(@AuthenticationPrincipal @NotNull Jwt principal,
                                                       @PathVariable("id") Long categoryId,
                                                       @Valid @NotNull @RequestBody AqlCategoryDto aqlCategoryDto) {

    var aqlCategory =
        aqlService.updateAqlCategory(principal.getSubject(),
            AqlCategory.builder().name(aqlCategoryDto.getName()).build(), categoryId);

    return ResponseEntity.ok(modelMapper.map(aqlCategory, AqlCategoryDto.class));
  }

  @AuditLog(description = "Delete AQL category")
  @DeleteMapping(value = "/category/{id}")
  @Operation(description = "Delete a category")
  @PreAuthorize(Role.CRITERIA_EDITOR)
  public void deleteAqlCategory(@AuthenticationPrincipal @NotNull Jwt principal, @PathVariable Long id) {
    aqlService.deleteCategoryById(principal.getSubject(), id);
  }

  /**
   * endpoint used in search area
   * @return
   */
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
  public ResponseEntity<Page<AqlCategoryDto>> getAqlCategories(@PageableDefault(size = 50) Pageable pageable, SearchCriteria searchCriteria) {
    Page<AqlCategory> searchResult = aqlService.getAqlCategories(pageable, searchCriteria);
    List<AqlCategoryDto> content = searchResult.getContent().stream()
            .map(category -> {
              AqlCategoryDto categoryDto = modelMapper.map(category, AqlCategoryDto.class);
              categoryDto.setAllowedToBeDeleted(aqlService.aqlCategoryIsAllowedToBeDeleted(category.getId()));
              return categoryDto;
            })
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
