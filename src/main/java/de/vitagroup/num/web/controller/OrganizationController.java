package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Organization;
import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.OrganizationDto;
import de.vitagroup.num.domain.dto.SearchCriteria;
import de.vitagroup.num.mapper.OrganizationMapper;
import de.vitagroup.num.service.OrganizationService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.service.logger.AuditLog;
import de.vitagroup.num.web.config.Role;
import io.swagger.annotations.ApiOperation;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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

@RestController
@RequestMapping(value = "/organization", produces = "application/json")
@AllArgsConstructor
public class OrganizationController extends CustomizedExceptionHandler {

  private final OrganizationService organizationService;
  private final OrganizationMapper mapper;

  @AuditLog
  @GetMapping("/domains")
  @ApiOperation(value = "Retrieves a list of all existing organization email domains")
  public ResponseEntity<List<String>> getAllMailDomains() {
    return ResponseEntity.ok(organizationService.getAllMailDomains());
  }

  @AuditLog
  @GetMapping("/{id}")
  @ApiOperation(value = "Retrieves an organization by external id")
  public ResponseEntity<OrganizationDto> getOrganizationById(
      @NotNull @NotEmpty @PathVariable Long id) {
    return ResponseEntity.ok(mapper.convertToDto(organizationService.getOrganizationById(id)));
  }

  @AuditLog
  @GetMapping()
  @ApiOperation(value = "Retrieves a list of available organizations")
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

  @AuditLog
  @PostMapping()
  @ApiOperation(value = "Creates an organization")
  @PreAuthorize(Role.SUPER_ADMIN)
  public ResponseEntity<OrganizationDto> createOrganization(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @Valid @NotNull @RequestBody OrganizationDto organizationDto) {
    return ResponseEntity.ok(
        mapper.convertToDto(organizationService.create(principal.getSubject(), organizationDto)));
  }

  @AuditLog
  @PutMapping(value = "/{id}")
  @ApiOperation(value = "Updates an organization")
  @PreAuthorize(Role.SUPER_ADMIN_OR_ORGANIZATION_ADMIN)
  public ResponseEntity<OrganizationDto> updateOrganization(
      @AuthenticationPrincipal @NotNull Jwt principal,
      @PathVariable("id") Long organizationId,
      @Valid @NotNull @RequestBody OrganizationDto organizationDto) {
    return ResponseEntity.ok(
        mapper.convertToDto(
            organizationService.update(
                organizationId,
                organizationDto,
                Roles.extractRoles(principal),
                principal.getSubject())));
  }
}
