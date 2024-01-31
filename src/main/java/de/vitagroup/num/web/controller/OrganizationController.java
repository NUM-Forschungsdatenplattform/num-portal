package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.model.Organization;
import de.vitagroup.num.domain.model.Roles;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.service.OrganizationService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
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
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/organization", produces = "application/json")
@AllArgsConstructor
@SecurityRequirement(name = "security_auth")
public class OrganizationController extends CustomizedExceptionHandler {

  private final OrganizationService organizationService;
  private final OrganizationMapper mapper;

  @AuditLog
  @GetMapping("/domains")
  @Operation(description = "Retrieves a list of all active existing organization email domains")
  public ResponseEntity<List<String>> getAllMailDomainsForActiveOrganizations() {
    return ResponseEntity.ok(organizationService.getMailDomainsByActiveOrganizations());
  }

  @AuditLog
  @GetMapping("/{id}")
  @Operation(description = "Retrieves an organization by external id")
  public ResponseEntity<OrganizationDto> getOrganizationById(@NotNull @PathVariable Long id) {
    return ResponseEntity.ok(mapper.convertToDto(organizationService.getOrganizationById(id)));
  }

  /**
   * used when edit user
   * @param principal
   * @return
   */
  @AuditLog
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

  @AuditLog
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

  @AuditLog(description = "Create organization")
  @PostMapping()
  @Operation(description = "Creates an organization")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<OrganizationDto> createOrganization(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody OrganizationDto organizationDto) {
    return ResponseEntity.ok(
        mapper.convertToDto(organizationService.create(principal.getSubject(), organizationDto)));
  }

  @AuditLog(description = "Update organization")
  @PutMapping(value = "/{id}")
  @Operation(description = "Updates an organization")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<OrganizationDto> updateOrganization(@AuthenticationPrincipal @NotNull Jwt principal,
      @NotNull @PathVariable("id") Long organizationId,
      @Valid @NotNull @RequestBody OrganizationDto organizationDto) {
    return ResponseEntity.ok(
        mapper.convertToDto(
            organizationService.update(
                organizationId,
                organizationDto,
                Roles.extractRoles(principal),
                principal.getSubject())));
  }

  @AuditLog(description = "Delete organization")
  @Operation(description = "Delete the given organization if no users are assigned to this organization")
  @DeleteMapping(value = "/{id}")
  @PreAuthorize(Role.SUPER_ADMIN)
  public void deleteOrganization(@AuthenticationPrincipal @NotNull Jwt principal,
                                 @NotNull @PathVariable("id") Long organizationId) {
    organizationService.deleteOrganization(organizationId, principal.getSubject());
  }
}
