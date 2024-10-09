package org.highmed.numportal.web.controller;

import org.highmed.numportal.domain.dto.CardDto;
import org.highmed.numportal.domain.dto.MetricsDto;
import org.highmed.numportal.domain.dto.NavigationItemDto;
import org.highmed.numportal.domain.dto.ProjectInfoDto;
import org.highmed.numportal.domain.model.Roles;
import org.highmed.numportal.service.ContentService;
import org.highmed.numportal.service.exception.CustomizedExceptionHandler;
import org.highmed.numportal.service.logger.ContextLog;
import org.highmed.numportal.web.config.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Validated
@Controller
@AllArgsConstructor
@RequestMapping(value = "/content", produces = "application/json")
@SecurityRequirement(name = "security_auth")
public class ContentController extends CustomizedExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
  private final ContentService contentService;

  @GetMapping("/navigation")
  @Operation(description = "Retrieves a list of navigation items")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<String> getNavigationItems() {
    return ResponseEntity.ok(contentService.getNavigationItems());
  }

  @GetMapping("/metrics")
  @Operation(description = "Retrieves platform metrics")
  public ResponseEntity<MetricsDto> getMetrics() {
    return ResponseEntity.ok(contentService.getMetrics());
  }

  @GetMapping("/graph/clinic")
  @Operation(description = "Retrieves the list of participating clinics")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<List<String>> getClinics(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(contentService.getClinics(principal.getSubject()));
  }

  @GetMapping("/graph/clinic/{name}/sofaDistribution")
  @Operation(description = "Retrieves sofa distribution of a clinic")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<Map<String, Integer>> getClinicDistributions(@PathVariable String name) {
    return ResponseEntity.ok(contentService.getClinicDistributions(name));
  }

  @GetMapping("/graph/clinic/sofaAverage")
  @PreAuthorize(Role.MANAGER)
  @Operation(description = "Retrieves the sofa averages of participating clinics", security = @SecurityRequirement(name = "security_auth"))
  public ResponseEntity<Map<String, Double>> getClinicAverages(@AuthenticationPrincipal @NotNull Jwt principal) {
    return ResponseEntity.ok(contentService.getClinicAverages(principal.getSubject()));
  }

  @GetMapping("/latest-projects")
  @Operation(description = "Retrieves latest project info")
  public ResponseEntity<List<ProjectInfoDto>> getLatestProjects(
      @AuthenticationPrincipal Jwt principal) {
    List<String> roles = new ArrayList<>();
    if (principal != null) {
      roles = Roles.extractRoles(principal);
    }
    return ResponseEntity.ok(contentService.getLatestProjects(roles));
  }

  @ContextLog(type = "ContentManagement", description = "Set the navigation items")
  @PostMapping("/navigation")
  @Operation(description = "Retrieves a list of navigation items")
  @PreAuthorize(Role.CONTENT_ADMIN)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = String.class))),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<String> setNavigationItems(
      @Valid @NotNull @RequestBody @Size(max = 5) List<NavigationItemDto> navigationItemDtos) {
    contentService.setNavigationItems(navigationItemDtos);
    for (NavigationItemDto navigationItemDto : navigationItemDtos) {
      logger.info("Set navigation item: {}", navigationItemDto);
    }
    return ResponseEntity.ok("Success");
  }

  @GetMapping("/cards")
  @Operation(description = "Retrieves a list of cards")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<String> getCards() {
    return ResponseEntity.ok(contentService.getCards());
  }

  @ContextLog(type = "ContentManagement", description = "Set a list of cards")
  @PostMapping("/cards")
  @Operation(description = "Retrieves a list of cards")
  @PreAuthorize(Role.CONTENT_ADMIN)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<String> setCards(
      @Valid @NotNull @RequestBody @Size(max = 8) List<CardDto> cardDtos) {
    contentService.setCards(cardDtos);
    for (CardDto cardDto : cardDtos) {
      logger.info("Set card item: {}", cardDto);
    }
    return ResponseEntity.ok("Success");
  }
}
