package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.Roles;
import de.vitagroup.num.domain.dto.CardDto;
import de.vitagroup.num.domain.dto.MetricsDto;
import de.vitagroup.num.domain.dto.NavigationItemDto;
import de.vitagroup.num.domain.dto.ProjectInfoDto;
import de.vitagroup.num.service.ContentService;
import de.vitagroup.num.service.exception.CustomizedExceptionHandler;
import de.vitagroup.num.web.config.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
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

@Validated
@Controller
@AllArgsConstructor
@RequestMapping(value = "/content", produces = "application/json")
public class ContentController extends CustomizedExceptionHandler {

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
  public ResponseEntity<List<String>> getClinics() {
    return ResponseEntity.ok(contentService.getClinics());
  }

  @GetMapping("/graph/clinic/{name}/sofaDistribution")
  @Operation(description = "Retrieves sofa distribution of a clinic")
  @PreAuthorize(Role.MANAGER)
  public ResponseEntity<Map<String, Integer>> getClinicDistributions(@PathVariable String name) {
    return ResponseEntity.ok(contentService.getClinicDistributions(name));
  }

  @GetMapping("/graph/clinic/sofaAverage")
  @PreAuthorize(Role.MANAGER)
  @Operation(description = "Retrieves the sofa averages of participating clinics")
  public ResponseEntity<Map<String, Double>> getClinicAverages() {
    return ResponseEntity.ok(contentService.getClinicAverages());
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

  @PostMapping("/navigation")
  @Operation(description = "Retrieves a list of navigation items")
  @PreAuthorize(Role.CONTENT_ADMIN)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "OK"),
    @ApiResponse(responseCode = "400", description = "Bad request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public ResponseEntity<String> setNavigationItems(
      @Valid @NotNull @RequestBody @Size(max = 5) List<NavigationItemDto> navigationItemDtos) {
    contentService.setNavigationItems(navigationItemDtos);
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
    return ResponseEntity.ok("Success");
  }
}
