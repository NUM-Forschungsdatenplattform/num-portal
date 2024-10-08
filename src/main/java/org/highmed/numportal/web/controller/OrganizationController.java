package org.highmed.numportal.web.controller;

import org.highmed.numportal.domain.dto.OrganizationDto;
import org.highmed.numportal.domain.dto.SearchCriteria;
import org.highmed.numportal.domain.model.Organization;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.mapper.OrganizationMapper;
import org.highmed.numportal.service.OrganizationService;
import org.highmed.numportal.service.exception.CustomizedExceptionHandler;
import org.highmed.numportal.service.logger.ContextLog;
import org.highmed.numportal.web.config.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/organization", produces = "application/json")
@AllArgsConstructor
@SecurityRequirement(name = "security_auth")
public class OrganizationController extends CustomizedExceptionHandler {

  private final OrganizationService organizationService;
  private final OrganizationMapper mapper;

  @GetMapping("/domains")
  @Operation(description = "Retrieves a list of all active existing organization email domains")
  public ResponseEntity<List<String>> getAllMailDomainsForActiveOrganizations() {
    return ResponseEntity.ok(organizationService.getMailDomainsByActiveOrganizations());
  }

  @GetMapping("/{id}")
  @Operation(description = "Retrieves an organization by external id")
  public ResponseEntity<OrganizationDto> getOrganizationById(@NotNull @PathVariable Long id) {
    return ResponseEntity.ok(mapper.convertToDto(organizationService.getOrganizationById(id)));
  }

  @GetMapping
  @Operation(description = "Retrieves a list of available organizations")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<List<OrganizationDto>> getAllOrganizations(
      @AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(organizationService
        .getAllOrganizations(Roles.extractRoles(principal), principal.getSubject())
        .stream()
        .map(mapper::convertToDto)
        .collect(Collectors.toList()));
  }

  @GetMapping("/all")
  @Operation(description = "Retrieves a list of available organizations")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<Page<OrganizationDto>> getOrganizations(@AuthenticationPrincipal @NotNull Jwt principal,
      @PageableDefault(size = 20) Pageable pageable,
      SearchCriteria criteria) {

    Page<Organization> organizationPage = organizationService
        .getAllOrganizations(Roles.extractRoles(principal), principal.getSubject(), criteria, pageable);
    List<OrganizationDto> content = organizationPage.getContent()
                                                    .stream()
                                                    .map(mapper::convertToDto)
                                                    .collect(Collectors.toList());
    return ResponseEntity.ok(new PageImpl<>(content, pageable, organizationPage.getTotalElements()));
  }

  @ContextLog(type = "OrgaManagement", description = "Create organization")
  @PostMapping()
  @Operation(description = "Creates an organization")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<OrganizationDto> createOrganization(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody OrganizationDto organizationDto) {
    return ResponseEntity.ok(
        mapper.convertToDto(organizationService.create(principal.getSubject(), organizationDto)));
  }

  @ContextLog(type = "OrgaManagement", description = "Update organization")
  @PutMapping(value = "/{id}")
  @Operation(description = "Updates an organization")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<OrganizationDto> updateOrganization(@AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable("id") Long organizationId,
      @Valid @NotNull @RequestBody OrganizationDto organizationDto) {

    OrganizationDto updatedOrganizationDto = mapper.convertToDto(
        organizationService.update(
            organizationId,
            organizationDto,
            Roles.extractRoles(principal),
            principal.getSubject()));

    return ResponseEntity.ok(updatedOrganizationDto);
  }

  @ContextLog(type = "OrgaManagement", description = "Delete organization")
  @Operation(description = "Delete the given organization if no users are assigned to this organization")
  @DeleteMapping(value = "/{id}")
  @PreAuthorize(Role.SUPER_ADMIN)
  public void deleteOrganization(@AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable("id") Long organizationId) {
    organizationService.deleteOrganization(organizationId, principal.getSubject());
  }
}
